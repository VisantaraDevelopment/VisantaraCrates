package me.bintanq.visantaracrates.util;

import me.bintanq.visantaracrates.VisantaraCrates;

public final class ReloadUtil {

    private ReloadUtil() {}

    public static void reloadAll(VisantaraCrates plugin) {
        plugin.reloadConfig();
        MessageManager.init(plugin);
        if (plugin.getPreviewManager() != null) plugin.getPreviewManager().loadAllPreviews();

        if (plugin.getRarityManager() != null) plugin.getRarityManager().reload();
        if (plugin.getParticleManager() != null) plugin.getParticleManager().stopAll();

        plugin.getCrateManager().loadAllCrates();

        if (plugin.getParticleManager() != null) plugin.getParticleManager().startAll();

        Logger.info("Full reload complete. &e"
                + plugin.getCrateManager().getAllCrates().size() + " &fcrates loaded, &e"
                + plugin.getRarityManager().getAll().size() + " &frarity tiers.");
    }
}