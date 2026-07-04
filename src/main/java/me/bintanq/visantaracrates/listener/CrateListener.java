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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CrateListener implements Listener {

    private final VisantaraCrates plugin;

    public CrateListener(VisantaraCrates plugin, CrateManager crateManager) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
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
        }
    }
}