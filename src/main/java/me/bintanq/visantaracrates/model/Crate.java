package me.bintanq.visantaracrates.model;

import com.google.gson.annotations.SerializedName;
import me.bintanq.visantaracrates.model.reward.Reward;
import me.bintanq.visantaracrates.scheduler.CrateSchedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crate — the full definition of a crate, GSON-serializable.
 *
 * Stored as /plugins/VisantaraCrates/crates/{id}.json and synced
 * to the Web Interface via WebSocket.
 */
public class Crate {

    @SerializedName("id")
    private String id;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("hologramLines")
    private List<String> hologramLines = new ArrayList<>();

    @SerializedName("hologramHeight")
    private double hologramHeight = 1.2;

    @SerializedName("locations")
    private List<SerializableLocation> locations = new ArrayList<>();



    /** All possible rewards from this crate. */
    @SerializedName("rewards")
    private List<Reward> rewards = new ArrayList<>();

    /** Cooldown in milliseconds between openings per player. 0 = no cooldown. */
    @SerializedName("cooldownMs")
    private long cooldownMs = 0;

    /** Pity configuration for this crate. */
    @SerializedName("pity")
    private PityConfig pity = new PityConfig();

    /** Optional scheduling: when this crate is openable. Null = always open. */
    @SerializedName("schedule")
    private CrateSchedule schedule = null;

    /** Preview GUI configuration — customizable per crate. */
    @SerializedName("preview")
    private PreviewConfig preview = new PreviewConfig();

    /** Centralized preview configuration ID. */
    @SerializedName("previewId")
    private String previewId = null;

    /** Whether mass-open is allowed for this crate. */
    @SerializedName("massOpenEnabled")
    private boolean massOpenEnabled = true;

    /** Max openings per mass-open call. -1 = unlimited. */
    @SerializedName("massOpenLimit")
    private int massOpenLimit = -1;

    @SerializedName("openRateLimit")
    private int openRateLimit = 0;

    @SerializedName("lifetimeOpenLimit")
    private int lifetimeOpenLimit = 0;

    @SerializedName("accessDeniedKnockback")
    private boolean accessDeniedKnockback = false;

    @SerializedName("knockbackStrength")
    private double knockbackStrength = 0.6;


    // NOTE: idleParticle and openParticle (legacy string fields) removed.
    // Particle type and effect are now fully defined inside idleAnimation and openAnimation.

    @SerializedName("idleAnimation")
    private AnimationConfig idleAnimation = new AnimationConfig();

    @SerializedName("openAnimation")
    private AnimationConfig openAnimation = new AnimationConfig();

    public static class AnimationConfig {
        @SerializedName("type")
        private String type = "SIMPLE";

        @SerializedName("particle")
        private String particle = "HAPPY_VILLAGER";

        public String getType()     { return type; }
        public String getParticle() { return particle; }

    }

    @SerializedName("guiAnimation")
    private GuiAnimationType guiAnimation = GuiAnimationType.ROULETTE;

    @SerializedName("guiAnimationSpeed")
    private double guiAnimationSpeed = 1.0;

    @SerializedName("particleAnimationSpeed")
    private double particleAnimationSpeed = 1.0;

    @SerializedName("openSound")
    private String openSound = "BLOCK_NOTE_BLOCK_HAT";

    @SerializedName("winSound")
    private String winSound = "UI_TOAST_CHALLENGE_COMPLETE";

    public AnimationConfig getIdleAnimation() { return idleAnimation != null ? idleAnimation : new AnimationConfig(); }
    public AnimationConfig getOpenAnimation() { return openAnimation != null ? openAnimation : new AnimationConfig(); }

    @SerializedName("enabled")
    private boolean enabled = true;

    /* ─── Stateful Physical Crate GUI Fields ─── */
    @SerializedName("crate-type")
    private String crateType = "FREE";

    @SerializedName("sound-on-open")
    private String soundOnOpen = "BLOCK_CHEST_OPEN";

