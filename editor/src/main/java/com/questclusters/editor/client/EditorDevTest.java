package com.questclusters.editor.client;

import com.questclusters.platform.Rl;
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
            com.questclusters.platform.Ftbq.setEditMode(ServerQuestFile.INSTANCE, sp, on);
            serverLocked = !ServerQuestFile.INSTANCE.getChapter(C3).isVisible(ServerQuestFile.INSTANCE.getOrCreateTeamData(sp));
        });
    }

    private static void command(String cmd) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        String name = Minecraft.getInstance().player.getGameProfile().getName();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd.replace("@me", name)));
    }

    private static final net.minecraft.resources.ResourceLocation GIF =
            Rl.of("quest_clusters_editor", "textures/gui/example.gif");

    private static com.questclusters.editor.data.DescriptionTable tableUnderTest;
    private static volatile String scriptQuest;
    /** Chapter images the QuestUtils step adds (text + guide point); it runs on 1.21.1 only. */
    //? if neoforge {
    private static final int SCRIPT_IMAGES = 2;
    //?} else {
    /*private static final int SCRIPT_IMAGES = 0;
    *///?}
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
        return imgs.stream().filter(i -> type.isInstance(CanvasElement.parse(com.questclusters.platform.Ftbq.clickData(i)))).count();
    }

    private static void counters() {
        EditorDebug.hiddenPurple = EditorDebug.hiddenSkipped = EditorDebug.hiddenRevealed = 0;
    }

    static void register() {
        DevSelfTest.addModuleSteps(presetSteps());
        DevSelfTest.addModuleSteps(anchorInClusterSteps());
        DevSelfTest.addModuleSteps(animatedLineSteps());
        DevSelfTest.addModuleSteps(markupSteps());
        DevSelfTest.addModuleSteps(descEditorSteps());
        DevSelfTest.addModuleSteps(bookCheckSteps());
        DevSelfTest.addModuleSteps(treeSteps());
        DevSelfTest.addModuleSteps(perfSteps());
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
                    ChapterImage a7 = com.questclusters.platform.Ftbq.image(ClientQuestFile.INSTANCE, A7);
                    DevSelfTest.moduleCheck(a7 != null
                            && com.questclusters.platform.Ftbq.clickData(a7).startsWith("quest_enhance:text:"), "editor: legacy 'click' canvas text restored");
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
                            .getSprite(Rl.mc("block/oak_planks"));
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
                    // the entity models must really be drawn (pig pink on D8, zombie green on D4), not just be set
                    DevSelfTest.moduleCheck(colouredPixels(qs, 0xD8L, true) > 8, "editor: pig model drawn on its quest ("
                            + colouredPixels(qs, 0xD8L, true) + " px)");
                    DevSelfTest.moduleCheck(colouredPixels(qs, 0xD4L, false) > 8, "editor: zombie model drawn on its kill quest ("
                            + colouredPixels(qs, 0xD4L, false) + " px)");
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
                    DevSelfTest.moduleCheck(count(imgs, CanvasElement.Anchor.class) == 2, "editor: guide point created (" + count(imgs, CanvasElement.Anchor.class) + " guide points)");
                    DevSelfTest.moduleCheck(count(imgs, CanvasElement.Gif.class) == 1, "editor: GIF element created");
                    DevSelfTest.moduleCheck(imgs.stream().anyMatch(i -> CanvasElement.parse(com.questclusters.platform.Ftbq.clickData(i)) instanceof CanvasElement.Text t
                            && t.text().equals("Devtest text") && t.scale() == 1.5), "editor: canvas text created");
                    DevSelfTest.moduleCheck(Math.abs(GifTextures.aspect(GIF) - 1F) < 0.01F, "editor: GIF decoded");
                    DevSelfTest.undo();
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleCheck(!ClientEditor.book().excludedImages.contains(A7), "editor: undo reverts the last editor change");
                    com.questclusters.platform.Ftbq.copyImage(0xA5L, ClientQuestFile.INSTANCE.getChapter(C2), 5, 5);
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
                        if (CanvasElement.parse(com.questclusters.platform.Ftbq.clickData(i)) instanceof CanvasElement.Anchor a) {
                            anchors++;
                            keys.add(a.uuid());
                        }
                    }
                    DevSelfTest.moduleCheck(anchors == 3 && keys.size() == 3, "editor: copied guide point gets a new id (" + anchors + " guide points, " + keys.size() + " ids)");
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
                            //? if neoforge {
                            && iw.getComponent().getImage() instanceof com.questclusters.editor.client.desc.GifIcon), "editor: description GIF");
                            //?} else {
                            /*&& iw.getComponent().image instanceof com.questclusters.editor.client.desc.GifIcon), "editor: description GIF");
                            *///?}
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
                    var ed = com.questclusters.editor.client.desc.TableEditorScreen.last;
                    if (ed == null) {
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
                    // the QuestUtils script API (KubeJS) is 1.21.1 only
                    //? if neoforge {
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
                    //?}
                    return 30;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    var imgs = file.getChapter(C2).getImages();
                    var pasted = imgs.stream().filter(i -> i.getImage().toString().startsWith("quest_clusters_editor:textures/pasted/")).findFirst();
                    DevSelfTest.moduleCheck(imgs.size() >= imagesBefore + 1 + SCRIPT_IMAGES && pasted.isPresent(), "editor: pasted image became a chapter image");
                    pasted.ifPresent(p -> {
                        var rl = Rl.parse(p.getImage().toString());
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
                    //? if neoforge {
                    Quest sq = scriptQuest == null ? null : file.getQuest(dev.ftb.mods.ftbquests.quest.QuestObjectBase.parseCodeString(scriptQuest));
                    DevSelfTest.moduleCheck(sq != null && sq.getTitle().getString().equals("Script quest") && sq.getRawDescription().size() == 3,
                            "editor: QuestUtils created a quest with title and description");
                    DevSelfTest.moduleCheck(imgs.stream().anyMatch(i -> CanvasElement.parse(com.questclusters.platform.Ftbq.clickData(i)) instanceof CanvasElement.Text t
                            && t.text().equals("Script text")), "editor: QuestUtils canvas text keeps its payload");
                    DevSelfTest.moduleCheck(sq != null && sq.getRawDescription().get(2).startsWith("{quest_enhance_video:"), "editor: video description line");
                    var line = sq == null ? null : sq.getDescription().get(2);
                    DevSelfTest.moduleCheck(line != null && line.getString().equals("Watch") && line.getStyle().getClickEvent() != null
                            && line.getStyle().getClickEvent().getValue().startsWith("quest_enhance_video/"), "editor: video link parsed");
                    //?}
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

    /** Pixels inside a quest's button in the last frame that look pig-pink ({@code pink}) or zombie-green. */
    static int colouredPixels(dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen given, long questId, boolean pink) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        // measure on the quest screen actually shown (earlier steps may have closed and reopened the book)
        var qs = mc.screen instanceof dev.ftb.mods.ftblibrary.ui.ScreenWrapper sw
                && sw.getGui() instanceof dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen shown ? shown : given;
        for (var w : qs.questPanel.getWidgets()) {
            if (w instanceof dev.ftb.mods.ftbquests.client.gui.quests.QuestButton qb
                    && !(w instanceof dev.ftb.mods.ftbquests.client.gui.quests.QuestLinkButton)
                    && qb.moveAndDeleteFocus().getMovableID() == questId) {
                double sx = (double) mc.getWindow().getWidth() / mc.getWindow().getGuiScaledWidth();
                double sy = (double) mc.getWindow().getHeight() / mc.getWindow().getGuiScaledHeight();
                // quest buttons are placed in the panel's content space: undo the scroll
                double gx = qs.questPanel.getX() - qs.questPanel.getScrollX() + w.posX;
                double gy = qs.questPanel.getY() - qs.questPanel.getScrollY() + w.posY;
                int x0 = (int) (gx * sx), y0 = (int) (gy * sy);
                int x1 = (int) ((gx + w.width) * sx), y1 = (int) ((gy + w.height) * sy);
                try (var img = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    int n = 0;
                    for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
                        for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                            int abgr = img.getPixelRGBA(x, y);
                            int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
                            if (pink ? r > 170 && r - g > 40 && b > 90 : g - r > 25 && g - b > 15 && g > 70) {
                                n++;
                            }
                        }
                    }
                    return n;
                }
            }
        }
        return -1;
    }

    private static final com.questclusters.editor.data.LinePreset RED =
            new com.questclusters.editor.data.LinePreset("qcred", "Red", "minecraft:block/redstone_block");
    private static final com.questclusters.editor.data.LinePreset BLUE =
            new com.questclusters.editor.data.LinePreset("qcblue", "Blue", "minecraft:block/lapis_block");

    /** Decorative line presets: chapter default, node override, the node menu, deleting presets. */
    static List<java.util.function.IntSupplier> presetSteps() {
        return List.of(
                () -> {
                    var qs = DevSelfTest.questScreen();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(C2), false);
                    qs.questPanel.scrollTo(0, 0.5);
                    DevSelfTest.moduleCheck(ClientEditor.book().chapter(C2).decorativeLines.size() == 1, "presets: test chapter has one line");
                    var p = new com.questclusters.editor.data.EditorPatch();
                    p.presets = List.of(RED, BLUE);
                    var c = ClientEditor.book().chapter(C2).copy();
                    c.linePreset = RED.id();
                    p.chapters.put(C2, c);
                    EditorActions.send("line_presets", p);
                    return 40;
                },
                () -> {
                    var book = ClientEditor.book();
                    DevSelfTest.moduleCheck(book.linePresets.size() == 2 && RED.id().equals(book.chapter(C2).linePreset),
                            "presets: saved and synced (" + book.linePresets.size() + ")");
                    DevSelfTest.moduleCheck(count(px -> px[0] > 140 && px[1] < 60 && px[2] < 60) > 30,
                            "presets: chapter default draws the line with its image (red)");
                    String first = book.chapter(C2).decorativeLines.get(0).get(0);
                    var items = com.questclusters.editor.client.gui.EditorMenus.nodeLineItems(DevSelfTest.questScreen(), C2, first);
                    DevSelfTest.moduleCheck(items.size() == 2 + 2 + 1, "presets: node menu lists default + presets + manage (" + items.size() + ")");
                    EditorActions.editChapter("line_preset", C2, x -> x.nodeLinePresets.put(first, BLUE.id()));
                    return 40;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_38_line_preset_node");
                    int blue = count(px -> px[2] > 120 && px[0] < 80 && px[2] - px[1] > 40);
                    int red = count(px -> px[0] > 140 && px[1] < 60 && px[2] < 60);
                    // the line is blue now; a few red pixels elsewhere (toasts, badges) don't matter
                    DevSelfTest.moduleCheck(blue > 30 && blue > red * 5,
                            "presets: the node's own preset wins over the chapter default (blue " + blue + ", red " + red + ")");
                    new com.questclusters.editor.client.gui.LinePresetsScreen(DevSelfTest.questScreen()).openGui();
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_39_line_presets_screen");
                    EditorActions.send("line_presets", com.questclusters.editor.client.gui.LinePresetsScreen.patch(ClientEditor.book(), List.of()));
                    EditorActions.reopen(null);
                    return 40;
                },
                () -> {
                    var c = ClientEditor.book().chapter(C2);
                    DevSelfTest.moduleCheck(ClientEditor.book().linePresets.isEmpty() && c.linePreset.isEmpty() && c.nodeLinePresets.isEmpty(),
                            "presets: deleting presets clears the chapter and node references");
                    return 1;
                });
    }

    /** Pixels of the last frame matching {@code test} on {r, g, b}. */
    static int count(java.util.function.Predicate<int[]> test) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        try (var img = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            int n = 0;
            for (int y = 0; y < img.getHeight(); y += 2) {
                for (int x = 0; x < img.getWidth(); x += 2) {
                    int abgr = img.getPixelRGBA(x, y);
                    if (test.test(new int[]{abgr & 0xFF, (abgr >> 8) & 0xFF, (abgr >> 16) & 0xFF})) n++;
                }
            }
            return n;
        }
    }

    /** A guide point inside an expanded cluster keeps its grey look (the cluster's title text must not leave blending off). */
    static List<java.util.function.IntSupplier> anchorInClusterSteps() {
        long cluster = 0xF1L, anchor = 0xA1L;
        boolean[] wasExpanded = new boolean[1];
        return List.of(
                () -> {
                    var qs = DevSelfTest.questScreen();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(C1), false);
                    var c = com.questclusters.client.ClientClusters.store().get(cluster).orElse(null);
                    DevSelfTest.moduleCheck(c != null, "guide point check: test cluster exists");
                    if (c != null) {
                        wasExpanded[0] = com.questclusters.client.ClusterViewState.isExpanded(c);
                        com.questclusters.client.ClusterViewState.setExpanded(c, true);
                    }
                    var img = com.questclusters.platform.Ftbq.movable(ClientQuestFile.INSTANCE, anchor);
                    qs.questPanel.scrollTo(img.getX(), img.getY());
                    return 30;
                },
                () -> {
                    int[] px = regionPixels(DevSelfTest.questScreen(), anchor);
                    // a decorative line crosses this guide point, so compare the disc's white and grey parts
                    DevSelfTest.moduleCheck(px[0] > 0 && px[2] > 20 && px[1] < px[2],
                            "guide point inside an expanded cluster is drawn grey, not white (" + px[1] + " white, " + px[2]
                                    + " grey of " + px[0] + " px)");
                    com.questclusters.client.ClientClusters.store().get(cluster)
                            .ifPresent(c -> com.questclusters.client.ClusterViewState.setExpanded(c, wasExpanded[0]));
                    return 5;
                });
    }

    /** {total, near-white, mid-grey} pixels of the last frame inside a canvas object's widget. */
    static int[] regionPixels(dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen given, long movableId) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var qs = mc.screen instanceof dev.ftb.mods.ftblibrary.ui.ScreenWrapper sw
                && sw.getGui() instanceof dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen shown ? shown : given;
        for (var w : qs.questPanel.getWidgets()) {
            if (w instanceof dev.ftb.mods.ftbquests.client.gui.quests.QuestPositionableButton pb
                    && pb.moveAndDeleteFocus().getMovableID() == movableId) {
                double sx = (double) mc.getWindow().getWidth() / mc.getWindow().getGuiScaledWidth();
                double sy = (double) mc.getWindow().getHeight() / mc.getWindow().getGuiScaledHeight();
                double gx = qs.questPanel.getX() - qs.questPanel.getScrollX() + w.posX;
                double gy = qs.questPanel.getY() - qs.questPanel.getScrollY() + w.posY;
                int x0 = (int) (gx * sx), y0 = (int) (gy * sy);
                int x1 = (int) ((gx + w.width) * sx), y1 = (int) ((gy + w.height) * sy);
                try (var img = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    int total = 0, white = 0, grey = 0;
                    for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
                        for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                            int abgr = img.getPixelRGBA(x, y);
                            int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
                            total++;
                            if (r > 240 && g > 240 && b > 240) white++;
                            else if (r > 60 && r < 200 && Math.abs(r - g) < 20 && Math.abs(r - b) < 20) grey++;
                        }
                    }
                    return new int[]{total, white, grey};
                }
            }
        }
        return new int[]{0, 0, 0};
    }

    /** Dynamic line textures: .mcmeta frame animation, flowing along the line, GIF frames. */
    static List<java.util.function.IntSupplier> animatedLineSteps() {
        var magma = new com.questclusters.editor.data.LinePreset("qcmagma", "Magma", "minecraft:block/magma");
        var flow = new com.questclusters.editor.data.LinePreset("qcflow", "Flow", "minecraft:block/redstone_block", 1.3F);
        var gif = new com.questclusters.editor.data.LinePreset("qcgif", "Gif", "quest_clusters_editor:textures/gui/example.gif");
        var strip = new com.questclusters.editor.data.LinePreset("qcstrip", "Strip", STRIP);
        float[] sample = new float[2];
        java.util.Set<Float> seen = new java.util.HashSet<>();
        int[] polls = {0};
        return List.of(
                () -> {
                    var qs = DevSelfTest.questScreen();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(C2), false);
                    qs.questPanel.scrollTo(0, 0.5);
                    var p = new com.questclusters.editor.data.EditorPatch();
                    p.presets = List.of(magma, flow, gif);
                    var c = ClientEditor.book().chapter(C2).copy();
                    c.linePreset = magma.id();
                    p.chapters.put(C2, c);
                    EditorActions.send("line_presets", p);
                    return 20;
                },
                () -> {
                    // sample every tick until two different frames were seen (or ~3 s passed): a fixed delay can
                    // land on the same frame of the cycle on slow machines
                    seen.add(com.questclusters.editor.client.gui.DecorativeLinesWidget.lastV0);
                    return seen.size() >= 2 || ++polls[0] > 60 ? 0 : -1;
                },
                () -> {
                    DevSelfTest.moduleCheck(seen.size() >= 2, "animated lines: .mcmeta frames play (" + seen + ")");
                    EditorActions.editChapter("line_preset", C2, x -> x.linePreset = flow.id());
                    return 10;
                },
                () -> {
                    sample[1] = com.questclusters.editor.client.gui.DecorativeLinesWidget.lastFlow;
                    return 7;
                },
                () -> {
                    float f = com.questclusters.editor.client.gui.DecorativeLinesWidget.lastFlow;
                    DevSelfTest.moduleCheck(f != sample[1], "animated lines: a preset with a speed flows (" + sample[1] + " -> " + f + ")");
                    EditorActions.editChapter("line_preset", C2, x -> x.linePreset = gif.id());
                    return 10;
                },
                () -> {
                    var tex = com.questclusters.editor.client.gui.DecorativeLinesWidget.lastTexture;
                    DevSelfTest.moduleCheck(tex != null && tex.getPath().startsWith("dynamic_gif/"), "animated lines: GIF presets draw the GIF's frames (" + tex + ")");
                    DevSelfTest.moduleShot("qc_40_line_gif");
                    // a horizontal frame strip (4 frames side by side): drawn one frame at a time, as tiles
                    EditorActions.send("line_presets", com.questclusters.editor.client.gui.LinePresetsScreen.patch(ClientEditor.book(),
                            List.of(strip.withLook(0xFFFFFFFF, 3F))));
                    EditorActions.editChapter("line_preset", C2, x -> x.linePreset = strip.id());
                    return 20;
                },
                DevSelfTest.waitFrames(3),
                () -> {
                    var rl = com.questclusters.platform.Rl.parse(STRIP);
                    var f0 = com.questclusters.client.Textures.frame(rl, 0L);
                    var f1 = com.questclusters.client.Textures.frame(rl, 100L);
                    DevSelfTest.moduleCheck(f0 != null && f1 != null && Math.abs(f0.u1() - f0.u0() - 0.25F) < 1e-4 && f0.v0() == 0F && f0.v1() == 1F
                            && Math.abs(f1.u0() - 0.25F) < 1e-4, "animated textures: a horizontal strip has 4 frames side by side ("
                            + (f0 == null ? null : f0.u0() + ".." + f0.u1()) + ", then " + (f1 == null ? null : f1.u0()) + ")");
                    int colours = stripColoursOnScreen();
                    DevSelfTest.moduleCheck(colours == 1, "animated lines: a horizontal strip shows one frame at a time (" + colours + " frame colours on screen)");
                    DevSelfTest.moduleShot("qc_46_line_strip");
                    var bad = com.questclusters.client.Textures.animationProblem(com.questclusters.platform.Rl.parse("quest_clusters:textures/devtest/bad_frames.png"));
                    DevSelfTest.moduleCheck("quest_clusters.animation.uneven".equals(bad) && com.questclusters.client.Textures.animationProblem(rl) == null,
                            "animated textures: book check names an uneven frame size (" + bad + ")");
                    EditorActions.send("line_presets", com.questclusters.editor.client.gui.LinePresetsScreen.patch(ClientEditor.book(), List.of()));
                    // the same strip as a chapter image
                    var t = new net.minecraft.nbt.CompoundTag();
                    t.putDouble("x", -6);
                    t.putDouble("y", 6);
                    t.putDouble("width", 4);
                    t.putDouble("height", 4);
                    t.putString("image", STRIP);
                    com.questclusters.platform.Ftbq.createImage(com.questclusters.platform.Ftbq.newImage(ClientQuestFile.INSTANCE.getChapter(C2), t, null), false);
                    DevSelfTest.questScreen().questPanel.scrollTo(-6, 6);
                    return 30;
                },
                DevSelfTest.waitFrames(3),
                () -> {
                    int colours = stripColoursOnScreen();
                    DevSelfTest.moduleCheck(colours == 1, "animated images: a chapter image with a frame strip shows one frame (" + colours + " frame colours on screen)");
                    DevSelfTest.moduleShot("qc_47_image_strip");
                    return 5;
                });
    }

    static final String STRIP = "quest_clusters:textures/devtest/strip_h.png";

    /** How many of strip_h.png's 4 frame colours cover a visible area of the last frame. */
    static int stripColoursOnScreen() {
        int[][] cols = {{201, 13, 177}, {13, 201, 177}, {177, 13, 201}, {201, 177, 13}};
        int n = 0;
        for (int[] c : cols) {
            int px = count(p -> Math.abs(p[0] - c[0]) <= 3 && Math.abs(p[1] - c[1]) <= 3 && Math.abs(p[2] - c[2]) <= 3);
            if (px > 15) n++;
        }
        return n;
    }

    /** MQA-style description markup on the "Markup test" quest: layout, spoiler click, Shift+O. */
    static List<java.util.function.IntSupplier> markupSteps() {
        long quest = 0xA31L;
        boolean[] wasEditing = new boolean[1];
        return List.of(
                () -> {
                    wasEditing[0] = ClientQuestFile.canClientPlayerEdit();
                    var q = ClientQuestFile.INSTANCE.getQuest(quest);
                    DevSelfTest.moduleCheck(q != null, "markup: test quest exists");
                    if (q != null) DevSelfTest.questScreen().open(q, false);
                    return 40;
                },
                () -> {
                    int marked = 0;
                    var kinds = new java.util.ArrayList<String>();
                    for (int i = 0; i < 16; i++) {
                        var f = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, i);
                        var info = com.questclusters.editor.client.desc.LineRender.info(f);
                        if (info != null && !info.markup().isPlain()) marked++;
                        kinds.add(info == null ? "?" : info.markup().kind() + (info.markup().isPlain() ? "" : "*"));
                    }
                    DevSelfTest.moduleCheck(marked == 14, "markup: 14 of 16 lines marked up (" + marked + ") " + kinds);
                    var heading = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, 0);
                    DevSelfTest.moduleCheck(heading != null && heading.scale == 2F && heading.component != null, "markup: # heading drawn at 2x");
                    var bar = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, 3);
                    DevSelfTest.moduleCheck(bar != null && bar.getPosX() == com.questclusters.editor.client.desc.LineRender.BAR_PAD,
                            "markup: > bar line indented (" + (bar == null ? null : bar.getPosX()) + ")");
                    var gap = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, 8);
                    DevSelfTest.moduleCheck(gap != null && gap.height == 8, "markup: [gap=8] is 8 px high");
                    DevSelfTest.moduleShot("qc_41_markup");
                    var spoiler = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, 6);
                    DevSelfTest.moduleCheck(spoiler != null && !com.questclusters.editor.client.desc.LineRender.revealed(
                            com.questclusters.editor.client.desc.LineRender.info(spoiler)), "markup: spoiler starts covered");
                    if (spoiler != null) {
                        DevSelfTest.realMove(spoiler.getX() + 2, spoiler.getY() + 2);
                    }
                    return 5;
                },
                DevSelfTest.waitFrames(3), // FTB updates hovering when it draws
                hoverLine(quest, 6),
                () -> {
                    var spoiler = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, 6);
                    // a real click: FTB works out what is under the mouse itself
                    if (spoiler != null) DevSelfTest.click(spoiler.getX() + spoiler.width / 2D, spoiler.getY() + spoiler.height / 2D, 0);
                    return 5;
                },
                () -> {
                    var spoiler = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, 6);
                    DevSelfTest.moduleCheck(spoiler != null && com.questclusters.editor.client.desc.LineRender.revealed(
                            com.questclusters.editor.client.desc.LineRender.info(spoiler)), "markup: clicking the spoiler shows it");
                    editMode(true);
                    return 20;
                },
                () -> {
                    var line = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, 3);
                    if (line != null) DevSelfTest.realMove(line.getX() + 2, line.getY() + 2);
                    return 5;
                },
                DevSelfTest.waitFrames(3),
                hoverLine(quest, 3),
                () -> {
                    DevSelfTest.questScreen().keyPressed(new dev.ftb.mods.ftblibrary.ui.input.Key(
                            org.lwjgl.glfw.GLFW.GLFW_KEY_O, 0, org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT));
                    return 20;
                },
                () -> {
                    var raw = ClientQuestFile.INSTANCE.getQuest(quest).getRawDescription();
                    DevSelfTest.moduleCheck(raw.size() > 3 && raw.get(3).equals("[center]> Left bar text"),
                            "markup: Shift+O over a line centres it (" + (raw.size() > 3 ? raw.get(3) : "?") + ")");
                    if (raw.size() > 3) {
                        com.questclusters.editor.client.desc.DescriptionLines.set(ClientQuestFile.INSTANCE.getQuest(quest), DevSelfTest.questScreen().viewQuestPanel, 3, "> Left bar text");
                    }
                    editMode(wasEditing[0]);
                    return 20;
                });
    }

    /** Quest tree view: dependency nesting, reading order, chapter headings in the all-chapters view. */
    /** Book check: planted problems are found, the screen lists and jumps, the server command agrees with the server's data. */
    static List<java.util.function.IntSupplier> bookCheckSteps() {
        int[] commandResult = {-1}, serverCount = {-2};
        com.questclusters.editor.client.gui.BookCheckScreen[] screen = new com.questclusters.editor.client.gui.BookCheckScreen[1];
        return List.of(
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    var a = file.getQuest(0xA20L);
                    var b = file.getQuest(0xA21L);
                    DevSelfTest.moduleCheck(a != null && b != null, "book check: loop test quests exist");
                    if (a == null || b == null) return 1;
                    boolean ab = a.hasDependency(b), ba = b.hasDependency(a);
                    // a loop, on the client copy only, undone right away
                    if (!ab) a.addDependency(b);
                    if (!ba) b.addDependency(a);
                    var planted = com.questclusters.editor.data.BookCheck.run(file, ClientEditor.book(), tex -> false);
                    if (!ab) a.removeDependency(b);
                    if (!ba) b.removeDependency(a);
                    DevSelfTest.moduleCheck(planted.stream().anyMatch(i -> i.kind().equals("loop")), "book check: dependency loop found");
                    DevSelfTest.moduleCheck(planted.stream().anyMatch(i -> i.kind().equals("missing_chapter_background")),
                            "book check: missing chapter background found");
                    var real = com.questclusters.editor.client.gui.BookCheckScreen.runClient();
                    DevSelfTest.moduleCheck(real.stream().noneMatch(i -> i.kind().equals("loop") || i.kind().startsWith("missing_image")
                            || i.kind().endsWith("background")), "book check: the test book itself has no loops or missing textures "
                            + real.stream().map(i -> i.kind() + "=" + i.text().getString()).toList());
                    screen[0] = new com.questclusters.editor.client.gui.BookCheckScreen(DevSelfTest.questScreen());
                    screen[0].openGui();
                    return 10;
                },
                DevSelfTest.waitFrames(3),
                () -> {
                    DevSelfTest.moduleShot("qc_45_book_check");
                    var target = screen[0].issues().stream().filter(i -> ClientQuestFile.INSTANCE.getBase(i.object())
                            instanceof dev.ftb.mods.ftbquests.quest.Quest).findFirst().orElse(null);
                    if (target != null) {
                        com.questclusters.editor.client.gui.BookCheckScreen.jump(DevSelfTest.questScreen(), target);
                        DevSelfTest.moduleCheck(DevSelfTest.questScreen().getViewedQuest() != null
                                && DevSelfTest.questScreen().getViewedQuest().id == target.object(), "book check: clicking an issue opens its quest");
                    } else {
                        DevSelfTest.questScreen().openGui();
                    }
                    var server = Minecraft.getInstance().getSingleplayerServer();
                    server.execute(() -> {
                        serverCount[0] = com.questclusters.editor.data.BookCheck.run(ServerQuestFile.INSTANCE,
                                com.questclusters.editor.server.ServerEditor.book(), null).size();
                        try {
                            commandResult[0] = server.getCommands().getDispatcher().execute("questclusters check",
                                    server.createCommandSourceStack().withSuppressedOutput());
                        } catch (Exception e) {
                            commandResult[0] = -3;
                        }
                    });
                    return 10;
                },
                () -> {
                    DevSelfTest.moduleCheck(commandResult[0] == serverCount[0], "book check: /questclusters check reports "
                            + commandResult[0] + " issues (server data: " + serverCount[0] + ")");
                    if (DevSelfTest.questScreen().getViewedQuest() != null) DevSelfTest.questScreen().closeQuest();
                    return 5;
                });
    }

    /** Description editor: the markup entries of the Insert menu, and the preview of the cursor line. */
    static List<java.util.function.IntSupplier> descEditorSteps() {
        dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen[] screen = new dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen[1];
        int[] danger = new int[1];
        java.util.function.Predicate<int[]> red = p -> p[0] > p[1] + 25 && p[0] > p[2] + 25;
        return List.of(
                () -> {
                    var cfg = new dev.ftb.mods.ftblibrary.config.ListConfig<String, dev.ftb.mods.ftblibrary.config.StringConfig>(
                            new dev.ftb.mods.ftblibrary.config.StringConfig());
                    cfg.setValue(new java.util.ArrayList<>(List.of("Plain line", "[!danger] Danger box")));
                    screen[0] = new dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen(net.minecraft.network.chat.Component.literal("Desc test"), cfg, accepted -> {
                    });
                    screen[0].openGui();
                    return 10;
                },
                () -> {
                    var box = ((com.questclusters.editor.mixin.client.MultilineEditorAccessor) screen[0]).quest_clusters_editor$textBox();
                    box.seekCursor(net.minecraft.client.gui.components.Whence.END, 0);
                    return 2;
                },
                DevSelfTest.waitFrames(3),
                () -> {
                    danger[0] = count(red);
                    DevSelfTest.moduleShot("qc_44_desc_preview");
                    var box = ((com.questclusters.editor.mixin.client.MultilineEditorAccessor) screen[0]).quest_clusters_editor$textBox();
                    box.seekCursor(net.minecraft.client.gui.components.Whence.ABSOLUTE, 0);
                    return 2;
                },
                DevSelfTest.waitFrames(3),
                () -> {
                    int plain = count(red);
                    DevSelfTest.moduleCheck(danger[0] > plain + 40, "desc editor: preview draws the [!danger] box of the cursor line (" + danger[0] + " vs " + plain + ")");
                    var acc = (com.questclusters.editor.mixin.client.MultilineEditorAccessor) screen[0];
                    var items = com.questclusters.editor.client.desc.InsertComponents.markupItems(null, line -> acc.quest_clusters_editor$insertAtEndOfLine("\n" + line));
                    DevSelfTest.moduleCheck(items.size() == 16, "desc editor: 16 markup entries in Insert (" + items.size() + ")");
                    try {
                        var f = dev.ftb.mods.ftblibrary.ui.ContextMenuItem.class.getDeclaredField("callback");
                        f.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        var cb = (java.util.function.Consumer<dev.ftb.mods.ftblibrary.ui.Button>) f.get(items.get(1));
                        cb.accept(null);
                    } catch (ReflectiveOperationException e) {
                        DevSelfTest.moduleCheck(false, "desc editor: menu callback (" + e + ")");
                    }
                    String text = acc.quest_clusters_editor$textBox().getText();
                    DevSelfTest.moduleCheck(text.startsWith("Plain line\n## ") && text.endsWith("[!danger] Danger box"),
                            "desc editor: Insert > Subheading adds a '## ' line after the cursor line");
                    DevSelfTest.questScreen().openGui();
                    return 10;
                });
    }

    static List<java.util.function.IntSupplier> treeSteps() {
        boolean[] wasEditing = new boolean[1];
        return List.of(
                () -> {
                    wasEditing[0] = ClientQuestFile.canClientPlayerEdit();
                    editMode(true);
                    var c2 = ClientQuestFile.INSTANCE.getChapter(C2);
                    var rows = com.questclusters.editor.client.gui.QuestTreeScreen.rows(c2, false);
                    var ids = rows.stream().map(r -> Long.toHexString(r.quest().id) + "@" + r.depth()).toList();
                    int d5 = -1, d6 = -1;
                    for (int i = 0; i < rows.size(); i++) {
                        if (rows.get(i).quest().id == D5) d5 = i;
                        if (rows.get(i).quest().id == D6) d6 = i;
                    }
                    DevSelfTest.moduleCheck(d5 >= 0 && d6 == d5 + 1 && rows.get(d6).depth() == rows.get(d5).depth() + 1,
                            "tree: a quest sits under its prerequisite " + ids);
                    DevSelfTest.moduleCheck(rows.size() >= 2 && rows.get(0).quest().getY() <= rows.get(rows.size() - 1).quest().getY(),
                            "tree: roots in reading order");
                    var all = com.questclusters.editor.client.gui.QuestTreeScreen.rows(c2, true);
                    long headings = all.stream().filter(r -> r.quest() == null).count();
                    DevSelfTest.moduleCheck(headings >= 3, "tree: all chapters with headings (" + headings + ")");
                    new com.questclusters.editor.client.gui.QuestTreeScreen(DevSelfTest.questScreen()).openGui();
                    return 20;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_42_quest_tree");
                    DevSelfTest.questScreen().openGui();
                    editMode(wasEditing[0]);
                    return 20;
                });
    }

    /** Performance options: visibility cache (same answers), off-screen line culling, background team save. */
    static List<java.util.function.IntSupplier> perfSteps() {
        long[] mtime = new long[1];
        java.nio.file.Path[] teamFile = new java.nio.file.Path[1];
        boolean[] wasEditing = new boolean[1];
        return List.of(
                () -> {
                    // player mode: in edit mode FTB Quests never asks whether a quest is visible
                    wasEditing[0] = ClientQuestFile.canClientPlayerEdit();
                    editMode(false);
                    return 20;
                },
                () -> {
                    var qs = DevSelfTest.questScreen();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(C1), false);
                    return 10;
                },
                () -> {
                    var file = ClientQuestFile.INSTANCE;
                    DevSelfTest.moduleCheck(com.questclusters.editor.perf.VisibilityCache.size() > 0, "perf: visibility cache in use ("
                            + com.questclusters.editor.perf.VisibilityCache.size() + ")");
                    // cached answers equal fresh ones
                    java.util.Map<Long, Boolean> cached = new java.util.HashMap<>();
                    file.forAllQuests(q -> cached.put(q.id, q.isVisible(file.selfTeamData)));
                    com.questclusters.editor.EditorConfig.VISIBILITY_CACHE.set(false);
                    int[] diff = {0};
                    file.forAllQuests(q -> {
                        if (cached.get(q.id) != q.isVisible(file.selfTeamData)) diff[0]++;
                    });
                    com.questclusters.editor.EditorConfig.VISIBILITY_CACHE.set(true);
                    DevSelfTest.moduleCheck(diff[0] == 0, "perf: cached visibility matches uncached (" + diff[0] + " differ)");
                    com.questclusters.editor.perf.VisibilityCache.culledLines = 0;
                    DevSelfTest.questScreen().questPanel.scrollTo(500, 500); // far away: every line is off screen
                    return 10;
                },
                () -> {
                    DevSelfTest.moduleCheck(com.questclusters.editor.perf.VisibilityCache.culledLines > 0,
                            "perf: off-screen dependency lines skipped (" + com.questclusters.editor.perf.VisibilityCache.culledLines + ")");
                    DevSelfTest.questScreen().questPanel.scrollTo(0, 0);
                    var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
                    var team = ClientQuestFile.INSTANCE.selfTeamData.getTeamId();
                    teamFile[0] = server.getWorldPath(dev.ftb.mods.ftbquests.quest.ServerQuestFile.FTBQUESTS_DATA).resolve(team + ".snbt");
                    mtime[0] = teamFile[0].toFile().lastModified();
                    String name = net.minecraft.client.Minecraft.getInstance().player.getGameProfile().getName();
                    server.execute(() -> {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                                "ftbquests change_progress " + name + " complete 0000000000000A31");
                        dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE.saveNow();
                    });
                    return 40;
                },
                () -> {
                    DevSelfTest.moduleCheck(com.questclusters.editor.EditorServerConfig.ASYNC_TEAM_SAVE.get(), "perf: background team save on");
                    var f = teamFile[0].toFile();
                    boolean written = f.lastModified() > mtime[0];
                    String text = "";
                    try {
                        text = java.nio.file.Files.readString(teamFile[0]);
                    } catch (java.io.IOException ignored) {
                    }
                    DevSelfTest.moduleCheck(written && text.contains("0000000000000A31"), "perf: team progress written in the background ("
                            + written + ", " + text.length() + " chars)");
                    var server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
                    String name = net.minecraft.client.Minecraft.getInstance().player.getGameProfile().getName();
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                            "ftbquests change_progress " + name + " reset 0000000000000A31"));
                    editMode(wasEditing[0]);
                    return 20;
                });
    }

    /** Keeps the mouse on a description line until FTB reports it hovered (at most ~2 s). */
    static java.util.function.IntSupplier hoverLine(long quest, int line) {
        int[] tries = {0};
        return () -> {
            var f = com.questclusters.editor.client.desc.LineRender.fieldFor(quest, line);
            if (f == null || f.isMouseOver() || ++tries[0] > 40) {
                tries[0] = 0;
                return 0;
            }
            DevSelfTest.realMove(f.getX() + 2, f.getY() + 2);
            return -1;
        };
    }
}
