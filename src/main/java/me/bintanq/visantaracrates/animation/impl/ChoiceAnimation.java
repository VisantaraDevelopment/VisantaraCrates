package me.bintanq.visantaracrates.animation.impl;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.animation.AnimationUtil;
import me.bintanq.visantaracrates.animation.CrateAnimation;
import me.bintanq.visantaracrates.animation.CrateSession;
import me.bintanq.visantaracrates.model.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChoiceAnimation implements CrateAnimation {

    private final VisantaraCrates plugin;

    private static final int SLOT_1 = 11;
    private static final int SLOT_2 = 13;
    private static final int SLOT_3 = 15;
    private static final int INV_SIZE = 27;
    private static final int TOTAL_SPINS = 15;

    public ChoiceAnimation(VisantaraCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(CrateSession session) {
        List<Reward> pool = session.getCrate().getRewards();
        Reward preRolled = session.getResult().getReward();

        String titleFormat = plugin.getConfig().getString("animations.choice.title", "&0&lChoose Reward: {crate}");
        String crateName = colorize(session.getCrate().getDisplayName() != null
                ? session.getCrate().getDisplayName() : session.getCrate().getId());
        String finalTitle = colorize(titleFormat.replace("{crate}", crateName));
        if (finalTitle.length() > 32) finalTitle = finalTitle.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, INV_SIZE, finalTitle);
        session.setInventory(inv);

        // Build custom filler item
        ItemStack filler = null;
        String fillerNexo = plugin.getConfig().getString("animations.choice.filler.nexo-item", "");
        if (plugin.isNexoEnabled() && !fillerNexo.isBlank()) {
            var nexoHook = plugin.getHookManager().getNexoHook();
            if (nexoHook != null) {
                filler = nexoHook.buildItemById(fillerNexo);
            }
        }
        if (filler == null) {
            String fillerMatStr = plugin.getConfig().getString("animations.choice.filler.material", "GRAY_STAINED_GLASS_PANE");
            Material mat = Material.matchMaterial(fillerMatStr.toUpperCase());
            if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;
            filler = new ItemStack(mat);
            int cmd = plugin.getConfig().getInt("animations.choice.filler.custom-model-data", -1);
            if (cmd > 0) {
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(cmd);
                    filler.setItemMeta(meta);
                }
            }
        }
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.setDisplayName("\u00A7r");
            filler.setItemMeta(fMeta);
        }

        // Fill all slots except 11, 13, 15
        for (int i = 0; i < INV_SIZE; i++) {
            if (i != SLOT_1 && i != SLOT_2 && i != SLOT_3) {
                inv.setItem(i, filler.clone());
            }
        }

        session.getPlayer().openInventory(inv);

        // Spin animation task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!session.isRunning()) return;

            if (session.getSpinCount() < TOTAL_SPINS) {
                // Spin phase: randomize items in slots 11, 13, 15
                inv.setItem(SLOT_1, AnimationUtil.buildDisplayItem(AnimationUtil.randomReward(pool), plugin.getHookManager()));
                inv.setItem(SLOT_2, AnimationUtil.buildDisplayItem(AnimationUtil.randomReward(pool), plugin.getHookManager()));
                inv.setItem(SLOT_3, AnimationUtil.buildDisplayItem(AnimationUtil.randomReward(pool), plugin.getHookManager()));

                double progress = (double) session.getSpinCount() / TOTAL_SPINS;
                AnimationUtil.playTickSound(session.getPlayer(), progress, session.getCrate().getOpenSound());

                session.advanceSpin();
            } else {
                finish(session, preRolled, pool, inv);
            }
        }, 0L, 3L); // slower spin speed for choice: 3 ticks per spin

        session.addTask(task);
    }

    private void finish(CrateSession session, Reward winner, List<Reward> pool, Inventory inv) {
        if (!session.isRunning() || session.isForfeited()) return;
        session.cancelAllTasks(); // stop spinning

        // Roll choice 1 and choice 3 (other than the winner)
        Reward c1 = rollAlternative(pool, winner);
        Reward c2 = winner;
        Reward c3 = rollAlternative(pool, winner, c1);

        // Save in session metadata
        session.getMetadata().put("choice_1", c1);
        session.getMetadata().put("choice_2", c2);
        session.getMetadata().put("choice_3", c3);
        session.getMetadata().put("choice_clickable", true);

        // Render choice items with "Click to choose" lore
        inv.setItem(SLOT_1, buildChoiceItem(c1, 1));
        inv.setItem(SLOT_2, buildChoiceItem(c2, 2));
        inv.setItem(SLOT_3, buildChoiceItem(c3, 3));

        // Visual cues: put green panes above and below the choices
        inv.setItem(SLOT_1 - 9, AnimationUtil.filler(Material.LIME_STAINED_GLASS_PANE));
        inv.setItem(SLOT_1 + 9, AnimationUtil.filler(Material.LIME_STAINED_GLASS_PANE));
        inv.setItem(SLOT_2 - 9, AnimationUtil.filler(Material.LIME_STAINED_GLASS_PANE));
        inv.setItem(SLOT_2 + 9, AnimationUtil.filler(Material.LIME_STAINED_GLASS_PANE));
        inv.setItem(SLOT_3 - 9, AnimationUtil.filler(Material.LIME_STAINED_GLASS_PANE));
        inv.setItem(SLOT_3 + 9, AnimationUtil.filler(Material.LIME_STAINED_GLASS_PANE));

        AnimationUtil.playWinSound(session.getPlayer(), session.getCrate().getWinSound());

        // Schedule timeout (15 seconds / 300 ticks) to auto-claim and close
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!session.isRunning() || session.isForfeited()) return;
            session.getPlayer().closeInventory();
            if (plugin.getAnimationManager().completeSession(session)) {
                plugin.getCrateManager().deliverRewardPublic(session.getPlayer(), session.getResult());
            }
        }, 300L);

        session.addTask(timeout);
    }

    private Reward rollAlternative(List<Reward> pool, Reward... excludes) {
        List<Reward> copy = new ArrayList<>(pool);
        for (Reward ex : excludes) {
            copy.remove(ex);
        }
        if (copy.isEmpty()) {
            return AnimationUtil.randomReward(pool);
        }
        return AnimationUtil.randomReward(copy);
    }

    private ItemStack buildChoiceItem(Reward reward, int index) {
        ItemStack item = AnimationUtil.buildDisplayItem(reward, plugin.getHookManager());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add("");
            lore.add(colorize("&a&l&k!&a&l CLICK TO CHOOSE &k!"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void cancel(CrateSession session) {
        session.cancelAllTasks();
    }

    @Override
    public boolean isRunning(CrateSession session) {
        return session.isRunning();
    }

    private String colorize(String s) {
        return s == null ? "" : s.replace("&", "\u00A7");
    }
}
