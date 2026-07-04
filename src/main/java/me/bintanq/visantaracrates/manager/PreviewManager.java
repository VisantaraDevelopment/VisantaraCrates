package me.bintanq.visantaracrates.manager;

import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.model.PreviewConfig;
import me.bintanq.visantaracrates.serializer.GsonProvider;
import me.bintanq.visantaracrates.util.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PreviewManager {

    private final VisantaraCrates plugin;
    private final ConcurrentHashMap<String, PreviewConfig> previewsRegistry = new ConcurrentHashMap<>();
    private File previewsDir;

    public PreviewManager(VisantaraCrates plugin) {
        this.plugin = plugin;
        loadAllPreviews();
    }

    public void loadAllPreviews() {
        previewsDir = new File(plugin.getDataFolder(), "previews");
        if (!previewsDir.exists()) {
            previewsDir.mkdirs();
            createExamplePreview();
        }

        previewsRegistry.clear();
        boolean useJson = false;
        String extension = useJson ? ".json" : ".yml";
        String oldExtension = useJson ? ".yml" : ".json";

        // Bidirectional auto-migration
        File[] oldFiles = previewsDir.listFiles((dir, name) -> name.endsWith(oldExtension));
        if (oldFiles != null) {
            for (File oldFile : oldFiles) {
                try {
                    PreviewConfig cfg = null;
                    if (useJson) {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(oldFile);
                        Map<String, Object> map = sectionToMap(yaml);
                        String json = GsonProvider.getGson().toJson(map);
                        cfg = GsonProvider.getGson().fromJson(json, PreviewConfig.class);
                    } else {
                        try (FileReader reader = new FileReader(oldFile, StandardCharsets.UTF_8)) {
                            cfg = GsonProvider.getGson().fromJson(reader, PreviewConfig.class);
                        }
                    }
                    if (cfg != null) {
                        String id = oldFile.getName().replace(oldExtension, "").toLowerCase();
                        savePreview(id, cfg);
                        oldFile.delete();
                        Logger.info("Migrated preview file: " + oldFile.getName() + " -> " + id + extension);
                    }
                } catch (Exception e) {
                    Logger.severe("Failed to migrate preview file '" + oldFile.getName() + "': " + e.getMessage());
                }
            }
        }

        File[] files = previewsDir.listFiles((dir, name) -> name.endsWith(extension));
        if (files == null || files.length == 0) {
            Logger.warn("No preview configs found in /previews/.");
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                PreviewConfig cfg;
                if (useJson) {
                    try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                        cfg = GsonProvider.getGson().fromJson(reader, PreviewConfig.class);
                    }
                } else {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    Map<String, Object> map = sectionToMap(yaml);
                    String json = GsonProvider.getGson().toJson(map);
                    cfg = GsonProvider.getGson().fromJson(json, PreviewConfig.class);
                }

                if (cfg != null) {
                    String id = file.getName().replace(extension, "").toLowerCase();
                    previewsRegistry.put(id, cfg);
                    loaded++;
                    Logger.debug("Loaded preview config: &e" + id);
                }
            } catch (Exception e) {
                Logger.severe("Failed to load preview file '" + file.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        Logger.info("Loaded &e" + loaded + " &fpreview configurations.");
    }

    public PreviewConfig getPreviewConfig(String id) {
        if (id == null) return null;
        return previewsRegistry.get(id.toLowerCase());
    }

    public void savePreview(String id, PreviewConfig cfg) {
        boolean useJson = false;
        String extension = useJson ? ".json" : ".yml";
        File file = new File(previewsDir, id + extension);
        try {
            if (useJson) {
                try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                    GsonProvider.getGson().toJson(cfg, writer);
                }
            } else {
                YamlConfiguration yaml = new YamlConfiguration();
                String json = GsonProvider.getGson().toJson(cfg);
                Map<String, Object> map = GsonProvider.getGson().fromJson(json, new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
                populateSection(yaml, map);
                yaml.save(file);
            }
            previewsRegistry.put(id.toLowerCase(), cfg);
        } catch (IOException e) {
            Logger.severe("Failed to save preview config '" + id + "': " + e.getMessage());
        }
    }

    private void createExamplePreview() {
        boolean useJson = false;
        String extension = useJson ? ".json" : ".yml";
        File example = new File(previewsDir, "default" + extension);

        String json = """
        {
          "title": "&0&lPreview &8» &e{crate} &7[Page {page}/{pages}]",
          "sortOrder": "RARITY_DESC",
          "borderMaterial": "GRAY_STAINED_GLASS_PANE",
          "showPity": true,
          "showKeyBalance": true,
          "showChance": true,
          "showWeight": false,
          "chanceFormat": "&7Chance: &e{chance}%",
          "prevButtonMaterial": "ARROW",
          "nextButtonMaterial": "ARROW",
          "closeButtonMaterial": "BARRIER",
          "rewardFooterLore": [],
          "showActualItem": true
        }
        """;

        try {
            PreviewConfig cfg = GsonProvider.getGson().fromJson(json, PreviewConfig.class);
            if (useJson) {
                try (FileWriter w = new FileWriter(example, StandardCharsets.UTF_8)) {
                    GsonProvider.getGson().toJson(cfg, w);
                }
            } else {
                YamlConfiguration yaml = new YamlConfiguration();
                Map<String, Object> map = GsonProvider.getGson().fromJson(json, new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
                populateSection(yaml, map);
                yaml.save(example);
            }
        } catch (IOException e) {
            Logger.severe("Failed to create example preview config: " + e.getMessage());
        }
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
