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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildersImpl implements Builders {

    private Builder creator;
    private final Map<UUID, Builder> buildersByUuid;

    public BuildersImpl(@Nullable Builder creator, List<@NotNull Builder> builders) {
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
    @NotNull
    public Collection<Builder> getAllBuilders() {
        return Collections.unmodifiableCollection(buildersByUuid.values());
    }

    @Override
    @Nullable
    public Builder getBuilder(UUID uuid) {
        return buildersByUuid.get(uuid);
    }

    @Override
    public List<String> getBuilderNames() {
        return getAllBuilders().stream()
                .map(Builder::getName)
                .collect(Collectors.toList());
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
    public void addBuilder(@NotNull Builder builder) {
        buildersByUuid.put(builder.getUniqueId(), builder);
    }

    @Override
    public void removeBuilder(@NotNull Builder builder) {
        removeBuilder(builder.getUniqueId());
    }

    @Override
    public void removeBuilder(@NotNull UUID uuid) {
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
        String template = Messages.getString("world_item_builders_builder_template", player);
        List<String> builderLines = new ArrayList<>();

        if (buildersByUuid.isEmpty()) {
            builderLines.add(template.replace("%builder%", "-").trim());
            return builderLines;
        }

        StringBuilder line = new StringBuilder();
        int count = 0;

        for (Builder builder : buildersByUuid.values()) {
            line.append(template.replace("%builder%", builder.getName()));
            count++;

            if (count == 3) {
                builderLines.add(line.toString().trim());
                line.setLength(0);
                count = 0;
            }
        }

        if (line.length() > 0) {
            builderLines.add(line.toString().trim());
        }

        return builderLines;
    }
} 