package me.bintanq.visantaracrates.listener;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.gui.PreviewGUI;
import me.bintanq.visantaracrates.util.KnockbackUtil;
import me.bintanq.visantaracrates.util.MessageManager;
import me.bintanq.visantaracrates.manager.CrateManager;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.util.PhysicalCrateItem;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CrateListener implements Listener {

    private final VisantaraCrates plugin;
    private final CrateManager  crateManager;
    private final PreviewGUI    previewGUI;

    public CrateListener(VisantaraCrates plugin, CrateManager crateManager) {
        this.plugin       = plugin;
        this.crateManager = crateManager;
        this.previewGUI   = new PreviewGUI(plugin, plugin.getRewardProcessor());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (PhysicalCrateItem.isCrate(plugin, item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (PhysicalCrateItem.isCrate(plugin, item)) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                String crateId = PhysicalCrateItem.getCrateId(plugin, item);

                Crate crate = plugin.getCrateManager().getCrate(crateId);
                if (crate == null) {
                    player.sendMessage("\u00A7cCrate configuration not found for: " + crateId);
                    return;
                }

                if (plugin.getAnimationManager().hasSession(player.getUniqueId())) {
                    MessageManager.send(player, "already-opening");
                    return;
                }

                String type = PhysicalCrateItem.getCrateType(plugin, item);
                if (type == null || type.isEmpty()) {
                    type = crate.getCrateType();
                }

                // Consume 1 physical crate item
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setItemInMainHand(item);

                // Start GUI
                me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder gui =
                        new me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder(plugin, player, crate, type, false);
                gui.openClosedGUI();
            }
            return;
        }

        Block block = event.getClickedBlock();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (block == null) return;
            Crate crate = getCrateAtBlock(block);
            if (crate == null) return;
            event.setCancelled(true);
            handlePreview(event.getPlayer(), crate);
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (block == null) return;
            Crate crate = getCrateAtBlock(block);
            if (crate == null) return;
            event.setCancelled(true);

            if (player.isSneaking()) {
                handleMassOpen(player, crate, block.getLocation());
            } else {
                handleOpen(player, crate, block.getLocation());
            }
        }
    }

    private void handleOpen(Player player, Crate crate, Location crateBlockLoc) {
        me.bintanq.visantaracrates.manager.CrateManager.OpenResult result =
                plugin.getCrateManager().canOpen(player, crate.getId());

        if (result == me.bintanq.visantaracrates.manager.CrateManager.OpenResult.MISSING_KEY
                || result == me.bintanq.visantaracrates.manager.CrateManager.OpenResult.LIFETIME_LIMIT_REACHED) {
            plugin.getCrateManager().sendOpenResultFeedbackPublic(player, result, crate.getId());
            KnockbackUtil.applyDenied(player, crate, crateBlockLoc);
            return;
        }

        // Apply cooldown
        if (crate.getCooldownMs() > 0) {
            plugin.getPlayerDataManager().setLastOpen(player.getUniqueId(), crate.getId());
        }

        // Start stateful GUI
        me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder gui =
                new me.bintanq.visantaracrates.gui.CrateGUI.CrateOpeningHolder(plugin, player, crate, crate.getCrateType(), true);
        gui.openClosedGUI();
    }

    private void handlePreview(Player player, Crate crate) {
        previewGUI.open(player, crate);
    }

    private void handleMassOpen(Player player, Crate crate, Location crateBlockLoc) {
        if (!player.hasPermission("VisantaraCrates.massopen")) {
            MessageManager.sendNoPermission(player);
            return;
        }
        if (!crate.isMassOpenEnabled()) {
            MessageManager.send(player, "mass-open-disabled", "{crate}", crate.getId());
            return;
        }
        if (!plugin.getKeyManager().hasRequiredKeys(player, crate)) {
            plugin.getCrateManager().sendOpenResultFeedbackPublic(player,
                    me.bintanq.visantaracrates.manager.CrateManager.OpenResult.MISSING_KEY, crate.getId());
            KnockbackUtil.applyDenied(player, crate, crateBlockLoc);
            return;
        }
        if (crate.getLifetimeOpenLimit() > 0
                && !player.hasPermission("VisantaraCrates.bypasslimit")) {
            int used = plugin.getPlayerDataManager().getLifetimeOpens(player.getUniqueId(), crate.getId());
            if (used >= crate.getLifetimeOpenLimit()) {
                plugin.getCrateManager().sendOpenResultFeedbackPublic(player,
                        me.bintanq.visantaracrates.manager.CrateManager.OpenResult.LIFETIME_LIMIT_REACHED, crate.getId());
                KnockbackUtil.applyDenied(player, crate, crateBlockLoc);
                return;
            }
        }
        crateManager.massOpen(player, crate.getId(), -1);
    }

    private Crate getCrateAtBlock(Block block) {
        return crateManager.getCrateAtLocation(
                block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ());
    }
}