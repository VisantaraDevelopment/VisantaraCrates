package me.bintanq.visantaracrates.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class UUIDResolver {

    private static final String MOJANG_API =
            "https://api.mojang.com/users/profiles/minecraft/";

    private UUIDResolver() {}


    public static CompletableFuture<UUID> resolve(String nameOrUuid,
                                                  java.util.concurrent.Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try { return UUID.fromString(nameOrUuid); } catch (IllegalArgumentException ignored) {}

            Player online = Bukkit.getPlayerExact(nameOrUuid);
            if (online != null) return online.getUniqueId();

            @SuppressWarnings("deprecation")
            OfflinePlayer offline = Bukkit.getOfflinePlayer(nameOrUuid);
            if (offline.hasPlayedBefore() && offline.getUniqueId() != null)
                return offline.getUniqueId();

            try {
                URL url = new URL(MOJANG_API + nameOrUuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() == 200) {
                    try (InputStreamReader reader =
                                 new InputStreamReader(conn.getInputStream())) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        String raw = json.get("id").getAsString();
                        String formatted = raw.replaceFirst(
                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                        return UUID.fromString(formatted);
                    }
                }
            } catch (Exception e) {
                Logger.warn("Mojang UUID lookup failed for '" + nameOrUuid + "': " + e.getMessage());
            }
            return null;
        }, executor);
    }
}