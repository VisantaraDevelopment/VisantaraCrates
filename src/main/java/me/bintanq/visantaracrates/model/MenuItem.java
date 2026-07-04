package me.bintanq.visantaracrates.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class MenuItem {

    @SerializedName("slot")
    private String slot = "0";

    @SerializedName("material")
    private String material = "STONE";

    @SerializedName("amount")
    private int amount = 1;

    @SerializedName("custom-model-data")
    private int customModelData = 0;

    @SerializedName("item-model")
    private String itemModel = null;

    @SerializedName("glow")
    private boolean glow = false;

    @SerializedName("hide-attributes")
    private boolean hideAttributes = false;

    @SerializedName("display-name")
    private String displayName = "";

    @SerializedName("lore")
    private List<String> lore = new ArrayList<>();

    @SerializedName("actions")
    private List<String> actions = new ArrayList<>();

    public MenuItem() {}

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int cmd) { this.customModelData = cmd; }

    public String getItemModel() { return itemModel; }
    public void setItemModel(String im) { this.itemModel = im; }

    public boolean isGlow() { return glow; }
    public void setGlow(boolean g) { this.glow = g; }

    public boolean isHideAttributes() { return hideAttributes; }
    public void setHideAttributes(boolean ha) { this.hideAttributes = ha; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String dn) { this.displayName = dn; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> l) { this.lore = l; }

    public List<String> getActions() { return actions; }
    public void setActions(List<String> a) { this.actions = a; }
}
