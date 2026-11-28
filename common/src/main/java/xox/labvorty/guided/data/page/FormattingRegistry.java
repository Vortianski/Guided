package xox.labvorty.guided.data.page;

import java.util.*;
import java.util.function.UnaryOperator;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;

public final class FormattingRegistry {
    public record Format(String marker, UnaryOperator<Style> transform) {}
    private record Active(String tag, UnaryOperator<Style> transform) {}
    private static final Map<ResourceLocation, Format> formats = new LinkedHashMap<>();

    static {
        register(ResourceLocation.parse("guided:bold"), "b", s -> s.withBold(true));
        register(ResourceLocation.parse("guided:italic"), "i", s -> s.withItalic(true));
        register(ResourceLocation.parse("guided:underline"), "u", s -> s.withUnderlined(true));
        register(ResourceLocation.parse("guided:strikethrough"), "s", s -> s.withStrikethrough(true));
        register(ResourceLocation.parse("guided:obfuscated"), "obf", s -> s.withObfuscated(true));
    }

    public static void register(ResourceLocation id, String marker, UnaryOperator<Style> transform) {
        if (!marker.matches("[a-zA-Z0-9_:-]+")) throw new IllegalArgumentException("Invalid tag");
        formats.put(id, new Format(marker, transform));
    }

    public static Component parse(String text) {
        MutableComponent result = Component.empty();
        List<Active> active = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        for (int i = 0; i < text.length();) {
            int end = text.charAt(i) == '<' ? text.indexOf('>', i + 1) : -1;
            if (end >= 0) {
                String tag = text.substring(i + 1, end);
                if (tag.equals("br") || tag.equals("br/")) {
                    pending.append('\n');
                    i = end + 1;
                    continue;
                }
                if (tag.startsWith("/")) {
                    String name = tag.substring(1);
                    int match = -1;
                    for (int n = active.size() - 1; n >= 0; n--) {
                        if (active.get(n).tag().equals(name)) { match = n; break; }
                    }
                    if (match >= 0) {
                        flush(result, pending, active);
                        active.remove(match);
                        i = end + 1;
                        continue;
                    }
                } else {
                    Active next = null;
                    for (Format format : formats.values()) {
                        if (format.marker().equals(tag)) { next = new Active(tag, format.transform()); break; }
                    }
                    if (tag.startsWith("color=")) {
                        String value = tag.substring(6).replace("\"", "").replace("'", "");
                        var color = TextColor.parseColor(value).result();
                        if (color.isPresent()) next = new Active("color", s -> s.withColor(color.get()));
                    }
                    if(tag.startsWith("entry=")) {
                        String id=tag.substring(6).replace("\"", "").replace("'", "");
                        next=new Active("entry", st->st.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"guided-entry:"+id)));
                    }
                    if (next != null) {
                        flush(result, pending, active);
                        active.add(next);
                        i = end + 1;
                        continue;
                    }
                }
            }
            pending.append(text.charAt(i++));
        }
        flush(result, pending, active);
        return result;
    }

    private static void flush(MutableComponent result, StringBuilder pending, List<Active> active) {
        if (pending.isEmpty()) return;
        Style style = Style.EMPTY;
        for (Active format : active) style = format.transform().apply(style);
        result.append(Component.literal(pending.toString()).setStyle(style));
        pending.setLength(0);
    }
}
