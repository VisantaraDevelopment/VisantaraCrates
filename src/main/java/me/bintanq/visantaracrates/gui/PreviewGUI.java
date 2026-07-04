package me.bintanq.visantaracrates.gui;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.model.PreviewConfig;
import me.bintanq.visantaracrates.model.MenuItem;
import me.bintanq.visantaracrates.model.reward.Reward;
import me.bintanq.visantaracrates.processor.RewardProcessor;
import me.bintanq.visantaracrates.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PreviewGUI {

    private final VisantaraCrates plugin;
    private final RewardProcessor rewardProcessor;

    public PreviewGUI(VisantaraCrates plugin, RewardProcessor rewardProcessor) {
        this.plugin = plugin;
        this.rewardProcessor = rewardProcessor;
    }

    /* ─────────────────────── Inventory Holder ─────────────────────── */

    public static class PreviewHolder implements InventoryHolder {
        private final Crate crate;
        private final int page;
        private final PreviewConfig config;

        public PreviewHolder(Crate crate, int page, PreviewConfig config) {
            this.crate = crate;
            this.page = page;
            this.config = config;
        }

        @Override
        public Inventory getInventory() { return null; }

        public Crate getCrate() { return crate; }
        public int getPage() { return page; }
        public PreviewConfig getConfig() { return config; }
    }

    /* ─────────────────────── Open ─────────────────────── */

    public void open(Player player, Crate crate) {
        open(player, crate, 0);
    }

    public void open(Player player, Crate crate, int page) {
        PreviewConfig cfg = null;
        if (crate.getPreviewId() != null && plugin.getPreviewManager() != null) {
            cfg = plugin.getPreviewManager().getPreviewConfig(crate.getPreviewId());
        }
        if (cfg == null) {
            cfg = crate.getPreview();
        }
        if (cfg == null) {
            cfg = new PreviewConfig();
        }

        List<Reward> sorted = sortRewards(crate.getRewards(), cfg.getSortOrder());
        double totalWeight = crate.getTotalWeight();

        // 1. Resolve pagination slots
        List<Integer> pSlots = parseSlots(cfg.getPagination().getSlots());
        if (pSlots.isEmpty()) {
            // Fallback slots if empty
            for (int row = 1; row <= 4; row++)
                for (int col = 1; col <= 7; col++)
                    pSlots.add(row * 9 + col);
        }

        int rewardPerPage = pSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / rewardPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String crateName = colorize(crate.getDisplayName() != null ? crate.getDisplayName() : crate.getId());
        String titleFormat = cfg.getSettings().getTitle();
        if (titleFormat == null || titleFormat.isEmpty()) {
            titleFormat = "&0&lPreview &8» &e{crate} &7[Page {page}/{pages}]";
        }
        String title = colorize(titleFormat)
                .replace("{crate}", crateName)
                .replace("{page}", String.valueOf(page + 1))
                .replace("{pages}", String.valueOf(totalPages));

        if (title.length() > 32) title = title.substring(0, 32);

        int rows = cfg.getSettings().getRows();
        Inventory inv = Bukkit.createInventory(new PreviewHolder(crate, page, cfg), rows * 9, title);

        // 2. Resolve legacy border glass filling if configured
        if (cfg.getBorderMaterial() != null && !cfg.getBorderMaterial().isEmpty()) {
            ItemStack borderFiller = resolveBorderItemStack(cfg, crate);
            fillBorder(inv, borderFiller);
        }

        // 3. Populate custom menu items
        if (cfg.getItems() != null) {
            for (MenuItem item : cfg.getItems().values()) {
                ItemStack stack = buildMenuItem(item, page, totalPages);
                List<Integer> slots = parseSlots(item.getSlot());
                for (int s : slots) {
                    if (s >= 0 && s < inv.getSize()) {
                        inv.setItem(s, stack);
                    }
                }
            }
        }

        // 4. Populate rewards in pagination slots
        int start = page * rewardPerPage;
        int end = Math.min(start + rewardPerPage, sorted.size());
        for (int i = start; i < end; i++) {
            int slot = pSlots.get(i - start);
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, buildRewardItem(sorted.get(i), totalWeight, cfg, crate));
            }
        }

        // 5. Fill remaining pagination slots with fillItem
        if (cfg.getPagination().getFillItem() != null) {
            ItemStack fillStack = buildMenuItem(cfg.getPagination().getFillItem(), page, totalPages);
            for (int i = (end - start); i < rewardPerPage; i++) {
                int slot = pSlots.get(i);
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, fillStack);
                }
            }
        }

        // 6. Populate previous/next buttons if pages > 1
        if (page > 0 && cfg.getPagination().getPreviousItem() != null) {
            MenuItem prevItem = cfg.getPagination().getPreviousItem();
            ItemStack prevStack = buildMenuItem(prevItem, page, totalPages);
            List<Integer> slots = parseSlots(prevItem.getSlot());
            for (int s : slots) {
                if (s >= 0 && s < inv.getSize()) inv.setItem(s, prevStack);
            }
        }

        if (page < totalPages - 1 && cfg.getPagination().getNextItem() != null) {
            MenuItem nextItem = cfg.getPagination().getNextItem();
            ItemStack nextStack = buildMenuItem(nextItem, page, totalPages);
            List<Integer> slots = parseSlots(nextItem.getSlot());
            for (int s : slots) {
                if (s >= 0 && s < inv.getSize()) inv.setItem(s, nextStack);
            }
        }

        player.openInventory(inv);
    }

    /* ─────────────────────── Reward Item Materializer ─────────────────────── */

    private ItemStack buildRewardItem(Reward reward, double totalWeight, PreviewConfig cfg, Crate crate) {
        ItemStack base = null;
        if (cfg.isShowActualItem()) {
            try { base = rewardProcessor.materializeItem(reward); } catch (Exception ignored) {}
        }
        if (base == null || base.getType().isAir()) base = new ItemStack(Material.PAPER);

        ItemStack display = base.clone();
        display.setAmount(Math.max(1, reward.getAmount()));

        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String rarityColor = plugin.getRarityManager().getColor(reward.getRarity());

        // 1. Resolve Display Name
        String nameTemplate = cfg.getReward().getRandomMode().getName();
        String rewardName = reward.getDisplayName() != null && !reward.getDisplayName().isEmpty()
                ? colorize(reward.getDisplayName())
                : rarityColor + reward.getId();
        
        String displayName = colorize(nameTemplate).replace("%reward_name%", rewardName);
        meta.setDisplayName(displayName.startsWith("\u00A7") ? displayName : "\u00A7r" + displayName);

        // 2. Resolve Lore Template
        List<String> templateLore = cfg.getReward().getRandomMode().getLore();
        meta.setLore(formatRewardLore(templateLore, reward, totalWeight, crate));

        display.setItemMeta(meta);
        return display;
    }

    private List<String> formatRewardLore(List<String> templateLines, Reward reward, double totalWeight, Crate crate) {
        List<String> lore = new ArrayList<>();
        double pct = reward.calculatePercentage(totalWeight);
        String chanceStr = formatChance(pct);
        String rarityColor = plugin.getRarityManager().getColor(reward.getRarity());
        String rarityName = plugin.getRarityManager().get(reward.getRarity()).getDisplayName();

        StringBuilder keysBuilder = new StringBuilder();
        if (crate.getRequiredKeys() != null) {
            for (Crate.KeyRequirement req : crate.getRequiredKeys()) {
                if (keysBuilder.length() > 0) keysBuilder.append(", ");
                keysBuilder.append(req.getAmount()).append("x ").append(req.getKeyId());
            }
        }
        String keysStr = keysBuilder.toString();

        for (String line : templateLines) {
            if (line.contains("%reward_lore%")) {
                if (reward.getLore() != null) {
                    for (String rl : reward.getLore()) {
                        lore.add(colorize(rl));
                    }
                }
            } else {
                lore.add(colorize(line)
                        .replace("%percentage%", chanceStr)
                        .replace("%chance%", chanceStr)
                        .replace("%required_keys%", keysStr)
                        .replace("%rarity%", rarityColor + rarityName)
                        .replace("%weight%", String.format("%.2f", reward.getWeight()))
                        .replace("%amount%", String.valueOf(reward.getAmount())));
            }
        }
        return lore;
    }

    /* ─────────────────────── Menu Item Builder ─────────────────────── */

    private ItemStack buildMenuItem(MenuItem item, int page, int totalPages) {
        ItemStack stack = null;
        var nexoHook = plugin.getHookManager().getNexoHook();

        if (nexoHook != null && nexoHook.isEnabled() && item.getItemModel() != null && !item.getItemModel().isEmpty()) {
            stack = nexoHook.buildItemById(item.getItemModel());
        }

        if (stack == null) {
            Material mat = parseMaterial(item.getMaterial(), Material.STONE);
            stack = new ItemStack(mat, Math.max(1, item.getAmount()));
        } else {
            stack.setAmount(Math.max(1, item.getAmount()));
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (item.getDisplayName() != null && !item.getDisplayName().isEmpty()) {
                meta.setDisplayName(colorize(item.getDisplayName())
                        .replace("%page%", String.valueOf(page + 1))
                        .replace("%pages%", String.valueOf(totalPages)));
            }
            if (item.getLore() != null && !item.getLore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String l : item.getLore()) {
                    coloredLore.add(colorize(l)
                            .replace("%page%", String.valueOf(page + 1))
                            .replace("%pages%", String.valueOf(totalPages)));
                }
                meta.setLore(coloredLore);
            }
            if (item.isGlow()) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /* ─────────────────────── Legacy Border Glass Resolver ─────────────────────── */

    private ItemStack resolveBorderItemStack(PreviewConfig cfg, Crate crate) {
        String borderStr = cfg.getBorderMaterial();
        var nexoHook = plugin.getHookManager().getNexoHook();
        if (nexoHook != null && nexoHook.isEnabled() && borderStr != null && !borderStr.isEmpty()) {
            ItemStack nexo = nexoHook.buildItemById(borderStr);
            if (nexo != null) {
                return makeFiller(nexo);
            }
        }

        Material m = null;
        if (borderStr != null && !borderStr.isEmpty()) {
            m = parseMaterial(borderStr, null);
        }
        if (m == null) {
            String highestRarity = crate.getRewards().stream()
                    .map(Reward::getRarity)
                    .max(Comparator.comparingInt(r -> plugin.getRarityManager().getOrder(r)))
                    .orElse(plugin.getRarityManager().getLowestId());
            m = plugin.getRarityManager().getBorderMaterial(highestRarity);
        }
        return makeFiller(new ItemStack(m != null ? m : Material.GRAY_STAINED_GLASS_PANE));
    }

    private void fillBorder(Inventory inv, ItemStack filler) {
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, filler.clone());
            }
        }
    }

    /* ─────────────────────── Sorting ─────────────────────── */

    private List<Reward> sortRewards(List<Reward> rewards, PreviewConfig.SortOrder order) {
        List<Reward> sorted = new ArrayList<>(rewards);
        switch (order) {
            case RARITY_DESC -> sorted.sort(Comparator
                    .comparingInt((Reward r) -> plugin.getRarityManager().getOrder(r.getRarity()))
                    .reversed()
                    .thenComparingDouble(Reward::getWeight).reversed());
            case RARITY_ASC  -> sorted.sort(Comparator
                    .comparingInt((Reward r) -> plugin.getRarityManager().getOrder(r.getRarity()))
                    .thenComparingDouble(Reward::getWeight));
            case WEIGHT_DESC -> sorted.sort(Comparator.comparingDouble(Reward::getWeight).reversed());
            case WEIGHT_ASC  -> sorted.sort(Comparator.comparingDouble(Reward::getWeight));
            case CONFIG_ORDER -> { /* unsorted */ }
        }
        return sorted;
    }

    /* ─────────────────────── Item Factories & Helpers ─────────────────────── */

    private ItemStack makeFiller(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName("\u00A7r"); item.setItemMeta(meta); }
        return item;
    }

    private String formatChance(double pct) {
        if (pct == 0)     return "0%";
        if (pct < 0.0001) return "< 0.0001%";
        if (pct < 0.01)   return String.format("%.4f%%", pct);
        return                   String.format("%.2f%%", pct);
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isEmpty()) return fallback;
        Material m = Material.matchMaterial(name.toUpperCase());
        return m != null ? m : fallback;
    }

    private String colorize(String s) {
        return s == null ? "" : s.replace("&", "\u00A7");
    }

    /* ─────────────────────── Slot Range Parser ─────────────────────── */

    public static List<Integer> parseSlots(String slotStr) {
        List<Integer> slots = new ArrayList<>();
        if (slotStr == null || slotStr.isEmpty()) return slots;
        String[] parts = slotStr.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                            slots.add(i);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    slots.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {}
            }
        }
        return slots;
    }

    public static int parseFirstSlot(String slotStr) {
        List<Integer> slots = parseSlots(slotStr);
        return slots.isEmpty() ? -1 : slots.get(0);
    }

    public static boolean isSlotMatch(int slot, String slotStr) {
        return parseSlots(slotStr).contains(slot);
    }
}