    @SerializedName("sound-on-select")
    private String soundOnSelect = "BLOCK_NOTE_BLOCK_BELL";

    @SerializedName("sound-on-deselect")
    private String soundOnDeselect = "BLOCK_NOTE_BLOCK_HAT";

    @SerializedName("sound-on-transition")
    private String soundOnTransition = "BLOCK_CHEST_OPEN";

    @SerializedName("sound-on-reroll")
    private String soundOnReroll = "BLOCK_ANVIL_USE";

    @SerializedName("sound-on-claim")
    private String soundOnClaim = "ENTITY_PLAYER_LEVELUP";

    @SerializedName("sound-on-click")
    private String soundOnClick = "UI_BUTTON_CLICK";

    @SerializedName("transition-ticks")
    private int transitionTicks = 40;

    @SerializedName("max-rerolls")
    private int maxRerolls = 2;

    @SerializedName("chest-background")
    private ChestBackgroundConfig chestBackground = new ChestBackgroundConfig();

    @SerializedName("titles")
    private TitlesConfig titles = new TitlesConfig();

    @SerializedName("slots")
    private SlotsConfig slots = new SlotsConfig();

    @SerializedName("buttons")
    private ButtonsConfig buttons = new ButtonsConfig();

    @SerializedName("claim-commands")
    private List<String> claimCommands = new ArrayList<>();

    @SerializedName("physical-item")
    private PhysicalItemConfig physicalItem = new PhysicalItemConfig();

    @SerializedName("physical-items")
    private PhysicalItemsConfig physicalItems;

    public String getCrateType() { return crateType != null ? crateType : "FREE"; }
    public String getSoundOnOpen() { return soundOnOpen != null ? soundOnOpen : "BLOCK_CHEST_OPEN"; }
    public String getSoundOnSelect() { return soundOnSelect != null ? soundOnSelect : "BLOCK_NOTE_BLOCK_BELL"; }
    public String getSoundOnDeselect() { return soundOnDeselect != null ? soundOnDeselect : "BLOCK_NOTE_BLOCK_HAT"; }
    public String getSoundOnTransition() { return soundOnTransition != null ? soundOnTransition : "BLOCK_CHEST_OPEN"; }
    public String getSoundOnReroll() { return soundOnReroll != null ? soundOnReroll : "BLOCK_ANVIL_USE"; }
    public String getSoundOnClaim() { return soundOnClaim != null ? soundOnClaim : "ENTITY_PLAYER_LEVELUP"; }
    public String getSoundOnClick() { return soundOnClick != null ? soundOnClick : "UI_BUTTON_CLICK"; }
    public int getTransitionTicks() { return transitionTicks <= 0 ? 40 : transitionTicks; }
    public int getMaxRerolls() { return maxRerolls < 0 ? 2 : maxRerolls; }
    public TitlesConfig getTitles() { return titles != null ? titles : new TitlesConfig(); }
    public SlotsConfig getSlots() { return slots != null ? slots : new SlotsConfig(); }
    public ButtonsConfig getButtons() { return buttons != null ? buttons : new ButtonsConfig(); }
    public List<String> getClaimCommands() { return claimCommands != null ? claimCommands : new ArrayList<>(); }
    public ChestBackgroundConfig getChestBackground() { return chestBackground != null ? chestBackground : new ChestBackgroundConfig(); }
    /** Legacy flat getter — returns the flat physical-item config. */
    public PhysicalItemConfig getPhysicalItem() { return physicalItem != null ? physicalItem : new PhysicalItemConfig(); }
    /** Type-aware getter — checks nested physical-items.free/premium first, then falls back to flat physical-item. */
    public PhysicalItemConfig getPhysicalItem(boolean isPremium) {
        if (physicalItems != null) {
            PhysicalItemConfig typed = isPremium ? physicalItems.getPremium() : physicalItems.getFree();
            if (typed != null) return typed;
        }
        return getPhysicalItem();
    }

