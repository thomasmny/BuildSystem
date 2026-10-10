/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.world.backup;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.WorldArchive;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

/**
 * Puts a backup in place of a world folder, so that a failure at any step leaves the old world there. Every move is an
 * atomic rename on one file system; nothing is copied.
 *
 * <p>Normally the backup is extracted next to the world folder and the two folders swap places. A world folder that is
 * a mount point or a symbolic link cannot be renamed like that, so for those the backup is extracted inside the world
 * folder, the world's own entries are moved aside and the backup's entries moved in.
 *
 * <p>The old world sits in a {@code replaced} folder until the backup is in place. That name is fixed, so a restore
 * that went wrong blocks the next one and can be undone at startup.
 */
@NullMarked
public final class WorldRestore {

    /**
     * Holds the folders of a restore. It holds no world data itself, so it is never listed as a world to import and is
     * left out of backups.
     */
    public static final String FOLDER = ".buildsystem-restore";

    /**
     * Created inside an in-place restore's folder once the world's own entries are all moved aside. While it exists,
     * every entry in the world folder came from the backup.
     */
    private static final String MOVING_IN = "moving-in";

    private final Path world;
    private final String id = UUID.randomUUID().toString();
    private boolean inPlace;

    public WorldRestore(File worldFolder) {
        this.world = worldFolder.toPath();
    }

    private static Path folder(Path world, boolean inPlace) {
        return inPlace ? world.resolve(FOLDER) : world.resolveSibling(FOLDER);
    }

    private static Path inFolder(Path world, boolean inPlace, String name) {
        return folder(world, inPlace).resolve(inPlace ? name : world.getFileName() + "." + name);
    }

    Path staged() {
        return inFolder(world, inPlace, id + ".staged");
    }

    Path replaced() {
        return inFolder(world, inPlace, "replaced");
    }

    private Path trash() {
        return inFolder(world, inPlace, id + ".trash");
    }

    /**
     * Extracts the backup while the world is still loaded. A broken archive, or an earlier restore that still needs
     * checking, fails here, before anything happens to the world.
     */
    public void stage(File archive) throws IOException {
        boolean renamable = !Files.isSymbolicLink(world)
                && Files.getFileStore(world)
                        .equals(Files.getFileStore(world.toAbsolutePath().getParent()));
        stage(archive, !renamable);
    }

    void stage(File archive, boolean inPlace) throws IOException {
        for (boolean mode : new boolean[] {false, true}) {
            Path replaced = inFolder(world, mode, "replaced");
            if (Files.exists(replaced)) {
                throw new IOException(
                        "Aborting restore: " + replaced + " is left from an earlier restore, check it first");
            }
        }
        this.inPlace = inPlace;
        WorldArchive.validate(archive, staged().toFile());
        Files.createDirectories(staged());
        WorldArchive.extract(archive, staged().toFile());
    }

    /**
     * Puts the extracted backup in place of the world. If any step fails, the old world is put back before the failure
     * is thrown; check {@link #worldIsBack()} to see whether that worked.
     */
    public void swap() throws IOException {
        try {
            if (inPlace) {
                moveEntries(world, replaced());
                Files.createFile(folder(world, true).resolve(MOVING_IN));
                moveEntries(staged(), world);
                Files.move(replaced(), trash(), ATOMIC_MOVE);
                Files.delete(folder(world, true).resolve(MOVING_IN));
            } else {
                Files.move(world, replaced(), ATOMIC_MOVE);
                Files.move(staged(), world, ATOMIC_MOVE);
                Files.move(replaced(), trash(), ATOMIC_MOVE);
            }
        } catch (IOException e) {
            try {
                rollBack(world, inPlace);
            } catch (IOException rollback) {
                e.addSuppressed(rollback);
            }
            throw e;
        }
    }

    /**
     * {@return whether the world folder holds a whole world, either the old one or the backup} False only when a
     * failed swap could not be undone, and the old world is still in {@link #replaced()}.
     */
    public boolean worldIsBack() {
        return Files.isDirectory(world) && !Files.exists(replaced());
    }

    /**
     * Deletes the extracted backup if it was not used, and the old world once the backup is in place. Only touches
     * this restore's own folders, so it can run after the world is free again.
     */
    public void cleanUp() throws IOException {
        for (Path leftover : List.of(staged(), trash())) {
            if (Files.exists(leftover)) {
                FileUtils.deleteDirectory(leftover);
            }
        }
        // Only succeeds once no other restore uses the folder.
        folder(world, inPlace).toFile().delete();
    }

    /**
     * Undoes a restore that a server stop interrupted, and deletes what it left behind. Runs at startup, before any
     * world is loaded.
     */
    public static void recover(File worldFolder, Logger logger) {
        Path world = worldFolder.toPath();
        try {
            if (Files.isDirectory(folder(world, true))) {
                if (rollBack(world, true)) {
                    logger.warning("Put " + world + " back the way it was before an interrupted restore");
                }
                FileUtils.deleteDirectory(folder(world, true));
            }

            Path replaced = inFolder(world, false, "replaced");
            if (rollBack(world, false)) {
                logger.warning("Moved " + world + " back from " + replaced + ", a restore was interrupted");
            } else if (Files.exists(replaced)) {
                logger.warning(replaced + " is left from an earlier restore. Check it and delete it, until then"
                        + " backups of this world cannot be restored");
            }

            Path folder = folder(world, false);
            if (Files.isDirectory(folder)) {
                String prefix = world.getFileName() + ".";
                List<Path> leftovers;
                try (Stream<Path> entries = Files.list(folder)) {
                    leftovers = entries.filter(entry -> {
                                String name = entry.getFileName().toString();
                                return name.startsWith(prefix) && (name.endsWith(".staged") || name.endsWith(".trash"));
                            })
                            .toList();
                }
                for (Path leftover : leftovers) {
                    FileUtils.deleteDirectory(leftover);
                }
                folder.toFile().delete();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not undo an interrupted restore of " + world, e);
        }
    }

    /**
     * Puts the old world back from {@code replaced}.
     *
     * @return Whether there was anything to put back
     */
    private static boolean rollBack(Path world, boolean inPlace) throws IOException {
        Path replaced = inFolder(world, inPlace, "replaced");
        if (!Files.exists(replaced)) {
            return false;
        }
        if (!inPlace) {
            if (Files.exists(world)) {
                return false;
            }
            Files.move(replaced, world, ATOMIC_MOVE);
            return true;
        }

        Path movingIn = folder(world, true).resolve(MOVING_IN);
        if (Files.exists(movingIn)) {
            for (Path entry : entries(world)) {
                FileUtils.deleteDirectory(entry);
            }
            Files.delete(movingIn);
        }
        moveEntries(replaced, world);
        Files.delete(replaced);
        return true;
    }

    private static void moveEntries(Path from, Path to) throws IOException {
        Files.createDirectories(to);
        for (Path entry : entries(from)) {
            Files.move(entry, to.resolve(entry.getFileName()), ATOMIC_MOVE);
        }
    }

    private static List<Path> entries(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.filter(entry -> !entry.getFileName().toString().equals(FOLDER))
                    .toList();
        }
    }
}
