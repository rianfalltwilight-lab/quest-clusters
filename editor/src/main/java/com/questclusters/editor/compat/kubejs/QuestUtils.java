package com.questclusters.editor.compat.kubejs;

import com.mojang.datafixers.util.Either;
import com.questclusters.editor.data.CanvasElement;
import com.questclusters.editor.data.DescriptionTable;
import com.questclusters.editor.data.VideoPaths;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftbquests.net.OpenQuestBookMessage;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.history.CreateOrDeleteRecord;
import dev.ftb.mods.ftbquests.quest.history.EditRecord;
import dev.ftb.mods.ftbquests.quest.history.events.CreateQuestObjects;
import dev.ftb.mods.ftbquests.quest.history.events.DeleteQuestObjects;
import dev.ftb.mods.ftbquests.quest.history.events.ModifyQuestObjects;
import dev.ftb.mods.ftbquests.quest.history.events.MoveItemWithinQuest;
import dev.ftb.mods.ftbquests.quest.history.events.MoveMovableObject;
import dev.ftb.mods.ftbquests.quest.history.events.UpdateTranslation;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import dev.ftb.mods.ftbquests.quest.translation.TranslationKey;
import dev.ftb.mods.ftbquests.util.ProgressChange;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * {@code QuestUtils} for KubeJS (same name and methods as Quest Enhance's binding, so scripts keep working).
 * Ids may be numbers or FTB code strings. Edits go through FTB Quests' own history: they are saved, synced to every
 * player and undoable in the editor. Canvas helpers write {@code click_action} correctly (Quest Enhance's helpers
 * wrote the legacy {@code click} key, which FTB Quests 2101 drops).
 */
@SuppressWarnings("unused")
public final class QuestUtils {
    private QuestUtils() {
    }

    // ------------------------------------------------------------------ plumbing

    static ServerQuestFile server() {
        return ServerQuestFile.getInstance().orElseThrow(() -> new IllegalStateException("No quest book loaded on the server"));
    }

    static BaseQuestFile file() {
        var s = ServerQuestFile.getInstance();
        if (s.isPresent()) return s.get();
        return dev.ftb.mods.ftbquests.api.FTBQuestsAPI.api().getQuestFile(true);
    }

    static long id(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof QuestObjectBase q) return q.id;
        if (o == null) return 0L;
        return QuestObjectBase.parseCodeString(o.toString().trim());
    }

    static String code(long id) {
        return QuestObjectBase.getCodeString(id);
    }

    @SuppressWarnings("unchecked")
    static <T> T get(Object id, Class<T> type) {
        QuestObjectBase o = file().getBase(id(id));
        if (!type.isInstance(o)) throw new IllegalArgumentException("No " + type.getSimpleName() + " with id " + id);
        return (T) o;
    }

    static HolderLookup.Provider lookup() {
        return file().holderLookup();
    }

    static void edit(QuestObjectBase o, Consumer<CompoundTag> change) {
        ServerQuestFile sqf = server();
        CompoundTag t = new CompoundTag();
        o.writeData(t, lookup());
        change.accept(t);
        ModifyQuestObjects.fromEditRecords(sqf, List.of(new EditRecord(o.id, o.getObjectType(), o.getTitle(), t)))
                .ifPresent(m -> sqf.getHistoryStack().addAndApply(sqf, m));
    }

    static String create(QuestObjectBase o) {
        ServerQuestFile sqf = server();
        long newId = sqf.newID();
        sqf.getHistoryStack().addAndApply(sqf, new CreateQuestObjects(List.of(CreateOrDeleteRecord.ofQuestObject(o).withNewID(newId)), null));
        return code(newId);
    }

    static void translate(QuestObjectBase o, TranslationKey key, Either<String, List<String>> value) {
        ServerQuestFile sqf = server();
        UpdateTranslation.create(sqf, o, sqf.getFallbackLocale(), key, value).ifPresent(e -> sqf.getHistoryStack().addAndApply(sqf, e));
    }

    static TeamData team(Player player) {
        return file().getOrCreateTeamData(player);
    }

    // ------------------------------------------------------------------ lookup

    public static QuestObjectBase getObject(Object id) {
        return file().getBase(id(id));
    }

    public static Quest getQuest(Object id) {
        return file().getBase(id(id)) instanceof Quest q ? q : null;
    }

    public static Chapter getChapter(Object id) {
        return file().getBase(id(id)) instanceof Chapter c ? c : null;
    }

    public static Task getTask(Object id) {
        return file().getBase(id(id)) instanceof Task t ? t : null;
    }

    public static List<String> getChapterImages(Object chapterId) {
        return get(chapterId, Chapter.class).getImages().stream().map(i -> code(i.id)).toList();
    }

    public static int getChapterImageCount(Object chapterId) {
        return get(chapterId, Chapter.class).getImages().size();
    }

    public static String getChapterImage(Object chapterId, int index) {
        List<ChapterImage> imgs = new ArrayList<>(get(chapterId, Chapter.class).getImages());
        return index >= 0 && index < imgs.size() ? code(imgs.get(index).id) : null;
    }

    public static String getChapterImageData(Object image) {
        CompoundTag t = new CompoundTag();
        get(image, ChapterImage.class).writeData(t, lookup());
        return t.toString();
    }

    static CanvasElement element(Object image) {
        return CanvasElement.parse(get(image, ChapterImage.class).getClickAction().actionData());
    }

    public static String getChapterImageType(Object image) {
        return switch (element(image)) {
            case CanvasElement.Gif g -> "gif";
            case CanvasElement.Video v -> "video";
            case CanvasElement.Text t -> "text";
            case CanvasElement.Anchor a -> "anchor";
            case null -> "image";
        };
    }

    public static String getChapterGifResource(Object image) {
        return element(image) instanceof CanvasElement.Gif g ? g.resource() : null;
    }

    public static String getChapterVideoPath(Object image) {
        return element(image) instanceof CanvasElement.Video v ? v.path() : null;
    }

    public static String getChapterText(Object image) {
        return element(image) instanceof CanvasElement.Text t ? t.text() : null;
    }

    public static String getChapterTextFont(Object image) {
        return element(image) instanceof CanvasElement.Text t ? t.font() : null;
    }

    public static String getDecorativeAnchorKey(Object image) {
        return element(image) instanceof CanvasElement.Anchor a ? a.nodeKey() : null;
    }

    // ------------------------------------------------------------------ progress

    public static boolean isStarted(Player player, Object id) {
        return team(player).isStarted(get(id, QuestObject.class));
    }

    public static boolean isCompleted(Player player, Object id) {
        return team(player).isCompleted(get(id, QuestObject.class));
    }

    public static boolean isVisible(Player player, Object id) {
        return get(id, QuestObject.class).isVisible(team(player));
    }

    public static long getProgress(Player player, Object taskId) {
        return team(player).getProgress(get(taskId, Task.class));
    }

    public static int getQuestProgress(Player player, Object questId) {
        return get(questId, Quest.class).getRelativeProgressFromChildren(team(player));
    }

    public static long getMaxProgress(Object taskId) {
        return get(taskId, Task.class).getMaxProgress();
    }

    public static int getCompletionCount(Player player, Object questId) {
        return team(player).getCompletionCount(get(questId, Quest.class));
    }

    public static void complete(Player player, Object id) {
        QuestObjectBase o = get(id, QuestObjectBase.class);
        o.forceProgress(team(player), new ProgressChange(o, player.getUUID()).setReset(false).withNotifications());
    }

    public static void reset(Player player, Object id) {
        QuestObjectBase o = get(id, QuestObjectBase.class);
        o.forceProgress(team(player), new ProgressChange(o, player.getUUID()).setReset(true));
    }

    public static void completeTask(Player player, Object taskId) {
        Task t = get(taskId, Task.class);
        team(player).setProgress(t, t.getMaxProgress());
    }

    public static void setProgress(Player player, Object taskId, long progress) {
        Task t = get(taskId, Task.class);
        team(player).setProgress(t, Math.max(0, Math.min(t.getMaxProgress(), progress)));
    }

    public static void addProgress(Player player, Object taskId, long progress) {
        team(player).addProgress(get(taskId, Task.class), progress);
    }

    public static void resetTask(Player player, Object taskId) {
        team(player).resetProgress(get(taskId, Task.class));
    }

    // ------------------------------------------------------------------ quest dependencies

    public static boolean arePrerequisitesCompleted(Player player, Object questId) {
        return team(player).areDependenciesComplete(get(questId, Quest.class));
    }

    public static int completedPrerequisiteCount(Player player, Object questId) {
        TeamData td = team(player);
        return (int) get(questId, Quest.class).streamDependencies().filter(td::isCompleted).count();
    }

    public static int prerequisiteCount(Object questId) {
        return (int) get(questId, Quest.class).streamDependencies().count();
    }

    public static List<String> prerequisites(Object questId) {
        return get(questId, Quest.class).streamDependencies().map(d -> code(d.id)).toList();
    }

    public static boolean hasPrerequisite(Object prerequisite, Object questId) {
        long p = id(prerequisite);
        return get(questId, Quest.class).streamDependencies().anyMatch(d -> d.id == p);
    }

    public static void setPrerequisite(Object prerequisite, Object questId) {
        String p = code(get(prerequisite, QuestObject.class).id);
        edit(get(questId, Quest.class), t -> {
            ListTag l = t.getList("dependencies", Tag.TAG_STRING);
            if (l.stream().noneMatch(e -> e.getAsString().equalsIgnoreCase(p))) l.add(StringTag.valueOf(p));
            t.put("dependencies", l);
        });
    }

    public static void removePrerequisite(Object prerequisite, Object questId) {
        long p = id(prerequisite);
        edit(get(questId, Quest.class), t -> {
            ListTag l = t.getList("dependencies", Tag.TAG_STRING);
            l.removeIf(e -> QuestObjectBase.parseCodeString(e.getAsString()) == p);
            t.put("dependencies", l);
        });
    }

    public static void clearPrerequisites(Object questId) {
        edit(get(questId, Quest.class), t -> t.remove("dependencies"));
    }

    // ------------------------------------------------------------------ visibility

    public static void hideQuest(Object questId) {
        edit(get(questId, Quest.class), t -> t.putBoolean("invisible", true));
    }

    public static void unhideQuest(Object questId) {
        edit(get(questId, Quest.class), t -> t.remove("invisible"));
    }

    public static boolean isQuestHidden(Object questId) {
        CompoundTag t = new CompoundTag();
        get(questId, Quest.class).writeData(t, lookup());
        return t.getBoolean("invisible");
    }

    public static void hideChapter(Object chapterId) {
        edit(get(chapterId, Chapter.class), t -> t.putBoolean("always_invisible", true));
    }

    public static void unhideChapter(Object chapterId) {
        edit(get(chapterId, Chapter.class), t -> t.remove("always_invisible"));
    }

    public static boolean isChapterHidden(Object chapterId) {
        CompoundTag t = new CompoundTag();
        get(chapterId, Chapter.class).writeData(t, lookup());
        return t.getBoolean("always_invisible");
    }

    // ------------------------------------------------------------------ text

    public static void setTitle(Object id, String title) {
        translate(get(id, QuestObjectBase.class), TranslationKey.TITLE, Either.left(title));
    }

    public static void setQuestSubtitle(Object questId, String subtitle) {
        translate(get(questId, Quest.class), TranslationKey.QUEST_SUBTITLE, Either.left(subtitle));
    }

    public static void setQuestDescription(Object questId, String... lines) {
        translate(get(questId, Quest.class), TranslationKey.QUEST_DESC, Either.right(List.of(lines)));
    }

    // ------------------------------------------------------------------ description lines

    static String json(Component c) {
        return Component.Serializer.toJson(c, lookup());
    }

    static Style link() {
        return Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true);
    }

    public static String descriptionWebLink(String text, String url) {
        return json(Component.literal(text).withStyle(link().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
    }

    public static String descriptionCopy(String text, String value) {
        return json(Component.literal(text).withStyle(link().withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))));
    }

    public static String descriptionCommand(String text, String command) {
        return json(Component.literal(text).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command.startsWith("/") ? command : "/" + command))));
    }

    public static String descriptionPonder(String text, String itemId) {
        return json(Component.literal(text).withStyle(link().withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, "quest_enhance:ponder/" + itemId))));
    }

    public static String descriptionHoverText(String text, String hover) {
        return json(Component.literal(text).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover)))));
    }

    public static String descriptionItemHover(String text, ItemStack stack) {
        return json(Component.literal(text).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)))));
    }

    public static String descriptionFont(String text, String font) {
        return json(Component.literal(text).withStyle(Style.EMPTY.withFont(ResourceLocation.parse(font))));
    }

    public static String descriptionTranslation(String key, String fallback) {
        return json(Component.translatableWithFallback(key, fallback));
    }

    public static String descriptionKeybind(String key) {
        return json(Component.keybind(key));
    }

    static String image(String head, int w, int h, String align, boolean fit, String hover) {
        StringBuilder b = new StringBuilder("{").append(head).append(" width:").append(w).append(" height:").append(h)
                .append(" align:").append(align == null ? "center" : align.toLowerCase(Locale.ROOT));
        if (fit) b.append(" fit:true");
        if (hover != null && !hover.isBlank()) b.append(" text:").append(hover.replace("{", "").replace("}", "").replace(" ", "%20"));
        return b.append("}").toString();
    }

    public static String descriptionImage(String icon, int width, int height, String align, boolean fit, String hover) {
        return image("image:" + icon.replace(" ", "%20"), width, height, align, fit, hover);
    }

    /** FTB Quests' own item-icon markup (Quest Enhance's {@code quest_enhance_item} form never rendered). */
    public static String descriptionItemIcon(ItemStack stack, int width, int height, String align, boolean fit, String hover) {
        return image("image:" + dev.ftb.mods.ftblibrary.icon.ItemIcon.getItemIcon(stack), width, height, align, fit, hover);
    }

    public static String descriptionGif(String resource) {
        return "{quest_enhance_gif:" + resource + "}";
    }

    public static String descriptionGif(String resource, int width, int height, String align, boolean fit, String hover) {
        return image("quest_enhance_gif:" + resource, width, height, align, fit, hover);
    }

    public static String descriptionVideo(String path, String text) {
        return VideoPaths.descriptionLine(path, text);
    }

    public static String descriptionTable(List<List<Object>> rows, boolean header, String align) {
        return descriptionTable(rows, header, align, 0, 18, 1, 0x808080, 0x374151, 0x1F2937, 0xFFFFFF);
    }

    public static String descriptionTable(List<List<Object>> rows, boolean header, String align, int tableWidth, int rowHeight,
                                          int lineWidth, int border, int headerColor, int cell, int text) {
        List<List<String>> r = new ArrayList<>();
        int cols = 1;
        for (List<Object> row : rows) {
            List<String> rr = new ArrayList<>();
            for (Object c : row) rr.add(String.valueOf(c));
            cols = Math.max(cols, rr.size());
            r.add(rr);
        }
        if (r.isEmpty()) r.add(new ArrayList<>(List.of("")));
        DescriptionTable t = new DescriptionTable();
        t.columns = Math.min(cols, DescriptionTable.MAX_COLUMNS);
        for (List<String> row : r.subList(0, Math.min(r.size(), DescriptionTable.MAX_ROWS))) {
            List<String> fixed = new ArrayList<>();
            for (int c = 0; c < t.columns; c++) fixed.add(c < row.size() ? row.get(c) : "");
            t.rows.add(fixed);
        }
        t.header = header;
        t.alignment = DescriptionTable.align(align);
        t.tableWidth = Math.max(0, Math.min(1000, tableWidth));
        t.rowHeight = Math.max(12, Math.min(30, rowHeight));
        t.lineWidth = Math.max(1, Math.min(4, lineWidth));
        t.borderColor = 0xFF000000 | border;
        t.headerColor = 0xFF000000 | headerColor;
        t.cellColor = 0xFF000000 | cell;
        t.textColor = 0xFF000000 | text;
        return t.toLine();
    }

    // ------------------------------------------------------------------ quest / task editing

    public static void setQuestSize(Object questId, double size) {
        edit(get(questId, Quest.class), t -> t.putDouble("size", size));
    }

    public static void setQuestShape(Object questId, String shape) {
        edit(get(questId, Quest.class), t -> t.putString("shape", shape));
    }

    public static void setQuestOptional(Object questId, boolean optional) {
        edit(get(questId, Quest.class), t -> t.putBoolean("optional", optional));
    }

    public static void setTaskOptional(Object taskId, boolean optional) {
        edit(get(taskId, Task.class), t -> t.putBoolean("optional_task", optional));
    }

    public static String getData(Object id) {
        CompoundTag t = new CompoundTag();
        get(id, QuestObjectBase.class).writeData(t, lookup());
        return t.toString();
    }

    /** Merges SNBT (a string or a compound tag) into the object's data. */
    public static void mergeData(Object id, Object data) {
        CompoundTag patch;
        try {
            patch = data instanceof CompoundTag c ? c : TagParser.parseTag(String.valueOf(data));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SNBT: " + e.getMessage());
        }
        edit(get(id, QuestObjectBase.class), t -> t.merge(patch));
    }

    // ------------------------------------------------------------------ creation

    public static String createChapter(Object groupId, String title) {
        ServerQuestFile sqf = server();
        ChapterGroup g = id(groupId) == 0L ? sqf.getDefaultChapterGroup() : get(groupId, ChapterGroup.class);
        Chapter c = new Chapter(0L, sqf, g, title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"));
        String id = create(c);
        setTitle(id, title);
        return id;
    }

    public static String createQuest(Object chapterId, double x, double y) {
        Quest q = new Quest(0L, get(chapterId, Chapter.class));
        CompoundTag t = new CompoundTag();
        t.putDouble("x", x);
        t.putDouble("y", y);
        q.readData(t, lookup());
        return create(q);
    }

    public static String createQuest(Object chapterId, double x, double y, String title) {
        String id = createQuest(chapterId, x, y);
        setTitle(id, title);
        return id;
    }

    static ResourceLocation typeId(String type) {
        return type.contains(":") ? ResourceLocation.parse(type) : ResourceLocation.fromNamespaceAndPath("ftbquests", type);
    }

    public static String createTask(Object questId, String type) {
        var tt = TaskTypes.TYPES.get(typeId(type));
        if (tt == null) throw new IllegalArgumentException("Unknown task type " + type);
        return create(tt.createTask(0L, get(questId, Quest.class)));
    }

    public static String createTask(Object questId, String type, String title) {
        String id = createTask(questId, type);
        setTitle(id, title);
        return id;
    }

    public static String createReward(Object questId, String type) {
        var rt = RewardTypes.TYPES.get(typeId(type));
        if (rt == null) throw new IllegalArgumentException("Unknown reward type " + type);
        Reward r = rt.createReward(0L, get(questId, Quest.class));
        return create(r);
    }

    public static String createReward(Object questId, String type, String title) {
        String id = createReward(questId, type);
        setTitle(id, title);
        return id;
    }

    // ------------------------------------------------------------------ canvas

    static String createImage(Object chapterId, String image, double x, double y, double w, double h, String payload) {
        ChapterImage img = new ChapterImage(0L, get(chapterId, Chapter.class));
        CompoundTag t = new CompoundTag();
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("width", w);
        t.putDouble("height", h);
        t.putString("image", image == null ? "" : image);
        if (payload != null) t.putString("click_action", "none:" + payload);
        img.readData(t, lookup());
        return create(img);
    }

    public static String createChapterImage(Object chapterId, String resource, double x, double y, double w, double h) {
        return createImage(chapterId, resource, x, y, w, h, null);
    }

    public static String createChapterGif(Object chapterId, String resource, double x, double y, double w, double h) {
        return createImage(chapterId, "", x, y, w, h, new CanvasElement.Gif(resource).encode());
    }

    public static String createChapterVideo(Object chapterId, String path, double x, double y, double w, double h) {
        return createImage(chapterId, "#212121", x, y, w, h, new CanvasElement.Video(VideoPaths.normalise(path)).encode());
    }

    public static String createChapterText(Object chapterId, String text, String font, double x, double y, double w, double h) {
        return createImage(chapterId, "", x, y, w, h,
                new CanvasElement.Text(text, font == null || font.isBlank() ? CanvasElement.DEFAULT_FONT : font, 1.0).encode());
    }

    public static String createDecorativeAnchor(Object chapterId, double x, double y) {
        return createImage(chapterId, "", x, y, 0.4, 0.4, new CanvasElement.Anchor(UUID.randomUUID().toString()).encode());
    }

    static void editImage(Object image, Consumer<CompoundTag> change) {
        edit(get(image, ChapterImage.class), change);
    }

    public static void setChapterImagePosition(Object image, double x, double y) {
        editImage(image, t -> {
            t.putDouble("x", x);
            t.putDouble("y", y);
        });
    }

    public static void setChapterImageSize(Object image, double w, double h) {
        editImage(image, t -> {
            t.putDouble("width", w);
            t.putDouble("height", h);
        });
    }

    public static void setChapterImageRotation(Object image, double degrees) {
        editImage(image, t -> t.putDouble("rotation", degrees));
    }

    public static void setChapterImageResource(Object image, String resource) {
        editImage(image, t -> t.putString("image", resource));
    }

    public static void setChapterImageColor(Object image, int rgb) {
        editImage(image, t -> t.putInt("color", rgb & 0xFFFFFF));
    }

    public static void setChapterImageAlpha(Object image, int alpha) {
        editImage(image, t -> t.putInt("alpha", Math.max(0, Math.min(255, alpha))));
    }

    public static void setChapterImageOrder(Object image, int order) {
        editImage(image, t -> t.putInt("order", order));
    }

    public static void setChapterImageHover(Object image, String... lines) {
        translate(get(image, ChapterImage.class), TranslationKey.TITLE, Either.left(String.join("\n", lines)));
    }

    /** Click action as "type:data" (FTB Quests' form), or a "quest_enhance:..." canvas payload. */
    public static void setChapterImageClick(Object image, String click) {
        String v = click == null ? "" : click;
        editImage(image, t -> {
            t.remove("click");
            if (v.isEmpty()) t.remove("click_action");
            else t.putString("click_action", v.startsWith(CanvasElement.PREFIX) ? "none:" + v : v);
        });
    }

    public static void setChapterImageEditorsOnly(Object image, boolean dev) {
        editImage(image, t -> t.putBoolean("dev", dev));
    }

    public static void setChapterImageAlignToCorner(Object image, boolean corner) {
        editImage(image, t -> t.putBoolean("corner", corner));
    }

    public static void setChapterImageDependency(Object image, Object questId) {
        String q = code(get(questId, Quest.class).id);
        editImage(image, t -> t.putString("dependency", q));
    }

    public static void clearChapterImageDependency(Object image) {
        editImage(image, t -> t.remove("dependency"));
    }

    public static void setChapterGifResource(Object image, String resource) {
        setChapterImageClick(image, new CanvasElement.Gif(resource).encode());
    }

    public static void setChapterVideoPath(Object image, String path) {
        setChapterImageClick(image, new CanvasElement.Video(VideoPaths.normalise(path)).encode());
    }

    public static void setChapterText(Object image, String text) {
        CanvasElement e = element(image);
        String font = e instanceof CanvasElement.Text t ? t.font() : CanvasElement.DEFAULT_FONT;
        double scale = e instanceof CanvasElement.Text t ? t.scale() : 1.0;
        setChapterImageClick(image, new CanvasElement.Text(text, font, scale).encode());
    }

    public static void setChapterTextFont(Object image, String font) {
        if (element(image) instanceof CanvasElement.Text t) {
            setChapterImageClick(image, new CanvasElement.Text(t.text(), font, t.scale()).encode());
        }
    }

    public static String copyChapterImage(Object image, Object chapterId, double x, double y) {
        Chapter target = get(chapterId, Chapter.class);
        ChapterImage copy = QuestObjectBase.copy(get(image, ChapterImage.class), () -> new ChapterImage(0L, target));
        CompoundTag t = new CompoundTag();
        copy.writeData(t, lookup());
        t.putDouble("x", x);
        t.putDouble("y", y);
        copy.readData(t, lookup());
        return create(copy);
    }

    public static void moveChapterImage(Object image, Object chapterId, double x, double y) {
        move(get(image, ChapterImage.class), get(chapterId, Chapter.class), x, y);
    }

    public static void deleteChapterImage(Object image) {
        deleteObject(image);
    }

    public static void deleteChapterImage(Object chapterId, int index) {
        String img = getChapterImage(chapterId, index);
        if (img != null) deleteObject(img);
    }

    // ------------------------------------------------------------------ other

    public static void deleteObject(Object id) {
        ServerQuestFile sqf = server();
        QuestObjectBase o = get(id, QuestObjectBase.class);
        sqf.getHistoryStack().addAndApply(sqf, new DeleteQuestObjects(CreateOrDeleteRecord.ofQuestObject(o)));
    }

    static void move(Movable m, Chapter to, double x, double y) {
        ServerQuestFile sqf = server();
        MoveMovableObject.create(m, to, x, y).ifPresent(e -> sqf.getHistoryStack().addAndApply(sqf, e));
    }

    public static void moveQuest(Object questId, Object chapterId, double x, double y) {
        move(get(questId, Quest.class), get(chapterId, Chapter.class), x, y);
    }

    public static void setQuestPosition(Object questId, double x, double y) {
        Quest q = get(questId, Quest.class);
        move(q, q.getChapter(), x, y);
    }

    public static void moveTaskLeft(Object taskId) {
        ServerQuestFile sqf = server();
        Task t = get(taskId, Task.class);
        sqf.getHistoryStack().addAndApply(sqf, new MoveItemWithinQuest(t.id, t.getObjectType(), false));
    }

    public static void moveTaskRight(Object taskId) {
        ServerQuestFile sqf = server();
        Task t = get(taskId, Task.class);
        sqf.getHistoryStack().addAndApply(sqf, new MoveItemWithinQuest(t.id, t.getObjectType(), true));
    }

    /** Client scripts: opens the quest book at this object. */
    public static void open(Object id) {
        dev.ftb.mods.ftbquests.client.ClientQuestFile.openBookToQuestObject(id(id));
    }

    /** Server scripts: opens the player's quest book at this object. */
    public static void open(ServerPlayer player, Object id) {
        NetworkManager.sendToPlayer(player, new OpenQuestBookMessage(id(id)));
    }

    /** SNBT text of a whole compound (debug helper). */
    public static String snbt(CompoundTag tag) {
        return SNBT.writeLines(tag).stream().reduce((a, b) -> a + "\n" + b).orElse("");
    }

    static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
