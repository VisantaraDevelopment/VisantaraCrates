package me.bintanq.visantaracrates.animation;

import me.bintanq.visantaracrates.model.reward.Reward;
import me.bintanq.visantaracrates.hook.HookManager;
import me.bintanq.visantaracrates.model.reward.Reward.RewardType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AnimationUtil {

    private AnimationUtil() {}

    public static final Material FILLER_MAT = Material.WHITE_STAINED_GLASS_PANE;


    public static ItemStack buildDisplayItem(Reward reward, HookManager hookManager) {
        if (reward.getType() == RewardType.CRATE) {
            me.bintanq.visantaracrates.VisantaraCrates plugin = me.bintanq.visantaracrates.VisantaraCrates.getInstance();
            if (plugin != null) {
                var targetCrate = plugin.getCrateManager().getCrate(reward.getCrateId());
                if (targetCrate != null) {
                    return me.bintanq.visantaracrates.util.PhysicalCrateItem.create(plugin, targetCrate, Math.max(1, reward.getAmount()), "FREE");
                }
            }
        }

        if (hookManager != null && reward.getNexoId() != null) {
            String nexoId = reward.getNexoId().toLowerCase();
            me.bintanq.visantaracrates.VisantaraCrates plugin = hookManager.getPlugin();
            if (plugin != null) {
                String targetCrateId = null;
                String clean = nexoId
                        .replace("_crate_key", "")
                        .replace("_key", "")
                        .replace("crate", "")
                        .replace("key", "")
                        .replace("_", "");
                if (clean.equals("vote")) {
                    clean = "vip";
                }

                for (String cId : plugin.getCrateManager().getCrateRegistry().keySet()) {
                    String cleanCrate = cId.toLowerCase().replace("crate", "").replace("_", "");
                    if (cleanCrate.equals(clean)) {
                        targetCrateId = cId;
                        break;
                    }
                }

                if (targetCrateId != null) {
                    var targetCrate = plugin.getCrateManager().getCrate(targetCrateId);
                    if (targetCrate != null) {
                        return me.bintanq.visantaracrates.util.PhysicalCrateItem.create(plugin, targetCrate, Math.max(1, reward.getAmount()), "FREE");
                    }
                }
            }
        }

        ItemStack base = null;

        if (hookManager != null) {
            if (reward.getNexoId() != null && !reward.getNexoId().isEmpty()) {
                var h = hookManager.getNexoHook();
                if (h != null && h.isEnabled()) {
                    base = h.buildItem(reward);
                }
            }
            if (base == null) {
                base = switch (reward.getType()) {
                    case MMOITEMS -> {
                        var h = hookManager.getMmoItemsHook();
                        yield (h != null && h.isEnabled()) ? h.buildItem(reward) : null;
                    }
                    case ITEMSADDER -> {
                        var h = hookManager.getItemsAdderHook();
                        yield (h != null && h.isEnabled()) ? h.buildItem(reward) : null;
                    }
                    case ORAXEN -> {
                        var h = hookManager.getOraxenHook();
                        yield (h != null && h.isEnabled()) ? h.buildItem(reward) : null;
                    }
                    case NEXO -> {
                        var h = hookManager.getNexoHook();
                        yield (h != null && h.isEnabled()) ? h.buildItem(reward) : null;
                    }
                    default -> null;
                };
            }
        }

        if (base == null || base.getType().isAir()) {
            Material mat = reward.getMaterial() != null
                    ? Material.matchMaterial(reward.getMaterial().toUpperCase())
                    : null;
            if (mat == null || mat.isAir()) mat = Material.PAPER;
            base = new ItemStack(mat);
        } else {
            base = base.clone();
        }

        base.setAmount(Math.max(1, reward.getAmount()));

        if (reward.getDisplayName() != null && !reward.getDisplayName().isEmpty()) {
            ItemMeta meta = base.getItemMeta();
            if (meta != null) {
                String displayName = reward.getDisplayName();
                displayName = displayName.replace("{amount}", String.valueOf(reward.getAmount()));
                if (reward.getAmount() > 1 && !displayName.contains(String.valueOf(reward.getAmount()))) {
                    displayName = displayName + " &7(x" + reward.getAmount() + ")";
                }
                meta.setDisplayName(fixItalic(displayName));
                if (reward.getLore() != null && !reward.getLore().isEmpty()) {
                    meta.setLore(reward.getLore().stream()
                            .map(AnimationUtil::fixItalic)
                            .toList());
                }
                base.setItemMeta(meta);
            }
        }
        return base;
    }

    public static ItemStack buildDisplayItem(Reward reward) {
        return buildDisplayItem(reward, null);
    }

    public static ItemStack filler() {
        return filler(FILLER_MAT);
    }

    public static ItemStack filler(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName("\u00A7r"); item.setItemMeta(meta); }
        return item;
    }

    public static void fillAll(Inventory inv) {
        fillAll(inv, FILLER_MAT);
    }

    public static void fillAll(Inventory inv, Material mat) {
        ItemStack f = filler(mat);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, f);
    }

    public static void playTickSound(Player player, double progress, String soundName) {
        float pitch = (float) (0.5 + progress * 1.5);
        pitch = Math.max(0.5f, Math.min(2.0f, pitch));
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 0.6f, pitch);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, pitch);
        }
    }

    public static void playWinSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public static Reward randomReward(List<Reward> rewards) {
        return rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
    }

    private static String fixItalic(String s) {
        if (s == null) return "";
        String colored = s.replace("&", "\u00A7");
        if (colored.startsWith("\u00A7")) return colored;
        return "\u00A7r" + colored;
    }
}