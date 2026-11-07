package com.questclusters.visuals.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.questclusters.visuals.QuestClustersVisuals;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shader backgrounds for the quest screen, chosen per chapter by {@code assets/<ns>/chapter_shaders.json} in resource
 * packs:
 * <pre>{ "default": "namespace:shader", "chapters": { "&lt;chapter id hex or file name&gt;": "namespace:shader" } }</pre>
 * Both {@code quest_clusters_visuals} and (for packs made for Certain Questing Additions) {@code certain_questing_additions}
 * are read; ours wins. Shaders are core shaders ({@code shaders/core/<name>.json}) with the uniforms
 * {@code size} (px), {@code scrollOffset} (px), {@code scrollSize} (px), {@code time} (s) and {@code zoom}.
 * Off unless "shader_background" is enabled in the settings.
 */
public final class ChapterShaders {
    public static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath(QuestClustersVisuals.MOD_ID, "chapter_background");
    static final ResourceLocation CQA_DEFAULT = ResourceLocation.fromNamespaceAndPath("certain_questing_additions", "custom_background");
    private static final String[] NAMESPACES = {QuestClustersVisuals.MOD_ID, "certain_questing_additions"};

    private static final Map<ResourceLocation, ShaderInstance> SHADERS = new HashMap<>();
    private static Map<String, ResourceLocation> byChapter = Map.of();
    private static ResourceLocation defaultShader = DEFAULT;

    private ChapterShaders() {
    }

    /** Resource reload: read the mappings, then compile every shader they mention. */
    public static void registerShaders(RegisterShadersEvent event) {
        ResourceProvider rp = event.getResourceProvider();
        Map<String, ResourceLocation> chapters = new HashMap<>();
        ResourceLocation def = null;
        // read the legacy namespace first so ours overrides it
        for (int i = NAMESPACES.length - 1; i >= 0; i--) {
            var res = rp.getResource(ResourceLocation.fromNamespaceAndPath(NAMESPACES[i], "chapter_shaders.json"));
            if (res.isEmpty()) continue;
            try (Reader r = res.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                if (root.has("default")) {
                    ResourceLocation d = ResourceLocation.tryParse(root.get("default").getAsString());
                    if (d != null) def = d;
                }
                if (root.has("chapters") && root.get("chapters").isJsonObject()) {
                    root.getAsJsonObject("chapters").entrySet().forEach(e -> {
                        ResourceLocation id = e.getValue().isJsonPrimitive() ? ResourceLocation.tryParse(e.getValue().getAsString()) : null;
                        if (id != null) chapters.put(normalize(e.getKey()), id);
                    });
                }
            } catch (Exception e) {
                QuestClustersVisuals.LOGGER.warn("Ignoring broken {}:chapter_shaders.json: {}", NAMESPACES[i], e.toString());
            }
        }
        byChapter = Map.copyOf(chapters);
        defaultShader = def != null ? def : DEFAULT;

        Set<ResourceLocation> ids = new LinkedHashSet<>();
        ids.add(DEFAULT);
        ids.add(CQA_DEFAULT);
        ids.add(defaultShader);
        ids.addAll(chapters.values());
        SHADERS.clear();
        for (ResourceLocation id : ids) {
            try {
                event.registerShader(new ShaderInstance(rp, id, DefaultVertexFormat.POSITION_TEX), s -> SHADERS.put(id, s));
            } catch (Exception e) {
                QuestClustersVisuals.LOGGER.warn("Chapter shader {} could not be loaded: {}", id, e.toString());
            }
        }
    }

    static String normalize(String key) {
        String k = key.trim().toLowerCase(Locale.ROOT);
        if (k.startsWith("0x")) k = k.substring(2);
        if (k.startsWith("#")) k = k.substring(1);
        return k;
    }

    public static ShaderInstance shaderFor(Chapter chapter) {
        ResourceLocation id = defaultShader;
        if (chapter != null) {
            ResourceLocation c = byChapter.get(normalize(chapter.getCodeString()));
            if (c == null) {
                // also accept the chapter's file name ("chapters/<name>.snbt")
                c = chapter.getPath().map(p -> p.getFileName().toString().replace(".snbt", ""))
                        .map(n -> byChapter.get(normalize(n))).orElse(null);
            }
            if (c != null) id = c;
        }
        ShaderInstance s = SHADERS.get(id);
        return s != null ? s : SHADERS.get(DEFAULT);
    }

    /**
     * Fills (x, y, w, h) with the chapter's shader.
     *
     * @param scrollX/scrollY centre of the visible canvas area, px
     * @param scrollW/scrollH whole canvas size, px
     */
    public static boolean draw(GuiGraphics g, Chapter chapter, int x, int y, int w, int h,
                               double scrollX, double scrollY, double scrollW, double scrollH, float zoom) {
        ShaderInstance shader = shaderFor(chapter);
        if (shader == null) return false;
        Minecraft mc = Minecraft.getInstance();
        float time = mc.level == null ? (System.currentTimeMillis() % 3_600_000L) / 1000F
                : ((mc.level.getGameTime() % 100_000L) + mc.getTimer().getGameTimeDeltaPartialTick(true)) / 20F;
        set(shader, "size", w, h);
        set(shader, "scrollOffset", (float) scrollX, (float) scrollY);
        set(shader, "scrollSize", (float) scrollW, (float) scrollH);
        if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
        if (shader.getUniform("zoom") != null) shader.getUniform("zoom").set(zoom);

        RenderSystem.setShader(() -> shader);
        RenderSystem.disableBlend();
        var m = g.pose().last().pose();
        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(m, x, y + h, 0).setUv(0, 1);
        buf.addVertex(m, x + w, y + h, 0).setUv(1, 1);
        buf.addVertex(m, x + w, y, 0).setUv(1, 0);
        buf.addVertex(m, x, y, 0).setUv(0, 0);
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.enableBlend();
        return true;
    }

    private static void set(ShaderInstance s, String name, float a, float b) {
        var u = s.getUniform(name);
        if (u != null) u.set(a, b);
    }
}
