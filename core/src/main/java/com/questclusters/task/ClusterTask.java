package com.questclusters.task;

import com.questclusters.QuestClusters;
import com.questclusters.client.ClientClusters;
import com.questclusters.data.ClusterStore;
import com.questclusters.data.QuestCluster;
import com.questclusters.server.ServerClusters;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * FTB Quests task "Complete quest cluster": done when all (or a set number of) quests in a cluster are completed.
 * Put it on a "milestone" quest to give a reward for the whole cluster, or make other quests depend on that quest
 * to use the cluster as a prerequisite. Progress, rewards, teams and history are all handled by FTB Quests itself.
 * <p>
 * Only quests count (quest links and images don't), and the quest holding this task never counts towards itself.
 */
public class ClusterTask extends Task {
    public static final TaskType TYPE = TaskTypes.register(
            ResourceLocation.fromNamespaceAndPath(QuestClusters.MOD_ID, "cluster"),
            ClusterTask::new,
            () -> ItemIcon.getItemIcon(Items.BOOKSHELF));

    /** Forces class loading (and so registration) during mod construction, before any quest file loads. */
    public static void init() {
    }

    private long clusterId;
    /** 0 = every quest in the cluster. */
    private int required;

    public ClusterTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return TYPE;
    }

    private ClusterStore store() {
        return getQuestFile().isServerSide() ? ServerClusters.store() : ClientClusters.store();
    }

    private Optional<QuestCluster> cluster() {
        return clusterId == 0L ? Optional.empty() : store().get(clusterId);
    }

    /** Quests of the cluster that count for this task. */
    private List<Quest> countedQuests() {
        List<Quest> out = new ArrayList<>();
        cluster().ifPresent(c -> {
            for (long m : c.members()) {
                if (getQuestFile().getBase(m) instanceof Quest q && q != getQuest()) {
                    out.add(q);
                }
            }
        });
        return out;
    }

    @Override
    public long getMaxProgress() {
        int total = countedQuests().size();
        if (total == 0) {
            return 1L; // nothing to complete yet: never auto-completes
        }
        return required <= 0 ? total : Math.min(required, total);
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this) || !checkTaskSequence(teamData)) {
            return;
        }
        List<Quest> quests = countedQuests();
        if (quests.isEmpty()) {
            return;
        }
        long done = quests.stream().filter(teamData::isCompleted).count();
        long progress = Math.min(done, getMaxProgress());
        if (progress != teamData.getProgress(this)) {
            teamData.setProgress(this, progress);
        }
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public MutableComponent getAltTitle() {
        Component name = cluster().map(c -> com.questclusters.client.gui.ClusterText.text(c.title()))
                .orElse(Component.translatable("quest_clusters.task.no_cluster"));
        return required > 0
                ? Component.translatable("quest_clusters.task.title_count", required, name)
                : Component.translatable("quest_clusters.task.title_all", name);
    }

    @Override
    public Icon getAltIcon() {
        return ItemIcon.getItemIcon(Items.BOOKSHELF);
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("cluster", QuestCluster.idToString(clusterId));
        if (required > 0) {
            nbt.putInt("required", required);
        }
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        clusterId = QuestCluster.idFromString(nbt.getString("cluster"));
        required = nbt.getInt("required");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeLong(clusterId);
        buffer.writeVarInt(required);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        clusterId = buffer.readLong();
        required = buffer.readVarInt();
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        // pick from the clusters of this quest's chapter first, then the rest
        List<Long> ids = new ArrayList<>();
        ids.add(0L);
        long chapter = getQuest().getChapter().id;
        store().byChapter(chapter).forEach(c -> ids.add(c.id()));
        store().all().stream().filter(c -> c.chapterId() != chapter).forEach(c -> ids.add(c.id()));
        if (!ids.contains(clusterId)) {
            ids.add(clusterId);
        }
        NameMap<Long> names = NameMap.of(0L, ids)
                .id(QuestCluster::idToString)
                .name(id -> id == 0L ? Component.translatable("quest_clusters.task.no_cluster")
                        : store().get(id).map(c -> com.questclusters.client.gui.ClusterText.text(c.title()))
                        .orElse(Component.literal(QuestCluster.idToString(id))))
                .create();
        config.addEnum("cluster", clusterId, v -> clusterId = v, names).setNameKey("quest_clusters.task.cluster");
        config.addInt("required", required, v -> required = v, 0, 0, QuestCluster.MAX_MEMBERS)
                .setNameKey("quest_clusters.task.required");
    }

    public long clusterId() {
        return clusterId;
    }
}
