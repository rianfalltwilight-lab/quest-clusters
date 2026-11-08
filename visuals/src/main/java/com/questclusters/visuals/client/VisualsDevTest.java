package com.questclusters.visuals.client;

import com.questclusters.client.DevSelfTest;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.IntSupplier;

/** Visuals checks for the dev self-test (only registered with -Dquest_clusters.devtest). */
final class VisualsDevTest {
    private static double size0;
    private static int zoom0;

    private VisualsDevTest() {
    }

    static void register() {
        DevSelfTest.addModuleSteps(List.<IntSupplier>of(
                () -> {
                    var qs = ClientQuestFile.openGui();
                    qs.open(ClientQuestFile.INSTANCE.getChapter(0xC1L), false);
                    DevSelfTest.moduleCheck(VisualsConfig.QUEST_HOVER_GLOW_ALPHA.get() == 77,
                            "visuals: Certain Questing Additions settings imported (glow " + VisualsConfig.QUEST_HOVER_GLOW_ALPHA.get() + ")");
                    DevSelfTest.moduleCheck(hasField(QuestButton.class, "quest_clusters_visuals$hover")
                            && hasField(ViewQuestPanel.class, "quest_clusters_visuals$open"), "visuals: hover and details animations applied");
                    return 10;
                },
                () -> {
                    var qs = DevSelfTest.questScreen();
                    size0 = qs.getQuestButtonSize();
                    zoom0 = qs.getZoom();
                    qs.addZoom(1);
                    long started = System.nanoTime();
                    DevSelfTest.moduleCheck(qs.getZoom() == Math.min(28, zoom0 + 4), "visuals: wheel sets the zoom target at once (" + zoom0 + " -> " + qs.getZoom() + ")");
                    DevSelfTest.moduleCheck(Math.abs(qs.getQuestButtonSize() - size0) < 0.5, "visuals: button size starts gliding, not jumping");
                    // the glide runs on wall-clock time (0.2 s): sample it 60 ms in, whatever the frame rate
                    try {
                        Thread.sleep(Math.max(0, 60 - (System.nanoTime() - started) / 1_000_000));
                    } catch (InterruptedException ignored) {
                    }
                    double mid = qs.getQuestButtonSize();
                    DevSelfTest.moduleCheck(mid > size0 + 0.01 && mid < qs.getZoom() * 1.5 - 0.01,
                            "visuals: smooth zoom in between (" + String.format("%.2f", mid) + ")");
                    return 20;
                },
                () -> {
                    var qs = DevSelfTest.questScreen();
                    DevSelfTest.moduleCheck(Math.abs(qs.getQuestButtonSize() - qs.getZoom() * 1.5) < 0.01, "visuals: smooth zoom arrives");
                    qs.addZoom(-1);
                    Icon e = Icon.getIcon("entity:minecraft:creeper[rotate=true]");
                    DevSelfTest.moduleCheck(e instanceof EntityIcon, "visuals: entity icon strings give entity icons");
                    DevSelfTest.moduleCheck(!(Icon.getIcon("entity:minecraft:zombie[rotate=maybe]") instanceof EntityIcon), "visuals: invalid entity icon falls back");
                    ResourceLocation apng = ResourceLocation.fromNamespaceAndPath("quest_clusters_visuals", "textures/gui/example.apng.png");
                    DevSelfTest.moduleCheck(ApngTextures.bindIfAnimated(apng), "visuals: .apng.png binds an animation frame");
                    DevSelfTest.moduleCheck(!ApngTextures.bindIfAnimated(ResourceLocation.withDefaultNamespace("textures/block/stone.png")),
                            "visuals: plain textures untouched");
                    var ch = ClientQuestFile.INSTANCE.getChapter(0xC1L);
                    DevSelfTest.moduleCheck(ChapterShaders.shaderFor(ch) != null, "visuals: chapter background shader compiled");
                    VisualsConfig.SHADER_BACKGROUND.set(true);
                    qs.questPanel.scrollTo(0, 3);
                    return 25;
                },
                () -> {
                    DevSelfTest.moduleShot("qc_20_visuals_shader_entity_apng");
                    VisualsConfig.SHADER_BACKGROUND.set(false);
                    return 5;
                }
        ));
    }

    private static boolean hasField(Class<?> c, String name) {
        for (var f : c.getDeclaredFields()) {
            if (f.getName().contains(name)) return true;
        }
        return false;
    }
}
