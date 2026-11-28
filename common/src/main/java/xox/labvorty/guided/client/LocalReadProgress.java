package xox.labvorty.guided.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import xox.labvorty.guided.GuidedMod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LocalReadProgress {
    private static Path currentFile;
    private static Map<ResourceLocation, Set<String>> currentProgress = new HashMap<>();

    private static Path getFile() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return null;
        String world;
        if (minecraft.getSingleplayerServer() != null) {
            world = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString();
        } else if (minecraft.getCurrentServer() != null) {
            world = minecraft.getCurrentServer().ip;
        } else {
            return null;
        }
        UUID worldId = UUID.nameUUIDFromBytes(world.getBytes(StandardCharsets.UTF_8));
        return minecraft.gameDirectory.toPath().resolve("guided/read/" + worldId + "/" + minecraft.player.getUUID() + ".json");
    }

    public static Map<ResourceLocation, Set<String>> load() {
        Path file = getFile();
        if (file != null && file.equals(currentFile)) return currentProgress;
        currentFile = file;
        currentProgress = new HashMap<>();
        if (file == null || !Files.isRegularFile(file)) return currentProgress;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                if (id == null) continue;
                Set<String> entries = new HashSet<>();
                for (JsonElement element : entry.getValue().getAsJsonArray()) entries.add(element.getAsString());
                currentProgress.put(id, entries);
            }
        } catch (Exception exception) {
            GuidedMod.LOGGER.warn("Unable to load Guided reading progress", exception);
        }
        return currentProgress;
    }

    public static void save(Map<ResourceLocation, Set<String>> progress) {
        Path file = getFile();
        if (file == null || !file.equals(currentFile) || progress != currentProgress) return;
        JsonObject root = new JsonObject();
        progress.forEach((id, entries) -> {
            JsonArray array = new JsonArray();
            entries.forEach(array::add);
            root.add(id.toString(), array);
        });
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, new Gson().toJson(root));
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            GuidedMod.LOGGER.warn("Unable to save Guided reading progress", exception);
        }
    }
}
