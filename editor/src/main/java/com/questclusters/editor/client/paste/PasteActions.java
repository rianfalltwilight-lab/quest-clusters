package com.questclusters.editor.client.paste;

import com.questclusters.editor.QuestClustersEditor;
import com.questclusters.editor.mixin.client.MultilineEditorAccessor;
import com.questclusters.mixin.client.QuestScreenAccessor;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.ui.misc.SimpleToast;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.CreateObjectMessage;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;

/** Pasting a clipboard image onto the canvas or into a quest description. */
public final class PasteActions {
    private PasteActions() {
    }

    /** True if the text clipboard is empty, i.e. an image may be there (avoids a slow image read on every Ctrl+V). */
    public static boolean maybeImage() {
        if (ClipboardImage.testOverride != null) return true;
        String s = Minecraft.getInstance().keyboardHandler.getClipboard();
        return s == null || s.isEmpty();
    }

    /** @return true if an image was pasted */
    public static boolean pasteOnCanvas(QuestScreen screen, boolean quiet) {
        Chapter chapter = ((QuestScreenAccessor) screen).quest_clusters$getSelectedChapter();
        if (chapter == null || !ClientQuestFile.INSTANCE.canEdit()) return false;
        BufferedImage img = ClipboardImage.read();
        if (img == null) {
            if (!quiet) SimpleToast.error(Component.translatable("quest_clusters_editor.paste.no_image"), Component.empty());
            return false;
        }
        ResourceLocation rl;
        try {
            rl = PastedImages.save(img);
        } catch (Exception e) {
            QuestClustersEditor.LOGGER.warn("Could not save pasted image", e);
            SimpleToast.error(Component.translatable("quest_clusters_editor.paste.failed"), Component.literal(e.toString()));
            return true;
        }
        double bs = Math.max(1, screen.getQuestButtonSize());
        double w = img.getWidth() / bs, h = img.getHeight() / bs;
        double longest = Math.max(w, h);
        if (longest > 8) {
            w *= 8 / longest;
            h *= 8 / longest;
        }
        var xy = ((QuestScreenAccessor) screen).quest_clusters$invokeGetSnappedXY();
        ChapterImage ci = new ChapterImage(0L, chapter);
        CompoundTag t = new CompoundTag();
        t.putDouble("x", xy.getFirst());
        t.putDouble("y", xy.getSecond());
        t.putDouble("width", Math.max(0.25, w));
        t.putDouble("height", Math.max(0.25, h));
        t.putString("image", rl.toString());
        ci.readData(t, ClientQuestFile.INSTANCE.holderLookup());
        NetworkManager.sendToServer(CreateObjectMessage.requestCreation(ci, false));
        return true;
    }

    /** @return true if an image was pasted (as a new description line) */
    public static boolean pasteInDescription(MultilineTextEditorScreen screen) {
        BufferedImage img = ClipboardImage.read();
        if (img == null) return false;
        try {
            ResourceLocation rl = PastedImages.save(img);
            int w = 200, h = Math.max(1, Math.round(w * img.getHeight() / (float) img.getWidth()));
            ((MultilineEditorAccessor) screen).quest_clusters_editor$insertAtEndOfLine(
                    "\n{image:" + rl + " width:" + w + " height:" + h + " align:center fit:true}");
        } catch (Exception e) {
            QuestClustersEditor.LOGGER.warn("Could not save pasted image", e);
        }
        return true;
    }
}
