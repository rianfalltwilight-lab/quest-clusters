package com.questclusters.editor.mixin.client;

import com.questclusters.editor.EditorConfig;
import com.questclusters.editor.client.desc.LineMarkup;
import com.questclusters.editor.client.desc.LineRender;
import dev.ftb.mods.ftblibrary.ui.MultilineTextBox;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Description editor: a strip under the text box shows the line under the cursor as the quest view will draw it
 * (line markup, headings, callouts, colours). The text box gives up that much height.
 */
@Mixin(value = MultilineTextEditorScreen.class, remap = false)
public abstract class DescriptionPreviewMixin {
    @Unique
    private static final int quest_clusters_editor$PREVIEW = 34;

    @Shadow
    @Final
    private Panel textBoxPanel;
    @Shadow
    @Final
    private MultilineTextBox textBox;
    @Shadow
    @Final
    private PanelScrollBar scrollBar;

    @Inject(method = "alignWidgets", at = @At("TAIL"))
    private void quest_clusters_editor$shrink(CallbackInfo ci) {
        if (!quest_clusters_editor$enabled()) return;
        textBoxPanel.setHeight(Math.max(20, textBoxPanel.height - quest_clusters_editor$PREVIEW));
        textBoxPanel.alignWidgets();
        scrollBar.setHeight(Math.max(20, scrollBar.height - quest_clusters_editor$PREVIEW));
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void quest_clusters_editor$draw(GuiGraphics g, Theme theme, int x, int y, int w, int h, CallbackInfo ci) {
        if (!quest_clusters_editor$enabled()) return;
        int top = textBoxPanel.getY() + textBoxPanel.height + 3, left = textBoxPanel.getX(), width = textBoxPanel.width;
        int bottom = y + h - 2;
        if (bottom - top < 12) return;
        g.fill(left, top, left + width, bottom, 0xC0101010);
        g.enableScissor(left, top, left + width, bottom);
        String line = quest_clusters_editor$currentLine(textBox.getText(), textBox.cursorPos());
        theme.drawString(g, Component.translatable("quest_clusters_editor.preview"), left + 3, top + 2, dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0x707070), 0);
        if (!line.isEmpty()) {
            LineMarkup.Line m = LineMarkup.parse(dev.ftb.mods.ftblibrary.util.client.ClientTextComponentUtils.parse(line), EditorConfig.CENTER_ALL.get());
            int ph = LineRender.previewHeight(m, theme, width - 8);
            int py = top + 11 + Math.max(0, (bottom - top - 11 - ph) / 2);
            LineRender.preview(m, g, theme, left + 4, py, width - 8);
        }
        g.disableScissor();
    }

    @Unique
    private static boolean quest_clusters_editor$enabled() {
        return EditorConfig.MARKUP.get();
    }

    /** The physical (unwrapped) line holding the cursor. */
    @Unique
    private static String quest_clusters_editor$currentLine(String text, int cursor) {
        cursor = Math.max(0, Math.min(cursor, text.length()));
        int start = text.lastIndexOf('\n', cursor - 1) + 1, end = text.indexOf('\n', cursor);
        return text.substring(start, end < 0 ? text.length() : end);
    }
}