    public static class ChestBackgroundConfig {
        @SerializedName("closed")
        private String closed = "";

        @SerializedName("open")
        private String open = "";

        @SerializedName("shift")
        private int shift = -8;

        public String getClosed() { return closed != null ? closed : ""; }
        public String getOpen() { return open != null ? open : ""; }
        public int getShift() { return shift; }
    }

    public static class TitleSet {
        @SerializedName("closed")
        private String closed;

        @SerializedName("open")
        private String open;

        @SerializedName("claim-off")
        private String claimOff;

        @SerializedName("claim-on")
        private String claimOn;

        @SerializedName("reroll-1")
        private String reroll1;

        @SerializedName("reroll-2")
        private String reroll2;

        @SerializedName("reroll-off")
        private String rerollOff;

        @SerializedName("claim-on-reroll-2")
        private String claimOnReroll2;

        @SerializedName("claim-on-reroll-1")
        private String claimOnReroll1;

        @SerializedName("claim-on-reroll-off")
        private String claimOnRerollOff;

        @SerializedName("claim-off-reroll-2")
        private String claimOffReroll2;

        @SerializedName("claim-off-reroll-1")
        private String claimOffReroll1;

        @SerializedName("claim-off-reroll-off")
        private String claimOffRerollOff;

        public String getClosed() { return closed; }
        public String getOpen() { return open; }
        public String getClaimOff() { return claimOff; }
        public String getClaimOn() { return claimOn; }
        public String getReroll1() { return reroll1; }
        public String getReroll2() { return reroll2; }
        public String getRerollOff() { return rerollOff; }
        public String getClaimOnReroll2() { return claimOnReroll2; }
        public String getClaimOnReroll1() { return claimOnReroll1; }
        public String getClaimOnRerollOff() { return claimOnRerollOff; }
        public String getClaimOffReroll2() { return claimOffReroll2; }
        public String getClaimOffReroll1() { return claimOffReroll1; }
        public String getClaimOffRerollOff() { return claimOffRerollOff; }
    }

    public static class TitlesConfig {
        @SerializedName("free")
        private TitleSet free = new TitleSet();

        @SerializedName("premium")
        private TitleSet premium = new TitleSet();

        @SerializedName("closed")
        private String closed = "CHEST_CLOSED";

        @SerializedName("open")
        private String open = "CHEST_OPEN";

        @SerializedName("claim-off")
        private String claimOff = "CLAIM_OFF";

        @SerializedName("claim-on")
        private String claimOn = "CLAIM_ON";

        @SerializedName("reroll-1")
        private String reroll1 = "REROLL_1";

        @SerializedName("reroll-2")
        private String reroll2 = "REROLL_2";

        @SerializedName("reroll-off")
        private String rerollOff = "REROLL_OFF";

        @SerializedName("claim-on-reroll-2")
        private String claimOnReroll2;

        @SerializedName("claim-on-reroll-1")
        private String claimOnReroll1;

        @SerializedName("claim-on-reroll-off")
        private String claimOnRerollOff;

        @SerializedName("claim-off-reroll-2")
        private String claimOffReroll2;

        @SerializedName("claim-off-reroll-1")
        private String claimOffReroll1;

        @SerializedName("claim-off-reroll-off")
        private String claimOffRerollOff;

        public String getClosed(boolean isPremium) {
            if (isPremium && premium != null && premium.getClosed() != null) return premium.getClosed();
            if (!isPremium && free != null && free.getClosed() != null) return free.getClosed();
            return closed != null ? closed : "CHEST_CLOSED";
        }

        public String getOpen(boolean isPremium) {
            if (isPremium && premium != null && premium.getOpen() != null) return premium.getOpen();
            if (!isPremium && free != null && free.getOpen() != null) return free.getOpen();
            return open != null ? open : "CHEST_OPEN";
        }

