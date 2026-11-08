package com.questclusters.xmod.kubejs;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.QuestObjectType;
import dev.ftb.mods.ftbquests.quest.QuestShape;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Binding {@code FTBQuests} (same methods as with FTB XMod Compat). */
public final class QuestsBinding {
    public static final QuestsBinding INSTANCE = new QuestsBinding();

    private QuestsBinding() {
    }

    public Map<String, QuestShape> getQuestShapes() {
        return QuestShape.map();
    }

    public Map<String, QuestObjectType> getQuestObjectTypes() {
        Map<String, QuestObjectType> out = new LinkedHashMap<>();
        for (QuestObjectType t : QuestObjectType.values()) out.put(t.getId(), t);
        return out;
    }

    public BaseQuestFile getFile(Level level) {
        return FTBQuestsAPI.api().getQuestFile(level.isClientSide());
    }

    public TeamData getData(Level level, UUID uuid) {
        return getFile(level).getOrCreateTeamData(uuid);
    }

    public TeamData getData(Player player) {
        return getFile(player.level()).getOrCreateTeamData(player);
    }

    public QuestObjectBase getObject(Level level, Object id) {
        return getFile(level).getBase(TeamDataJS.id(id));
    }

    public TeamDataJS getServerDataFromPlayer(Player player) {
        return new TeamDataJS.OfPlayer(player);
    }

    public TeamDataJS getServerDataFromSource(CommandSourceStack source) {
        return source.getPlayer() == null ? null : new TeamDataJS.OfPlayer(source.getPlayer());
    }
}
