package me.bintanq.visantaracrates.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.model.CrateStoreConfig;
import me.bintanq.visantaracrates.model.reward.Reward;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Crate Store Preview GUI — uses triumph-gui PaginatedGui for reward pagination.
 * Opened via /vc preview <player> <crate>.
 */
public class CrateStoreGUI {

    private final VisantaraCrates plugin;

    public CrateStoreGUI(VisantaraCrates plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the crate store preview GUI for the given player and crate.
     */
    public void open(Player player, Crate crate) {
        CrateStoreConfig cfg = plugin.getPreviewManager().getCrateStoreConfig();
        if (cfg == null) {
            player.sendMessage(colorize("&cCrate store preview config not found."));
            return;
        }

        // Parse reward slots for pageSize calculation
        List<Integer> rewardSlotsList = PreviewGUI.parseSlots(cfg.getRewardSlots());
        int pageSize = rewardSlotsList.isEmpty() ? 15 : rewardSlotsList.size();

        // Resolve title — use titles map first, fallback to dynamic {crate} placeholder replacement
        String titleTemplate = cfg.getTitles().get(crate.getId());
        if (titleTemplate == null) {
            titleTemplate = cfg.getTitles().get(crate.getId().toLowerCase());
        }
        if (titleTemplate == null) {
            titleTemplate = cfg.getTitle();
        }
        String crateName = crate.getDisplayName() != null ? crate.getDisplayName() : crate.getId();
        String titleStr = titleTemplate.replace("{crate}", crateName).replace("%crate%", crateName);
        titleStr = parsePlaceholders(player, titleStr);

        // Resolve rows dynamically per crate ID
        int resolvedRows = cfg.getRows();
        Integer customRows = cfg.getCrateRows().get(crate.getId());
        if (customRows == null) {
            customRows = cfg.getCrateRows().get(crate.getId().toLowerCase());
        }
        if (customRows != null) {
            resolvedRows = customRows;
        }

        // Create paginated GUI
        PaginatedGui gui = Gui.paginated()
                .title(Component.text(titleStr))
                .rows(resolvedRows)
                .pageSize(pageSize)
                .disableAllInteractions()
                .create();

        // 1. Fill all slots with filler except the reward pagination slots
        GuiItem fillerItem = buildConfigItem(
                cfg.getFiller().getMaterial(),
                cfg.getFiller().getNexoId(),
                cfg.getFiller().getDisplayName(),
                null
        );
        for (int i = 0; i < resolvedRows * 9; i++) {
            if (!rewardSlotsList.contains(i)) {
                gui.setItem(i, fillerItem);
            }
        }

        // 2. Place store icon (dynamic physical crate item or overridden custom config, not paginated)
        CrateStoreConfig.StoreIconConfig iconCfg = cfg.getStoreIcons().get(crate.getId());
        if (iconCfg == null) {
            iconCfg = cfg.getStoreIcons().get(crate.getId().toLowerCase());
        }

        GuiItem storeIcon;
        if (iconCfg != null) {
            ItemStack customStack = resolveItemStack(iconCfg.getMaterial(), iconCfg.getNexoId());
            ItemMeta meta = customStack.getItemMeta();
            if (meta != null) {
                if (iconCfg.getCustomModelData() > 0) {
                    meta.setCustomModelData(iconCfg.getCustomModelData());
                }
                if (iconCfg.getDisplayName() != null && !iconCfg.getDisplayName().isEmpty()) {
                    meta.setDisplayName(colorize(iconCfg.getDisplayName()));
                }
                if (iconCfg.getLore() != null && !iconCfg.getLore().isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String l : iconCfg.getLore()) {
                        coloredLore.add(colorize(l));
                    }
                    meta.setLore(coloredLore);
                }
                customStack.setItemMeta(meta);
            }
            storeIcon = ItemBuilder.from(customStack).asGuiItem();
        } else {
            ItemStack storeIconStack = me.bintanq.visantaracrates.util.PhysicalCrateItem.create(plugin, crate, 1, crate.getCrateType());
            if (cfg.getStoreIcon() != null) {
                ItemMeta meta = storeIconStack.getItemMeta();
                if (meta != null) {
                    if (cfg.getStoreIcon().getDisplayName() != null && !cfg.getStoreIcon().getDisplayName().isEmpty() && !cfg.getStoreIcon().getDisplayName().equals("&aCrate Store")) {
                        meta.setDisplayName(colorize(cfg.getStoreIcon().getDisplayName()));
                    }
                    if (cfg.getStoreIcon().getLore() != null && !cfg.getStoreIcon().getLore().isEmpty()) {
                        List<String> coloredLore = new ArrayList<>();
                        for (String l : cfg.getStoreIcon().getLore()) {
                            coloredLore.add(colorize(l));
                        }
                        meta.setLore(coloredLore);
                    }
                    storeIconStack.setItemMeta(meta);
                }
            }
            storeIcon = ItemBuilder.from(storeIconStack).asGuiItem();
        }

        for (int slot : cfg.getStoreIcon().getSlots()) {
            if (slot >= 0 && slot < resolvedRows * 9) {
                gui.setItem(slot, storeIcon);
            }
        }

        // 3. Place arrow up (previous page)
        // Arrows will be dynamically placed via updateArrows() before open

        // Check if we should hide the action buttons for this crate
        boolean showButtons = true;
        for (String hiddenId : cfg.getHideButtonsFor()) {
            if (hiddenId.equalsIgnoreCase(crate.getId())) {
                showButtons = false;
                break;
            }
        }

        if (showButtons) {
            // 5. Place buy button
            for (int slot : cfg.getBuyButton().getSlots()) {
                if (slot >= 0 && slot < resolvedRows * 9) {
                    GuiItem buyItem = buildConfigItemWithAction(
                            cfg.getBuyButton().getMaterial(),
                            cfg.getBuyButton().getNexoId(),
                            cfg.getBuyButton().getDisplayName(),
                            cfg.getBuyButton().getLore(),
                            event -> {
                                Player p = (Player) event.getWhoClicked();
                                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                                executeStoreCommand(p, cfg.getBuyButton().getCommand());
                            }
                    );
                    gui.setItem(slot, buyItem);
                }
            }

            // 6. Place coin exchange button
            for (int slot : cfg.getExchangeButton().getSlots()) {
                if (slot >= 0 && slot < resolvedRows * 9) {
                    GuiItem exchangeItem = buildConfigItemWithAction(
                            cfg.getExchangeButton().getMaterial(),
                            cfg.getExchangeButton().getNexoId(),
                            cfg.getExchangeButton().getDisplayName(),
                            cfg.getExchangeButton().getLore(),
                            event -> {
                                Player p = (Player) event.getWhoClicked();
                                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                                executeStoreCommand(p, cfg.getExchangeButton().getCommand());
                            }
                    );
                    gui.setItem(slot, exchangeItem);
                }
            }
        }

        // 7. Add paginated reward items
        double totalWeight = crate.getTotalWeight();
        for (Reward reward : crate.getRewards()) {
            GuiItem rewardItem = buildRewardGuiItem(reward, totalWeight, cfg, crate);
            gui.addItem(rewardItem);
        }

        // Update dynamic arrow states
        updateArrows(gui, cfg, fillerItem, resolvedRows);

        // Open for the player
        gui.open(player);
    }

