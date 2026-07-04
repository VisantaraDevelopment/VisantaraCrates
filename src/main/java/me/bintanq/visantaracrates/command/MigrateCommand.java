package me.bintanq.visantaracrates.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.bintanq.visantaracrates.VisantaraCrates;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.util.PhysicalCrateItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MigrateCommand implements CommandExecutor {

    private final VisantaraCrates plugin;

    public MigrateCommand(VisantaraCrates plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player targetPlayer;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cConsole must specify a player: /cratesmigrate <player>"));
                return true;
            }
            targetPlayer = (Player) sender;
        } else {
            if (!sender.hasPermission("visantaracrates.admin")) {
                sender.sendMessage(colorize("&cYou do not have permission to migrate other players."));
                return true;
            }
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(colorize("&cPlayer not found or offline: " + args[0]));
                return true;
            }
        }

        Player player = targetPlayer;
        UUID uuid = player.getUniqueId();
        sender.sendMessage(colorize("&eConnecting to old database and scanning for keys..."));

        plugin.getAsyncExecutor().execute(() -> {
            final File finalDbFile = new File(plugin.getDataFolder(), "migrations/database.db");

            if (!finalDbFile.exists()) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(colorize("&cError: Migration database file not found at " + finalDbFile.getAbsolutePath()))
                );
                return;
            }

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + finalDbFile.getAbsolutePath())) {
                String selectSql = "SELECT Id, VirtualKeys FROM pc_player_table WHERE UniqueId = ? OR LOWER(PlayerName) = LOWER(?)";
                String virtualKeysJson = null;
                int dbRowId = -1;

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, player.getName());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            dbRowId = rs.getInt("Id");
                            virtualKeysJson = rs.getString("VirtualKeys");
                        }
                    }
                }

                if (virtualKeysJson == null || virtualKeysJson.isEmpty() || virtualKeysJson.equals("{}")) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(colorize("&cNo old virtual keys found in the database for " + player.getName()))
                    );
                    return;
                }

                JsonObject keysObj = JsonParser.parseString(virtualKeysJson).getAsJsonObject();
                Map<String, Integer> migratedKeys = new HashMap<>();

                for (String keyId : keysObj.keySet()) {
                    int amount = keysObj.get(keyId).getAsInt();
                    if (amount > 0) {
                        migratedKeys.put(keyId, amount);
                    }
                }

                if (migratedKeys.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(colorize("&cNo old virtual keys with balance > 0 found for " + player.getName()))
                    );
                    return;
                }

                Map<String, String[]> keyMapping = new HashMap<>();
                keyMapping.put("rarekey", new String[]{"RareCrate", "FREE"});
                keyMapping.put("premiumrarekey", new String[]{"RareCrate", "PREMIUM"});
                keyMapping.put("vipkey", new String[]{"VIPCrate", "FREE"});
                keyMapping.put("premiumvipkey", new String[]{"VIPCrate", "PREMIUM"});
                keyMapping.put("legendarykey", new String[]{"LegendaryCrate", "FREE"});
                keyMapping.put("premiumlegendarykey", new String[]{"LegendaryCrate", "PREMIUM"});

                Map<Crate, Integer> cratesToGive = new HashMap<>();
                Map<Crate, String> crateTypesToGive = new HashMap<>();

                for (Map.Entry<String, Integer> entry : migratedKeys.entrySet()) {
                    String oldKeyId = entry.getKey();
                    int amount = entry.getValue();

                    String[] mapped = keyMapping.get(oldKeyId.toLowerCase());
                    if (mapped != null) {
                        String crateId = mapped[0];
                        String crateType = mapped[1];
                        Crate crate = plugin.getCrateManager().getCrate(crateId);
                        if (crate != null) {
                            cratesToGive.put(crate, amount);
                            crateTypesToGive.put(crate, crateType);
                        } else {
                            Bukkit.getConsoleSender().sendMessage(colorize("&c[Migration] Active crate not found: " + crateId));
                        }
                    } else {
                        Bukkit.getConsoleSender().sendMessage(colorize("&e[Migration] Warning: Key '" + oldKeyId + "' from player " + player.getName() + " has no mapping."));
                    }
                }

                if (cratesToGive.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(colorize("&cFound virtual keys: " + migratedKeys + ", but no matching active crates were found for them."))
                    );
                    return;
                }

                for (Crate crate : cratesToGive.keySet()) {
                    for (String keyId : migratedKeys.keySet()) {
                        String[] mapped = keyMapping.get(keyId.toLowerCase());
                        if (mapped != null && mapped[0].equalsIgnoreCase(crate.getId())) {
                            keysObj.addProperty(keyId, 0);
                        }
                    }
                }
                String updatedJson = keysObj.toString();
                String updateSql = "UPDATE pc_player_table SET VirtualKeys = ? WHERE Id = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, updatedJson);
                    psUpdate.setInt(2, dbRowId);
                    psUpdate.executeUpdate();
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        sender.sendMessage(colorize("&cPlayer went offline during migration. Virtual keys have been consumed but physical crates could not be delivered."));
                        return;
                    }

                    for (Map.Entry<Crate, Integer> entry : cratesToGive.entrySet()) {
                        Crate crate = entry.getKey();
                        int amount = entry.getValue();
                        String type = crateTypesToGive.get(crate);

                        ItemStack item = PhysicalCrateItem.create(plugin, crate, amount, type);
                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        } else {
                            player.getInventory().addItem(item);
                        }
                        player.sendMessage(colorize("&a[Migration] Converted " + amount + "x old virtual " + crate.getId() + " key(s) to physical crates!"));
                    }
                    if (sender != player) {
                        sender.sendMessage(colorize("&aSuccessfully migrated virtual keys for " + player.getName() + " to physical crates."));
                    }
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(colorize("&cDatabase error during migration: " + e.getMessage()))
                );
                e.printStackTrace();
            }
        });

        return true;
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
