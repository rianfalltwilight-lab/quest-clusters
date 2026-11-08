package com.questclusters.editor.mixin.client;

import com.questclusters.editor.client.ClientEditor;
import com.questclusters.editor.client.EditorActions;
import com.questclusters.editor.data.LinePreset;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Chapter → Edit → Appearance: "Default decorative line preset". Stored in the editor's chapter data (not the
 * chapter file); the value is sent as an editor change when the chapter settings are accepted.
 */
@Mixin(value = Chapter.class, remap = false)
public abstract class ChapterLinePresetConfigMixin {
    @Inject(method = "fillConfigGroup", at = @At("TAIL"))
    private void quest_clusters_editor$linePreset(ConfigGroup config, CallbackInfo ci) {
        long chapterId = ((Chapter) (Object) this).id;
        var book = ClientEditor.book();
        String current = book.preset(book.chapter(chapterId).linePreset) != null ? book.chapter(chapterId).linePreset : "";
        List<String> ids = new ArrayList<>();
        ids.add("");
        book.linePresets.stream().map(LinePreset::id).forEach(ids::add);
        NameMap<String> names = NameMap.of("", ids).name(id -> {
            LinePreset p = book.preset(id);
            return p == null ? Component.translatable("quest_clusters_editor.menu.line_presets.builtin") : Component.literal(p.name());
        }).create();
        config.getOrCreateSubgroup("appearance").addEnum("decorative_line_preset", current, v -> {
            if (!v.equals(ClientEditor.book().chapter(chapterId).linePreset)) {
                EditorActions.editChapter("line_preset", chapterId, c -> c.linePreset = v);
            }
        }, names).setNameKey("quest_clusters_editor.chapter.decorative_line_preset");
    }
}
