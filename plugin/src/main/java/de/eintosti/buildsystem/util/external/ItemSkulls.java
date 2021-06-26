package de.eintosti.buildsystem.util.external;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

public class ItemSkulls {
    private static Class<?> skullMetaClass;

    static {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

        try {
            skullMetaClass = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftMetaSkull");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param skinURL The URL to the skin-image (full skin)
     * @return The ItemStack (SKULL_ITEM) with the given look (skin-image)
     */
    public static ItemStack getSkull(String skinURL) {
        return getSkull(skinURL, 1);
    }

    /**
     * @param skinURL The URL to the skin-image (full skin)
     * @param amount  The amount of skulls (for ItemStack)
     * @return The ItemStack (SKULL_ITEM) with the given look (skin-image)
     */
    public static ItemStack getSkull(String skinURL, int amount) {
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        skull.setAmount(amount);
        try {
            Field profileField = skullMetaClass.getDeclaredField("profile");
            profileField.setAccessible(true);
            if (!skinURL.startsWith("http://textures.minecraft.net/texture/")) {
                skinURL = "http://textures.minecraft.net/texture/" + skinURL;
            }
            profileField.set(meta, getProfile(skinURL));
        } catch (Exception e) {
            e.printStackTrace();
        }
        skull.setItemMeta(meta);
        return skull;
    }

    private static GameProfile getProfile(String skinURL) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        String base64encoded = Base64.getEncoder().encodeToString(("{textures:{SKIN:{url:\"" + skinURL + "\"}}}").getBytes());
        Property property = new Property("textures", base64encoded);
        profile.getProperties().put("textures", property);
        return profile;
    }
}
