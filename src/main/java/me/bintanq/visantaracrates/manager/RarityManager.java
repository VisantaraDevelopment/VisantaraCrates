package me.bintanq.visantaracrates.manager;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.model.RarityDefinition;
import me.bintanq.visantaracrates.util.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class RarityManager {

    private final VisantaraCrates plugin;
    private final List<RarityDefinition> rarities = new ArrayList<>();
    private final Map<String, RarityDefinition> byId = new LinkedHashMap<>();

    public RarityManager(VisantaraCrates plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rarities.clear();
        byId.clear();

        File file = new File(plugin.getDataFolder(), "rarities.yml");
        if (!file.exists()) {
            saveDefaultRarities(file);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("rarities");

        if (section == null || section.getKeys(false).isEmpty()) {
            Logger.warn("rarities.yml has no 'rarities' section — using defaults.");
            defaultRarities().forEach(r -> { rarities.add(r); byId.put(r.getId(), r); });
        } else {
            for (String key : section.getKeys(false)) {
                ConfigurationSection r = section.getConfigurationSection(key);
                if (r == null) continue;
                RarityDefinition def = new RarityDefinition(
                        key.toUpperCase(),
                        r.getString("displayName", key),
                        r.getString("color", "&f"),
                        r.getString("hexColor", "#aaaaaa"),
                        r.getInt("order", 0),
                        r.getString("borderMaterial", "GRAY_STAINED_GLASS_PANE"),
                        r.getString("icon", "⬜"));
                rarities.add(def);
                byId.put(key.toUpperCase(), def);
            }
        }

        rarities.sort(Comparator.comparingInt(RarityDefinition::getOrder));
        Logger.info("Loaded &e" + rarities.size() + " &frarity tiers: "
                + rarities.stream().map(RarityDefinition::getId).collect(Collectors.joining(", ")));
    }

    public RarityDefinition get(String id) {
        if (id == null) return getFallback();
        return byId.getOrDefault(id.toUpperCase(), getFallback());
    }

    public boolean exists(String id) {
        return id != null && byId.containsKey(id.toUpperCase());
    }

    public String getColor(String rarityId)     { return get(rarityId).getColorCode(); }
    public String getHexColor(String rarityId)  { return get(rarityId).getHexColor(); }
    public int    getOrder(String rarityId)     { return get(rarityId).getOrder(); }

    public Material getBorderMaterial(String rarityId) {
        String matName = get(rarityId).getBorderMaterial();
        Material mat = matName != null ? Material.matchMaterial(matName.toUpperCase()) : null;
        return mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
    }

    public List<String> getAllIds() {
        return rarities.stream().map(RarityDefinition::getId).collect(Collectors.toList());
    }

    public List<RarityDefinition> getAll() { return Collections.unmodifiableList(rarities); }

    public boolean isAtOrAbove(String rarityId, String minimumId) {
        return getOrder(rarityId) >= getOrder(minimumId);
    }

    public List<String> getIdsAtOrAbove(String minimumId) {
        int minOrder = getOrder(minimumId);
        return rarities.stream()
                .filter(r -> r.getOrder() >= minOrder)
                .map(RarityDefinition::getId)
                .collect(Collectors.toList());
    }

    public String getHighestRarity(Collection<String> rarityIds) {
        return rarityIds.stream()
                .max(Comparator.comparingInt(this::getOrder))
                .orElse(getLowestId());
    }

    public String getLowestId()  { return rarities.isEmpty() ? "COMMON" : rarities.get(0).getId(); }
    public String getHighestId() { return rarities.isEmpty() ? "LEGENDARY"  : rarities.get(rarities.size() - 1).getId(); }

    public void saveRarities(List<RarityDefinition> updated) {
        File file = new File(plugin.getDataFolder(), "rarities.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (RarityDefinition def : updated) {
            String path = "rarities." + def.getId().toLowerCase();
            config.set(path + ".displayName",    def.getDisplayName());
            config.set(path + ".color",          def.getColor());
            config.set(path + ".hexColor",       def.getHexColor());
            config.set(path + ".order",          def.getOrder());
            config.set(path + ".borderMaterial", def.getBorderMaterial());
            config.set(path + ".icon",           def.getIcon());
        }
        try {
            config.save(file);
            reload();
            Logger.info("Rarities saved and reloaded — &e" + updated.size() + " &ftiers.");
        } catch (Exception e) {
            Logger.severe("Failed to save rarities.yml: " + e.getMessage());
        }
    }

    private void saveDefaultRarities(File file) {
        file.getParentFile().mkdirs();
        try {
            InputStream in = plugin.getResource("rarities.yml");
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                defaults.save(file);
                Logger.info("Created default rarities.yml");
                return;
            }
        } catch (Exception ignored) {}

        // Fallback: tulis default secara programatik jika resource tidak ada
        YamlConfiguration config = new YamlConfiguration();
        for (RarityDefinition def : defaultRarities()) {
            String path = "rarities." + def.getId().toLowerCase();
            config.set(path + ".displayName",    def.getDisplayName());
            config.set(path + ".color",          def.getColor());
            config.set(path + ".hexColor",       def.getHexColor());
            config.set(path + ".order",          def.getOrder());
            config.set(path + ".borderMaterial", def.getBorderMaterial());
            config.set(path + ".icon",           def.getIcon());
        }
        try {
            config.save(file);
            Logger.info("Created default rarities.yml");
        } catch (Exception e) {
            Logger.severe("Failed to create rarities.yml: " + e.getMessage());
        }
    }

    private List<RarityDefinition> defaultRarities() {
        return List.of(
                new RarityDefinition("COMMON",    "Common",    "&#B9B9B9", "#B9B9B9", 0, "GRAY_STAINED_GLASS_PANE",    ""),
                new RarityDefinition("RARE",      "Rare",      "&#00EAE2", "#00EAE2", 1, "BLUE_STAINED_GLASS_PANE",    ""),
                new RarityDefinition("EPIC",      "Epic",      "&#9C00F6", "#9C00F6", 2, "MAGENTA_STAINED_GLASS_PANE", ""),
                new RarityDefinition("LEGENDARY", "Legendary", "&#FFC800", "#FFC800", 3, "ORANGE_STAINED_GLASS_PANE",  ""));
    }

    private RarityDefinition getFallback() {
        return rarities.isEmpty()
                ? new RarityDefinition("COMMON", "Common", "&f", "#aaaaaa", 0, "GRAY_STAINED_GLASS_PANE", "⬜")
                : rarities.get(0);
    }
}