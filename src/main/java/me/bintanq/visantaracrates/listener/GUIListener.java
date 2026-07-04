package me.bintanq.visantaracrates.listener;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.animation.CrateSession;
import me.bintanq.visantaracrates.gui.PreviewGUI;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.model.PreviewConfig;
import me.bintanq.visantaracrates.model.MenuItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.List;

public class GUIListener implements Listener {

    private final VisantaraCrates plugin;

    public GUIListener(VisantaraCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (event.getInventory().getHolder() instanceof me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder holder) {
            event.setCancelled(true);
            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            int clickedSlot = event.getRawSlot();
            Crate crate = holder.getCrate();
            boolean isPremium = holder.isPremium();

            if (holder.getState() == me.bintanq.visantaracrates.gui.CrateGUI.GUIState.CLOSED) {
                if (crate.getSlots().getOpenSlots(isPremium).contains(clickedSlot)) {
                    playSound(player, crate.getSoundOnClick());
                    holder.triggerOpen();
                }
            } else if (holder.getState() == me.bintanq.visantaracrates.gui.CrateGUI.GUIState.SELECTION) {
                List<Integer> rewardSlots = holder.getActiveRewardSlots();
                if (rewardSlots.contains(clickedSlot)) {
                    holder.toggleSelection(clickedSlot);
                } else if (crate.getSlots().getClaimSlots(isPremium).contains(clickedSlot)) {
                    playSound(player, crate.getSoundOnClick());
                    holder.triggerClaim();
                } else if (isPremium && crate.getSlots().getRerollSlots(isPremium).contains(clickedSlot)) {
                    playSound(player, crate.getSoundOnClick());
                    holder.triggerReroll();
                }
            }
            return;
        }

        CrateSession session = plugin.getAnimationManager().getSession(player.getUniqueId());

        if (session != null) {
            event.setCancelled(true);
            if (session.getCrate().getGuiAnimation() == me.bintanq.visantaracrates.model.Crate.GuiAnimationType.CHOICE) {
                Boolean clickable = (Boolean) session.getMetadata().getOrDefault("choice_clickable", false);
                if (Boolean.TRUE.equals(clickable)) {
                    int slot = event.getRawSlot();
                    me.bintanq.visantaracrates.model.reward.Reward chosen = null;
                    if (slot == 11) {
                        chosen = (me.bintanq.visantaracrates.model.reward.Reward) session.getMetadata().get("choice_1");
                    } else if (slot == 13) {
                        chosen = (me.bintanq.visantaracrates.model.reward.Reward) session.getMetadata().get("choice_2");
                    } else if (slot == 15) {
                        chosen = (me.bintanq.visantaracrates.model.reward.Reward) session.getMetadata().get("choice_3");
                    }

                    if (chosen != null) {
                        session.getMetadata().put("choice_clickable", false);
                        org.bukkit.inventory.ItemStack chosenItem = chosen.isCommandOnly() ? null
                                : plugin.getRewardProcessor().materializeItem(chosen);
                        me.bintanq.visantaracrates.model.reward.RewardResult newResult =
                                new me.bintanq.visantaracrates.model.reward.RewardResult(
                                        chosen,
                                        chosenItem,
                                        chosen.getCommands(),
                                        session.getResult().isPityGuaranteed(),
                                        session.getResult().getPityAtRoll()
                                );
                        session.setResult(newResult);

                        player.closeInventory();
                        if (plugin.getAnimationManager().completeSession(session)) {
                            plugin.getCrateManager().deliverRewardPublic(player, session.getResult());
                        }
                    }
                }
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof PreviewGUI.PreviewHolder holder) {
            event.setCancelled(true);

            if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            int clickedSlot = event.getRawSlot();
            PreviewConfig config = holder.getConfig();
            Crate crate = holder.getCrate();
            int currentPage = holder.getPage();

            // 1. Check previous page button
            if (config.getPagination().getPreviousItem() != null) {
                MenuItem prevItem = config.getPagination().getPreviousItem();
                if (PreviewGUI.isSlotMatch(clickedSlot, prevItem.getSlot())) {
                    executeActions(player, prevItem.getActions());
                    if (currentPage > 0) {
                        new PreviewGUI(plugin, plugin.getRewardProcessor()).open(player, crate, currentPage - 1);
                    }
                    return;
                }
            }

            // 2. Check next page button
            if (config.getPagination().getNextItem() != null) {
                MenuItem nextItem = config.getPagination().getNextItem();
                if (PreviewGUI.isSlotMatch(clickedSlot, nextItem.getSlot())) {
                    executeActions(player, nextItem.getActions());
                    List<me.bintanq.visantaracrates.model.reward.Reward> sorted = crate.getRewards();
                    List<Integer> pSlots = PreviewGUI.parseSlots(config.getPagination().getSlots());
                    if (pSlots.isEmpty()) {
                        for (int r = 1; r <= 4; r++)
                            for (int c = 1; c <= 7; c++)
                                pSlots.add(r * 9 + c);
                    }
                    int rewardPerPage = pSlots.size();
                    int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / rewardPerPage));
                    if (currentPage < totalPages - 1) {
                        new PreviewGUI(plugin, plugin.getRewardProcessor()).open(player, crate, currentPage + 1);
                    }
                    return;
                }
            }

            // 3. Check custom menu items
            if (config.getItems() != null) {
                for (MenuItem item : config.getItems().values()) {
                    if (PreviewGUI.isSlotMatch(clickedSlot, item.getSlot())) {
                        executeActions(player, item.getActions());
                        return;
                    }
                }
            }
        }
    }

    private void executeActions(Player player, List<String> actions) {
        if (actions == null) return;
        for (String action : actions) {
            String trimmed = action.trim();
            if (trimmed.equalsIgnoreCase("[CLOSE_INVENTORY]")) {
                player.closeInventory();
            } else if (trimmed.toUpperCase().startsWith("[SOUND]")) {
                try {
                    String data = trimmed.substring("[SOUND]".length()).trim();
                    String[] parts = data.split(";");
                    if (parts.length >= 1) {
                        String soundName = parts[0].trim();
                        float volume = parts.length >= 2 ? Float.parseFloat(parts[1].trim()) : 1.0f;
                        float pitch = parts.length >= 3 ? Float.parseFloat(parts[2].trim()) : 1.0f;
                        org.bukkit.Sound sound = null;
                        try {
                            sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
                        } catch (IllegalArgumentException ignored) {}
                        if (sound != null) {
                            player.playSound(player.getLocation(), sound, volume, pitch);
                        } else {
                            player.playSound(player.getLocation(), soundName, volume, pitch);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        if (plugin.getAnimationManager().hasSession(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getInventory().getHolder() instanceof PreviewGUI.PreviewHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        CrateSession session = plugin.getAnimationManager().getSession(player.getUniqueId());
        if (session == null)
            return;

        // If it's a physical crate GUI session
        me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder holder =
                (me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder) session.getMetadata().get("gui_holder");

        if (holder != null) {
            if (holder.isTransitioning()) {
                return; // Ignore close during transition reopens
            }

            if (holder.getState() == me.bintanq.visantaracrates.gui.CrateGUI.GUIState.CLOSED) {
                holder.refundCrate();
                plugin.getAnimationManager().completeSession(session);
            } else if (holder.getState() == me.bintanq.visantaracrates.gui.CrateGUI.GUIState.SPINNING
                    || holder.getState() == me.bintanq.visantaracrates.gui.CrateGUI.GUIState.SELECTION
                    || holder.getState() == me.bintanq.visantaracrates.gui.CrateGUI.GUIState.TRANSITION) {
                if (player.isOnline()) {
                    // Prevent close by reopening on next tick
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            holder.setTransitioning(true);
                            player.openInventory(holder.getInventory());
                            holder.setTransitioning(false);
                        }
                    });
                } else {
                    holder.autoClaim();
                }
            }
            return;
        }

        // Standard animation close logic
        if (session.isRunning()) {
            session.setForfeited(true);
            session.setRunning(false);
            session.cancelAllTasks();
        }

        if (plugin.getAnimationManager().completeSession(session)) {
            plugin.getCrateManager().deliverRewardPublic(player, session.getResult());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CrateSession session = plugin.getAnimationManager().getSession(player.getUniqueId());
        if (session != null) {
            me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder holder =
                    (me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder) session.getMetadata().get("gui_holder");
            if (holder != null) {
                holder.cancelAllTasks();
                holder.autoClaim();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;
        CrateSession session = plugin.getAnimationManager().getSession(player.getUniqueId());
        if (session == null || !session.isRunning())
            return;

        if (session.getInventory() != null
                && event.getInventory() != session.getInventory()) {
            event.setCancelled(true);
        }
    }

    private void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), soundName.toLowerCase(), 1.0f, 1.0f);
        }
    }
}