package me.bintanq.visantaracrates.hook.impl;

import com.nexomc.nexo.api.NexoItems;
import me.bintanq.visantaracrates.hook.ItemHook;
import me.bintanq.visantaracrates.model.reward.Reward;
import me.bintanq.visantaracrates.util.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * NexoHook — integration with Nexo for custom item rewards and keys.
 */
public class NexoHook implements ItemHook {

    private boolean enabled = false;

    public NexoHook() {
        try {
            Class.forName("com.nexomc.nexo.api.NexoItems");
            enabled = true;
        } catch (ClassNotFoundException e) {
            enabled = false;
        }
    }

    @Override
    public String getPluginName() { return "Nexo"; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public ItemStack buildItem(Reward reward) {
        if (!enabled) return null;
        ItemStack item = buildItemById(reward.getNexoId());
        if (item != null) {
            item.setAmount(Math.max(1, reward.getAmount()));
        }
        return item;
    }

    /**
     * Builds a Nexo item by its ID directly.
     */
    public ItemStack buildItemById(String nexoId) {
        if (!enabled || nexoId == null || nexoId.isBlank()) return null;
        try {
            com.nexomc.nexo.items.ItemBuilder builder = NexoItems.itemFromId(nexoId);
            
            // Fallback 1: strip "nexo:" prefix
            if (builder == null && nexoId.toLowerCase().startsWith("nexo:")) {
                builder = NexoItems.itemFromId(nexoId.substring(5));
            }
            
            // Fallback 2: prepend "nexo:" prefix
            if (builder == null && !nexoId.contains(":")) {
                builder = NexoItems.itemFromId("nexo:" + nexoId);
            }

            if (builder == null) {
                Logger.debug("Nexo: Unknown item '" + nexoId + "'");
                return null;
            }
            return builder.build();
        } catch (Exception e) {
            Logger.warn("Nexo buildItemById failed for '" + nexoId + "': " + e.getMessage());
            return null;
        }
    }

    @Override
    public int countKey(Player player, String keyId) {
        if (!enabled) return 0;
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            try {
                String nexoId = NexoItems.idFromItem(item);
                if (keyId.equalsIgnoreCase(nexoId)) count += item.getAmount();
            } catch (Exception ignored) {}
        }
        return count;
    }

    @Override
    public boolean removeKey(Player player, String keyId, int amount) {
        if (!enabled) return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            try {
                String nexoId = NexoItems.idFromItem(item);
                if (!keyId.equalsIgnoreCase(nexoId)) continue;
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            } catch (Exception ignored) {}
        }
        return remaining <= 0;
    }
}