    /* ─────────────────────── Reward Item Builder ─────────────────────── */

    private GuiItem buildRewardGuiItem(Reward reward, double totalWeight, CrateStoreConfig cfg, Crate crate) {
        // Try to materialize the actual item
        ItemStack base = null;
        try {
            base = plugin.getRewardProcessor().materializeItem(reward);
        } catch (Exception ignored) {}
        if (base == null || base.getType().isAir()) {
            base = new ItemStack(Material.PAPER);
        }

        ItemStack display = base.clone();
        display.setAmount(Math.max(1, reward.getAmount()));

        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            // Display name
            me.bintanq.visantaracrates.model.RarityDefinition rarityDef = plugin.getRarityManager().get(reward.getRarity());
            String rawName = reward.getDisplayName() != null && !reward.getDisplayName().isEmpty()
                    ? stripColor(reward.getDisplayName())
                    : reward.getId();
            String tag = rarityDef.getIcon();
            String rarityColorCode = rarityDef.getColor();
            String displayName = colorize(tag + " " + rarityColorCode + rawName);
            meta.setDisplayName(displayName.startsWith("\u00A7") ? displayName : "\u00A7r" + displayName);

            // Lore
            List<String> templateLore = cfg.getReward().getLore();
            meta.setLore(formatRewardLore(templateLore, reward, totalWeight, crate));

            display.setItemMeta(meta);
        }

