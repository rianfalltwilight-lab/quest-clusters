package com.questclusters.editor.client.desc;

import com.questclusters.editor.client.GifTextures;
import com.questclusters.editor.data.DescriptionTable;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * "Insert component" for quest descriptions: config pages that write description lines (vanilla text JSON, FTB image
 * markup, or table / GIF / placeholder markup), and re-editing such lines from the quest details view.
 */
public final class InsertComponents {
    private InsertComponents() {
    }

    static Component tr(String key, Object... args) {
        return Component.translatable("quest_clusters_editor.insert." + key, args);
    }

    /** The menu; {@code out} receives each finished line, {@code back} is reopened after a page closes. */
    public static List<ContextMenuItem> menu(BaseScreen back, String selected, Consumer<String> out) {
        Map<String, Object> init = new HashMap<>();
        if (selected != null && !selected.isBlank() && !selected.contains("\n")) init.put("text", selected);
        List<ContextMenuItem> nav = new ArrayList<>(), media = new ArrayList<>(), text = new ArrayList<>();
        nav.add(item("link", back, init, out));
        nav.add(item("quest_page", back, init, out));
        if (Ponder.available()) nav.add(item("ponder", back, init, out));
        nav.add(item("copy", back, init, out));
        nav.add(new ContextMenuItem(tr("command"), Icons.CHAT, b -> open("command", back, init, out))
                .setYesNoText(tr("command.warning")));
        media.add(item("image", back, init, out));
        media.add(item("item_icon", back, init, out));
        media.add(item("item_tooltip", back, init, out));
        if (!GifTextures.list().isEmpty()) media.add(item("gif", back, init, out));
        text.add(item("hover", back, init, out));
        text.add(item("font", back, init, out));
        text.add(item("translate", back, init, out));
        text.add(item("keybind", back, init, out));
        text.add(item("persistent", back, init, out));
        text.add(new ContextMenuItem(tr("table"), Icons.SETTINGS, b -> TableEditorScreen.open(defaultTable(), t -> {
            back.openGui();
            out.accept(t.toLine());
        })));
        text.add(item("obfuscated", back, init, out));
        return List.of(ContextMenuItem.subMenu(tr("group.nav"), Icons.COMPASS, nav),
                ContextMenuItem.subMenu(tr("group.media"), Icons.ART, media),
                ContextMenuItem.subMenu(tr("group.text"), Icons.NOTES, text));
    }

    static DescriptionTable defaultTable() {
        return DescriptionTable.createDefault(List.of(
                List.of(tr("table.h1").getString(), tr("table.h2").getString(), tr("table.h3").getString()),
                List.of(tr("table.c1").getString(), tr("table.c2").getString(), tr("table.c3").getString()),
                List.of(tr("table.c4").getString(), tr("table.c5").getString(), tr("table.c6").getString())));
    }

    private static ContextMenuItem item(String kind, BaseScreen back, Map<String, Object> init, Consumer<String> out) {
        return new ContextMenuItem(tr(kind), Icons.ADD, b -> open(kind, back, init, out));
    }

    // ------------------------------------------------------------------ pages

