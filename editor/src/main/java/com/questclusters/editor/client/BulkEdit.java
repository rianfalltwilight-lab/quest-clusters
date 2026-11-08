package com.questclusters.editor.client;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.CreateObjectMessage;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bulk Edit (edit the first quest, copy every changed property to the others) and Bulk Add Task. */
public final class BulkEdit {
    private BulkEdit() {
    }

    public static void edit(QuestScreen screen, List<Quest> quests) {
        Quest first = quests.getFirst();
        var lookup = ClientQuestFile.INSTANCE.holderLookup();
        CompoundTag before = new CompoundTag();
        first.writeData(before, lookup);
        ConfigGroup group = new ConfigGroup(FTBQuestsAPI.MOD_ID, accepted -> {
            EditorActions.reopen(screen);
            if (!accepted) {
                first.readData(before, lookup); // the page edits the object directly: put it back
                first.clearCachedData();
                return;
            }
            CompoundTag after = new CompoundTag();
            first.writeData(after, lookup);
            List<String> changed = new ArrayList<>();
            for (String k : after.getAllKeys()) if (!Objects.equals(after.get(k), before.get(k))) changed.add(k);
            for (String k : before.getAllKeys()) if (!after.contains(k)) changed.add(k);
            changed.removeAll(List.of("id", "x", "y"));
            EditObjectMessage.sendToServer(first);
            for (Quest q : quests.subList(1, quests.size())) {
                CompoundTag t = new CompoundTag();
                q.writeData(t, lookup);
                for (String k : changed) {
                    Tag v = after.get(k);
                    if (v == null) t.remove(k);
                    else t.put(k, v.copy());
                }
                q.readData(t, lookup);
                q.clearCachedData();
                EditObjectMessage.sendToServer(q);
            }
        });
        first.fillConfigGroup(first.createSubGroup(group));
        new EditConfigScreen(group).openGui();
    }

    public static List<ContextMenuItem> addTaskItems(QuestScreen screen, List<Quest> quests) {
        List<ContextMenuItem> out = new ArrayList<>();
        for (TaskType type : TaskTypes.TYPES.values()) {
            out.add(new ContextMenuItem(type.getDisplayName(), type.getIconSupplier(), b ->
                    type.getGuiProvider().openCreationGui(screen.questPanel, quests.getFirst(), task -> {
                        for (Quest q : quests) {
                            Task copy = QuestObjectBase.copy(task, () -> type.createTask(0L, q));
                            if (copy != null) NetworkManager.sendToServer(CreateObjectMessage.requestCreation(copy));
                        }
                    })));
        }
        return out;
    }
}
