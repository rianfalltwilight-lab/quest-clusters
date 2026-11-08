package com.questclusters.editor.client.video;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.questclusters.editor.QuestClustersEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.watermedia.WaterMedia;
import org.watermedia.api.player.MediaPlayer;
import org.watermedia.api.player.PlayerAPI;

import java.nio.file.Path;

/**
 * Full-screen video player on WATERMeDIA 3 (only loaded when it is installed). Space: play / pause / replay,
 * Left / Right: back / forward, F: full screen, Esc: back to the previous screen.
 */
final class WaterMediaPlayer extends Screen {
    private static final float[] SPEEDS = {0.5F, 1.0F, 1.5F, 2.0F};
    private final Screen previous;
    private final String path;
    private final Path file;
    private String error;
    private MediaPlayer player;
    private int speed = 1, volume = 100;
    private boolean draggingTime, draggingVolume;
    private Button playButton, speedButton, muteButton;

    private WaterMediaPlayer(Screen previous, String path, Path file, String error) {
        super(Component.translatable("quest_clusters_editor.video.title"));
        this.previous = previous;
        this.path = path;
        this.file = file;
        this.error = error;
    }

    static Screen screen(Screen previous, String path, Path file, String error) {
        return new WaterMediaPlayer(previous, path, file, error);
    }

    private void start() {
        if (file == null || player != null) return;
        if (!WaterMedia.loaded()) {
            error = "backend";
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            Thread render = Thread.currentThread();
            player = PlayerAPI.player(PlayerAPI.mrl(file.toUri()), () -> PlayerAPI.glEngine(render, mc), PlayerAPI::alEngine);
            player.volume(volume);
            player.speed(SPEEDS[speed]);
            player.start();
        } catch (Throwable t) {
            QuestClustersEditor.LOGGER.warn("Video playback failed for {}", path, t);
            error = "failed";
        }
    }

    @Override
    protected void init() {
        start();
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose()).bounds(6, 6, 50, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("quest_clusters_editor.video.fullscreen"),
                b -> minecraft.getWindow().toggleFullScreen()).bounds(width - 86, 6, 80, 20).build());
        int y = height - 26;
        playButton = addRenderableWidget(Button.builder(Component.empty(), b -> playPause()).bounds(6, y, 60, 20).build());
        speedButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            speed = (speed + 1) % SPEEDS.length;
            if (player != null) player.speed(SPEEDS[speed]);
        }).bounds(70, y, 40, 20).build());
        muteButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (player != null) player.mute(!player.mute());
        }).bounds(width - 116, y, 50, 20).build());
    }

    private void playPause() {
        if (player == null) return;
        if (player.ended() || player.stopped() || player.error()) {
            player.release();
            player = null;
            error = null;
            start();
        } else {
            player.togglePlay();
        }
    }

    @Override
    public void tick() {
        if (player != null) {
            boolean ended = player.ended() || player.stopped() || player.error();
            playButton.setMessage(Component.translatable(ended ? "quest_clusters_editor.video.replay"
                    : player.paused() ? "quest_clusters_editor.video.play" : "quest_clusters_editor.video.pause"));
            muteButton.setMessage(Component.translatable(player.mute() ? "quest_clusters_editor.video.unmute" : "quest_clusters_editor.video.mute"));
            if (player.error() && error == null) error = "failed";
        }
        speedButton.setMessage(Component.literal(SPEEDS[speed] + "x"));
    }

    // progress bar: between the speed button and the volume bar; volume: right of mute
    private int barX0() {
        return 116;
    }

    private int barX1() {
        return width - 190;
    }

    private int volX0() {
        return width - 62;
    }

    private int volX1() {
        return width - 8;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF000000);
        if (player != null && player.texture() != MediaPlayer.NO_TEXTURE && player.width() > 0 && player.height() > 0) {
            float aspect = player.width() / (float) player.height();
            int areaH = height - 64, w = width, h = Math.round(w / aspect);
            if (h > areaH) {
                h = areaH;
                w = Math.round(h * aspect);
            }
            int x = (width - w) / 2, y = 32 + (areaH - h) / 2;
            RenderSystem.setShaderTexture(0, (int) player.texture());
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f m = g.pose().last().pose();
            var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buf.addVertex(m, x, y, 0).setUv(0, 0);
            buf.addVertex(m, x, y + h, 0).setUv(0, 1);
            buf.addVertex(m, x + w, y + h, 0).setUv(1, 1);
            buf.addVertex(m, x + w, y, 0).setUv(1, 0);
            BufferUploader.drawWithShader(buf.buildOrThrow());
        }
        if (error != null) {
            Component msg = Component.translatable("quest_clusters_editor.video.error." + error, path);
            g.drawCenteredString(font, msg, width / 2, height / 2 - 4, 0xFFFF5555);
        } else if (player != null && (player.loading() || player.buffering() || player.waiting())) {
            g.drawCenteredString(font, Component.translatable("quest_clusters_editor.video.loading"), width / 2, height / 2 - 4, 0xFFAAAAAA);
        }
        // progress
        int by = height - 18;
        g.fill(barX0(), by, barX1(), by + 4, 0xFF333333);
        if (player != null && player.duration() > 0) {
            float f = Math.min(1F, player.time() / (float) player.duration());
            g.fill(barX0(), by, barX0() + Math.round((barX1() - barX0()) * f), by + 4, 0xFF00B4B4);
            g.drawString(font, time(player.time()) + " / " + time(player.duration()), barX1() + 6, by - 2, 0xFFDDDDDD);
        }
        // volume
        g.fill(volX0(), by, volX1(), by + 4, 0xFF333333);
        g.fill(volX0(), by, volX0() + (volX1() - volX0()) * volume / 100, by + 4, 0xFFDDDDDD);
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    private static String time(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (my >= height - 22 && my <= height - 10) {
            if (mx >= barX0() && mx <= barX1()) {
                draggingTime = true;
                seek(mx);
                return true;
            }
            if (mx >= volX0() && mx <= volX1()) {
                draggingVolume = true;
                setVolume(mx);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingTime) seek(mx);
        else if (draggingVolume) setVolume(mx);
        return draggingTime || draggingVolume || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingTime = draggingVolume = false;
        return super.mouseReleased(mx, my, button);
    }

    private void seek(double mx) {
        if (player != null && player.canSeek() && player.duration() > 0) {
            double f = Math.max(0, Math.min(1, (mx - barX0()) / (barX1() - barX0())));
            player.seek(Math.round(player.duration() * f));
        }
    }

    private void setVolume(double mx) {
        volume = (int) Math.round(Math.max(0, Math.min(1, (mx - volX0()) / (volX1() - volX0()))) * 100);
        if (player != null) player.volume(volume);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_SPACE -> playPause();
            case GLFW.GLFW_KEY_LEFT -> {
                if (player != null) player.rewind();
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (player != null) player.forward();
            }
            case GLFW.GLFW_KEY_F -> minecraft.getWindow().toggleFullScreen();
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public void removed() {
        if (player != null) {
            try {
                player.stop();
                player.release();
            } catch (Throwable ignored) {
            }
            player = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
