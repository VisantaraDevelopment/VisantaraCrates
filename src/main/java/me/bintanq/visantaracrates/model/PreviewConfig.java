package me.bintanq.visantaracrates.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreviewConfig {

    @SerializedName("items")
    private Map<String, MenuItem> items = new HashMap<>();

    @SerializedName("settings")
    private SettingsConfig settings = new SettingsConfig();

    @SerializedName("pagination")
    private PaginationConfig pagination = new PaginationConfig();

    @SerializedName("reward")
    private RewardDisplayConfig reward = new RewardDisplayConfig();

    // Support legacy properties for backward compatibility
    @SerializedName("sortOrder")
    private SortOrder sortOrder = SortOrder.RARITY_DESC;

    @SerializedName("showActualItem")
    private boolean showActualItem = true;

    @SerializedName("borderMaterial")
    private String borderMaterial = null;

    public String getBorderMaterial() { return borderMaterial; }
    public void setBorderMaterial(String bm) { this.borderMaterial = bm; }

    public enum SortOrder {
        RARITY_DESC, RARITY_ASC, WEIGHT_DESC, WEIGHT_ASC, CONFIG_ORDER
    }

    public Map<String, MenuItem> getItems() { return items != null ? items : new HashMap<>(); }
    public void setItems(Map<String, MenuItem> items) { this.items = items; }

    public SettingsConfig getSettings() { return settings != null ? settings : new SettingsConfig(); }
    public void setSettings(SettingsConfig settings) { this.settings = settings; }

    public PaginationConfig getPagination() { return pagination != null ? pagination : new PaginationConfig(); }
    public void setPagination(PaginationConfig pagination) { this.pagination = pagination; }

    public RewardDisplayConfig getReward() { return reward != null ? reward : new RewardDisplayConfig(); }
    public void setReward(RewardDisplayConfig reward) { this.reward = reward; }

    public SortOrder getSortOrder() { return sortOrder != null ? sortOrder : SortOrder.RARITY_DESC; }
    public void setSortOrder(SortOrder o) { this.sortOrder = o; }

    public boolean isShowActualItem() { return showActualItem; }
    public void setShowActualItem(boolean b) { this.showActualItem = b; }

    /* ─────────────────────── Inner Configuration Classes ─────────────────────── */

    public static class SettingsConfig {
        @SerializedName("type")
        private String type = "REWARDS_PREVIEW";

        @SerializedName("title")
        private String title = "&0&lPreview &8» &e{crate} &7[Page {page}/{pages}]";

        @SerializedName("rows")
        private int rows = 6;

        @SerializedName("showPity")
        private boolean showPity = false;

        @SerializedName("showKeyBalance")
        private boolean showKeyBalance = true;

        @SerializedName("showChance")
        private boolean showChance = true;

        @SerializedName("showWeight")
        private boolean showWeight = false;

        @SerializedName("chanceFormat")
        private String chanceFormat = "&7Chance: &e{chance}%";

        public String getType() { return type; }
        public String getTitle() { return title; }
        public void setTitle(String t) { this.title = t; }

        public int getRows() { return rows <= 0 ? 6 : rows; }
        public void setRows(int r) { this.rows = r; }

        public boolean isShowPity() { return showPity; }
        public void setShowPity(boolean b) { this.showPity = b; }

        public boolean isShowKeyBalance() { return showKeyBalance; }
        public boolean isShowChance() { return showChance; }
        public boolean isShowWeight() { return showWeight; }
        public String getChanceFormat() { return chanceFormat != null ? chanceFormat : "&7Chance: &e{chance}%"; }
    }

    public static class PaginationConfig {
        @SerializedName("slots")
        private String slots = "10-16, 19-25, 28-34, 37-43";

        @SerializedName("fill-item")
        private MenuItem fillItem = null;

        @SerializedName("previous-item")
        private MenuItem previousItem = null;

        @SerializedName("next-item")
        private MenuItem nextItem = null;

        public String getSlots() { return slots != null ? slots : "10-16, 19-25, 28-34, 37-43"; }
        public MenuItem getFillItem() { return fillItem; }
        public MenuItem getPreviousItem() { return previousItem; }
        public MenuItem getNextItem() { return nextItem; }
    }

    public static class RewardDisplayConfig {
        @SerializedName("random-mode")
        private RewardTemplate randomMode = new RewardTemplate("&7%reward_name%", List.of("%reward_lore%"));

        @SerializedName("selective-mode")
        private RewardTemplate selectiveMode = new RewardTemplate("&7%reward_name%", List.of("%reward_lore%", "", "&eRequired Keys: &6%required_keys%"));

        public RewardTemplate getRandomMode() { return randomMode; }
        public RewardTemplate getSelectiveMode() { return selectiveMode; }
    }

    public static class RewardTemplate {
        @SerializedName("name")
        private String name;

        @SerializedName("lore")
        private List<String> lore = new ArrayList<>();

        public RewardTemplate() {}
        public RewardTemplate(String name, List<String> lore) {
            this.name = name;
            this.lore = lore;
        }

        public String getName() { return name; }
        public List<String> getLore() { return lore != null ? lore : new ArrayList<>(); }
    }
}
