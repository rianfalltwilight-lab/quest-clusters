package com.questclusters.editor.client;

import com.questclusters.client.DevSelfTest;
import com.questclusters.editor.client.gui.DecorativeLinesWidget;
import com.questclusters.editor.data.BookExtras;
import com.questclusters.editor.migration.QuestEnhanceMigration;
import com.questclusters.editor.mixin.client.QuestScreenEditorAccessor;
import com.questclusters.migration.MigrationRunner;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.questclusters.editor.client.gui.EditorMenus;
import com.questclusters.editor.data.CanvasElement;
import com.questclusters.editor.data.ChapterExtras;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.net.CopyChapterImageMessage;
import dev.ftb.mods.ftbquests.net.UndoRedoRequestMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import java.util.function.IntSupplier;

/** Editor checks for the dev self-test (fixture chapters C2 "editor_test" and C3 "editor_locked"). */
final class EditorDevTest {
    private static final long C1 = 0xC1L, C2 = 0xC2L, C3 = 0xC3L, D5 = 0xD5L, D6 = 0xD6L, A6 = 0xA6L, A7 = 0xA7L;
    private static volatile Boolean serverLocked;

    private EditorDevTest() {
    }

    private static void editMode(boolean on) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        var uuid = Minecraft.getInstance().player.getUUID();
        server.execute(() -> {
            var sp = server.getPlayerList().getPlayer(uuid);
            ServerQuestFile.INSTANCE.getOrCreateTeamData(sp).setPlayerEditMode(sp, on);
            serverLocked = !ServerQuestFile.INSTANCE.getChapter(C3).isVisible(ServerQuestFile.INSTANCE.getOrCreateTeamData(sp));
        });
    }

    private static void command(String cmd) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        String name = Minecraft.getInstance().player.getGameProfile().getName();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd.replace("@me", name)));
    }

    private static final net.minecraft.resources.ResourceLocation GIF =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("quest_clusters_editor", "textures/gui/example.gif");

    private static com.questclusters.editor.data.DescriptionTable tableUnderTest;
    private static volatile String scriptQuest;
    private static int imagesBefore;

    private static void collect(dev.ftb.mods.ftblibrary.ui.Panel p, List<dev.ftb.mods.ftblibrary.ui.Widget> out) {
        for (var w : p.getWidgets()) {
            out.add(w);
            if (w instanceof dev.ftb.mods.ftblibrary.ui.Panel sub) collect(sub, out);
        }
    }

    private static String rawText(dev.ftb.mods.ftblibrary.ui.TextField tf) {
        try {
            var f = dev.ftb.mods.ftblibrary.ui.TextField.class.getDeclaredField("rawText");
            f.setAccessible(true);
            return ((net.minecraft.network.chat.Component) f.get(tf)).getString();
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    private static List<String> keys(List<ContextMenuItem> menu) {
        List<String> out = new ArrayList<>();
        for (ContextMenuItem i : menu) {
            if (i.getTitle().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().startsWith("quest_clusters_editor.menu.")) {
                out.add(t.getKey().substring("quest_clusters_editor.menu.".length()));
            }
        }
        return out;
    }

    private static long count(java.util.Collection<ChapterImage> imgs, Class<?> type) {
        return imgs.stream().filter(i -> type.isInstance(CanvasElement.parse(i.getClickAction().actionData()))).count();
    }

    private static void counters() {
        EditorDebug.hiddenPurple = EditorDebug.hiddenSkipped = EditorDebug.hiddenRevealed = 0;
    }

    static void register() {
        DevSelfTest.addModuleSteps(DevSelfTest.configScreenSteps("quest_clusters_editor", "qc_37_editor_settings", () -> List.of(
                "quest_clusters_editor.configuration.title",
                "quest_clusters_editor.configuration.render_kill_task_entity_models",
                "quest_clusters_editor.configuration.open_advancement_tasks")));
        DevSelfTest.addModuleSteps(List.<IntSupplier>of(
                () -> {
                    Path quests = FMLPaths.CONFIGDIR.get().resolve("ftbquests/quests");
                    DevSelfTest.moduleCheck(MigrationRunner.applied(quests).containsKey(QuestEnhanceMigration.ID), "editor: Quest Enhance import recorded");
                    DevSelfTest.moduleCheck(Files.exists(quests.resolve("quest_clusters/editor/00000000000000C2.snbt"))
                            && Files.exists(quests.resolve("quest_clusters/editor/book.snbt")), "editor: own data files written");
                    BookExtras b = ClientEditor.book();
                    DevSelfTest.moduleCheck(b.sidebarBackground.endsWith("dark_oak_planks.png"), "editor: sidebar background synced");
                    DevSelfTest.moduleCheck(b.chapter(C2).background.endsWith("deepslate_tiles.png"), "editor: chapter background synced");
                    DevSelfTest.moduleCheck(b.quest(D5).boxBackground().endsWith("gold_block.png")
                            && b.quest(D5).viewBackground().endsWith("sculk.png") && b.quest(D5).disableScrollPaging(), "editor: quest backgrounds + scroll paging synced");
                    DevSelfTest.moduleCheck(b.chapter(C1).decorativeLines.size() == 2 && b.chapter(C2).decorativeLines.size() == 1, "editor: decorative lines imported");
                    DevSelfTest.moduleCheck(b.chapter(C2).hiddenLines.size() == 1 && b.chapter(C2).hiddenLines.get(0).revealOnHover(), "editor: hidden line imported");
                    DevSelfTest.moduleCheck(b.chapter(C3).prerequisites.equals(List.of(D6)), "editor: chapter prerequisites imported");
                    DevSelfTest.moduleCheck(b.excludedImages.contains(A6), "editor: box-select exclusion imported");
                    DevSelfTest.moduleCheck(ClientQuestFile.INSTANCE.getBase(A7) instanceof ChapterImage img
                            && img.getClickAction().actionData().startsWith("quest_enhance:text:"), "editor: legacy 'click' canvas text restored");
                    DevSelfTest.moduleCheck(ClientQuestFile.INSTANCE.getChapter(C3).isVisible(ClientQuestFile.INSTANCE.selfTeamData),
                            "editor: locked chapter visible in edit mode");
                    var lineTex = com.questclusters.editor.client.gui.DecorativeLinesWidget.texture();
                    DevSelfTest.moduleCheck(lineTex.equals(com.questclusters.editor.client.gui.DecorativeLinesWidget.TEXTURE)
                            && com.questclusters.client.Textures.size(lineTex) != null,
                            "editor: decorative lines use a resource-pack texture (" + lineTex + ")");
                    // backgrounds picked as block-atlas sprite ids draw the sprite, not the missing texture
                    var icon = ClientEditor.icon("minecraft:block/oak_planks");
                    var sprite = net.minecraft.client.Minecraft.getInstance().getModelManager()
                            .getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                            .getSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/oak_planks"));
                    DevSelfTest.moduleCheck(icon instanceof dev.ftb.mods.ftblibrary.icon.AtlasSpriteIcon
                            && !sprite.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()),
                            "editor: sprite-id backgrounds resolve to the atlas sprite (" + icon.getClass().getSimpleName() + ")");
                    command("ftbquests change_progress @me reset 00000000000000D6"); // earlier runs completed it
                    editMode(false);
                    return 40;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    DevSelfTest.moduleCheck(!file.canEdit(), "editor: player mode");
                    DevSelfTest.moduleCheck(!file.getChapter(C3).isVisible(file.selfTeamData), "editor: chapter hidden until its prerequisite is done (client)");
                    DevSelfTest.moduleCheck(Boolean.TRUE.equals(serverLocked), "editor: ... and on the server");
                    var qs = ClientQuestFile.openGui();
                    qs.open(file.getChapter(C2), false);
                    qs.questPanel.scrollTo(0, 0.5);
                    counters();
                    return 30;
                },
                () -> {
                    DevSelfTest.moduleCheck(DecorativeLinesWidget.lastDrawn == 2, "editor: decorative line drawn as 2 spokes (" + DecorativeLinesWidget.lastDrawn + ")");
                    DevSelfTest.moduleCheck(EditorDebug.hiddenSkipped > 0 && EditorDebug.hiddenPurple == 0, "editor: hidden line not drawn for players");
                    DevSelfTest.moduleShot("qc_30_editor_player");
                    DevSelfTest.questScreen().open((Quest) ClientQuestFile.INSTANCE.getBase(D5), false);
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleCheck(DevSelfTest.questScreen().isViewingQuest(), "editor: details view open");
                    DevSelfTest.moduleShot("qc_31_editor_details");
                    command("ftbquests change_progress @me complete 00000000000000D6");
                    return 40;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    DevSelfTest.moduleCheck(file.getChapter(C3).isVisible(file.selfTeamData), "editor: chapter unlocks when its prerequisite is completed");
                    editMode(true);
                    return 40;
                },
                () -> {
                    var qs = ClientQuestFile.openGui();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(C2), false);
                    qs.questPanel.scrollTo(0, 0.5);
                    counters();
                    return 30;
                },
                () -> {
                    var qs = DevSelfTest.questScreen();
                    DevSelfTest.moduleCheck(EditorDebug.hiddenPurple > 0, "editor: hidden line drawn purple in edit mode");
                    DevSelfTest.moduleShot("qc_32_editor_edit");
                    var sel = ((QuestScreenAccessor) qs).quest_clusters$getSelectedObjects();
                    sel.clear();
                    var acc = (QuestScreenEditorAccessor) qs;
                    acc.quest_clusters_editor$setPrevMouseX(-100000);
                    acc.quest_clusters_editor$setPrevMouseY(-100000);
                    acc.quest_clusters_editor$selectAllInBox(100000, 100000, qs.questPanel.getScrollX(), qs.questPanel.getScrollY());
                    boolean hasQuest = sel.stream().anyMatch(m -> m.getMovableID() == D5);
                    boolean hasText = sel.stream().anyMatch(m -> m.getMovableID() == A6);
                    boolean hasOther = sel.stream().anyMatch(m -> m.getMovableID() == A7);
                    DevSelfTest.moduleCheck(hasQuest && hasOther && !hasText, "editor: box selection skips excluded images");
                    sel.clear();
                    return 5;
                },
                // ------------------------------------------------------------ E2: editing
                () -> {
                    var qs = DevSelfTest.questScreen();
                    var file = ClientQuestFile.INSTANCE;
                    Chapter c2 = file.getChapter(C2);
                    List<ContextMenuItem> m = new ArrayList<>();
                    EditorMenus.canvasItems(m, qs, 0, 3);
                    DevSelfTest.moduleCheck(keys(m).containsAll(List.of("canvas_text.add", "guide_point.add", "gif.add")), "editor: canvas menu entries " + keys(m));
                    m.clear();
                    EditorMenus.objectItems(m, qs, c2);
                    DevSelfTest.moduleCheck(keys(m).containsAll(List.of("chapter_background", "prerequisites")), "editor: chapter menu entries " + keys(m));
                    m.clear();
                    EditorMenus.objectItems(m, qs, file.getQuest(D5));
                    DevSelfTest.moduleCheck(keys(m).containsAll(List.of("quest_background", "view_background", "scroll_paging", "hidden_lines")),
                            "editor: quest menu entries " + keys(m));
                    var sel = ((QuestScreenAccessor) qs).quest_clusters$getSelectedObjects();
                    sel.clear();
                    sel.add(file.getQuest(D5));
                    sel.add(file.getQuest(D6));
                    m.clear();
                    EditorMenus.selectionItems(m, qs);
                    DevSelfTest.moduleCheck(keys(m).containsAll(List.of("bulk_quest_background", "decorative.remove", "transform")),
                            "editor: selection menu entries " + keys(m));
                    sel.clear();

                    EditorActions.editChapter("chapter_background", C2, c -> c.background = "minecraft:textures/block/stone.png");
                    EditorActions.editQuests("scroll_paging", List.of(D6), x -> x.withDisableScrollPaging(true));
                    EditorActions.editChapter("decorative_line", C2, c -> c.decorativeLines.add(List.of("q:d6", "q:d5")));
                    EditorActions.editChapter("hidden_line", C2, c -> {
                        c.hiddenLines.clear();
                        c.hiddenLines.add(new ChapterExtras.HiddenLine("q:d6", "q:d5", false));
                    });
                    EditorActions.editChapter("prerequisites", C3, c -> c.prerequisites.add(D5));
                    CanvasEdit.addGuidePoint(c2, 4, 3);
                    CanvasEdit.addGif(c2, -4, 3, GIF);
                    CanvasEdit.createText(qs, c2, 0, 4, new CanvasElement.Text("Devtest text", CanvasElement.DEFAULT_FONT, 1.5));
                    EditorActions.setExcluded(List.of(A7), true);
                    return 30;
                },
                () -> {
                    BookExtras b = ClientEditor.book();
                    DevSelfTest.moduleCheck(b.chapter(C2).background.endsWith("stone.png"), "editor: chapter background edited");
                    DevSelfTest.moduleCheck(b.quest(D6).disableScrollPaging(), "editor: scroll paging toggled");
                    DevSelfTest.moduleCheck(b.chapter(C2).decorativeLines.size() == 2, "editor: decorative line added");
                    DevSelfTest.moduleCheck(b.chapter(C2).hiddenLines.size() == 1 && !b.chapter(C2).hiddenLines.get(0).revealOnHover(), "editor: hidden line edited");
                    DevSelfTest.moduleCheck(b.chapter(C3).prerequisites.equals(List.of(D6, D5)), "editor: prerequisite added");
                    DevSelfTest.moduleCheck(b.excludedImages.contains(A7), "editor: box-select exclusion set");
                    var imgs = ClientQuestFile.INSTANCE.getChapter(C2).getImages();
                    DevSelfTest.moduleCheck(count(imgs, CanvasElement.Anchor.class) == 2, "editor: guide point created");
                    DevSelfTest.moduleCheck(count(imgs, CanvasElement.Gif.class) == 1, "editor: GIF element created");
                    DevSelfTest.moduleCheck(imgs.stream().anyMatch(i -> CanvasElement.parse(i.getClickAction().actionData()) instanceof CanvasElement.Text t
                            && t.text().equals("Devtest text") && t.scale() == 1.5), "editor: canvas text created");
                    DevSelfTest.moduleCheck(Math.abs(GifTextures.aspect(GIF) - 1F) < 0.01F, "editor: GIF decoded");
                    NetworkManager.sendToServer(UndoRedoRequestMessage.undo());
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleCheck(!ClientEditor.book().excludedImages.contains(A7), "editor: undo reverts the last editor change");
                    NetworkManager.sendToServer(new CopyChapterImageMessage(0xA5L, C2, 5, 5));
                    var qs = DevSelfTest.questScreen();
                    var sel = ((QuestScreenAccessor) qs).quest_clusters$getSelectedObjects();
                    sel.clear();
                    sel.add(ClientQuestFile.INSTANCE.getQuest(D5));
                    sel.add(ClientQuestFile.INSTANCE.getQuest(D6));
                    CanvasEdit.transform(qs, CanvasEdit.MIRROR_H);
                    return 30;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    DevSelfTest.moduleCheck(file.getQuest(D5).getX() == 2.0 && file.getQuest(D6).getX() == -2.0,
                            "editor: mirror swaps positions (" + file.getQuest(D5).getX() + ", " + file.getQuest(D6).getX() + ")");
                    var keys = new java.util.HashSet<String>();
                    int anchors = 0;
                    for (ChapterImage i : file.getChapter(C2).getImages()) {
                        if (CanvasElement.parse(i.getClickAction().actionData()) instanceof CanvasElement.Anchor a) {
                            anchors++;
                            keys.add(a.uuid());
                        }
                    }
                    DevSelfTest.moduleCheck(anchors == 3 && keys.size() == 3, "editor: copied guide point gets a new id");
                    var qs = DevSelfTest.questScreen();
                    DevSelfTest.moduleCheck(CanvasEdit.copy(qs) && CanvasEdit.hasMultiClipboard(), "editor: two quests copied");
                    CanvasEdit.paste(file.getChapter(C2), 0, 6, true);
                    ((QuestScreenAccessor) qs).quest_clusters$getSelectedObjects().clear();
                    qs.questPanel.scrollTo(0, 2);
                    return 30;
                },
                () -> {
                    DevSelfTest.moduleCheck(ClientQuestFile.INSTANCE.getChapter(C2).getQuests().size() == 6, "editor: multi-paste made 2 copies");
                    DevSelfTest.moduleShot("qc_33_editor_edited");
                    command("questenhance persistent set @me qc.test Hello placeholder");
                    return 10;
                },
                // ------------------------------------------------------------ E3: descriptions, models
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    DevSelfTest.moduleCheck(file.getQuest(0xD8L).getIcon() instanceof EntityModelIcon, "editor: quest model shows the entity");
                    DevSelfTest.moduleCheck(file.getQuest(0xD4L).getIcon() instanceof EntityModelIcon, "editor: quest with one kill task shows its entity");
                    DevSelfTest.moduleCheck(file.getQuest(0xD4L).getTasks().iterator().next().getIcon() instanceof EntityModelIcon, "editor: kill task shows its entity");
                    DevSelfTest.moduleCheck(dev.ftb.mods.ftblibrary.icon.Icon.getIcon("quest_clusters_editor:textures/gui/example.gif")
                            instanceof com.questclusters.editor.client.desc.GifIcon, "editor: .gif icon strings play GIFs");
                    DevSelfTest.questScreen().open(file.getQuest(0xD8L), false);
                    return 40;
                },
                () -> {
                    var view = DevSelfTest.questScreen().viewQuestPanel;
                    List<dev.ftb.mods.ftblibrary.ui.Widget> all = new ArrayList<>();
                    collect(view, all);
                    var table = all.stream().filter(w -> w instanceof com.questclusters.editor.client.desc.TableWidget).findFirst();
                    DevSelfTest.moduleCheck(table.isPresent() && table.get().height > 40, "editor: description table drawn as a grid ("
                            + table.map(w -> w.height).orElse(0) + " px)");
                    DevSelfTest.moduleCheck(all.stream().anyMatch(w -> w instanceof dev.ftb.mods.ftbquests.client.gui.ImageComponentWidget iw
                            && iw.getComponent().getImage() instanceof com.questclusters.editor.client.desc.GifIcon), "editor: description GIF");
                    DevSelfTest.moduleCheck(all.stream().anyMatch(w -> w instanceof dev.ftb.mods.ftblibrary.ui.TextField tf
                            && rawText(tf).equals("Hello placeholder")), "editor: persistent-data placeholder shows the value");
                    DevSelfTest.moduleShot("qc_34_editor_description");
                    // component pages build the JSON Quest Enhance books use, and re-editing recognises it
                    var v = new java.util.HashMap<String, Object>();
                    v.put("text", "Wiki");
                    v.put("url", "https://example.com");
                    String link = com.questclusters.editor.client.desc.InsertComponents.build("link", v);
                    DevSelfTest.moduleCheck(link != null && link.contains("open_url") && link.contains("https://example.com"), "editor: link component JSON " + link);
                    v.clear();
                    v.put("key", "key.jump");
                    String kb = com.questclusters.editor.client.desc.InsertComponents.build("keybind", v);
                    DevSelfTest.moduleCheck(kb != null && kb.contains("\"keybind\":\"key.jump\""), "editor: key binding component JSON");
                    DevSelfTest.moduleCheck(com.questclusters.editor.client.desc.InsertComponents.reedit(DevSelfTest.questScreen(), link, x -> {
                    }) && Minecraft.getInstance().screen != null, "editor: link line reopens its page");
                    tableUnderTest = ((com.questclusters.editor.client.desc.TableWidget) table.orElseThrow()).table().copy();
                    return 10;
                },
                () -> {
                    com.questclusters.editor.client.desc.TableEditorScreen.open(tableUnderTest, t -> tableUnderTest = t);
                    return 10;
                },
                () -> {
                    if (!(com.questclusters.editor.client.desc.TableEditorScreen.last instanceof com.questclusters.editor.client.desc.TableEditorScreen ed)) {
                        DevSelfTest.moduleCheck(false, "editor: table editor opens");
                        return 5;
                    }
                    ed.testSelect(0, 1, 0, 2);
                    ed.testMerge();
                    ed.testType(2, 0, "Edited");
                    DevSelfTest.moduleCheck(ed.table().mergeAt(0, 2) != null && ed.table().cell(2, 0).equals("Edited"), "editor: table editor merges and types");
                    ed.testUndo();
                    DevSelfTest.moduleCheck(ed.table().cell(2, 0).equals("Gold"), "editor: table editor undo");
                    return 5;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_35_table_editor");
                    return 5;
                },
                () -> {
                    var qs = ClientQuestFile.openGui();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(C2), false);
                    var sel = ((QuestScreenAccessor) qs).quest_clusters$getSelectedObjects();
                    sel.clear();
                    sel.add(ClientQuestFile.INSTANCE.getQuest(D5));
                    sel.add(ClientQuestFile.INSTANCE.getQuest(D6));
                    List<ContextMenuItem> m = new ArrayList<>();
                    EditorMenus.selectionItems(m, qs);
                    DevSelfTest.moduleCheck(keys(m).containsAll(List.of("bulk_edit", "bulk_add_task")), "editor: bulk edit entries");
                    sel.clear();
                    // ------------------------------------------------------------ E4: paste, scripts, video
                    var img = new java.awt.image.BufferedImage(32, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    for (int x = 0; x < 32; x++) for (int y = 0; y < 16; y++) img.setRGB(x, y, x < 16 ? 0xFFE04040 : 0xFF40A0E0);
                    com.questclusters.editor.client.paste.ClipboardImage.testOverride = img;
                    imagesBefore = ClientQuestFile.INSTANCE.getChapter(C2).getImages().size();
                    DevSelfTest.moduleCheck(com.questclusters.editor.client.paste.PasteActions.pasteOnCanvas(qs, false), "editor: clipboard image pasted");
                    com.questclusters.editor.client.paste.ClipboardImage.testOverride = null;
                    var server = Minecraft.getInstance().getSingleplayerServer();
                    server.execute(() -> {
                        String q = com.questclusters.editor.compat.kubejs.QuestUtils.createQuest("00000000000000C2", 6, 6, "Script quest");
                        scriptQuest = q;
                        com.questclusters.editor.compat.kubejs.QuestUtils.setQuestDescription(q, "Made by a script",
                                com.questclusters.editor.compat.kubejs.QuestUtils.descriptionTable(List.of(List.of("a", "b"), List.of("1", "2")), true, "center"),
                                com.questclusters.editor.compat.kubejs.QuestUtils.descriptionVideo("intro/start.mp4", "Watch"));
                        com.questclusters.editor.compat.kubejs.QuestUtils.createChapterText("00000000000000C2", "Script text", "", 6, 7, 3, 0.5);
                        com.questclusters.editor.compat.kubejs.QuestUtils.createDecorativeAnchor("00000000000000C2", 7, 6);
                        com.questclusters.editor.compat.kubejs.QuestUtils.setQuestShape(q, "hexagon");
                    });
                    return 30;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    var imgs = file.getChapter(C2).getImages();
                    var pasted = imgs.stream().filter(i -> i.getImage().toString().startsWith("quest_clusters_editor:textures/pasted/")).findFirst();
                    DevSelfTest.moduleCheck(imgs.size() >= imagesBefore + 3 && pasted.isPresent(), "editor: pasted image became a chapter image");
                    pasted.ifPresent(p -> {
                        var rl = net.minecraft.resources.ResourceLocation.parse(p.getImage().toString());
                        DevSelfTest.moduleCheck(Minecraft.getInstance().getTextureManager().getTexture(rl, null) != null, "editor: pasted texture shown without reload");
                    });
                    // cleanup keeps the used image (only in memory so far) and deletes an unused one
                    try {
                        Path dir = FMLPaths.CONFIGDIR.get().resolve("quest_clusters_editor/assets/quest_clusters_editor/textures/pasted");
                        java.nio.file.Files.write(dir.resolve("clipboard_1.png"), new byte[]{1});
                        var msg = com.questclusters.editor.client.paste.PastedImages.cleanup().getString();
                        DevSelfTest.moduleCheck(!java.nio.file.Files.exists(dir.resolve("clipboard_1.png"))
                                && pasted.isPresent() && java.nio.file.Files.exists(dir.resolve(pasted.get().getImage().toString().substring(
                                "quest_clusters_editor:textures/pasted/".length()))), "editor: cleanup removes only unused images (" + msg + ")");
                    } catch (Exception e) {
                        DevSelfTest.moduleCheck(false, "editor: cleanup " + e);
                    }
                    Quest sq = scriptQuest == null ? null : file.getQuest(dev.ftb.mods.ftbquests.quest.QuestObjectBase.parseCodeString(scriptQuest));
                    DevSelfTest.moduleCheck(sq != null && sq.getTitle().getString().equals("Script quest") && sq.getRawDescription().size() == 3,
                            "editor: QuestUtils created a quest with title and description");
                    DevSelfTest.moduleCheck(imgs.stream().anyMatch(i -> CanvasElement.parse(i.getClickAction().actionData()) instanceof CanvasElement.Text t
                            && t.text().equals("Script text")), "editor: QuestUtils canvas text keeps its payload");
                    DevSelfTest.moduleCheck(sq != null && sq.getRawDescription().get(2).startsWith("{quest_enhance_video:"), "editor: video description line");
                    var line = sq == null ? null : sq.getDescription().get(2);
                    DevSelfTest.moduleCheck(line != null && line.getString().equals("Watch") && line.getStyle().getClickEvent() != null
                            && line.getStyle().getClickEvent().getValue().startsWith("quest_enhance_video/"), "editor: video link parsed");
                    var icon = com.questclusters.editor.EntityModels.withVideo(net.minecraft.world.item.ItemStack.EMPTY, "intro/start.mp4");
                    DevSelfTest.moduleCheck("intro/start.mp4".equals(com.questclusters.editor.EntityModels.videoOf(icon))
                            && com.questclusters.editor.EntityModels.isVideoPlaceholder(icon)
                            && com.questclusters.editor.EntityModels.withVideo(icon, "").isEmpty(), "editor: node video stored in the icon");
                    DevSelfTest.moduleCheck(!com.questclusters.editor.client.video.VideoSupport.available(), "editor: no WATERMeDIA in this run");
                    com.questclusters.editor.client.video.VideoSupport.open("intro/start.mp4"); // must only print a chat message
                    var qs = DevSelfTest.questScreen();
                    qs.questPanel.scrollTo(3, 3);
                    return 10;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_36_editor_e4");
                    return 5;
                }
        ));
    }
}
