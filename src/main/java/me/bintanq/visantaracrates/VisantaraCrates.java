package me.bintanq.visantaracrates;

import me.bintanq.visantaracrates.animation.AnimationManager;
import me.bintanq.visantaracrates.api.VisantaraCratesAPI;
import me.bintanq.visantaracrates.command.VisantaraCratesCommand;
import me.bintanq.visantaracrates.database.DatabaseManager;
import me.bintanq.visantaracrates.database.impl.MySQLDatabase;
import me.bintanq.visantaracrates.database.impl.SQLiteDatabase;
import me.bintanq.visantaracrates.hook.HookManager;
import me.bintanq.visantaracrates.listener.CrateListener;
import me.bintanq.visantaracrates.listener.GUIListener;
import me.bintanq.visantaracrates.listener.PlayerListener;
import me.bintanq.visantaracrates.log.LogManager;
import me.bintanq.visantaracrates.manager.CrateManager;
import me.bintanq.visantaracrates.manager.KeyManager;
import me.bintanq.visantaracrates.manager.PreviewManager;
import me.bintanq.visantaracrates.manager.PlayerDataManager;
import me.bintanq.visantaracrates.manager.RarityManager;
import me.bintanq.visantaracrates.particle.ParticleManager;
import me.bintanq.visantaracrates.placeholder.VisantaraPlaceholderExpansion;
import me.bintanq.visantaracrates.processor.RewardProcessor;
import me.bintanq.visantaracrates.serializer.GsonProvider;
import me.bintanq.visantaracrates.util.ConfigMigrator;
import me.bintanq.visantaracrates.util.Logger;
import me.bintanq.visantaracrates.util.VersionChecker;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VisantaraCrates extends JavaPlugin {

    private static VisantaraCrates instance;
    public static VisantaraCrates getInstance() { return instance; }

    public static final ThreadLocal<String> ACTIVE_CRATE_TYPE = new ThreadLocal<>();

    private ExecutorService asyncExecutor;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private RarityManager rarityManager;
    private CrateManager crateManager;
    private KeyManager keyManager;
    private RewardProcessor rewardProcessor;
    private LogManager logManager;
    private HookManager hookManager;
    private ParticleManager particleManager;
    private AnimationManager animationManager;
    private PreviewManager previewManager;
    private BukkitAudiences adventure;
    private VersionChecker versionChecker;
    private boolean nexoEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        int pluginId = 31014;
        Metrics metrics = new Metrics(this, pluginId);

        Logger.info("&b=============================");
        Logger.info("&b  Visantara&fCrates &7v" + getDescription().getVersion());
        Logger.info("&b  By bintanq");
        Logger.info("&b=============================");

        saveDefaultConfig();
        saveResource("keys.yml", false);
        saveResource("previews/default.yml", false);
        saveResource("crates/RareCrate.yml", false);
        saveResource("crates/VIPCrate.yml", false);
        saveResource("crates/LegendaryCrate.yml", false);
        saveResource("visantaracrates.db", false);
        GsonProvider.init();
        me.bintanq.visantaracrates.util.MessageManager.init(this);
        me.bintanq.visantaracrates.util.PhysicalCrateItem.init(this);

        // Detect Nexo
        if (getServer().getPluginManager().getPlugin("Nexo") != null) {
            nexoEnabled = true;
            Logger.info("Nexo found — &aNexo item support enabled.");
        }

        // Run config migrations before anything reads config
        ConfigMigrator.migrateConfig(this);
        ConfigMigrator.migrateRarities(this);
        ConfigMigrator.migrateCrateFiles(this);

        int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        asyncExecutor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "VisantaraCrates-Async-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        java.io.File migrationsDir = new java.io.File(getDataFolder(), "migrations");
        if (!migrationsDir.exists()) {
            migrationsDir.mkdirs();
        }

        if (!initDatabase()) {
            Logger.severe("Database initialization failed! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initManagers();

        animationManager = new AnimationManager(this);

        crateManager.loadAllCrates();
        hookManager.registerAll();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new VisantaraPlaceholderExpansion(this, playerDataManager, crateManager).register();
            Logger.info("PlaceholderAPI hook &aregistered.");
        }

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this, playerDataManager), this);
        pm.registerEvents(new CrateListener(this, crateManager), this);
        pm.registerEvents(new GUIListener(this), this);
        Logger.info("Listeners &aregistered.");

        new VisantaraCratesCommand(this);
        var migrateExecutor = new me.bintanq.visantaracrates.command.MigrateCommand(this);
        var cmd1 = getCommand("cratesmigrate");
        if (cmd1 != null) cmd1.setExecutor(migrateExecutor);
        var cmd2 = getCommand("cratesmigrateadmin");
        if (cmd2 != null) cmd2.setExecutor(migrateExecutor);
        Logger.info("Commands &aregistered.");

        particleManager = new ParticleManager(this);
        particleManager.startAll();

        this.adventure = BukkitAudiences.create(this);

        // Initialize public API
        VisantaraCratesAPI.init(this);
        Logger.info("Developer API &ainitialized.");

        // Async version check
        versionChecker = new VersionChecker(this);
        pm.registerEvents(versionChecker, this);
        versionChecker.checkAsync();

        Logger.info("&aVisantaraCrates enabled successfully!");
    }

    @Override
    public void onDisable() {
        Logger.info("Shutting down VisantaraCrates...");

        ifNotNull(particleManager,  ParticleManager::stopAll);
        ifNotNull(crateManager,     CrateManager::shutdown);
        ifNotNull(playerDataManager,PlayerDataManager::flushAll);
        ifNotNull(logManager,       LogManager::shutdown);

        if (animationManager != null) {
            animationManager.shutdown();
        }

        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    Logger.warn("Async executor did not terminate in time, forcing shutdown.");
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (databaseManager != null) databaseManager.close();

        if (this.adventure != null) {
            this.adventure.close();
        }

        Logger.info("&cVisantaraCrates disabled.");
    }

    private void initManagers() {
        rarityManager     = new RarityManager(this);
        Objects.requireNonNull(rarityManager, "RarityManager failed to initialize");
        logManager        = new LogManager(databaseManager, asyncExecutor);
        playerDataManager = new PlayerDataManager(databaseManager, asyncExecutor);
        hookManager       = new HookManager(this);
        rewardProcessor   = new RewardProcessor(this, hookManager);
        keyManager        = new KeyManager(this, playerDataManager);
        previewManager    = new PreviewManager(this);
        crateManager      = new CrateManager(this, playerDataManager, rewardProcessor, logManager, keyManager);
        Logger.info("All managers initialized.");
    }

    private boolean initDatabase() {
        String type = getConfig().getString("database.type", "sqlite");
        try {
            if ("mysql".equalsIgnoreCase(type)) {
                databaseManager = new MySQLDatabase(this);
                Logger.info("Using &aMySQL &fdatabase.");
            } else {
                databaseManager = new SQLiteDatabase(this);
                Logger.info("Using &aSQLite &fdatabase.");
            }
            databaseManager.init();
            return true;
        } catch (Exception e) {
            Logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static <T> void ifNotNull(T obj, java.util.function.Consumer<T> action) {
        if (obj != null) action.accept(obj);
    }

    /** Whether the global pity system is enabled (config: settings.enable-pity). */
    public boolean isPityEnabled() {
        return getConfig().getBoolean("settings.enable-pity", true);
    }

    /** Whether the Nexo plugin is present on this server. */
    public boolean isNexoEnabled() { return nexoEnabled; }

    public ExecutorService getAsyncExecutor()          { return asyncExecutor; }
    public DatabaseManager getDatabaseManager()        { return databaseManager; }
    public PlayerDataManager getPlayerDataManager()    { return playerDataManager; }
    public RarityManager getRarityManager()            { return rarityManager; }
    public CrateManager getCrateManager()              { return crateManager; }
    public PreviewManager getPreviewManager()          { return previewManager; }
    public KeyManager getKeyManager()                  { return keyManager; }
    public RewardProcessor getRewardProcessor()        { return rewardProcessor; }
    public LogManager getLogManager()                  { return logManager; }
    public HookManager getHookManager()                { return hookManager; }
    public ParticleManager getParticleManager()        { return particleManager; }
    public AnimationManager getAnimationManager()      { return animationManager; }
    public BukkitAudiences adventure() { return adventure; }
    public VersionChecker getVersionChecker()          { return versionChecker; }
}