        public String getClaimOff(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOff() != null) return premium.getClaimOff();
            if (!isPremium && free != null && free.getClaimOff() != null) return free.getClaimOff();
            return claimOff != null ? claimOff : "CLAIM_OFF";
        }

        public String getClaimOn(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOn() != null) return premium.getClaimOn();
            if (!isPremium && free != null && free.getClaimOn() != null) return free.getClaimOn();
            return claimOn != null ? claimOn : "CLAIM_ON";
        }

        public String getReroll1(boolean isPremium) {
            if (isPremium && premium != null && premium.getReroll1() != null) return premium.getReroll1();
            if (!isPremium && free != null && free.getReroll1() != null) return free.getReroll1();
            return reroll1 != null ? reroll1 : "REROLL_1";
        }

        public String getReroll2(boolean isPremium) {
            if (isPremium && premium != null && premium.getReroll2() != null) return premium.getReroll2();
            if (!isPremium && free != null && free.getReroll2() != null) return free.getReroll2();
            return reroll2 != null ? reroll2 : "REROLL_2";
        }

        public String getRerollOff(boolean isPremium) {
            if (isPremium && premium != null && premium.getRerollOff() != null) return premium.getRerollOff();
            if (!isPremium && free != null && free.getRerollOff() != null) return free.getRerollOff();
            return rerollOff != null ? rerollOff : "REROLL_OFF";
        }

        /** Combined claim-on + reroll state resolvers for premium */
        public String getClaimOnReroll2(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOnReroll2() != null) return premium.getClaimOnReroll2();
            if (!isPremium && free != null && free.getClaimOnReroll2() != null) return free.getClaimOnReroll2();
            if (claimOnReroll2 != null) return claimOnReroll2;
            return getClaimOn(isPremium);
        }

        public String getClaimOnReroll1(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOnReroll1() != null) return premium.getClaimOnReroll1();
            if (!isPremium && free != null && free.getClaimOnReroll1() != null) return free.getClaimOnReroll1();
            if (claimOnReroll1 != null) return claimOnReroll1;
            return getClaimOn(isPremium);
        }

        public String getClaimOnRerollOff(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOnRerollOff() != null) return premium.getClaimOnRerollOff();
            if (!isPremium && free != null && free.getClaimOnRerollOff() != null) return free.getClaimOnRerollOff();
            if (claimOnRerollOff != null) return claimOnRerollOff;
            return getClaimOn(isPremium);
        }

        /** Combined claim-off + reroll state resolvers for premium */
        public String getClaimOffReroll2(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOffReroll2() != null) return premium.getClaimOffReroll2();
            if (!isPremium && free != null && free.getClaimOffReroll2() != null) return free.getClaimOffReroll2();
            if (claimOffReroll2 != null) return claimOffReroll2;
            return getReroll2(isPremium);
        }

        public String getClaimOffReroll1(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOffReroll1() != null) return premium.getClaimOffReroll1();
            if (!isPremium && free != null && free.getClaimOffReroll1() != null) return free.getClaimOffReroll1();
            if (claimOffReroll1 != null) return claimOffReroll1;
            return getReroll1(isPremium);
        }

        public String getClaimOffRerollOff(boolean isPremium) {
            if (isPremium && premium != null && premium.getClaimOffRerollOff() != null) return premium.getClaimOffRerollOff();
            if (!isPremium && free != null && free.getClaimOffRerollOff() != null) return free.getClaimOffRerollOff();
            if (claimOffRerollOff != null) return claimOffRerollOff;
            return getRerollOff(isPremium);
        }

        public String getClosed() { return getClosed(false); }
        public String getOpen() { return getOpen(false); }
        public String getClaimOff() { return getClaimOff(false); }
        public String getClaimOn() { return getClaimOn(false); }
        public String getReroll1() { return getReroll1(false); }
        public String getReroll2() { return getReroll2(false); }
        public String getRerollOff() { return getRerollOff(false); }
    }

    /**
     * Per-type (free/premium) slot configuration.
     */
    public static class TypeSlotsConfig {
        @SerializedName("reward-slots")
        private List<Integer> rewardSlots;

        @SerializedName("open-slots")
        private List<Integer> openSlots;

        @SerializedName("claim-slots")
        private List<Integer> claimSlots;

        @SerializedName("reroll-slots")
        private List<Integer> rerollSlots;

        public List<Integer> getRewardSlots() { return rewardSlots; }
        public List<Integer> getOpenSlots() { return openSlots; }
        public List<Integer> getClaimSlots() { return claimSlots; }
        public List<Integer> getRerollSlots() { return rerollSlots; }
    }

    public static class SlotsConfig {
        // Legacy flat fields (backward compatibility)
        @SerializedName("reward-slots")
        private List<Integer> rewardSlots = java.util.Arrays.asList(12, 13, 15);

        @SerializedName("open-slot")
        private int openSlot = 31;

        @SerializedName("open-slots")
        private List<Integer> openSlots = new ArrayList<>();

        @SerializedName("claim-slot")
        private int claimSlot = 31;

        @SerializedName("claim-slots")
        private List<Integer> claimSlots = new ArrayList<>();

        @SerializedName("reroll-slot")
        private int rerollSlot = 32;

        @SerializedName("reroll-slots")
        private List<Integer> rerollSlots = new ArrayList<>();

        // New per-type nested configs
        @SerializedName("free")
        private TypeSlotsConfig free;

        @SerializedName("premium")
        private TypeSlotsConfig premium;

        // ─── Legacy flat getters (backward compat, default to non-premium) ───
        public List<Integer> getRewardSlots() { return getRewardSlots(false); }
        public int getOpenSlot() { return openSlot == 0 ? 31 : openSlot; }
        public int getClaimSlot() { return claimSlot == 0 ? 31 : claimSlot; }
        public int getRerollSlot() { return rerollSlot == 0 ? 32 : rerollSlot; }

        public List<Integer> getOpenSlots() { return getOpenSlots(false); }
        public List<Integer> getClaimSlots() { return getClaimSlots(false); }
        public List<Integer> getRerollSlots() { return getRerollSlots(false); }

        // ─── Per-type getters ───
        private TypeSlotsConfig getTypeConfig(boolean isPremium) {
            return isPremium ? premium : free;
        }

        public List<Integer> getRewardSlots(boolean isPremium) {
            TypeSlotsConfig tc = getTypeConfig(isPremium);
            if (tc != null && tc.getRewardSlots() != null && !tc.getRewardSlots().isEmpty()) {
                return tc.getRewardSlots();
            }
            return rewardSlots != null ? rewardSlots : java.util.Arrays.asList(12, 13, 15);
        }

        public List<Integer> getOpenSlots(boolean isPremium) {
            TypeSlotsConfig tc = getTypeConfig(isPremium);
            if (tc != null && tc.getOpenSlots() != null && !tc.getOpenSlots().isEmpty()) {
                return tc.getOpenSlots();
            }
            // Legacy fallback
            List<Integer> list = new ArrayList<>();
            if (openSlots != null && !openSlots.isEmpty()) {
                list.addAll(openSlots);
            } else {
                list.add(openSlot == 0 ? 31 : openSlot);
            }
            return list;
        }

        public List<Integer> getClaimSlots(boolean isPremium) {
            TypeSlotsConfig tc = getTypeConfig(isPremium);
            if (tc != null && tc.getClaimSlots() != null && !tc.getClaimSlots().isEmpty()) {
                return tc.getClaimSlots();
            }
            // Legacy fallback
            List<Integer> list = new ArrayList<>();
            if (claimSlots != null && !claimSlots.isEmpty()) {
                list.addAll(claimSlots);
            } else {
                list.add(claimSlot == 0 ? 31 : claimSlot);
            }
            return list;
        }

        public List<Integer> getRerollSlots(boolean isPremium) {
            TypeSlotsConfig tc = getTypeConfig(isPremium);
            if (tc != null && tc.getRerollSlots() != null && !tc.getRerollSlots().isEmpty()) {
                return tc.getRerollSlots();
            }
            // Legacy fallback
            List<Integer> list = new ArrayList<>();
            if (rerollSlots != null && !rerollSlots.isEmpty()) {
                list.addAll(rerollSlots);
            } else {
                list.add(rerollSlot == 0 ? 32 : rerollSlot);
            }
            return list;
        }
    }

    public static class GUIButton {
        @SerializedName("nexoId")
        private String nexoId = "";

        @SerializedName("material")
        private String material = "PAPER";

        @SerializedName("customModelData")
        private int customModelData = -1;

        @SerializedName("displayName")
        private String displayName = "";

        @SerializedName("lore")
        private List<String> lore = new ArrayList<>();

        public String getNexoId() { return nexoId != null ? nexoId : ""; }
        public String getMaterial() { return material != null ? material : "PAPER"; }
        public int getCustomModelData() { return customModelData; }
        public String getDisplayName() { return displayName != null ? displayName : ""; }
        public List<String> getLore() { return lore != null ? lore : new ArrayList<>(); }
    }

    public static class ButtonsConfig {
        @SerializedName("open-button")
        private GUIButton openButton = new GUIButton();

        @SerializedName("open-button-inactive")
        private GUIButton openButtonInactive = new GUIButton();

        @SerializedName("open-button-shift")
        private int openButtonShift = 0;

        @SerializedName("claim-button-enabled")
        private GUIButton claimButtonEnabled = new GUIButton();

        @SerializedName("claim-button-disabled")
        private GUIButton claimButtonDisabled = new GUIButton();

        @SerializedName("claim-button-shift")
        private int claimButtonShift = 0;

        @SerializedName("select-button-selected")
        private GUIButton selectButtonSelected = new GUIButton();

        @SerializedName("reroll-button")
        private GUIButton rerollButton = new GUIButton();

        @SerializedName("reroll-button-1")
        private GUIButton rerollButton1 = new GUIButton();

        @SerializedName("reroll-button-disabled")
        private GUIButton rerollButtonDisabled = new GUIButton();

        @SerializedName("reroll-button-shift")
        private int rerollButtonShift = 0;

        @SerializedName("gui-filler")
        private GUIButton guiFiller = new GUIButton();

        @SerializedName("reward-slot-item")
        private GUIButton rewardSlotItem;

        @SerializedName("open-slot-item")
        private GUIButton openSlotItem;

        @SerializedName("claim-slot-item")
        private GUIButton claimSlotItem;

        @SerializedName("reroll-slot-item")
        private GUIButton rerollSlotItem;

        public GUIButton getOpenButton() { return openButton != null ? openButton : new GUIButton(); }
        public GUIButton getOpenButtonInactive() { return openButtonInactive != null ? openButtonInactive : new GUIButton(); }
        public int getOpenButtonShift() { return openButtonShift; }
        public GUIButton getClaimButtonEnabled() { return claimButtonEnabled != null ? claimButtonEnabled : new GUIButton(); }
        public GUIButton getClaimButtonDisabled() { return claimButtonDisabled != null ? claimButtonDisabled : new GUIButton(); }
        public int getClaimButtonShift() { return claimButtonShift; }
        public GUIButton getSelectButtonSelected() { return selectButtonSelected != null ? selectButtonSelected : new GUIButton(); }
        public GUIButton getRerollButton() { return rerollButton != null ? rerollButton : new GUIButton(); }
        public GUIButton getRerollButton1() { return rerollButton1 != null ? rerollButton1 : new GUIButton(); }
        public GUIButton getRerollButtonDisabled() { return rerollButtonDisabled != null ? rerollButtonDisabled : new GUIButton(); }
        public int getRerollButtonShift() { return rerollButtonShift; }
        public GUIButton getGuiFiller() { return guiFiller != null ? guiFiller : new GUIButton(); }
        /** Slot-specific placeholder items — fall back to gui-filler if not configured. */
        public GUIButton getRewardSlotItem() { return rewardSlotItem != null ? rewardSlotItem : getGuiFiller(); }
        public GUIButton getOpenSlotItem() { return openSlotItem != null ? openSlotItem : getGuiFiller(); }
        public GUIButton getClaimSlotItem() { return claimSlotItem != null ? claimSlotItem : getGuiFiller(); }
        public GUIButton getRerollSlotItem() { return rerollSlotItem != null ? rerollSlotItem : getGuiFiller(); }
    }

    public enum GuiAnimationType {
        ROULETTE, SHUFFLER, BOUNDARY, SINGLE_SPIN, FLICKER, CHOICE
    }

    public static class PityConfig {
        @SerializedName("enabled")
        private boolean enabled = false;

        @SerializedName("threshold")
        private int threshold = 50;

        @SerializedName("rareRarityMinimum")
        private String rareRarityMinimum = "RARE";

        @SerializedName("bonusChancePerOpen")
        private double bonusChancePerOpen = 2.0;

        @SerializedName("softPityStart")
        private int softPityStart = 40;

        public boolean isEnabled() { return enabled; }
        public int getThreshold() { return threshold; }
        public String getRareRarityMinimum() { return rareRarityMinimum; }
        public double getBonusChancePerOpen() { return bonusChancePerOpen; }
        public int getSoftPityStart() { return softPityStart; }
    }



    public static class SerializableLocation {
        @SerializedName("world")  public String world;
        @SerializedName("x")      public double x;
        @SerializedName("y")      public double y;
        @SerializedName("z")      public double z;
        @SerializedName("yaw")    public float yaw;
        @SerializedName("pitch")  public float pitch;

        public SerializableLocation() {}
        public SerializableLocation(String world, double x, double y, double z) {
            this.world = world; this.x = x; this.y = y; this.z = z;
        }
    }

    @SerializedName("location")
    @com.google.gson.annotations.Expose(serialize = false, deserialize = true)
    private SerializableLocation _legacyLocation = null;

    public void migrateLegacyLocation() {
        if (_legacyLocation != null) {
            addLocation(_legacyLocation);
            _legacyLocation = null;
        }
    }

    /* ─────────────────────── Computed Helpers ─────────────────────── */

    public double getTotalWeight() {
        return rewards.stream().mapToDouble(Reward::getWeight).sum();
    }

    public boolean isCurrentlyOpenable() {
        if (schedule == null) return true;
        return schedule.isCurrentlyActive();
    }

    public boolean hasLocationAt(String world, int bx, int by, int bz) {
        return getLocations().stream().anyMatch(loc ->
                loc != null
                        && loc.world != null
                        && loc.world.equals(world)
                        && (int) loc.x == bx
                        && (int) loc.y == by
                        && (int) loc.z == bz);
    }

    public boolean addLocation(SerializableLocation loc) {
        if (hasLocationAt(loc.world, (int) loc.x, (int) loc.y, (int) loc.z)) return false;
        getLocations().add(loc);
        return true;
    }

    public boolean removeLocation(int index) {
        if (index < 0 || index >= getLocations().size()) return false;
        getLocations().remove(index);
        return true;
    }

    /* ─────────────────────── Getters / Setters ─────────────────────── */


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<String> getHologramLines() { return hologramLines; }
    public List<SerializableLocation> getLocations() {
        if (locations == null) locations = new ArrayList<>();
        return locations;
    }
    public double getHologramHeight() { return hologramHeight; }
    public void setLocations(List<SerializableLocation> locations) {
        this.locations = locations;
    }


    public boolean isAccessDeniedKnockback() { return accessDeniedKnockback; }
    public void setAccessDeniedKnockback(boolean v) { this.accessDeniedKnockback = v; }
    public double getKnockbackStrength() { return Math.max(0, Math.min(knockbackStrength, 3.0)); }
    public void setKnockbackStrength(double v) { this.knockbackStrength = v; }


    public List<Reward> getRewards() { return rewards; }

    public long getCooldownMs() { return cooldownMs; }
    public void setCooldownMs(long cooldownMs) { this.cooldownMs = cooldownMs; }

    public PityConfig getPity() { return pity; }
    public CrateSchedule getSchedule() { return schedule; }
    public void setSchedule(CrateSchedule schedule) { this.schedule = schedule; }

    public PreviewConfig getPreview() { return preview != null ? preview : new PreviewConfig(); }
    public String getPreviewId() { return previewId; }
    public void setPreviewId(String previewId) { this.previewId = previewId; }
    public boolean isMassOpenEnabled() {
        if (crateType != null && (crateType.equalsIgnoreCase("FREE") || crateType.equalsIgnoreCase("PREMIUM"))) {
            return false;
        }
        return massOpenEnabled;
    }
    public int getMassOpenLimit() { return massOpenLimit; }

    public int getOpenRateLimit()       { return openRateLimit; }
    public void setOpenRateLimit(int v) { this.openRateLimit = v; }
    public int getLifetimeOpenLimit()       { return lifetimeOpenLimit; }
    public void setLifetimeOpenLimit(int v) { this.lifetimeOpenLimit = v; }

    public GuiAnimationType getGuiAnimation() { return guiAnimation != null ? guiAnimation : GuiAnimationType.ROULETTE; }
    public void setGuiAnimation(GuiAnimationType guiAnimation) { this.guiAnimation = guiAnimation; }

    public double getGuiAnimationSpeed() { return guiAnimationSpeed > 0 ? guiAnimationSpeed : 1.0; }
    public void setGuiAnimationSpeed(double guiAnimationSpeed) { this.guiAnimationSpeed = guiAnimationSpeed; }

    public double getParticleAnimationSpeed() { return particleAnimationSpeed > 0 ? particleAnimationSpeed : 1.0; }
    public void setParticleAnimationSpeed(double particleAnimationSpeed) { this.particleAnimationSpeed = particleAnimationSpeed; }

    public String getOpenSound() { return openSound != null ? openSound : "BLOCK_NOTE_BLOCK_HAT"; }
    public void setOpenSound(String openSound) { this.openSound = openSound; }

    public String getWinSound() { return winSound != null ? winSound : "UI_TOAST_CHALLENGE_COMPLETE"; }
    public void setWinSound(String winSound) { this.winSound = winSound; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public static class PhysicalItemConfig {
        @SerializedName("material")
        private String material = "CHEST";

        @SerializedName("nexoId")
        private String nexoId = "";

        @SerializedName("customModelData")
        private int customModelData = -1;

        @SerializedName("displayName")
        private String displayName = "&6{crate} &8(&7{type}&8)";

        @SerializedName("lore")
        private List<String> lore = new java.util.ArrayList<>();

        public String getMaterial() { return material != null ? material : "CHEST"; }
        public String getNexoId() { return nexoId != null ? nexoId : ""; }
        public int getCustomModelData() { return customModelData; }
        public String getDisplayName() { return displayName != null ? displayName : "&6{crate} &8(&7{type}&8)"; }
        public List<String> getLore() { return lore != null ? lore : new java.util.ArrayList<>(); }
    }

    /** Nested per-type physical item config (free/premium). */
    public static class PhysicalItemsConfig {
        @SerializedName("free")
        private PhysicalItemConfig free;

        @SerializedName("premium")
        private PhysicalItemConfig premium;

        public PhysicalItemConfig getFree() { return free; }
        public PhysicalItemConfig getPremium() { return premium; }
    }
}