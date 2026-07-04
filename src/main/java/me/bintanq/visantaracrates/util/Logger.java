package me.bintanq.visantaracrates.util;

import me.bintanq.visantaracrates.VisantaraCrates;
import org.bukkit.Bukkit;

/**
 * Logger — centralized colored console logging for VisantaraCrates.
 */
public final class Logger {

    private static final String PREFIX = "[VisantaraCrates] ";

    private Logger() {}

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize("&f" + PREFIX + message));
    }

    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize("&e" + PREFIX + "[WARN] " + message));
    }

    public static void severe(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize("&c" + PREFIX + "[ERROR] " + message));
    }

    public static void debug(String message) {
        VisantaraCrates plugin = VisantaraCrates.getInstance();
        if (plugin != null && plugin.getConfig().getBoolean("settings.debug", false)) {
            Bukkit.getConsoleSender().sendMessage(colorize("&7" + PREFIX + "[DEBUG] " + message));
        }
    }

    private static String colorize(String msg) {
        return msg.replace("&", "\u00A7");
    }
}
