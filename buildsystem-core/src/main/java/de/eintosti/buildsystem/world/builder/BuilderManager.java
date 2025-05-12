package de.eintosti.buildsystem.world.builder;

import de.eintosti.buildsystem.Messages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages builders for a BuildWorld. This class handles all operations related to adding, removing, and managing builders.
 */
public class BuilderManager {

    private final List<Builder> builders;
    private Builder creator;

    public BuilderManager() {
        this.builders = new ArrayList<>();
    }

    public BuilderManager(@Nullable Builder creator) {
        this();
        this.creator = creator;
    }

    /**
     * Gets the creator of the world.
     *
     * @return The creator, if any, {@code null} otherwise
     */
    @Nullable
    public Builder getCreator() {
        return creator;
    }

    /**
     * Sets the creator of the world.
     *
     * @param creator The new creator
     */
    public void setCreator(@Nullable Builder creator) {
        this.creator = creator;
    }

    /**
     * Checks if the world has a creator.
     *
     * @return {@code true} if the world has a creator, {@code false} otherwise
     */
    public boolean hasCreator() {
        return creator != null;
    }

    /**
     * Checks if the given player is the creator of the world.
     *
     * @param player The player to check
     * @return {@code true} if the player is the creator, {@code false} otherwise
     */
    public boolean isCreator(Player player) {
        return hasCreator() && player.getUniqueId().equals(creator.getUniqueId());
    }

    /**
     * Gets an unmodifiable list of all builders.
     *
     * @return List of builders
     */
    @NotNull
    public List<Builder> getBuilders() {
        return Collections.unmodifiableList(builders);
    }

    /**
     * Gets a builder by their UUID.
     *
     * @param uuid The UUID to search for
     * @return The builder if found, {@code null} otherwise
     */
    @Nullable
    public Builder getBuilder(UUID uuid) {
        return builders.stream()
                .filter(builder -> builder.getUniqueId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a list of all {@link Builder} names
     *
     * @return A list of all builder names
     */
    public List<String> getBuilderNames() {
        return getBuilders().stream()
                .map(Builder::getName)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a UUID belongs to a builder.
     *
     * @param uuid The UUID to check
     * @return {@code true} if the given UUID belongs to a builder, {@code false} otherwise
     */
    public boolean isBuilder(UUID uuid) {
        return getBuilder(uuid) != null;
    }

    /**
     * Adds a builder to the world.
     *
     * @param builder The builder to add
     */
    public void addBuilder(@NotNull Builder builder) {
        builders.add(builder);
    }

    /**
     * Removes a builder from the world.
     *
     * @param builder The builder to remove
     */
    public void removeBuilder(@NotNull Builder builder) {
        builders.remove(builder);
    }

    /**
     * Removes a builder by their UUID.
     *
     * @param uuid The UUID of the builder to remove
     */
    public void removeBuilder(@NotNull UUID uuid) {
        Builder builder = getBuilder(uuid);
        if (builder != null) {
            removeBuilder(builder);
        }
    }

    /**
     * Serializes the builder list to a string format.
     *
     * @return The serialized builders string
     */
    public String serializeBuilders() {
        StringBuilder builderList = new StringBuilder();
        for (Builder builder : builders) {
            builderList.append(";").append(builder.toString());
        }
        return builderList.length() > 0 ? builderList.substring(1) : builderList.toString();
    }

    /**
     * Format the {@code %builder%} placeholder.
     *
     * @param player The player to parse the placeholders against
     * @return The list of builders which have been added to the given world as a string
     */
    public String getBuildersInfo(Player player) {
        String template = Messages.getString("world_item_builders_builder_template", player);
        List<String> builderNames = getBuilderNames();

        String string = "";
        if (builderNames.isEmpty()) {
            string = template.replace("%builder%", "-").trim();
        } else {
            for (String builderName : builderNames) {
                string = string.concat(template.replace("%builder%", builderName));
            }
            string = string.trim();
        }

        return string.substring(0, string.length() - 1);
    }
} 