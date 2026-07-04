package me.bintanq.visantaracrates.model;

/**
 * KeyDefinition — represents a key definition loaded from keys.yml.
 * Keys are identified by their ID and can be shared across multiple crates.
 */
public class KeyDefinition {

    private final String id;
    private final String displayName;
    private final String material;
    private final int customModelData;
    private final java.util.List<String> lore;
    private final String nexoItem;

    public KeyDefinition(String id, String displayName, String material,
                         int customModelData, java.util.List<String> lore,
                         String nexoItem) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.lore = lore != null ? lore : new java.util.ArrayList<>();
        this.nexoItem = nexoItem;
    }

    public String getId()             { return id; }
    public String getDisplayName()    { return displayName; }
    public String getMaterial()       { return material; }
    public int getCustomModelData()   { return customModelData; }
    public java.util.List<String> getLore() { return lore; }
    public String getNexoItem()       { return nexoItem; }
    public boolean hasNexoItem()      { return nexoItem != null && !nexoItem.isBlank(); }
}
