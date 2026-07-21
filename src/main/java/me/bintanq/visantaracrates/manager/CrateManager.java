package me.bintanq.visantaracrates.manager;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.api.dto.RewardSnapshot;
import me.bintanq.visantaracrates.api.event.CrateOpenEvent;
import me.bintanq.visantaracrates.api.event.CrateRewardEvent;
import me.bintanq.visantaracrates.log.LogManager;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.model.PlayerData;
import me.bintanq.visantaracrates.model.SaveReport;
import me.bintanq.visantaracrates.model.reward.RewardResult;
import me.bintanq.visantaracrates.serializer.GsonProvider;
import me.bintanq.visantaracrates.util.Logger;
import me.bintanq.visantaracrates.util.MessageManager;
import me.bintanq.visantaracrates.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CrateManager {

    private final VisantaraCrates plugin;
    private final PlayerDataManager playerDataManager;
    private final me.bintanq.visantaracrates.processor.RewardProcessor rewardProcessor;
    private final LogManager logManager;

    private final Object saveLock = new Object();

    private final ConcurrentHashMap<String, Crate> crateRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> locationIndex = new ConcurrentHashMap<>();
    private final Set<UUID> openingLock = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> rateLimitTracker
            = new ConcurrentHashMap<>();
    private File cratesDir;

    public CrateManager(VisantaraCrates plugin, PlayerDataManager playerDataManager,
                        me.bintanq.visantaracrates.processor.RewardProcessor rewardProcessor,
                        LogManager logManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.rewardProcessor = rewardProcessor;
        this.logManager = logManager;
    }

    private String locationKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public void loadAllCrates() {
        cratesDir = new File(plugin.getDataFolder(), "crates");
        if (!cratesDir.exists()) {
            cratesDir.mkdirs();
            createExampleCrate();
        }

        boolean useJson = false;
        String extension = useJson ? ".json" : ".yml";
        String oldExtension = useJson ? ".yml" : ".json";

        // Perform auto-migration if files of the other format exist
        File[] oldFiles = cratesDir.listFiles((dir, name) -> name.endsWith(oldExtension));
        if (oldFiles != null) {
            for (File oldFile : oldFiles) {
                try {
                    Crate crate = null;
                    if (useJson) {
                        org.bukkit.configuration.file.YamlConfiguration yaml =
                                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(oldFile);
                        Map<String, Object> map = sectionToMap(yaml);
                        String json = GsonProvider.getGson().toJson(map);
                        crate = GsonProvider.getGson().fromJson(json, Crate.class);
                    } else {
                        try (FileReader reader = new FileReader(oldFile, StandardCharsets.UTF_8)) {
                            crate = GsonProvider.getGson().fromJson(reader, Crate.class);
                        }
                    }
                    if (crate != null) {
                        if (crate.getId() == null || crate.getId().isEmpty()) {
                            crate.setId(oldFile.getName().replace(oldExtension, ""));
                        }
                        saveCrate(crate);
                        oldFile.delete();
                        Logger.info("Migrated crate file: " + oldFile.getName() + " -> " + crate.getId() + extension);
                    }
                } catch (Exception e) {
                    Logger.severe("Failed to migrate crate file '" + oldFile.getName() + "': " + e.getMessage());
                }
            }
        }

        File[] files = cratesDir.listFiles((dir, name) -> name.endsWith(extension));
        if (files == null || files.length == 0) {
            Logger.warn("No crate files found in /crates/. Create example crate first.");
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                Crate crate;
                if (useJson) {
                    try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                        crate = GsonProvider.getGson().fromJson(reader, Crate.class);
                    }
                } else {
                    org.bukkit.configuration.file.YamlConfiguration yaml =
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                    Map<String, Object> map = sectionToMap(yaml);
                    String json = GsonProvider.getGson().toJson(map);
                    crate = GsonProvider.getGson().fromJson(json, Crate.class);
                }

                if (crate != null) {
                    crate.migrateLegacyLocation();
                    if (crate.getId() == null || crate.getId().isEmpty())
                        crate.setId(file.getName().replace(extension, ""));
                    crateRegistry.put(crate.getId(), crate);
                    loaded++;
                    Logger.info("Loaded crate: &e" + crate.getId() + " &f(Type: &e" + crate.getCrateType() + "&f, Slots: &e" + crate.getSlots().getRewardSlots() + "&f)");
                }
            } catch (Exception e) {
                Logger.severe("Failed to load crate file '" + file.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        Logger.info("Loaded &e" + loaded + " &fcrates.");
        locationIndex.clear();
        crateRegistry.values().forEach(c -> {
            if (c.getLocations() != null) {
                c.getLocations().forEach(loc ->
                        locationIndex.put(locationKey(loc.world, (int)loc.x, (int)loc.y, (int)loc.z), c.getId()));
            }
        });
    }

    public void saveCrate(Crate crate) {
        synchronized (saveLock) {
            locationIndex.entrySet().removeIf(e -> e.getValue().equals(crate.getId()));

            boolean useJson = false;
            String extension = useJson ? ".json" : ".yml";
            File file = new File(cratesDir, crate.getId() + extension);
            try {
                if (useJson) {
                    try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                        GsonProvider.getGson().toJson(crate, writer);
                    }
                } else {
                    org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
                    String json = GsonProvider.getGson().toJson(crate);
                    Map<String, Object> map = GsonProvider.getGson().fromJson(json, new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
                    populateSection(yaml, map);
                    yaml.save(file);
                }
            } catch (IOException e) {
                Logger.severe("Failed to save crate '" + crate.getId() + "': " + e.getMessage());
                return;
            }
            crateRegistry.put(crate.getId(), crate);

            if (crate.getLocations() != null) {
                for (Crate.SerializableLocation loc : crate.getLocations()) {
                    locationIndex.put(locationKey(loc.world, (int)loc.x, (int)loc.y, (int)loc.z), crate.getId());
                }
            }
        }
    }

    public SaveReport.Entry diffCrate(Crate before, Crate after) {
        if (before == null)
            return new SaveReport.Entry("CRATE", SaveReport.ChangeType.ADDED,
                    "Created crate '" + after.getId() + "'" +
                            " displayName='" + after.getDisplayName() + "'");

        List<String> changes = new ArrayList<>();

        // Basic fields
        if (!java.util.Objects.equals(before.getDisplayName(), after.getDisplayName()))
            changes.add("displayName '" + before.getDisplayName() + "' → '" + after.getDisplayName() + "'");
        if (before.isEnabled() != after.isEnabled())
            changes.add("enabled " + before.isEnabled() + " → " + after.isEnabled());
        if (before.getCooldownMs() != after.getCooldownMs())
            changes.add("cooldown " + before.getCooldownMs() + "ms → " + after.getCooldownMs() + "ms");
        if (before.isMassOpenEnabled() != after.isMassOpenEnabled())
            changes.add("massOpen " + before.isMassOpenEnabled() + " → " + after.isMassOpenEnabled());
        if (before.getMassOpenLimit() != after.getMassOpenLimit())
            changes.add("massOpenLimit " + before.getMassOpenLimit() + " → " + after.getMassOpenLimit());
        if (before.getOpenRateLimit() != after.getOpenRateLimit())
            changes.add("openRateLimit " + before.getOpenRateLimit() + " → " + after.getOpenRateLimit());
        if (before.getLifetimeOpenLimit() != after.getLifetimeOpenLimit())
            changes.add("lifetimeOpenLimit " + before.getLifetimeOpenLimit() + " → " + after.getLifetimeOpenLimit());
        if (before.isAccessDeniedKnockback() != after.isAccessDeniedKnockback())
            changes.add("knockback " + before.isAccessDeniedKnockback() + " → " + after.isAccessDeniedKnockback());
        if (Double.compare(before.getKnockbackStrength(), after.getKnockbackStrength()) != 0)
            changes.add("knockbackStrength " + before.getKnockbackStrength() + " → " + after.getKnockbackStrength());

        // Animations
        if (before.getGuiAnimation() != after.getGuiAnimation())
            changes.add("guiAnimation " + before.getGuiAnimation() + " → " + after.getGuiAnimation());
        if (!before.getIdleAnimation().getType().equals(after.getIdleAnimation().getType()))
            changes.add("idleAnimation " + before.getIdleAnimation().getType() + " → " + after.getIdleAnimation().getType());
        if (!before.getIdleAnimation().getParticle().equals(after.getIdleAnimation().getParticle()))
            changes.add("idleParticle " + before.getIdleAnimation().getParticle() + " → " + after.getIdleAnimation().getParticle());
        if (!before.getOpenAnimation().getType().equals(after.getOpenAnimation().getType()))
            changes.add("openAnimation " + before.getOpenAnimation().getType() + " → " + after.getOpenAnimation().getType());
        if (!before.getOpenAnimation().getParticle().equals(after.getOpenAnimation().getParticle()))
            changes.add("openParticle " + before.getOpenAnimation().getParticle() + " → " + after.getOpenAnimation().getParticle());

        // Hologram
        if (Double.compare(before.getHologramHeight(), after.getHologramHeight()) != 0)
            changes.add("hologramHeight " + before.getHologramHeight() + " → " + after.getHologramHeight());
        if (!before.getHologramLines().equals(after.getHologramLines()))
            changes.add("hologramLines (" + before.getHologramLines().size() + " → " + after.getHologramLines().size() + " lines)");

        // Pity
        Crate.PityConfig bp = before.getPity(), ap = after.getPity();
        if (bp.isEnabled() != ap.isEnabled())
            changes.add("pity " + bp.isEnabled() + " → " + ap.isEnabled());
        else if (ap.isEnabled()) {
            if (bp.getThreshold() != ap.getThreshold())
                changes.add("pity.threshold " + bp.getThreshold() + " → " + ap.getThreshold());
            if (bp.getSoftPityStart() != ap.getSoftPityStart())
                changes.add("pity.softStart " + bp.getSoftPityStart() + " → " + ap.getSoftPityStart());
            if (!java.util.Objects.equals(bp.getRareRarityMinimum(), ap.getRareRarityMinimum()))
                changes.add("pity.minRarity " + bp.getRareRarityMinimum() + " → " + ap.getRareRarityMinimum());
            if (Double.compare(bp.getBonusChancePerOpen(), ap.getBonusChancePerOpen()) != 0)
                changes.add("pity.bonusChance " + bp.getBonusChancePerOpen() + " → " + ap.getBonusChancePerOpen());
        }

        // Schedule
        boolean beforeHasSched = before.getSchedule() != null;
        boolean afterHasSched  = after.getSchedule() != null;
        if (beforeHasSched != afterHasSched) {
            changes.add(afterHasSched ? "schedule added (" + after.getSchedule().getMode() + ")"
                    : "schedule removed");
        } else if (beforeHasSched && !before.getSchedule().getMode().equals(after.getSchedule().getMode())) {
            changes.add("schedule " + before.getSchedule().getMode() + " → " + after.getSchedule().getMode());
        }

        // Rewards — added/removed
        Set<String> beforeIds = before.getRewards().stream()
                .map(me.bintanq.visantaracrates.model.reward.Reward::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> afterIds = after.getRewards().stream()
                .map(me.bintanq.visantaracrates.model.reward.Reward::getId)
                .collect(java.util.stream.Collectors.toSet());
        afterIds.stream().filter(id -> !beforeIds.contains(id))
                .forEach(id -> changes.add("added reward '" + id + "'"));
        beforeIds.stream().filter(id -> !afterIds.contains(id))
                .forEach(id -> changes.add("removed reward '" + id + "'"));
        // Weight changes
        after.getRewards().forEach(ar -> before.getRewards().stream()
                .filter(br -> br.getId().equals(ar.getId()))
                .findFirst().ifPresent(br -> {
                    if (Double.compare(br.getWeight(), ar.getWeight()) != 0)
                        changes.add("reward '" + ar.getId() + "' weight " + br.getWeight() + " → " + ar.getWeight());
                }));

        // Locations
        int beforeLocs = before.getLocations().size();
        int afterLocs  = after.getLocations().size();
        if (beforeLocs != afterLocs)
            changes.add("locations " + beforeLocs + " → " + afterLocs);

        // Build detail string — compact jika banyak
        String detail;
        if (changes.isEmpty()) {
            detail = "no tracked fields changed";
        } else if (changes.size() <= 3) {
            detail = String.join("; ", changes);
        } else {
            // Ambil 3 pertama, sisanya ringkas
            detail = String.join("; ", changes.subList(0, 3))
                    + " (+" + (changes.size() - 3) + " more)";
        }

        return new SaveReport.Entry("CRATE", SaveReport.ChangeType.MODIFIED,
                "Modified crate '" + after.getId() + "': " + detail);
    }

    public enum OpenResult {
        SUCCESS, NOT_FOUND, DISABLED, NOT_SCHEDULED,
        ON_COOLDOWN, MISSING_KEY, ALREADY_OPENING,
        RATE_LIMITED, LIFETIME_LIMIT_REACHED
    }

    public OpenResult canOpen(Player player, String crateId) {
        if (plugin.getAnimationManager().hasSession(player.getUniqueId()))
            return OpenResult.ALREADY_OPENING;
        Crate crate = crateRegistry.get(crateId);
        if (crate == null) return OpenResult.NOT_FOUND;
        if (!crate.isEnabled()) return OpenResult.DISABLED;
        if (openingLock.contains(player.getUniqueId())) return OpenResult.ALREADY_OPENING;
        if (!crate.isCurrentlyOpenable()) return OpenResult.NOT_SCHEDULED;

        PlayerData data = playerDataManager.getOrEmpty(player.getUniqueId());
        if (crate.getCooldownMs() > 0
                && data.isOnCooldown(crateId, crate.getCooldownMs())
                && !player.hasPermission("VisantaraCrates.bypasscooldown"))
            return OpenResult.ON_COOLDOWN;

        if (crate.getOpenRateLimit() > 0) {
            long minInterval = 1000L / crate.getOpenRateLimit();
            ConcurrentHashMap<String, Long> playerMap = rateLimitTracker
                    .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
            Long last = playerMap.get(crateId);
            if (last != null && System.currentTimeMillis() - last < minInterval) {
                return OpenResult.RATE_LIMITED;
            }
        }

        if (crate.getLifetimeOpenLimit() > 0
                && !player.hasPermission("VisantaraCrates.bypasslimit")) {
            int lifetimeUsed = playerDataManager.getLifetimeOpens(player.getUniqueId(), crateId);
            if (lifetimeUsed >= crate.getLifetimeOpenLimit())
                return OpenResult.LIFETIME_LIMIT_REACHED;
        }
        return OpenResult.SUCCESS;
    }

    public boolean openCrate(Player player, String crateId) {
        OpenResult check = canOpen(player, crateId);
        if (check != OpenResult.SUCCESS) {
            sendOpenResultFeedback(player, check, crateId);
            return false;
        }
        return executeOpen(player, crateId, false);
    }

    private boolean executeOpen(Player player, String crateId, boolean skipCooldownCheck) {
        Crate crate = crateRegistry.get(crateId);
        if (crate == null || !crate.isEnabled()) return false;
        if (openingLock.contains(player.getUniqueId())) return false;
        if (!crate.isCurrentlyOpenable()) return false;

        PlayerData data = playerDataManager.getOrEmpty(player.getUniqueId());
        openingLock.add(player.getUniqueId());

        try {

            if (crate.getLifetimeOpenLimit() > 0
                    && !player.hasPermission("VisantaraCrates.bypasslimit")) {
                int lifetimeUsed = playerDataManager.getLifetimeOpens(player.getUniqueId(), crateId);
                if (lifetimeUsed >= crate.getLifetimeOpenLimit()) {
                    sendOpenResultFeedback(player, OpenResult.LIFETIME_LIMIT_REACHED, crateId);
                    return false;
                }
            }

            String consumedKeyId = "physical";
            RewardResult result = rewardProcessor.roll(crate, data);

            if (plugin.isPityEnabled()) {
                boolean isRare = plugin.getRarityManager()
                        .isAtOrAbove(result.getReward().getRarity(), crate.getPity().getRareRarityMinimum());

                if (isRare || result.isPityGuaranteed()) {
                    playerDataManager.resetPity(player.getUniqueId(), crateId);
                } else {
                    playerDataManager.incrementPity(player.getUniqueId(), crateId);
                }
            }

            if (crate.getCooldownMs() > 0)
                playerDataManager.setLastOpen(player.getUniqueId(), crateId);

            if (crate.getOpenRateLimit() > 0) {
                rateLimitTracker
                        .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                        .put(crateId, System.currentTimeMillis());
            }
            playerDataManager.incrementLifetimeOpens(player.getUniqueId(), crateId);

            // Fire API events
            RewardSnapshot snap = RewardSnapshot.from(result.getReward(), crate.getTotalWeight());
            CrateRewardEvent rewardEvent = new CrateRewardEvent(player, crateId, snap);
            Bukkit.getPluginManager().callEvent(rewardEvent);
            CrateOpenEvent openEvent = new CrateOpenEvent(player, crateId, snap,
                    result.isPityGuaranteed(), result.getPityAtRoll());
            Bukkit.getPluginManager().callEvent(openEvent);

            plugin.getAnimationManager().startAnimation(player, crate, result, consumedKeyId);

            if (plugin.getParticleManager() != null)
                plugin.getParticleManager().playOpenEffect(crate, player.getLocation());

            if (result.getReward().isBroadcast()) {
                plugin.getServer().broadcastMessage(result.getReward().getBroadcastMessage()
                        .replace("{player}", player.getName())
                        .replace("{reward}", result.getReward().getDisplayName())
                        .replace("&", "\u00A7"));
            }

            org.bukkit.Location loc = player.getLocation();
            me.bintanq.visantaracrates.log.CrateLog crateLog = new me.bintanq.visantaracrates.log.CrateLog(
                    player.getUniqueId(), player.getName(), crateId,
                    result.getReward().getId(), result.getReward().getDisplayName(),
                    result.getPityAtRoll(), System.currentTimeMillis(),
                    loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                    loc.getX(), loc.getY(), loc.getZ());

            logManager.log(crateLog);

            return true;
        } finally {
            openingLock.remove(player.getUniqueId());
        }
    }

    /**
     * Internal — for mass open only. Keys already consumed in batch.
     * Skips key check and consume step.
     */
    private boolean executeOpenNoKeyConsume(Player player, String crateId) {
        Crate crate = crateRegistry.get(crateId);
        if (crate == null || !crate.isEnabled()) return false;
        if (!crate.isCurrentlyOpenable()) return false;

        if (crate.getLifetimeOpenLimit() > 0
                && !player.hasPermission("VisantaraCrates.bypasslimit")) {
            int lifetimeUsed = playerDataManager.getLifetimeOpens(player.getUniqueId(), crateId);
            if (lifetimeUsed >= crate.getLifetimeOpenLimit()) return false;
        }

        PlayerData data = playerDataManager.getOrEmpty(player.getUniqueId());

        try {
            RewardResult result = rewardProcessor.roll(crate, data);

            if (plugin.isPityEnabled()) {
                boolean isRare = plugin.getRarityManager()
                        .isAtOrAbove(result.getReward().getRarity(), crate.getPity().getRareRarityMinimum());

                if (isRare || result.isPityGuaranteed()) {
                    playerDataManager.resetPity(player.getUniqueId(), crateId);
                } else {
                    playerDataManager.incrementPity(player.getUniqueId(), crateId);
                }
            }

            if (crate.getCooldownMs() > 0)
                playerDataManager.setLastOpen(player.getUniqueId(), crateId);
            playerDataManager.incrementLifetimeOpens(player.getUniqueId(), crateId);

            // Fire API events for mass open
            RewardSnapshot snap = RewardSnapshot.from(result.getReward(), crate.getTotalWeight());
            CrateRewardEvent rewardEvent = new CrateRewardEvent(player, crateId, snap);
            Bukkit.getPluginManager().callEvent(rewardEvent);
            if (rewardEvent.isCancelled()) return false;
            CrateOpenEvent openEvent = new CrateOpenEvent(player, crateId, snap,
                    result.isPityGuaranteed(), result.getPityAtRoll());
            Bukkit.getPluginManager().callEvent(openEvent);

            // Mass open: deliver langsung tanpa animasi GUI
            deliverRewardPublic(player, result);

            if (result.getReward().isBroadcast()) {
                plugin.getServer().broadcastMessage(result.getReward().getBroadcastMessage()
                        .replace("{player}", player.getName())
                        .replace("{reward}", result.getReward().getDisplayName())
                        .replace("&", "\u00A7"));
            }

            org.bukkit.Location loc = player.getLocation();
            me.bintanq.visantaracrates.log.CrateLog crateLog = new me.bintanq.visantaracrates.log.CrateLog(
                    player.getUniqueId(), player.getName(), crateId,
                    result.getReward().getId(), result.getReward().getDisplayName(),
                    result.getPityAtRoll(), System.currentTimeMillis(),
                    loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                    loc.getX(), loc.getY(), loc.getZ());

            logManager.log(crateLog);

            return true;
        } catch (Exception e) {
            Logger.severe("executeOpenNoKeyConsume error: " + e.getMessage());
            return false;
        }
    }

    public void deliverRewardPublic(Player player, RewardResult result) {
        if (result.hasItem()) {
            ItemStack item = result.getItemStack();
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                MessageManager.send(player, "inventory-full", "{reward}", result.getReward().getDisplayName());
            } else {
                player.getInventory().addItem(item);
            }
        }
        if (result.hasCommands()) rewardProcessor.executeCommands(player, result);
        MessageManager.send(player, "reward-received", "{reward}", result.getReward().getDisplayName());
    }

    public void deliverAndLogReward(Player player, Crate crate, me.bintanq.visantaracrates.model.reward.Reward reward) {
        ItemStack rewardItem = reward.isCommandOnly() ? null : rewardProcessor.materializeItem(reward);
        RewardResult res = new RewardResult(reward, rewardItem, reward.getCommands(), false, 0);
        deliverRewardPublic(player, res);

        // Broadcast if enabled
        if (reward.isBroadcast()) {
            String bcMsg = reward.getBroadcastMessage();
            if (bcMsg == null || bcMsg.isEmpty()) {
                bcMsg = plugin.getConfig().getString("messages.broadcast-default", "&6✦ &e{player} &7won &6{reward} &7from &e{crate}&7!");
            }
            plugin.getServer().broadcastMessage(bcMsg
                    .replace("{player}", player.getName())
                    .replace("{reward}", reward.getDisplayName() != null ? reward.getDisplayName() : reward.getId())
                    .replace("{crate}", crate.getDisplayName() != null ? crate.getDisplayName() : crate.getId())
                    .replace("&", "\u00A7"));
        }

        // Log to database
        org.bukkit.Location loc = player.getLocation();
        me.bintanq.visantaracrates.log.CrateLog crateLog = new me.bintanq.visantaracrates.log.CrateLog(
                player.getUniqueId(), player.getName(), crate.getId(),
                reward.getId(), reward.getDisplayName(),
                0, System.currentTimeMillis(),
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                loc.getX(), loc.getY(), loc.getZ());
        logManager.log(crateLog);
    }

    private void sendOpenResultFeedback(Player player, OpenResult result, String crateId) {
        switch (result) {
            case NOT_FOUND   -> MessageManager.send(player, "crate-not-found", "{crate}", crateId);
            case DISABLED    -> MessageManager.send(player, "crate-disabled", "{crate}", crateId);
            case NOT_SCHEDULED -> {
                Crate crate = crateRegistry.get(crateId);
                String sched = crate != null && crate.getSchedule() != null
                        ? crate.getSchedule().getNextOpenDescription() : "Unknown";
                MessageManager.send(player, "crate-not-open", "{crate}", crateId, "{schedule}", sched);
            }
            case ON_COOLDOWN -> {
                Crate crate = crateRegistry.get(crateId);
                long rem = crate != null
                        ? playerDataManager.getRemainingCooldown(player.getUniqueId(), crateId, crate.getCooldownMs()) : 0;
                MessageManager.send(player, "cooldown-active", "{time}", TimeUtil.formatDuration(rem));
            }
            case MISSING_KEY -> {
                MessageManager.send(player, "key-not-found", "{key}", crateId);
            }
            case ALREADY_OPENING -> MessageManager.send(player, "already-opening");
            case RATE_LIMITED        -> MessageManager.send(player, "rate-limited");
            case LIFETIME_LIMIT_REACHED -> {
                Crate crate = crateRegistry.get(crateId);
                MessageManager.send(player, "lifetime-limit-reached",
                        "{limit}", String.valueOf(crate != null ? crate.getLifetimeOpenLimit() : 0));
            }
            default              -> MessageManager.send(player, "crate-not-found", "{crate}", crateId);
        }
    }

    public void sendOpenResultFeedbackPublic(Player player, OpenResult result, String crateId) {
        sendOpenResultFeedback(player, result, crateId);
    }

    private void createExampleCrate() {
        boolean useJson = false;
        String extension = useJson ? ".json" : ".yml";
        File example = new File(cratesDir, "example_crate" + extension);
        if (example.exists()) return;

        RarityManager rm = plugin.getRarityManager();
        List<me.bintanq.visantaracrates.model.RarityDefinition> rarities = rm.getAll();

        String lowestRarity  = rarities.isEmpty() ? "COMMON"  : rarities.get(0).getId();
        String midRarity     = rarities.size() > 2 ? rarities.get(2).getId() : rarities.get(rarities.size() / 2).getId();
        String highestRarity = rarities.isEmpty() ? "LEGENDARY"  : rarities.get(rarities.size() - 1).getId();
        String pityMinRarity = rarities.size() >= 5 ? rarities.get(rarities.size() - 2).getId() : highestRarity;

        String json = """
        {
          "id": "example_crate",
          "displayName": "&b&lExample Crate",
          "hologramLines": ["&b&lEXAMPLE CRATE", "&7Left-click to preview!", "&7Right-click to open!"],
          "hologramHeight": 1.2,
          "requiredKeys": [
            { "keyId": "example_key", "amount": 1, "type": "VIRTUAL" }
          ],
          "rewards": [
            {
              "id": "diamond",
              "displayName": "&bDiamond",
              "weight": 50.0,
              "rarity": "%s",
              "type": "VANILLA",
              "material": "DIAMOND",
              "amount": 1
            },
            {
              "id": "emerald",
              "displayName": "&aEmerald",
              "weight": 25.0,
              "rarity": "%s",
              "type": "VANILLA",
              "material": "EMERALD",
              "amount": 2
            },
            {
              "id": "netherite",
              "displayName": "&4&lNetherite Ingot",
              "weight": 5.0,
              "rarity": "%s",
              "type": "VANILLA",
              "material": "NETHERITE_INGOT",
              "amount": 1,
              "broadcast": true,
              "broadcastMessage": "&e{player} &7won &4Netherite&7 from Example Crate!"
            },
            {
              "id": "cmd_reward",
              "displayName": "&d&lMythic Command",
              "weight": 1.0,
              "rarity": "%s",
              "type": "COMMAND",
              "commands": ["console: give {player} minecraft:nether_star 5"],
              "broadcast": true,
              "broadcastMessage": "&d✦ {player} got a top-tier reward!"
            }
          ],
          "preview": {
            "sortOrder": "RARITY_DESC",
            "showChance": true,
            "showPity": true,
            "showKeyBalance": true,
            "showActualItem": true
          },
          "cooldownMs": 3600000,
          "pity": {
            "enabled": true,
            "threshold": 50,
            "rareRarityMinimum": "%s",
            "bonusChancePerOpen": 2.0,
            "softPityStart": 40
          },
          "massOpenEnabled": true,
          "massOpenLimit": 64,
          "openRateLimit": 0,
          "lifetimeOpenLimit": 0,
          "enabled": true,
          "guiAnimation": "ROULETTE",
          "guiAnimationSpeed": 1.0,
          "particleAnimationSpeed": 1.0,
          "openSound": "BLOCK_NOTE_BLOCK_HAT",
          "winSound": "UI_TOAST_CHALLENGE_COMPLETE"
        }
        """.formatted(lowestRarity, midRarity, pityMinRarity, highestRarity, pityMinRarity);

        try {
            Crate crate = GsonProvider.getGson().fromJson(json, Crate.class);
            if (useJson) {
                try (FileWriter w = new FileWriter(example, StandardCharsets.UTF_8)) {
                    GsonProvider.getGson().toJson(crate, w);
                }
            } else {
                org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
                Map<String, Object> map = GsonProvider.getGson().fromJson(json, new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
                populateSection(yaml, map);
                yaml.save(example);
            }
        } catch (IOException e) {
            Logger.severe("Failed to create example crate file: " + e.getMessage());
        }
    }

    public Crate getCrateAtLocation(String world, int x, int y, int z) {
        String crateId = locationIndex.get(locationKey(world, x, y, z));
        return crateId != null ? crateRegistry.get(crateId) : null;
    }

    public void shutdown() {
        openingLock.clear();
        rateLimitTracker.clear();
    }

    public void cleanupPlayer(UUID uuid) {
        rateLimitTracker.remove(uuid);
    }

    public Crate getCrate(String id) { return crateRegistry.get(id); }
    public Collection<Crate> getAllCrates() { return crateRegistry.values(); }
    public Map<String, Crate> getCrateRegistry() { return Collections.unmodifiableMap(crateRegistry); }

    public void registerCrate(Crate crate) {
        saveCrate(crate);
        if (plugin.getParticleManager()  != null) plugin.getParticleManager().startIdleParticles(crate);
    }

    public void removeCrate(String id) {
        synchronized (saveLock) {
            locationIndex.entrySet().removeIf(e -> e.getValue().equals(id));
            crateRegistry.remove(id);
            File fileJson = new File(cratesDir, id + ".json");
            File fileYaml = new File(cratesDir, id + ".yml");
            if (fileJson.exists()) fileJson.delete();
            if (fileYaml.exists()) fileYaml.delete();
        }
        if (plugin.getParticleManager()  != null) plugin.getParticleManager().stopIdleParticles(id);
    }

    private Map<String, Object> sectionToMap(org.bukkit.configuration.ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object val = section.get(key);
            if (val instanceof org.bukkit.configuration.ConfigurationSection sub) {
                map.put(key, sectionToMap(sub));
            } else if (val instanceof List<?> list) {
                List<Object> mappedList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof org.bukkit.configuration.ConfigurationSection subSec) {
                        mappedList.add(sectionToMap(subSec));
                    } else if (item instanceof Map<?,?> subMap) {
                        mappedList.add(subMap);
                    } else {
                        mappedList.add(item);
                    }
                }
                map.put(key, mappedList);
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private void populateSection(org.bukkit.configuration.ConfigurationSection section, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof Map) {
                org.bukkit.configuration.ConfigurationSection sub = section.createSection(key);
                populateSection(sub, (Map<String, Object>) val);
            } else if (val instanceof List<?> list) {
                List<Object> mappedList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        mappedList.add(item);
                    } else {
                        mappedList.add(item);
                    }
                }
                section.set(key, mappedList);
            } else {
                section.set(key, val);
            }
        }
    }
}