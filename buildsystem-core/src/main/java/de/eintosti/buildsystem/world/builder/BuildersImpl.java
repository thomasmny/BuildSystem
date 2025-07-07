package de.eintosti.buildsystem.world.builder;

import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BuildersImpl implements Builders {

    @Nullable
    private Builder creator;
    private final Map<UUID, Builder> buildersByUuid;

    public BuildersImpl(@Nullable Builder creator, List<Builder> builders) {
        this.creator = creator;
        this.buildersByUuid = builders.stream().collect(Collectors.toMap(Builder::getUniqueId, Function.identity()));
    }

    @Override
    @Nullable
    public Builder getCreator() {
        return creator;
    }

    @Override
    public void setCreator(@Nullable Builder creator) {
        this.creator = creator;
    }

    @Override
    public boolean hasCreator() {
        return creator != null && !creator.getName().equals("-");
    }

    @Override
    public boolean isCreator(Player player) {
        return hasCreator() && player.getUniqueId().equals(creator.getUniqueId());
    }

    @Override
    public Collection<Builder> getAllBuilders() {
        return Collections.unmodifiableCollection(buildersByUuid.values());
    }

    @Override
    @Nullable
    public Builder getBuilder(UUID uuid) {
        return buildersByUuid.get(uuid);
    }

    @Override
    @Unmodifiable
    public List<String> getBuilderNames() {
        return getAllBuilders().stream()
                .map(Builder::getName)
                .toList();
    }

    @Override
    public boolean isBuilder(Player player) {
        return isBuilder(player.getUniqueId());
    }

    @Override
    public boolean isBuilder(UUID uuid) {
        return buildersByUuid.containsKey(uuid);
    }

    @Override
    public void addBuilder(Builder builder) {
        buildersByUuid.put(builder.getUniqueId(), builder);
    }

    @Override
    public void removeBuilder(Builder builder) {
        removeBuilder(builder.getUniqueId());
    }

    @Override
    public void removeBuilder(UUID uuid) {
        buildersByUuid.remove(uuid);
    }

    @Override
    public String asPlaceholder(Player player) {
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

    /**
     * Formats the builders for the lore of the world item.
     *
     * @param player The player to display the lore to
     * @return A list of formatted builder lines, each containing up to 3 builders
     */
    public List<String> formatBuildersForLore(Player player) {
        final int buildersPerLine = 3;
        String template = Messages.getString("world_item_builders_builder_template", player); // e.g., "&b%builder%&7, "

        String[] templateParts = template.split("%builder%");
        String prefix = templateParts.length > 0 ? templateParts[0] : "";
        String suffix = templateParts.length > 1 ? templateParts[1] : "";

        List<String> loreLines = new ArrayList<>();
        Collection<Builder> allBuilders = buildersByUuid.values();

        if (allBuilders.isEmpty()) {
            loreLines.add((prefix + "-").trim());
            return loreLines;
        }

        List<String> currentLineBuilders = new ArrayList<>();
        int index = 0;

        for (Builder builder : allBuilders) {
            boolean isLastInLine = (index + 1) % buildersPerLine == 0 || (index + 1) == allBuilders.size();
            String formattedBuilder = prefix + builder.getName() + (isLastInLine ? "" : suffix);
            currentLineBuilders.add(formattedBuilder);

            if (isLastInLine) {
                loreLines.add(String.join("", currentLineBuilders).trim());
                currentLineBuilders.clear();
            }

            index++;
        }

        return loreLines;
    }
} 