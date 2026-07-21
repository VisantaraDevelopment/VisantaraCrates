package me.bintanq.visantaracrates.gui;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.animation.AnimationUtil;
import me.bintanq.visantaracrates.animation.CrateSession;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.model.reward.Reward;
import me.bintanq.visantaracrates.model.reward.RewardResult;
import me.bintanq.visantaracrates.util.MessageManager;
import me.bintanq.visantaracrates.util.PhysicalCrateItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CrateGUI {

    public enum GUIState {
        CLOSED,
        TRANSITION,
        SPINNING,
        SELECTION,
        CLAIMED
    }

    public static class CrateOpeningHolder implements InventoryHolder {
        private final VisantaraCrates plugin;
        private final Player player;
        private final Crate crate;
        private final String crateType; // FREE or PREMIUM
        private final String consumedKeyId; // null if physical item
        private final boolean isBlockClick;
        private final CrateSession dummySession; // for integration with AnimationManager

        private Inventory inventory;
        private String currentTitle = "";
        private GUIState state = GUIState.CLOSED;
        private int rerollsRemaining;
        private final List<Reward> rolledRewards = new ArrayList<>();
        private final Set<Integer> selectedIndices = new HashSet<>();
        private boolean isTransitioning = false;
        private final List<BukkitTask> tasks = new ArrayList<>();
        private final List<Integer> activeRewardSlots = new ArrayList<>();

        public CrateOpeningHolder(VisantaraCrates plugin, Player player, Crate crate, String crateType) {
            this(plugin, player, crate, crateType, false);
        }

        public CrateOpeningHolder(VisantaraCrates plugin, Player player, Crate crate, String crateType, boolean isBlockClick) {
            this.plugin = plugin;
            this.player = player;
            this.crate = crate;
            this.crateType = crateType.toUpperCase();
            this.isBlockClick = isBlockClick;
            this.consumedKeyId = null;
            this.rerollsRemaining = crate.getMaxRerolls();
            boolean isPremium = this.crateType.equals("PREMIUM");
            this.activeRewardSlots.addAll(crate.getSlots().getRewardSlots(isPremium));

            plugin.getLogger().info("[VisantaraCrates] Opening stateful GUI for " + player.getName() + " - Crate: " + crate.getId() + " - Resolved Type: " + this.crateType + " (isBlockClick: " + isBlockClick + ")");

            // Create a dummy session to register with AnimationManager so player is "opening a crate"
            RewardResult fallbackResult = new RewardResult(null, null, new ArrayList<>(), false, 0);
            this.dummySession = new CrateSession(player, crate, fallbackResult);
            this.dummySession.setRunning(true);
            this.dummySession.getMetadata().put("gui_holder", this);
        }

        public CrateOpeningHolder(VisantaraCrates plugin, Player player, Crate crate, String crateType, String consumedKeyId) {
            this.plugin = plugin;
            this.player = player;
            this.crate = crate;
            this.crateType = crateType.toUpperCase();
            this.isBlockClick = (consumedKeyId != null);
            this.consumedKeyId = consumedKeyId;
            this.rerollsRemaining = crate.getMaxRerolls();
            boolean isPremium = this.crateType.equals("PREMIUM");
            this.activeRewardSlots.addAll(crate.getSlots().getRewardSlots(isPremium));

            plugin.getLogger().info("[VisantaraCrates] Opening stateful GUI for " + player.getName() + " - Crate: " + crate.getId() + " - Resolved Type: " + this.crateType + " (consumedKeyId: " + consumedKeyId + ")");

            // Create a dummy session to register with AnimationManager so player is "opening a crate"
            RewardResult fallbackResult = new RewardResult(null, null, new ArrayList<>(), false, 0);
            this.dummySession = new CrateSession(player, crate, fallbackResult);
            this.dummySession.setRunning(true);
            this.dummySession.getMetadata().put("gui_holder", this);
            if (consumedKeyId != null) {
                this.dummySession.getMetadata().put("consumedKeyId", consumedKeyId);
            }
        }

        public Player getPlayer() { return player; }
        public boolean isPremium() { return crateType.equals("PREMIUM"); }
        public List<Integer> getActiveRewardSlots() { return activeRewardSlots; }
        public Crate getCrate() { return crate; }
        public String getCrateType() { return crateType; }
        public GUIState getState() { return state; }
        public void setTransitioning(boolean transitioning) { this.isTransitioning = transitioning; }
        public boolean isTransitioning() { return isTransitioning; }
        public CrateSession getDummySession() { return dummySession; }

        public void openClosedGUI() {
            this.state = GUIState.CLOSED;
            this.isTransitioning = true;
            String title = getActiveTitle();
            this.inventory = Bukkit.createInventory(this, 36, title);
            this.currentTitle = title;

            renderBackgroundAndOpenButton();

            dummySession.setInventory(this.inventory);
            player.openInventory(this.inventory);
            this.isTransitioning = false;

            // Register in AnimationManager
            plugin.getAnimationManager().registerSession(dummySession);
        }

        private void renderBackgroundAndOpenButton() {
            inventory.clear();
            ItemStack filler = buildButtonItem(plugin, crate.getButtons().getGuiFiller());
            for (int i = 0; i < 36; i++) {
                inventory.setItem(i, filler.clone());
            }
            // Overlay open-slot items with their own display name
            boolean isPremium = crateType.equals("PREMIUM");
            ItemStack openSlotItem = buildButtonItem(plugin, crate.getButtons().getOpenSlotItem());
            for (int slot : crate.getSlots().getOpenSlots(isPremium)) {
                if (slot >= 0 && slot < 36) inventory.setItem(slot, openSlotItem.clone());
            }
        }

        private void openTransitionGUI() {
            this.state = GUIState.TRANSITION;
            this.isTransitioning = true;
            String title = getActiveTitle();
            this.inventory = Bukkit.createInventory(this, 36, title);
            this.currentTitle = title;

            // Fill with filler
            ItemStack filler = buildButtonItem(plugin, crate.getButtons().getGuiFiller());
            for (int i = 0; i < 36; i++) {
                inventory.setItem(i, filler.clone());
            }

            dummySession.setInventory(this.inventory);
            player.openInventory(this.inventory);
            this.isTransitioning = false;
        }

        public void triggerOpen() {
            if (this.state != GUIState.CLOSED) return;

            if (isBlockClick) {
                String consumed = "physical";
                dummySession.getMetadata().put("consumedKeyId", consumed);
            }

            // Open transition GUI immediately instead of closing
            openTransitionGUI();

            // Play transition sound
            playSound(player, crate.getSoundOnTransition());

            // Schedule transition task
            BukkitTask waitTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    this.isTransitioning = false;
                    return;
                }
                startSpinning();
            }, crate.getTransitionTicks());
            tasks.add(waitTask);
        }

        private void startSpinning() {
            this.state = GUIState.SPINNING;
            this.isTransitioning = true;

            String title = getActiveTitle();
            this.inventory = Bukkit.createInventory(this, 36, title);
            this.currentTitle = title;

            // Initial filler rendering
            ItemStack filler = buildButtonItem(plugin, crate.getButtons().getGuiFiller());
            for (int i = 0; i < 36; i++) {
                inventory.setItem(i, filler.clone());
            }

            // Overlay slot-specific placeholder items
            boolean isPremiumType = crateType.equals("PREMIUM");
            ItemStack rewardPlaceholder = buildButtonItem(plugin, crate.getButtons().getRewardSlotItem());
            for (int slot : activeRewardSlots) {
                if (slot >= 0 && slot < 36) inventory.setItem(slot, rewardPlaceholder.clone());
            }
            ItemStack claimPlaceholder = buildButtonItem(plugin, crate.getButtons().getClaimSlotItem());
            for (int slot : crate.getSlots().getClaimSlots(isPremiumType)) {
                if (slot >= 0 && slot < 36) inventory.setItem(slot, claimPlaceholder.clone());
            }
            if (isPremiumType) {
                ItemStack rerollPlaceholder = buildButtonItem(plugin, crate.getButtons().getRerollSlotItem());
                for (int slot : crate.getSlots().getRerollSlots(true)) {
                    if (slot >= 0 && slot < 36) inventory.setItem(slot, rerollPlaceholder.clone());
                }
            }

            dummySession.setInventory(this.inventory);
            player.openInventory(this.inventory);
            this.isTransitioning = false;

            // Roll final rewards beforehand so we know what they are
            rollRewards();

            // Run spinning animation loop
            List<Reward> pool = crate.getRewards();
            List<Integer> rewardSlots = activeRewardSlots;
            final int totalSpins = 15;
            final int[] spinCount = {0};

            BukkitTask spinTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    cancelAllTasks();
                    return;
                }

                if (spinCount[0] < totalSpins) {
                    try {
                        VisantaraCrates.ACTIVE_CRATE_TYPE.set(this.crateType);
                        // Randomize items in reward slots
                        for (int slot : rewardSlots) {
                            Reward randReward = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
                            inventory.setItem(slot, AnimationUtil.buildDisplayItem(randReward, plugin.getHookManager()));
                        }
                    } finally {
                        VisantaraCrates.ACTIVE_CRATE_TYPE.remove();
                    }

                    // Play tick sound
                    double progress = (double) spinCount[0] / totalSpins;
                    AnimationUtil.playTickSound(player, progress, crate.getOpenSound());

                    spinCount[0]++;
                } else {
                    finishSpinning();
                }
            }, 0L, 3L);

            tasks.add(spinTask);
        }

        private void finishSpinning() {
            cancelAllTasks();
            this.state = GUIState.SELECTION;

            try {
                VisantaraCrates.ACTIVE_CRATE_TYPE.set(this.crateType);
                // Set final rolled reward items in slots
                List<Integer> rewardSlots = activeRewardSlots;
                for (int i = 0; i < rewardSlots.size(); i++) {
                    int slot = rewardSlots.get(i);
                    Reward reward = rolledRewards.get(i);
                    inventory.setItem(slot, AnimationUtil.buildDisplayItem(reward, plugin.getHookManager()));
                }
            } finally {
                VisantaraCrates.ACTIVE_CRATE_TYPE.remove();
            }

            // Render buttons for selection phase
            updateActionButtons();

            // Reopen GUI with the new dynamic title if changed
            updateTitleAndInventory();

            AnimationUtil.playWinSound(player, crate.getWinSound());
        }

        private void rollRewards() {
            rolledRewards.clear();
            selectedIndices.clear();

            // 1. Primary roll (respects pity)
            var data = plugin.getPlayerDataManager().getOrEmpty(player.getUniqueId());
            RewardResult primaryResult = plugin.getRewardProcessor().roll(crate, data);
            rolledRewards.add(primaryResult.getReward());

            // Handle pity increment/reset (temporarily bypassed for physical crates)
            /*
            if (plugin.isPityEnabled() && crate.getPity().isEnabled()) {
                boolean isRare = plugin.getRarityManager()
                        .isAtOrAbove(primaryResult.getReward().getRarity(), crate.getPity().getRareRarityMinimum());

                if (isRare || primaryResult.isPityGuaranteed()) {
                    plugin.getPlayerDataManager().resetPity(player.getUniqueId(), crate.getId());
                } else {
                    plugin.getPlayerDataManager().incrementPity(player.getUniqueId(), crate.getId());
                }
            }
            */

            plugin.getPlayerDataManager().incrementLifetimeOpens(player.getUniqueId(), crate.getId());

            // 2. Extra rolls (distinct if possible)
            List<Reward> pool = new ArrayList<>(crate.getRewards());
            pool.remove(primaryResult.getReward());

            int needed = activeRewardSlots.size();
            while (rolledRewards.size() < needed && !pool.isEmpty()) {
                Reward extra = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
                rolledRewards.add(extra);
                pool.remove(extra);
            }
            while (rolledRewards.size() < needed) {
                rolledRewards.add(crate.getRewards().get(ThreadLocalRandom.current().nextInt(crate.getRewards().size())));
            }

            // Shuffle so primary isn't always slot 1
            Collections.shuffle(rolledRewards);
        }

        public void toggleSelection(int clickedSlot) {
            if (this.state != GUIState.SELECTION) return;

            List<Integer> rewardSlots = activeRewardSlots;
            int index = rewardSlots.indexOf(clickedSlot);
            if (index == -1) return;

            int required = crateType.equals("PREMIUM") ? 3 : 1;

            try {
                VisantaraCrates.ACTIVE_CRATE_TYPE.set(this.crateType);
                if (selectedIndices.contains(index)) {
                    // Deselect
                    selectedIndices.remove(index);
                    playSound(player, crate.getSoundOnDeselect());
                } else {
                    // Select
                    if (crateType.equals("FREE")) {
                        selectedIndices.clear(); // Free can only select 1
                    }
                    if (selectedIndices.size() < required) {
                        selectedIndices.add(index);
                        playSound(player, crate.getSoundOnSelect());
                    }
                }

                // Update item display in slots
                for (int i = 0; i < rewardSlots.size(); i++) {
                    int slot = rewardSlots.get(i);
                    Reward reward = rolledRewards.get(i);
                    
                    // Keep reward item in reward slot itself
                    inventory.setItem(slot, AnimationUtil.buildDisplayItem(reward, plugin.getHookManager()));

                    int indicatorSlot = slot + 9;
                    if (selectedIndices.contains(i)) {
                        ItemStack selectBtn = buildButtonItem(plugin, crate.getButtons().getSelectButtonSelected());
                        ItemMeta meta = selectBtn.getItemMeta();
                        if (meta != null) {
                            ItemStack baseRewardItem = AnimationUtil.buildDisplayItem(reward, plugin.getHookManager());
                            ItemMeta baseMeta = baseRewardItem.getItemMeta();
                            String baseName = (baseMeta != null && baseMeta.hasDisplayName())
                                    ? baseMeta.getDisplayName() : (reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId());
                            
                            String format = crate.getButtons().getSelectButtonSelected().getDisplayName();
                            if (format == null || format.isEmpty()) {
                                format = "&a✔ &f{reward}";
                            }
                            meta.setDisplayName(colorize(format.replace("{reward}", baseName).replace("{reward_name}", baseName)));
                            
                            if (baseMeta != null && baseMeta.hasLore()) {
                                meta.setLore(baseMeta.getLore());
                            }
                            selectBtn.setItemMeta(meta);
                        }
                        inventory.setItem(indicatorSlot, selectBtn);
                    } else {
                        ItemStack unselectBtn = buildButtonItem(plugin, crate.getButtons().getGuiFiller());
                        ItemMeta meta = unselectBtn.getItemMeta();
                        if (meta != null) {
                            ItemStack baseRewardItem = AnimationUtil.buildDisplayItem(reward, plugin.getHookManager());
                            ItemMeta baseMeta = baseRewardItem.getItemMeta();
                            String baseName = (baseMeta != null && baseMeta.hasDisplayName())
                                    ? baseMeta.getDisplayName() : (reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId());
                            
                            meta.setDisplayName(colorize("&eSelect: &f" + baseName));
                            
                            if (baseMeta != null && baseMeta.hasLore()) {
                                meta.setLore(baseMeta.getLore());
                            }
                            unselectBtn.setItemMeta(meta);
                        }
                        inventory.setItem(indicatorSlot, unselectBtn);
                    }
                }
            } finally {
                VisantaraCrates.ACTIVE_CRATE_TYPE.remove();
            }

            updateActionButtons();
            updateTitleAndInventory();
        }

        private String formatGlyphPlaceholder(String id) {
            if (id == null || id.isEmpty()) return "";
            if (id.startsWith("%") && id.endsWith("%")) {
                return id;
            }
            if (id.toLowerCase().startsWith("nexo_")) {
                return "%" + id + "%";
            }
            return "%nexo_" + id + "%";
        }

        private String parsePlaceholders(Player player, String text) {
            if (text == null) return "";
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                text = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            }
            return colorize(text);
        }

        private String getActiveTitle() {
            boolean isPremium = crateType.equals("PREMIUM");
            int required = isPremium ? 3 : 1;
            String glyphTitle = "";

            if (state == GUIState.CLOSED || state == GUIState.TRANSITION) {
                glyphTitle = crate.getTitles().getClosed(isPremium);
            } else if (state == GUIState.SPINNING) {
                glyphTitle = crate.getTitles().getOpen(isPremium);
            } else if (state == GUIState.SELECTION) {
                if (selectedIndices.size() == required) {
                    // Claim ON
                    if (isPremium) {
                        if (rerollsRemaining >= 2)      glyphTitle = crate.getTitles().getClaimOnReroll2(true);
                        else if (rerollsRemaining == 1) glyphTitle = crate.getTitles().getClaimOnReroll1(true);
                        else                            glyphTitle = crate.getTitles().getClaimOnRerollOff(true);
                    } else {
                        glyphTitle = crate.getTitles().getClaimOn(false);
                    }
                } else {
                    // Claim OFF
                    if (isPremium) {
                        if (rerollsRemaining >= 2)      glyphTitle = crate.getTitles().getClaimOffReroll2(true);
                        else if (rerollsRemaining == 1) glyphTitle = crate.getTitles().getClaimOffReroll1(true);
                        else                            glyphTitle = crate.getTitles().getClaimOffRerollOff(true);
                    } else {
                        glyphTitle = crate.getTitles().getClaimOff(false);
                    }
                }
            }

            // Title only contains the glyph background — nexo shifts are handled by another plugin
            return parsePlaceholders(player, glyphTitle);
        }

        private void updateTitleAndInventory() {
            String newTitle = getActiveTitle();
            if (newTitle.equals(currentTitle)) {
                return;
            }

            this.isTransitioning = true;
            ItemStack[] contents = this.inventory.getContents();

            Inventory newInv = Bukkit.createInventory(this, 36, newTitle);
            newInv.setContents(contents);

            this.inventory = newInv;
            this.currentTitle = newTitle;
            this.dummySession.setInventory(newInv);

            player.openInventory(newInv);
            this.isTransitioning = false;
        }

        private void updateActionButtons() {
            // Claim/Reroll button items are now rendered in title background by resource pack.
        }

        public void triggerReroll() {
            if (this.state != GUIState.SELECTION || !crateType.equals("PREMIUM") || rerollsRemaining <= 0) return;

            rerollsRemaining--;
            selectedIndices.clear();

            // Open transition GUI immediately instead of closing
            openTransitionGUI();

            playSound(player, crate.getSoundOnReroll());

            BukkitTask transitionTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    this.isTransitioning = false;
                    return;
                }
                startSpinning();
            }, crate.getTransitionTicks());
            tasks.add(transitionTask);
        }

        public void triggerClaim() {
            int required = crateType.equals("PREMIUM") ? 3 : 1;
            if (this.state != GUIState.SELECTION || selectedIndices.size() < required) return;

            this.state = GUIState.CLAIMED;
            this.isTransitioning = true;

            // Play claim sound before closing
            playSound(player, crate.getSoundOnClaim());

            player.closeInventory();
            this.isTransitioning = false;

            try {
                VisantaraCrates.ACTIVE_CRATE_TYPE.set(this.crateType);
                // Deliver selected rewards
                for (int idx : selectedIndices) {
                    Reward reward = rolledRewards.get(idx);
                    plugin.getCrateManager().deliverAndLogReward(player, crate, reward);
                }

                // Run coin claim commands
                executeClaimCommands();
            } finally {
                VisantaraCrates.ACTIVE_CRATE_TYPE.remove();
            }

            // Complete session
            plugin.getAnimationManager().completeSession(dummySession);
        }

        public void autoClaim() {
            if (this.state == GUIState.CLOSED || this.state == GUIState.TRANSITION) {
                refundCrate();
                plugin.getAnimationManager().completeSession(dummySession);
                return;
            }
            if (this.state == GUIState.CLAIMED) return;

            this.state = GUIState.CLAIMED;

            // Auto claim random selections
            int required = crateType.equals("PREMIUM") ? 3 : 1;
            List<Integer> poolIndices = new ArrayList<>();
            for (int i = 0; i < rolledRewards.size(); i++) poolIndices.add(i);
            Collections.shuffle(poolIndices);

            try {
                VisantaraCrates.ACTIVE_CRATE_TYPE.set(this.crateType);
                List<Integer> finalChoices = poolIndices.subList(0, Math.min(required, poolIndices.size()));
                for (int idx : finalChoices) {
                    Reward reward = rolledRewards.get(idx);
                    plugin.getCrateManager().deliverAndLogReward(player, crate, reward);
                }

                executeClaimCommands();
            } finally {
                VisantaraCrates.ACTIVE_CRATE_TYPE.remove();
            }
            plugin.getAnimationManager().completeSession(dummySession);
        }

        public void refundCrate() {
            if (!isBlockClick) {
                // Refund physical crate item
                ItemStack crateItem = PhysicalCrateItem.create(plugin, crate, 1, crateType);
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), crateItem);
                } else {
                    player.getInventory().addItem(crateItem);
                }
                player.sendMessage(colorize("&cSession cancelled. Physical crate returned."));
            }
        }

        private void executeClaimCommands() {
            if (crate.getId().equalsIgnoreCase("VIPCrate")) return;
            int amount = crateType.equals("PREMIUM") ? 3 : 1;
            List<String> commands = crate.getClaimCommands();
            if (commands != null) {
                for (String cmd : commands) {
                    String processed = cmd.replace("%player%", player.getName())
                            .replace("%amount%", String.valueOf(amount));
                    if (processed.startsWith("player:")) {
                        player.performCommand(processed.substring(7).trim());
                    } else {
                        String finalCmd = processed.startsWith("console:") ? processed.substring(8).trim() : processed;
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                    }
                }
            }
        }

        public void cancelAllTasks() {
            for (BukkitTask task : tasks) {
                try { task.cancel(); } catch (Exception ignored) {}
            }
            tasks.clear();
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    private static ItemStack buildButtonItem(VisantaraCrates plugin, Crate.GUIButton button) {
        ItemStack item = null;
        if (plugin.isNexoEnabled() && !button.getNexoId().isEmpty()) {
            var nexoHook = plugin.getHookManager().getNexoHook();
            if (nexoHook != null) {
                item = nexoHook.buildItemById(button.getNexoId());
            }
        }
        if (item == null) {
            Material mat = Material.matchMaterial(button.getMaterial().toUpperCase());
            if (mat == null) mat = Material.PAPER;
            item = new ItemStack(mat);
            if (button.getCustomModelData() > 0) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(button.getCustomModelData());
                    item.setItemMeta(meta);
                }
            }
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!button.getDisplayName().isEmpty()) {
                meta.setDisplayName(colorize(button.getDisplayName()));
            }
            if (!button.getLore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String l : button.getLore()) {
                    coloredLore.add(colorize(l));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), soundName.toLowerCase(), 1.0f, 1.0f);
        }
    }

    private static String colorize(String s) {
        return s == null ? "" : s.replace("&", "\u00A7");
    }
}
