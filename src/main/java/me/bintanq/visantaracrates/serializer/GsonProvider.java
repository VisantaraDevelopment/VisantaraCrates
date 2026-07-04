package me.bintanq.visantaracrates.serializer;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import me.bintanq.visantaracrates.model.Crate;
import me.bintanq.visantaracrates.model.PlayerData;
import me.bintanq.visantaracrates.model.reward.Reward;
import me.bintanq.visantaracrates.scheduler.CrateSchedule;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public final class GsonProvider {

    private static Gson GSON;
    private static Gson GSON_COMPACT; // For DB storage (no pretty print)

    private GsonProvider() {}

    public static void init() {
        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .disableHtmlEscaping()
                .registerTypeAdapter(UUID.class, new UUIDAdapter());

        GSON         = builder.create();
        GSON_COMPACT = new GsonBuilder()
                .serializeNulls()
                .disableHtmlEscaping()
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .create();
    }

    /** Pretty-printed GSON — use for file I/O and WebSocket messages. */
    public static Gson getGson() {
        if (GSON == null) init();
        return GSON;
    }

    /** Compact GSON — use for DB column values. */
    public static Gson getCompact() {
        if (GSON_COMPACT == null) init();
        return GSON_COMPACT;
    }

    public static String toJson(Object object) {
        return getCompact().toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return getGson().fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        return getGson().fromJson(json, type);
    }

    /** Serialize a Crate to pretty JSON for file storage / WebSocket. */
    public static String serializeCrate(Crate crate) {
        return getGson().toJson(crate);
    }

    /** Deserialize a Crate from JSON. */
    public static Crate deserializeCrate(String json) {
        return getGson().fromJson(json, Crate.class);
    }

    /** Serialize PlayerData compactly for DB column. */
    public static String serializePlayerData(PlayerData data) {
        return getCompact().toJson(data);
    }

    public static PlayerData deserializePlayerData(String json) {
        return getCompact().fromJson(json, PlayerData.class);
    }

    /* ─────────────────────── Type Adapters ─────────────────────── */

    /** Serializes UUID as plain string "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx". */
    private static class UUIDAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        @Override
        public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            return UUID.fromString(json.getAsString());
        }
    }
}