    /** Opens the config page of a component kind, pre-filled with {@code init}; {@code out} gets the line. */
    public static void open(String kind, BaseScreen back, Map<String, Object> init0, Consumer<String> out) {
        Map<String, Object> v = new HashMap<>(init0);
        ConfigGroup g = new ConfigGroup("quest_clusters_editor.insert", accepted -> {
            back.openGui();
            if (accepted) {
                String line = build(kind, v);
                if (line != null) out.accept(line);
            }
        });
        String text = (String) v.getOrDefault("text", "");
        switch (kind) {
            case "link" -> {
                str(g, v, "text", text.isEmpty() ? "Link" : text);
                str(g, v, "url", "https://");
            }
            case "quest_page" -> {
                str(g, v, "text", text.isEmpty() ? "Open quest" : text);
                ConfigQuestObject<QuestObjectBase> q = new ConfigQuestObject<>(o -> o instanceof Quest);
                g.add("quest", q, (QuestObjectBase) v.get("quest"), x -> v.put("quest", x), null);
                g.addInt("page", (Integer) v.getOrDefault("page", 1), x -> v.put("page", x), 1, 1, 100);
            }
            case "ponder" -> {
                str(g, v, "text", text.isEmpty() ? tr("ponder.default").getString() : text);
                List<String> items = Ponder.items().stream().map(ResourceLocation::toString).toList();
                if (items.isEmpty()) items = List.of("minecraft:air");
                v.putIfAbsent("item", items.getFirst());
                g.addEnum("item", (String) v.get("item"), x -> v.put("item", x), NameMap.of(items.getFirst(), items).create());
            }
            case "copy" -> {
                str(g, v, "text", text.isEmpty() ? "Copy" : text);
                str(g, v, "value", "");
            }
            case "command" -> {
                str(g, v, "text", text.isEmpty() ? "Run" : text);
                str(g, v, "command", "/");
            }
            case "hover" -> {
                str(g, v, "text", text);
                str(g, v, "hover", "");
            }
            case "font" -> {
                str(g, v, "text", text);
                List<String> fonts = com.questclusters.editor.client.CanvasEditFonts.fonts();
                v.putIfAbsent("font", fonts.getFirst());
                g.addEnum("font", (String) v.get("font"), x -> v.put("font", x), NameMap.of(fonts.getFirst(), fonts).create());
            }
            case "translate" -> {
                str(g, v, "key", "");
                str(g, v, "text", text);
            }
            case "keybind" -> {
                List<String> keys = new ArrayList<>();
                for (KeyMapping k : Minecraft.getInstance().options.keyMappings) keys.add(k.getName());
                keys.sort(String::compareTo);
                v.putIfAbsent("key", keys.contains("key.inventory") ? "key.inventory" : keys.getFirst());
                g.addEnum("key", (String) v.get("key"), x -> v.put("key", x),
                        NameMap.of(keys.getFirst(), keys).name(k -> Component.translatable(k)).create());
            }
            case "obfuscated" -> str(g, v, "text", text);
            case "item_tooltip" -> {
                str(g, v, "text", text);
                item(g, v);
            }
            case "persistent" -> {
                str(g, v, "key", "example.key");
                g.addBool("i18n", (Boolean) v.getOrDefault("i18n", false), x -> v.put("i18n", x), false);
            }
            case "image" -> {
                str(g, v, "url", "https://example.com/image.png");
                imageFields(g, v, 100, 100, true);
            }
            case "item_icon" -> {
                item(g, v);
                imageFields(g, v, 18, 18, false);
            }
            case "gif" -> {
                List<String> gifs = GifTextures.list().stream().map(ResourceLocation::toString).toList();
                v.putIfAbsent("gif", gifs.getFirst());
                g.addEnum("gif", (String) v.get("gif"), x -> v.put("gif", x), NameMap.of(gifs.getFirst(), gifs).create());
                imageFields(g, v, 200, 0, false);
            }
            default -> {
                return;
            }
        }
        new EditConfigScreen(g).openGui();
    }

    private static void str(ConfigGroup g, Map<String, Object> v, String id, String def) {
        v.putIfAbsent(id, def);
        g.addString(id, (String) v.get(id), x -> v.put(id, x), "");
    }

    private static void item(ConfigGroup g, Map<String, Object> v) {
        ItemStack s = (ItemStack) v.getOrDefault("item", ItemStack.EMPTY);
        g.addItemStack("item", s, x -> v.put("item", x), ItemStack.EMPTY, true, false);
    }

    enum Align {
        LEFT, CENTER, RIGHT;
        static final NameMap<Align> NAMES = NameMap.of(CENTER, values()).create();
    }

    private static void imageFields(ConfigGroup g, Map<String, Object> v, int w, int h, boolean fit) {
        g.addInt("width", (Integer) v.getOrDefault("width", w), x -> v.put("width", x), w, 0, 1000);
        g.addInt("height", (Integer) v.getOrDefault("height", h), x -> v.put("height", x), h, 0, 1000);
        g.addEnum("align", (Align) v.getOrDefault("align", Align.CENTER), x -> v.put("align", x), Align.NAMES);
        g.addBool("fit", (Boolean) v.getOrDefault("fit", fit), x -> v.put("fit", x), fit);
        str(g, v, "hover", "");
    }

    // ------------------------------------------------------------------ building lines

