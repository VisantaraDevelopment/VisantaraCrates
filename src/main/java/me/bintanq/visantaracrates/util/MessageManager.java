package me.bintanq.visantaracrates.util;

import me.bintanq.visantaracrates.VisantaraCrates;
import org.bukkit.command.CommandSender;


public final class MessageManager {

    private static VisantaraCrates plugin;

    private MessageManager() {}

    public static void init(VisantaraCrates p) { plugin = p; }

    /* ─────────────────────── Chat Messages ─────────────────────── */


    public static void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public static void sendRaw(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(getRaw(key, placeholders));
    }

    public static String get(String key, String... placeholders) {
        String prefix = getRaw("prefix");
        String msg    = getRaw(key, placeholders);
        return color(prefix + msg);
    }

    public static String getRaw(String key, String... placeholders) {
        String msg = plugin.getConfig().getString("messages." + key,
                "[MSG NOT FOUND: " + key + "]");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return color(msg);
    }

    public static boolean has(String key) {
        return plugin.getConfig().contains("messages." + key);
    }

    /* ─────────────────────── GUI Messages ─────────────────────── */

    public static String getGui(String key, String... placeholders) {
        String msg = plugin.getConfig().getString("gui-messages." + key, "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return color(msg);
    }


    public static boolean hasGui(String key) {
        return plugin.getConfig().contains("gui-messages." + key);
    }

    /* ─────────────────────── Convenience Senders ─────────────────────── */

    public static void sendNoPermission(CommandSender sender) {
        send(sender, "no-permission");
    }

    public static void sendPlayerOnly(CommandSender sender) {
        send(sender, "player-only");
    }

    public static void sendPlayerNotFound(CommandSender sender, String name) {
        send(sender, "player-not-found", "{player}", name);
    }

    public static void sendCrateNotFound(CommandSender sender, String crateId) {
        send(sender, "crate-not-found", "{crate}", crateId);
    }

    public static void sendInvalidNumber(CommandSender sender) {
        send(sender, "invalid-number");
    }

    public static void sendReloadSuccess(CommandSender sender) {
        send(sender, "reload-success");
    }

    /* ─────────────────────── Broadcast ─────────────────────── */

    public static void broadcast(String key, String... placeholders) {
        plugin.getServer().broadcastMessage(get(key, placeholders));
    }

    /* ─────────────────────── Colorize ─────────────────────── */

    public static String color(String s) {
        return s == null ? "" : s.replace("&", "\u00A7");
    }
}