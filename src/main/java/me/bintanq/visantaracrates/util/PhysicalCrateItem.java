package me.bintanq.visantaracrates.util;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.model.Crate;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class PhysicalCrateItem {

    public static final String PDC_CRATE_ID = "visantaracrates_crate_id";
    public static final String PDC_CRATE_TYPE = "visantaracrates_crate_type";

    private static NamespacedKey idKey;
    private static NamespacedKey typeKey;

    public static void init(VisantaraCrates plugin) {
        idKey = new NamespacedKey(plugin, PDC_CRATE_ID);
        typeKey = new NamespacedKey(plugin, PDC_CRATE_TYPE);
    }

    private PhysicalCrateItem() {}

    public static ItemStack create(VisantaraCrates plugin, Crate crate, int amount, String type) {
        String crateId = crate.getId();
        String upperType = type.toUpperCase();
        boolean isPremium = upperType.equals("PREMIUM");
        Crate.PhysicalItemConfig cfg = crate.getPhysicalItem(isPremium);

        ItemStack item = null;
        if (plugin.isNexoEnabled() && !cfg.getNexoId().isEmpty()) {
            var nexoHook = plugin.getHookManager().getNexoHook();
            if (nexoHook != null) {
                item = nexoHook.buildItemById(cfg.getNexoId());
            }
        }

        if (item == null) {
            String matStr = cfg.getMaterial();
            Material material = Material.matchMaterial(matStr.toUpperCase());
            if (material == null) material = Material.CHEST;
            item = new ItemStack(material);
        }

        item.setAmount(Math.max(1, Math.min(amount, 64)));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Custom Model Data
        int cmd = cfg.getCustomModelData();
        if (cmd > 0) meta.setCustomModelData(cmd);

        String rawDisplayName = cfg.getDisplayName()
                .replace("{crate}", crate.getDisplayName() != null ? crate.getDisplayName() : crateId)
                .replace("{type}", upperType);
        meta.setDisplayName(colorize(rawDisplayName));

        List<String> lore = new ArrayList<>();
        if (cfg.getLore() != null && !cfg.getLore().isEmpty()) {
            for (String line : cfg.getLore()) {
                lore.add(colorize(line.replace("{type}", upperType)));
            }
        } else {
            // Default fallback lore
            String idLore = plugin.getConfig().getString("crates.physical.id-lore", "&7Crate ID: &e{crate}")
                    .replace("{crate}", crateId);
            String typeLore = plugin.getConfig().getString("crates.physical.type-lore", "&7Type: &b{type}")
                    .replace("{type}", upperType);
            lore.add(colorize(idLore));
            lore.add(colorize(typeLore));
            List<String> extraLore = plugin.getConfig().getStringList("crates.physical.extra-lore");
            if (extraLore != null) {
                for (String line : extraLore) {
                    lore.add(colorize(line));
                }
            }
        }
        meta.setLore(lore);

        // Initialize static keys if not done
        if (idKey == null || typeKey == null) {
            idKey = new NamespacedKey(plugin, PDC_CRATE_ID);
            typeKey = new NamespacedKey(plugin, PDC_CRATE_TYPE);
        }

        // Clean up stacking blockers (unique instance/UUID/timestamp tags added by custom item plugins)
        for (NamespacedKey key : new ArrayList<>(meta.getPersistentDataContainer().getKeys())) {
            String keyName = key.getKey().toLowerCase();
            String ns = key.getNamespace().toLowerCase();
            
            // Keep VisantaraCrates keys
            if (ns.equals("visantaracrates")) {
                continue;
            }
            
            // Keep custom item identifying ID keys, remove other custom keys to ensure stacking
            if (ns.equals("nexo") || ns.equals("itemsadder") || ns.equals("oraxen") || ns.equals("mmoitems")) {
                if (keyName.equals("id") || keyName.endsWith("_id")) {
                    continue;
                }
                meta.getPersistentDataContainer().remove(key);
                continue;
            }
            
            // For other namespaces, remove if they look like unique/instance keys
            if (keyName.contains("uuid") || keyName.contains("instance") || keyName.contains("unique") 
                || keyName.contains("serial") || keyName.contains("timestamp") || keyName.contains("random")
                || keyName.contains("created") || keyName.contains("hash") || keyName.contains("time")
                || keyName.contains("updated")) {
                meta.getPersistentDataContainer().remove(key);
            }
        }

        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, crateId);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, upperType);

        item.setItemMeta(meta);
        return item;
    }

    public static String getCrateId(VisantaraCrates plugin, ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        if (meta == null) return null;
        if (idKey == null) idKey = new NamespacedKey(plugin, PDC_CRATE_ID);
        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public static String getCrateType(VisantaraCrates plugin, ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        if (meta == null) return null;
        if (typeKey == null) typeKey = new NamespacedKey(plugin, PDC_CRATE_TYPE);
        return meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }

    public static boolean isCrate(VisantaraCrates plugin, ItemStack item) {
        return getCrateId(plugin, item) != null;
    }

    private static String colorize(String s) {
        if (s == null) return "";
        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([a-fA-F0-9]{6})");
        java.util.regex.Matcher matcher = hexPattern.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder builder = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                builder.append("§").append(c);
            }
            matcher.appendReplacement(sb, builder.toString());
        }
        matcher.appendTail(sb);
        return sb.toString().replace("&", "§");
    }
}