        return ItemBuilder.from(display).asGuiItem();
    }

    private List<String> formatRewardLore(List<String> templateLines, Reward reward, double totalWeight, Crate crate) {
        List<String> lore = new ArrayList<>();
        double pct = crate.getRewardChance(reward);
        String chanceStr = formatChance(pct);
        String rarityColor = plugin.getRarityManager().getColor(reward.getRarity());
        String rarityName = plugin.getRarityManager().get(reward.getRarity()).getDisplayName();

        for (String line : templateLines) {
            if (line.contains("%percentage%") || line.contains("%chance%")) {
                continue;
            }
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
                        .replace("%rarity%", rarityColor + rarityName)
                        .replace("%weight%", String.format("%.2f", reward.getWeight()))
                        .replace("%amount%", String.valueOf(reward.getAmount())));
            }
        }
        return lore;
    }

    /* ─────────────────────── Item Builders ─────────────────────── */

    private GuiItem buildConfigItem(String materialStr, String nexoId, String displayName, List<String> lore) {
        ItemStack stack = resolveItemStack(materialStr, nexoId);
        applyMeta(stack, displayName, lore);
        return ItemBuilder.from(stack).asGuiItem();
    }

    private GuiItem buildConfigItemWithAction(String materialStr, String nexoId, String displayName,
                                               List<String> lore,
                                               dev.triumphteam.gui.components.GuiAction<org.bukkit.event.inventory.InventoryClickEvent> action) {
        ItemStack stack = resolveItemStack(materialStr, nexoId);
        applyMeta(stack, displayName, lore);
        return ItemBuilder.from(stack).asGuiItem(action);
    }

    private ItemStack resolveItemStack(String materialStr, String nexoId) {
        // Try Nexo first
        if (nexoId != null && !nexoId.isEmpty() && plugin.isNexoEnabled()) {
            var nexoHook = plugin.getHookManager().getNexoHook();
            if (nexoHook != null) {
                ItemStack nexoItem = nexoHook.buildItemById(nexoId);
                if (nexoItem != null) return nexoItem;
            }
        }

        // Fallback to vanilla material
        Material mat = Material.matchMaterial(materialStr != null ? materialStr.toUpperCase() : "STONE");
        if (mat == null) mat = Material.STONE;
        return new ItemStack(mat);
    }

    private void applyMeta(ItemStack stack, String displayName, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(colorize(displayName));
        }
        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) {
                coloredLore.add(colorize(l));
            }
            meta.setLore(coloredLore);
        }
        stack.setItemMeta(meta);
    }

    /* ─────────────────────── Helpers ─────────────────────── */

    private String formatChance(double pct) {
        if (pct == 0) return "0%";
        if (pct < 0.0001) return "< 0.0001%";
        if (pct < 0.01) return String.format("%.4f%%", pct);
        return String.format("%.2f%%", pct);
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        return colorize(text);
    }

    private void updateArrows(PaginatedGui gui, CrateStoreConfig cfg, GuiItem fillerItem, int resolvedRows) {
        int currentPage = gui.getCurrentPageNum();
        int totalPages = gui.getPagesNum();

        // 1. Resolve Arrow Up
        if (cfg.getArrowUp().getSlot() >= 0 && cfg.getArrowUp().getSlot() < resolvedRows * 9) {
            if (currentPage > 1) {
                // Show arrow up
                GuiItem arrowUp = buildConfigItemWithAction(
                        cfg.getArrowUp().getMaterial(),
                        cfg.getArrowUp().getNexoId(),
                        cfg.getArrowUp().getDisplayName(),
                        null,
                        event -> {
                            Player p = (Player) event.getWhoClicked();
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                            if (gui.previous()) {
                                Bukkit.getScheduler().runTask(plugin, () -> updateArrows(gui, cfg, fillerItem, resolvedRows));
                            }
                        }
                );
                gui.setItem(cfg.getArrowUp().getSlot(), arrowUp);
            } else {
                // Hide arrow up (use filler)
                gui.setItem(cfg.getArrowUp().getSlot(), fillerItem);
            }
        }

        // 2. Resolve Arrow Down
        if (cfg.getArrowDown().getSlot() >= 0 && cfg.getArrowDown().getSlot() < resolvedRows * 9) {
            if (currentPage < totalPages) {
                // Show arrow down
                GuiItem arrowDown = buildConfigItemWithAction(
                        cfg.getArrowDown().getMaterial(),
                        cfg.getArrowDown().getNexoId(),
                        cfg.getArrowDown().getDisplayName(),
                        null,
                        event -> {
                            Player p = (Player) event.getWhoClicked();
                            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                            if (gui.next()) {
                                Bukkit.getScheduler().runTask(plugin, () -> updateArrows(gui, cfg, fillerItem, resolvedRows));
                            }
                        }
                );
                gui.setItem(cfg.getArrowDown().getSlot(), arrowDown);
            } else {
                // Hide arrow down (use filler)
                gui.setItem(cfg.getArrowDown().getSlot(), fillerItem);
            }
        }

        gui.update();
    }

    private String colorize(String s) {
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

    private String stripColor(String s) {
        if (s == null) return "";
        return org.bukkit.ChatColor.stripColor(colorize(s));
    }

    private void executeStoreCommand(Player p, String rawCmd) {
        if (rawCmd == null || rawCmd.isEmpty()) return;
        String cmd = rawCmd.replace("%player%", p.getName()).replace("{player}", p.getName());
        if (cmd.startsWith("player:")) {
            p.performCommand(cmd.substring(7).trim());
        } else if (cmd.startsWith("console:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring(8).trim());
        } else if (cmd.startsWith("/")) {
            p.performCommand(cmd.substring(1).trim());
        } else {
            p.performCommand(cmd);
        }
    }
}
