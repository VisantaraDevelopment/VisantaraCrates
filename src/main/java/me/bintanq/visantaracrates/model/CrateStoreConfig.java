package me.bintanq.visantaracrates.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration model for the Crate Store Preview GUI.
 * Deserialized from previews/crate-store.yml via GSON.
 */
public class CrateStoreConfig {

    @SerializedName("title")
    private String title = "&0&lPreview &8» &e{crate}";

    @SerializedName("titles")
    private Map<String, String> titles = new HashMap<>();

    @SerializedName("hide-buttons-for")
    private List<String> hideButtonsFor = new ArrayList<>();

    @SerializedName("crate-rows")
    private Map<String, Integer> crateRows = new HashMap<>();

    @SerializedName("rows")
    private int rows = 6;

    @SerializedName("store-icon")
    private StoreIconConfig storeIcon = new StoreIconConfig();

    @SerializedName("store-icons")
    private Map<String, StoreIconConfig> storeIcons = new HashMap<>();

    @SerializedName("reward-slots")
    private String rewardSlots = "11-15, 20-24, 29-33";

    @SerializedName("arrow-up")
    private ArrowConfig arrowUp = new ArrowConfig();

    @SerializedName("arrow-down")
    private ArrowConfig arrowDown = new ArrowConfig();

    @SerializedName("buy-button")
    private ButtonConfig buyButton = new ButtonConfig();

    @SerializedName("exchange-button")
    private ButtonConfig exchangeButton = new ButtonConfig();

    @SerializedName("filler")
    private FillerConfig filler = new FillerConfig();

    @SerializedName("reward")
    private RewardDisplayConfig reward = new RewardDisplayConfig();

    /* ─────────────────────── Getters ─────────────────────── */

    public String getTitle() { return title != null ? title : "&0&lPreview &8» &e{crate}"; }
    public Map<String, String> getTitles() { return titles != null ? titles : new HashMap<>(); }
    public List<String> getHideButtonsFor() { return hideButtonsFor != null ? hideButtonsFor : new ArrayList<>(); }
    public Map<String, Integer> getCrateRows() { return crateRows != null ? crateRows : new HashMap<>(); }
    public int getRows() { return rows <= 0 ? 6 : rows; }
    public StoreIconConfig getStoreIcon() { return storeIcon != null ? storeIcon : new StoreIconConfig(); }
    public Map<String, StoreIconConfig> getStoreIcons() { return storeIcons != null ? storeIcons : new HashMap<>(); }
    public String getRewardSlots() { return rewardSlots != null ? rewardSlots : "11-15, 20-24, 29-33"; }
    public ArrowConfig getArrowUp() { return arrowUp != null ? arrowUp : new ArrowConfig(); }
    public ArrowConfig getArrowDown() { return arrowDown != null ? arrowDown : new ArrowConfig(); }
    public ButtonConfig getBuyButton() { return buyButton != null ? buyButton : new ButtonConfig(); }
    public ButtonConfig getExchangeButton() { return exchangeButton != null ? exchangeButton : new ButtonConfig(); }
    public FillerConfig getFiller() { return filler != null ? filler : new FillerConfig(); }
    public RewardDisplayConfig getReward() { return reward != null ? reward : new RewardDisplayConfig(); }


    public static class StoreIconConfig {
        @SerializedName("slots")
        private List<Integer> slots = List.of(19);

        @SerializedName("material")
        private String material = "CHEST";

        @SerializedName("nexo-id")
        private String nexoId = "";

        @SerializedName("custom-model-data")
        private int customModelData = -1;

        @SerializedName("display-name")
        private String displayName = "&aCrate Store";

        @SerializedName("lore")
        private List<String> lore = new ArrayList<>();

        public List<Integer> getSlots() { return slots != null ? slots : List.of(19); }
        public String getMaterial() { return material != null ? material : "CHEST"; }
        public String getNexoId() { return nexoId != null ? nexoId : ""; }
        public int getCustomModelData() { return customModelData; }
        public String getDisplayName() { return displayName != null ? displayName : "&aCrate Store"; }
        public List<String> getLore() { return lore != null ? lore : new ArrayList<>(); }
    }

    public static class ArrowConfig {
        @SerializedName("slot")
        private int slot = -1;

        @SerializedName("material")
        private String material = "ARROW";

        @SerializedName("nexo-id")
        private String nexoId = "";

        @SerializedName("display-name")
        private String displayName = "";

        public int getSlot() { return slot; }
        public String getMaterial() { return material != null ? material : "ARROW"; }
        public String getNexoId() { return nexoId != null ? nexoId : ""; }
        public String getDisplayName() { return displayName != null ? displayName : ""; }
    }

    public static class ButtonConfig {
        @SerializedName("slots")
        private List<Integer> slots = new ArrayList<>();

        @SerializedName("material")
        private String material = "PAPER";

        @SerializedName("nexo-id")
        private String nexoId = "";

        @SerializedName("display-name")
        private String displayName = "";

        @SerializedName("lore")
        private List<String> lore = new ArrayList<>();

        @SerializedName("command")
        private String command = "";

        public List<Integer> getSlots() { return slots != null ? slots : new ArrayList<>(); }
        public String getMaterial() { return material != null ? material : "PAPER"; }
        public String getNexoId() { return nexoId != null ? nexoId : ""; }
        public String getDisplayName() { return displayName != null ? displayName : ""; }
        public List<String> getLore() { return lore != null ? lore : new ArrayList<>(); }
        public String getCommand() { return command != null ? command : ""; }
    }

    public static class FillerConfig {
        @SerializedName("material")
        private String material = "GRAY_STAINED_GLASS_PANE";

        @SerializedName("nexo-id")
        private String nexoId = "";

        @SerializedName("display-name")
        private String displayName = "&r";

        public String getMaterial() { return material != null ? material : "GRAY_STAINED_GLASS_PANE"; }
        public String getNexoId() { return nexoId != null ? nexoId : ""; }
        public String getDisplayName() { return displayName != null ? displayName : "&r"; }
    }

    public static class RewardDisplayConfig {
        @SerializedName("name")
        private String name = "&7%reward_name%";

        @SerializedName("lore")
        private List<String> lore = List.of("%reward_lore%");

        public String getName() { return name != null ? name : "&7%reward_name%"; }
        public List<String> getLore() { return lore != null ? lore : new ArrayList<>(); }
    }
}
