package me.bintanq.visantaracrates.util;

import me.bintanq.visantaracrates.VisantaraCrates;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class PhysicalKeyItem {

    private static final String PDC_KEY = "qc_key_id";

    private PhysicalKeyItem() {}

    public static ItemStack create(
            VisantaraCrates plugin,
            String keyId,
            int amount,
            String displayName,
            List<String> lore,
            Material material,
            int customModelData
    ) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(amount, 64)));
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(colorize(displayName));

        List<String> coloredLore = new ArrayList<>();
        if (lore != null) lore.forEach(l -> coloredLore.add(colorize(l)));
        meta.setLore(coloredLore);

        if (customModelData > 0) meta.setCustomModelData(customModelData);

        NamespacedKey nsKey = new NamespacedKey(plugin, PDC_KEY);
        meta.getPersistentDataContainer().set(nsKey, PersistentDataType.STRING, keyId);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(VisantaraCrates plugin, String keyId, int amount) {
        String displayName = plugin.getConfig().getString(
                        "keys.physical.display-name", "&bCrate Key &8[&7{key}&8]")
                .replace("{key}", keyId);
        List<String> lore = new ArrayList<>();
        String idLine = plugin.getConfig().getString(
                "keys.physical.id-lore", "&7Key ID: &e{key}").replace("{key}", keyId);
        lore.add(idLine);
        lore.addAll(plugin.getConfig().getStringList("keys.physical.extra-lore"));
        return create(plugin, keyId, amount, displayName, lore, Material.TRIPWIRE_HOOK, -1);
    }

    public static String getKeyId(VisantaraCrates plugin, ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey nsKey = new NamespacedKey(plugin, PDC_KEY);
        return meta.getPersistentDataContainer().get(nsKey, PersistentDataType.STRING);
    }

    public static boolean isKey(VisantaraCrates plugin, ItemStack item, String keyId) {
        return keyId != null && keyId.equals(getKeyId(plugin, item));
    }

    private static String colorize(String s) {
        return s == null ? "" : s.replace("&", "\u00A7");
    }
}