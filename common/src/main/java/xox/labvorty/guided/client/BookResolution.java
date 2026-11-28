package xox.labvorty.guided.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import xox.labvorty.guided.data.book.Content;
import xox.labvorty.guided.data.page.PageProviders;

public final class BookResolution {
    private final Content.Book book;

    public BookResolution(Content.Book book) {
        this.book = book;
    }

    public JsonObject resolve(LocalPlayer player) {
        JsonObject visible = new JsonObject();

        for (Content.Entry entry : book.entries()) {
            if (!PageProviders.passes(entry.json(), player)) continue;
            JsonArray pages = new JsonArray();

            if (entry.json().has("pages")) {
                for (JsonElement page : entry.json().getAsJsonArray("pages")) {
                    JsonObject resolved = PageProviders.resolve(page.getAsJsonObject(), player, 0);
                    if (resolved != null) {
                        pages.add(resolved);
                    }
                }
            }

            visible.add(entry.id(), pages);
        }

        return visible;
    }
}