    public static String build(String kind, Map<String, Object> v) {
        String text = (String) v.getOrDefault("text", "");
        Style link = Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true);
        Component c = switch (kind) {
            case "link" -> lit(text).withStyle(link.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, (String) v.get("url"))));
            case "quest_page" -> v.get("quest") instanceof Quest q ? lit(text).withStyle(link.withClickEvent(
                    new ClickEvent(ClickEvent.Action.CHANGE_PAGE, q.getCodeString() + "/" + v.getOrDefault("page", 1)))) : null;
            case "ponder" -> lit(text).withStyle(link.withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, Ponder.PREFIX + v.get("item"))));
            case "copy" -> lit(text).withStyle(link.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, (String) v.get("value"))));
            case "command" -> {
                String cmd = (String) v.get("command");
                yield cmd.length() < 2 || !cmd.startsWith("/") ? null : lit(text).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)
                        .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)));
            }
            case "hover" -> lit(text).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, lit((String) v.get("hover")))));
            case "font" -> lit(text).withStyle(Style.EMPTY.withFont(ResourceLocation.parse((String) v.get("font"))));
            case "translate" -> Component.translatableWithFallback((String) v.get("key"), text);
            case "keybind" -> {
                String k = (String) v.get("key");
                yield Component.translatableWithFallback(k, k).append(Component.literal(": ")).append(Component.keybind(k));
            }
            case "obfuscated" -> lit(text).withStyle(ChatFormatting.OBFUSCATED);
            case "item_tooltip" -> v.get("item") instanceof ItemStack s && !s.isEmpty() ? lit(text).withStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(s)))) : null;
            default -> null;
        };
        if (c != null) {
            return Component.Serializer.toJson(c, ClientQuestFile.INSTANCE.holderLookup());
        }
        return switch (kind) {
            case "persistent" -> {
                String k = (String) v.get("key");
                yield k.isEmpty() || k.length() > 128 || k.matches(".*[\\s{}].*") ? null
                        : DescriptionComponents.placeholderLine(k, (Boolean) v.getOrDefault("i18n", false));
            }
            case "image" -> imageMarkup("image:" + ((String) v.get("url")).replace(" ", "%20"), v);
            case "item_icon" -> v.get("item") instanceof ItemStack s && !s.isEmpty() ? imageMarkup("image:" + ItemIcon.getItemIcon(s), v) : null;
            case "gif" -> imageMarkup(DescriptionComponents.GIF_KEY + ":" + v.get("gif"), v);
            default -> null;
        };
    }

    private static MutableComponent lit(String s) {
        return Component.literal(s);
    }

    private static String imageMarkup(String head, Map<String, Object> v) {
        StringBuilder b = new StringBuilder("{").append(head);
        int w = (Integer) v.getOrDefault("width", 100), h = (Integer) v.getOrDefault("height", 100);
        if (w > 0) b.append(" width:").append(w);
        if (h > 0) b.append(" height:").append(h);
        b.append(" align:").append(((Align) v.getOrDefault("align", Align.CENTER)).name().toLowerCase(Locale.ROOT));
        if ((Boolean) v.getOrDefault("fit", false)) b.append(" fit:true");
        String hover = (String) v.getOrDefault("hover", "");
        if (!hover.isBlank()) b.append(" text:").append(hover.replace("{", "").replace("}", "").replace(" ", "%20"));
        return b.append("}").toString();
    }

    // ------------------------------------------------------------------ re-editing a raw line

    /** Opens the right editor for a raw description line; false if it is not one of ours (FTB Quests then handles it). */
    public static boolean reedit(BaseScreen back, String raw, Consumer<String> out) {
        String s = raw.trim();
        String payload = DescriptionTable.payloadOf(s);
        if (payload != null) {
            DescriptionTable t = DescriptionTable.decode(payload);
            if (t == null) return false;
            TableEditorScreen.open(t, edited -> {
                back.openGui();
                out.accept(edited.toLine());
            });
            return true;
        }
        if (s.startsWith("{") && s.endsWith("}") && !s.startsWith("{\"")) {
            Map<String, String> p = StringUtils.splitProperties(s.substring(1, s.length() - 1));
            Map<String, Object> v = new HashMap<>();
            if (p.containsKey("width")) v.put("width", parseInt(p.get("width"), 100));
            if (p.containsKey("height")) v.put("height", parseInt(p.get("height"), 100));
            v.put("align", Align.valueOf(safeAlign(p.getOrDefault("align", "center"))));
            v.put("fit", "true".equals(p.get("fit")));
            v.put("hover", p.getOrDefault("text", "").replace("%20", " "));
            if (p.containsKey(DescriptionComponents.GIF_KEY)) {
                v.put("gif", p.get(DescriptionComponents.GIF_KEY));
                open("gif", back, v, out);
                return true;
            }
            String img = p.get("image");
            if (img != null && img.startsWith("item:")) {
                if (ItemIcon.getItemIcon(img.substring(5)) instanceof ItemIcon ii) v.put("item", ii.getStack());
                open("item_icon", back, v, out);
                return true;
            }
            if (img != null && (img.startsWith("http://") || img.startsWith("https://"))) {
                v.put("url", img.replace("%20", " "));
                open("image", back, v, out);
                return true;
            }
            return false;
        }
        if (!s.startsWith("{")) return false;
        Component c;
        try {
            c = Component.Serializer.fromJson(s, ClientQuestFile.INSTANCE.holderLookup());
        } catch (Exception e) {
            return false;
        }
        if (c == null) return false;
        Map<String, Object> v = new HashMap<>();
        String text = c.getContents() instanceof PlainTextContents.LiteralContents l ? l.text() : c.getString();
        v.put("text", text);
        // key binding: translate + ": " + keybind
        if (c.getContents() instanceof TranslatableContents t && c.getSiblings().size() == 2
                && c.getSiblings().get(1).getContents() instanceof KeybindContents kb && kb.getName().equals(t.getKey())) {
            v.put("key", t.getKey());
            open("keybind", back, v, out);
            return true;
        }
        if (!c.getSiblings().isEmpty()) return false;
        if (DescriptionComponents.placeholderKey(c) != null) {
            v.put("key", DescriptionComponents.placeholderKey(c));
            v.put("i18n", DescriptionComponents.placeholderI18n(c));
            open("persistent", back, v, out);
            return true;
        }
        Style st = c.getStyle();
        ClickEvent click = st.getClickEvent();
        if (click != null) {
            String val = click.getValue();
            switch (click.getAction()) {
                case OPEN_URL -> {
                    v.put("url", val);
                    open("link", back, v, out);
                    return true;
                }
                case COPY_TO_CLIPBOARD -> {
                    v.put("value", val);
                    open("copy", back, v, out);
                    return true;
                }
                case RUN_COMMAND -> {
                    v.put("command", val);
                    open("command", back, v, out);
                    return true;
                }
                case CHANGE_PAGE -> {
                    if (val.startsWith(Ponder.PREFIX)) {
                        v.put("item", val.substring(Ponder.PREFIX.length()));
                        open("ponder", back, v, out);
                        return true;
                    }
                    int slash = val.indexOf('/');
                    if (slash > 0 && ClientQuestFile.INSTANCE.getBase(QuestObjectBase.parseCodeString(val.substring(0, slash))) instanceof Quest q) {
                        v.put("quest", q);
                        v.put("page", parseInt(val.substring(slash + 1), 1));
                        open("quest_page", back, v, out);
                        return true;
                    }
                }
                default -> {
                }
            }
        }
        HoverEvent hover = st.getHoverEvent();
        if (hover != null && hover.getValue(HoverEvent.Action.SHOW_TEXT) instanceof Component h) {
            v.put("hover", h.getString());
            open("hover", back, v, out);
            return true;
        }
        if (hover != null && hover.getValue(HoverEvent.Action.SHOW_ITEM) instanceof HoverEvent.ItemStackInfo info) {
            v.put("item", info.getItemStack());
            open("item_tooltip", back, v, out);
            return true;
        }
        if (c.getContents() instanceof TranslatableContents t) {
            v.put("key", t.getKey());
            v.put("text", t.getFallback() == null ? "" : t.getFallback());
            open("translate", back, v, out);
            return true;
        }
        if (s.contains("\"font\"") && st.getFont() != null) {
            v.put("font", st.getFont().toString());
            open("font", back, v, out);
            return true;
        }
        if (st.isObfuscated()) {
            open("obfuscated", back, v, out);
            return true;
        }
        return false;
    }

    private static String safeAlign(String a) {
        String u = a.toUpperCase(Locale.ROOT);
        return u.equals("LEFT") || u.equals("RIGHT") ? u : "CENTER";
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return d;
        }
    }
}
