package com.questclusters.editor.client.paste;

import com.mojang.blaze3d.platform.NativeImage;
import com.questclusters.editor.QuestClustersEditor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Pasted images: saved as PNG in {@code config/quest_clusters_editor/assets/quest_clusters_editor/textures/pasted/}
 * (a resource pack the editor registers), shown at once without a resource reload. Quest Enhance's pasted images
 * ({@code quest_enhance:textures/ftb/...} in {@code config/quest_enhance/}) keep working the same way.
 * The files exist only on the machine that pasted them: share them in a resource pack for other players.
 */
public final class PastedImages {
    public static final String NAMESPACE = QuestClustersEditor.MOD_ID, PATH = "textures/pasted/";
    private static final Pattern FILE = Pattern.compile("clipboard_[0-9]+\\.png");
    private static final Pattern REF = Pattern.compile("(?:quest_clusters_editor:textures/pasted/|quest_enhance:textures/ftb/)(clipboard_[0-9]+\\.png)");

    private PastedImages() {
    }

    public static Path packRoot() {
        return FMLPaths.CONFIGDIR.get().resolve("quest_clusters_editor");
    }

    public static Path legacyPackRoot() {
        return FMLPaths.CONFIGDIR.get().resolve("quest_enhance");
    }

    static Path folder() {
        return packRoot().resolve("assets/" + NAMESPACE + "/" + PATH);
    }

    static Path legacyFolder() {
        return legacyPackRoot().resolve("assets/quest_enhance/textures/ftb");
    }

    /** Saves the image and registers its texture; returns its resource location. */
    public static ResourceLocation save(BufferedImage img) throws IOException {
        Files.createDirectories(folder());
        String name = "clipboard_" + System.currentTimeMillis() + ".png";
        Path file = folder().resolve(name);
        ImageIO.write(img, "png", file.toFile());
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(NAMESPACE, PATH + name);
        ensureRegistered(rl);
        return rl;
    }

    /** File behind a pasted-image location, or null if the location is not one. */
    static Path fileOf(ResourceLocation rl) {
        String name = null;
        Path dir = null;
        if (rl.getNamespace().equals(NAMESPACE) && rl.getPath().startsWith(PATH)) {
            name = rl.getPath().substring(PATH.length());
            dir = folder();
        } else if (rl.getNamespace().equals("quest_enhance") && rl.getPath().startsWith("textures/ftb/")) {
            name = rl.getPath().substring("textures/ftb/".length());
            dir = legacyFolder();
        }
        return name == null || name.contains("/") || name.contains("..") ? null : dir.resolve(name);
    }

    /** Registers a pasted image's texture from its file if the resource packs have not loaded it yet. */
    public static void ensureRegistered(ResourceLocation rl) {
        Path file = fileOf(rl);
        if (file == null) return;
        var tm = Minecraft.getInstance().getTextureManager();
        AbstractTexture existing = tm.getTexture(rl, null);
        if (existing != null || !Files.isRegularFile(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            tm.register(rl, new DynamicTexture(NativeImage.read(in)));
        } catch (IOException e) {
            QuestClustersEditor.LOGGER.warn("Could not load pasted image {}: {}", file, e.toString());
        }
    }

    /** Deletes pasted images no quest book file refers to. Returns the chat message to show. */
    public static Component cleanup() {
        Set<String> used = new HashSet<>();
        Path game = FMLPaths.GAMEDIR.get();
        // what the loaded book uses counts too: a fresh paste may not be saved to disk yet
        var file = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
        if (file != null) {
            for (var ch : file.getAllChapters()) {
                ch.getImages().forEach(img -> collect(img.getImage().toString(), used));
                ch.getQuests().forEach(q -> q.getRawDescription().forEach(l -> collect(l, used)));
            }
        }
        try {
            for (Path root : List.of(game.resolve("config/ftbquests"), game.resolve("defaultconfigs/ftbquests"), game.resolve("local/ftbquests"))) {
                scan(root, used);
            }
            Path saves = game.resolve("saves");
            if (Files.isDirectory(saves)) {
                try (Stream<Path> s = Files.walk(saves, 5)) {
                    for (Path p : s.filter(p -> Files.isDirectory(p) && p.getFileName().toString().equals("ftbquests")).toList()) scan(p, used);
                }
            }
        } catch (IOException e) {
            return Component.translatable("quest_clusters_editor.cleanup.failed", e.getMessage()).withStyle(ChatFormatting.RED);
        }
        long freed = 0;
        int deleted = 0;
        var tm = Minecraft.getInstance().getTextureManager();
        for (Path dir : List.of(folder(), legacyFolder())) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> s = Files.list(dir)) {
                for (Path f : s.toList()) {
                    String n = f.getFileName().toString();
                    if (FILE.matcher(n).matches() && !used.contains(n)) {
                        freed += Files.size(f);
                        Files.delete(f);
                        deleted++;
                        String ns = dir.equals(folder()) ? NAMESPACE + ":" + PATH : "quest_enhance:textures/ftb/";
                        tm.release(ResourceLocation.parse(ns + n));
                    }
                }
            } catch (IOException e) {
                return Component.translatable("quest_clusters_editor.cleanup.failed", e.getMessage()).withStyle(ChatFormatting.RED);
            }
        }
        String size = freed >= 1 << 20 ? String.format("%.1f MiB", freed / 1048576.0) : String.format("%.1f KiB", freed / 1024.0);
        return Component.translatable("quest_clusters_editor.cleanup.done", deleted, size);
    }

    private static void collect(String text, Set<String> used) {
        Matcher m = REF.matcher(text);
        while (m.find()) used.add(m.group(1));
    }

    private static void scan(Path root, Set<String> used) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> s = Files.walk(root, 5)) {
            for (Path f : s.filter(Files::isRegularFile).toList()) {
                String n = f.getFileName().toString();
                if (n.endsWith(".snbt") || n.endsWith(".json") || n.endsWith(".txt") || n.endsWith(".toml")) {
                    Matcher m = REF.matcher(new String(Files.readAllBytes(f), java.nio.charset.StandardCharsets.UTF_8));
                    while (m.find()) used.add(m.group(1));
                }
            }
        }
    }
}
