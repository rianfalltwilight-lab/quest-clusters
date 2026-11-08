package com.questclusters.editor.client.video;

import com.questclusters.editor.QuestClustersEditor;
import com.questclusters.editor.data.VideoPaths;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

/** Finding and opening videos. Playback needs WATERMeDIA; everything else works without it. */
public final class VideoSupport {
    private VideoSupport() {
    }

    public static boolean available() {
        return ModList.get().isLoaded("watermedia");
    }

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve("quest_clusters_editor/videos");
    }

    /** Quest Enhance's video folder, still used for books made with it. */
    public static Path legacyFolder() {
        return FMLPaths.CONFIGDIR.get().resolve("quest_enhance/assets/quest_enhance/videos/ftb");
    }

    /** All videos to pick from: files in the video folders, then videos shipped in resource packs ("assets/..."). */
    public static List<String> list() {
        List<String> out = new ArrayList<>();
        for (Path dir : List.of(folder(), legacyFolder())) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.walk(dir, 8)) {
                s.filter(Files::isRegularFile).filter(p -> VideoPaths.isVideoFile(p.getFileName().toString()))
                        .map(p -> dir.relativize(p).toString().replace('\\', '/'))
                        .filter(p -> !out.contains(p)).forEach(out::add);
            } catch (IOException ignored) {
            }
        }
        Minecraft.getInstance().getResourceManager().listResources("videos", rl -> VideoPaths.isVideoFile(rl.getPath())).keySet()
                .forEach(rl -> {
                    String p = "assets/" + rl.getPath().substring("videos/".length());
                    if (!rl.getPath().startsWith("videos/ftb/") && !out.contains(p)) out.add(p);
                });
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    /** The playable file for a stored path, or null with the reason in {@code error[0]}. */
    public static Path resolve(String raw, String[] error) {
        String n = VideoPaths.normalise(raw);
        if (n == null) {
            error[0] = "invalid";
            return null;
        }
        if (VideoPaths.isBundled(n)) {
            String rel = n.substring("assets/".length());
            for (String ns : List.of(QuestClustersEditor.MOD_ID, "quest_enhance")) {
                ResourceLocation rl = ResourceLocation.tryBuild(ns, "videos/" + rel);
                if (rl == null) continue;
                var res = Minecraft.getInstance().getResourceManager().getResource(rl);
                if (res.isEmpty()) continue;
                try (InputStream in = res.get().open()) {
                    String ext = rel.contains(".") ? rel.substring(rel.lastIndexOf('.')).toLowerCase(Locale.ROOT) : ".mp4";
                    Path tmp = Path.of(System.getProperty("java.io.tmpdir"), "quest_clusters_editor_videos",
                            UUID.nameUUIDFromBytes(rl.toString().getBytes()) + ext);
                    Files.createDirectories(tmp.getParent());
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    tmp.toFile().deleteOnExit();
                    return tmp;
                } catch (IOException e) {
                    error[0] = "failed";
                    return null;
                }
            }
            error[0] = "missing";
            return null;
        }
        for (Path dir : List.of(folder(), legacyFolder())) {
            Path p = dir.resolve(n).normalize();
            if (p.startsWith(dir.normalize()) && Files.isRegularFile(p)) return p;
        }
        error[0] = "missing";
        return null;
    }

    /** Opens the player (or says why it cannot). */
    public static void open(String path) {
        Minecraft mc = Minecraft.getInstance();
        if (!available()) {
            mc.gui.getChat().addMessage(Component.translatable("quest_clusters_editor.video.need_watermedia").withStyle(ChatFormatting.RED));
            return;
        }
        String[] error = {null};
        Path file = resolve(path, error);
        mc.setScreen(WaterMediaPlayer.screen(mc.screen, path, file, error[0]));
    }
}
