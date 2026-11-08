package com.questclusters.editor.client.desc;

import com.questclusters.editor.client.GifTextures;
import com.questclusters.editor.data.DescriptionTable;
import dev.ftb.mods.ftblibrary.util.client.ClientTextComponentUtils;
import dev.ftb.mods.ftblibrary.util.client.ImageComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;

/** Description markup: {@code {quest_enhance_table:...}} and {@code {quest_enhance_gif:...}} (Quest Enhance's syntax). */
public final class DescriptionComponents {
    public static final String GIF_KEY = "quest_enhance_gif";
    public static final String PERSISTENT_KEY = "quest_enhance.player_persistent_data";
    /** Insertion text marking the stand-in component of a table line. */
    private static final String TABLE_MARK = "quest_clusters_editor:table:";

    private DescriptionComponents() {
    }

    public static void register() {
        ClientTextComponentUtils.addCustomParser(DescriptionComponents::parse);
    }

    static Component parse(String s, Map<String, String> map) {
        if (map.containsKey(DescriptionTable.KEY)) {
            String payload = map.get(DescriptionTable.KEY);
            DescriptionTable t = DescriptionTable.decode(payload);
            if (t == null) return Component.translatable("quest_clusters_editor.table.invalid").withStyle(ChatFormatting.RED);
            StringBuilder fallback = new StringBuilder();
            for (int r = 0; r < t.rows.size(); r++) {
                if (r > 0) fallback.append('\n');
                fallback.append(String.join(" | ", t.rows.get(r)));
            }
            return Component.literal(fallback.toString()).withStyle(Style.EMPTY.withInsertion(TABLE_MARK + payload));
        }
        if (map.containsKey(GIF_KEY)) {
            String id = map.get(GIF_KEY);
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null || !rl.getPath().toLowerCase(Locale.ROOT).endsWith(".gif")) {
                return Component.translatable("quest_clusters_editor.gif.invalid").withStyle(ChatFormatting.RED);
            }
            int w = number(map.get("width"), 200);
            int h = number(map.get("height"), Math.max(1, Math.round(w / GifTextures.aspect(rl))));
            ImageComponent img = ImageComponent.create(rl.toString(), w, h,
                    ImageComponent.ImageAlign.byName(map.getOrDefault("align", "center")), "true".equals(map.get("fit")), "");
            MutableComponent out = MutableComponent.create(img);
            if (map.containsKey("text")) {
                out.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        ClientTextComponentUtils.parse(map.get("text").replace("%20", " ")))));
            }
            return out;
        }
        return null;
    }

    private static int number(String s, int d) {
        try {
            return s == null ? d : Math.max(1, Math.min(1000, Integer.parseInt(s.trim())));
        } catch (NumberFormatException e) {
            return d;
        }
    }

    /** Table payload carried by a parsed description line, or null. */
    public static String tablePayload(Component c) {
        String ins = c.getStyle().getInsertion();
        if (ins != null && ins.startsWith(TABLE_MARK)) return ins.substring(TABLE_MARK.length());
        for (Component s : c.getSiblings()) {
            String p = tablePayload(s);
            if (p != null) return p;
        }
        return null;
    }

    /** Placeholder key of a top-level persistent-data component, or null. */
    public static String placeholderKey(Component c) {
        if (c.getContents() instanceof TranslatableContents t && PERSISTENT_KEY.equals(t.getKey())) {
            for (Object a : t.getArgs()) {
                String s = a instanceof Component ac ? ac.getString() : String.valueOf(a);
                if (s.startsWith("key:")) {
                    String k = s.substring(4);
                    if (!k.isEmpty() && k.length() <= 128 && !k.matches(".*[\\s{}].*")) return k;
                }
            }
        }
        return null;
    }

    public static boolean placeholderI18n(Component c) {
        if (c.getContents() instanceof TranslatableContents t) {
            for (Object a : t.getArgs()) {
                String s = a instanceof Component ac ? ac.getString() : String.valueOf(a);
                if (s.equals("i18n:true")) return true;
            }
        }
        return false;
    }

    /** All placeholder keys in a component tree (also nested ones, which are requested but not substituted). */
    public static void collectKeys(Component c, java.util.Set<String> out) {
        String k = placeholderKey(c);
        if (k != null) out.add(k);
        c.getSiblings().forEach(s -> collectKeys(s, out));
    }

    /** The JSON line for a placeholder. */
    public static String placeholderLine(String key, boolean i18n) {
        return "{\"translate\":\"" + PERSISTENT_KEY + "\",\"with\":[\"key:" + key + "\",\"i18n:" + i18n + "\"]}";
    }
}
