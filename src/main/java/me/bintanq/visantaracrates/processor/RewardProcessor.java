package me.bintanq.visantaracrates.processor;

import me.bintanq.visantaracrates.hook.HookManager;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.model.PlayerData;
import me.bintanq.visantaracrates.model.reward.Reward;
import me.bintanq.visantaracrates.model.reward.RewardResult;
import me.bintanq.visantaracrates.util.Logger;
import me.bintanq.visantaracrates.VisantaraCrates;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RewardProcessor {

    private final VisantaraCrates plugin;
    private final HookManager hookManager;

    public RewardProcessor(VisantaraCrates plugin, HookManager hookManager) {
        this.plugin = plugin;
        this.hookManager = hookManager;
    }

    public RewardResult roll(Crate crate, PlayerData playerData) {
        List<Reward> rewards = crate.getRewards();
        if (rewards == null || rewards.isEmpty())
            throw new IllegalStateException("Crate '" + crate.getId() + "' has no rewards configured!");

        boolean pityGlobal = plugin.isPityEnabled();
        Crate.PityConfig pity = crate.getPity();
        int currentPity = playerData.getPity(crate.getId());

        // Hard pity guarantee — only when pity is globally enabled and per-crate enabled
        if (pityGlobal && pity.isEnabled() && currentPity >= pity.getThreshold()) {
            Reward guaranteed = selectGuaranteedRare(rewards, pity.getRareRarityMinimum());
            if (guaranteed != null) {
                Logger.debug("HARD PITY triggered for " + playerData.getUuid() + " on crate " + crate.getId());
                return buildResult(guaranteed, true, currentPity);
            }
        }

        // Soft pity boost — only when pity is globally enabled and per-crate enabled
        List<Reward> effectiveRewards = (pityGlobal && pity.isEnabled() && currentPity >= pity.getSoftPityStart())
                ? applyPityBoost(rewards, pity, currentPity, crate.getId())
                : rewards;

        Reward selected;
        if (crate.getRarityChances() != null && !crate.getRarityChances().isEmpty()) {
            selected = rollTwoStage(crate, effectiveRewards, pity, currentPity, pityGlobal);
        } else {
            selected = weightedRoll(effectiveRewards);
        }

        return buildResult(selected, false, currentPity);
    }

    private Reward rollTwoStage(Crate crate, List<Reward> effectiveRewards, Crate.PityConfig pity, int currentPity, boolean pityGlobal) {
        Map<String, Double> rarityChances = crate.getRarityChances();
        
        // Find which rarities actually have rewards
        Set<String> activeRarities = new HashSet<>();
        for (Reward r : effectiveRewards) {
            activeRarities.add(r.getRarity().toUpperCase());
        }

        // Copy and filter rarity chances
        Map<String, Double> activeChances = new HashMap<>();
        for (Map.Entry<String, Double> entry : rarityChances.entrySet()) {
            String key = entry.getKey().toUpperCase();
            if (activeRarities.contains(key)) {
                activeChances.put(key, entry.getValue());
            }
        }

        if (activeChances.isEmpty()) {
            return weightedRoll(effectiveRewards);
        }

        // Apply soft pity boost to the rarity chances if pity is enabled and active
        if (pityGlobal && pity.isEnabled() && currentPity >= pity.getSoftPityStart()) {
            int stepsAboveSoft = currentPity - pity.getSoftPityStart();
            double bonusPerRare = pity.getBonusChancePerOpen() * stepsAboveSoft; // percentage boost
            List<String> rareRarities = plugin.getRarityManager().getIdsAtOrAbove(pity.getRareRarityMinimum());
            
            for (String rareRarity : rareRarities) {
                String key = rareRarity.toUpperCase();
                if (activeChances.containsKey(key)) {
                    activeChances.put(key, activeChances.get(key) + bonusPerRare);
                }
            }
        }

        // Roll a rarity tier
        double totalChanceWeight = activeChances.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalChanceWeight <= 0) {
            return weightedRoll(effectiveRewards);
        }

        double random = ThreadLocalRandom.current().nextDouble(totalChanceWeight);
        double cumWeight = 0;
        String selectedRarity = null;
        for (Map.Entry<String, Double> entry : activeChances.entrySet()) {
            cumWeight += entry.getValue();
            if (random < cumWeight) {
                selectedRarity = entry.getKey();
                break;
            }
        }
        if (selectedRarity == null) {
            selectedRarity = activeChances.keySet().iterator().next();
        }

        // Filter effective rewards by selected rarity
        final String finalRarity = selectedRarity;
        List<Reward> tierRewards = effectiveRewards.stream()
                .filter(r -> r.getRarity().equalsIgnoreCase(finalRarity))
                .toList();

        if (tierRewards.isEmpty()) {
            return weightedRoll(effectiveRewards);
        }

        // Roll within the selected rarity tier
        return weightedRoll(tierRewards);
    }

    private Reward weightedRoll(List<Reward> rewards) {
        double totalWeight = rewards.stream().mapToDouble(Reward::getWeight).sum();
        if (totalWeight <= 0)
            return rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));

        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumWeight = 0;
        for (Reward reward : rewards) {
            cumWeight += reward.getWeight();
            if (random < cumWeight) return reward;
        }
        return rewards.get(rewards.size() - 1);
    }

    private List<Reward> applyPityBoost(List<Reward> original, Crate.PityConfig pity,
                                        int currentPity, String crateId) {
        int stepsAboveSoft    = currentPity - pity.getSoftPityStart();
        double totalWeight    = original.stream().mapToDouble(Reward::getWeight).sum();
        double bonusPerRare   = (pity.getBonusChancePerOpen() / 100.0) * totalWeight * stepsAboveSoft;
        List<String> rareRarities = plugin.getRarityManager().getIdsAtOrAbove(pity.getRareRarityMinimum());

        Logger.debug("Soft pity active on " + crateId + " — steps=" + stepsAboveSoft +
                " bonus/rare=" + String.format("%.2f", bonusPerRare));

        return original.stream()
                .map(r -> rareRarities.contains(r.getRarity().toUpperCase())
                        ? cloneWithWeight(r, r.getWeight() + bonusPerRare)
                        : r)
                .toList();
    }

    private Reward selectGuaranteedRare(List<Reward> rewards, String minimumRarity) {
        List<String> qualifyingRarities = plugin.getRarityManager().getIdsAtOrAbove(minimumRarity);
        List<Reward> rares = rewards.stream()
                .filter(r -> qualifyingRarities.contains(r.getRarity().toUpperCase()))
                .toList();
        return rares.isEmpty() ? null : weightedRoll(rares);
    }

    private RewardResult buildResult(Reward reward, boolean pityGuaranteed, int pityAtRoll) {
        ItemStack item = reward.isCommandOnly() ? null : materializeItem(reward);
        return new RewardResult(reward, item, reward.getCommands(), pityGuaranteed, pityAtRoll);
    }

    public ItemStack materializeItem(Reward reward) {
        return switch (reward.getType()) {
            case VANILLA, VANILLA_WITH_COMMANDS, COMMAND -> {
                if (reward.getNexoId() != null && !reward.getNexoId().isEmpty()) {
                    var h = hookManager.getNexoHook();
                    if (h != null) {
                        ItemStack item = h.buildItem(reward);
                        if (item != null) yield item;
                    }
                }
                if (reward.getMmoItemsId() != null && !reward.getMmoItemsId().isEmpty() && reward.getMmoItemsType() != null) {
                    var h = hookManager.getMmoItemsHook();
                    if (h != null) {
                        ItemStack item = h.buildItem(reward);
                        if (item != null) yield item;
                    }
                }
                if (reward.getItemsAdderId() != null && !reward.getItemsAdderId().isEmpty()) {
                    var h = hookManager.getItemsAdderHook();
                    if (h != null) {
                        ItemStack item = h.buildItem(reward);
                        if (item != null) yield item;
                    }
                }
                if (reward.getOraxenId() != null && !reward.getOraxenId().isEmpty()) {
                    var h = hookManager.getOraxenHook();
                    if (h != null) {
                        ItemStack item = h.buildItem(reward);
                        if (item != null) yield item;
                    }
                }
                yield buildVanillaItem(reward);
            }
            case MMOITEMS -> {
                var h = hookManager.getMmoItemsHook();
                yield h != null ? h.buildItem(reward) : fallback(reward, "MMOItems");
            }
            case ITEMSADDER -> {
                var h = hookManager.getItemsAdderHook();
                yield h != null ? h.buildItem(reward) : fallback(reward, "ItemsAdder");
            }
            case ORAXEN -> {
                var h = hookManager.getOraxenHook();
                yield h != null ? h.buildItem(reward) : fallback(reward, "Oraxen");
            }
            case NEXO -> {
                var h = hookManager.getNexoHook();
                yield h != null ? h.buildItem(reward) : fallback(reward, "Nexo");
            }
            case CRATE -> {
                Crate targetCrate = plugin.getCrateManager().getCrate(reward.getCrateId());
                if (targetCrate == null) {
                    Logger.warn("Crate '" + reward.getCrateId() + "' not found for reward '" + reward.getId() + "'.");
                    yield new ItemStack(Material.AIR);
                }
                yield me.bintanq.visantaracrates.util.PhysicalCrateItem.create(plugin, targetCrate, reward.getAmount(), "FREE");
            }
        };
    }

    private ItemStack buildVanillaItem(Reward reward) {
        Material material = Material.matchMaterial(
                reward.getMaterial() != null ? reward.getMaterial() : "STONE");
        if (material == null) {
            Logger.warn("Invalid material '" + reward.getMaterial() + "' in reward '" + reward.getId() + "'. Using STONE.");
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material, Math.max(1, reward.getAmount()));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (reward.getItemName() != null && !reward.getItemName().isEmpty())
            meta.setDisplayName(colorize(reward.getItemName()));

        if (reward.getLore() != null && !reward.getLore().isEmpty())
            meta.setLore(reward.getLore().stream().map(this::colorize).toList());

        if (reward.getCustomModelData() > 0)
            meta.setCustomModelData(reward.getCustomModelData());

        item.setItemMeta(meta);

        if (reward.getEnchantments() != null) {
            for (Reward.EnchantmentEntry entry : reward.getEnchantments()) {
                try {
                    Enchantment ench = Enchantment.getByKey(
                            NamespacedKey.minecraft(entry.enchantment.toLowerCase()));
                    if (ench != null) item.addUnsafeEnchantment(ench, entry.level);
                } catch (Exception e) {
                    Logger.warn("Unknown enchantment: " + entry.enchantment);
                }
            }
        }
        return item;
    }

    private ItemStack fallback(Reward reward, String pluginName) {
        Logger.warn(pluginName + " hook not loaded — reward '" + reward.getId() + "' falls back to vanilla.");
        return buildVanillaItem(reward);
    }

    public void executeCommands(Player player, RewardResult result) {
        if (!result.hasCommands()) return;
        for (String rawCmd : result.getCommands()) {
            String cmd = rawCmd
                    .replace("%player%", player.getName())
                    .replace("{player}", player.getName());
            if (cmd.startsWith("player:")) {
                player.performCommand(cmd.substring(7).trim());
            } else {
                String finalCmd = cmd.startsWith("console:") ? cmd.substring(8).trim() : cmd;
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCmd);
            }
        }
    }

    private Reward cloneWithWeight(Reward original, double newWeight) {
        return new WeightOverrideReward(original, newWeight);
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }

    private static class WeightOverrideReward extends Reward {
        private final Reward delegate;
        private final double overrideWeight;

        WeightOverrideReward(Reward delegate, double overrideWeight) {
            this.delegate = delegate;
            this.overrideWeight = overrideWeight;
        }

        @Override public String getId()            { return delegate.getId(); }
        @Override public String getDisplayName()   { return delegate.getDisplayName(); }
        @Override public double getWeight()        { return overrideWeight; }
        @Override public String getRarity()        { return delegate.getRarity(); }
        @Override public RewardType getType()      { return delegate.getType(); }
        @Override public String getMaterial()      { return delegate.getMaterial(); }
        @Override public int    getAmount()        { return delegate.getAmount(); }
        @Override public String getItemName()      { return delegate.getItemName(); }
        @Override public java.util.List<String> getLore()              { return delegate.getLore(); }
        @Override public java.util.List<EnchantmentEntry> getEnchantments() { return delegate.getEnchantments(); }
        @Override public int    getCustomModelData()                   { return delegate.getCustomModelData(); }
        @Override public String getMmoItemsType()  { return delegate.getMmoItemsType(); }
        @Override public String getMmoItemsId()    { return delegate.getMmoItemsId(); }
        @Override public String getItemsAdderId()  { return delegate.getItemsAdderId(); }
        @Override public String getOraxenId()      { return delegate.getOraxenId(); }
        @Override public String getNexoId()         { return delegate.getNexoId(); }
        @Override public String getCrateId()        { return delegate.getCrateId(); }
        @Override public java.util.List<String> getCommands()         { return delegate.getCommands(); }
        @Override public boolean isBroadcast()     { return delegate.isBroadcast(); }
        @Override public String getBroadcastMessage()                 { return delegate.getBroadcastMessage(); }
        @Override public boolean isCommandOnly()   { return delegate.isCommandOnly(); }
    }
}