package com.questclusters.xmod.stages;

import com.questclusters.xmod.QuestClustersXMod;
import dev.ftb.mods.ftblibrary.integration.stages.StageHelper;
import dev.ftb.mods.ftblibrary.integration.stages.StageProvider;
import dev.ftb.mods.ftbquests.quest.task.StageTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Stages for FTB Quests' stage tasks and stage barriers.
 * <ul>
 * <li>Without FTB XMod Compat, picks the stages implementation FTB Library uses: KubeJS stages if KubeJS is
 * installed, else Game Stages (the 1.21.1 port "Game Stages Reborn", same mod id and API), else FTB Library's
 * default (entity tags). With XMod, its configurable choice stays in charge.</li>
 * <li>Either way, stage tasks are checked as soon as a stage changes instead of on the next once-a-second
 * check.</li>
 * </ul>
 */
public final class Stages {
    private Stages() {
    }

    public static void setup() {
        if (!QuestClustersXMod.loaded("ftbxmodcompat")) {
            StageProvider provider = null;
            if (QuestClustersXMod.loaded("kubejs")) {
                provider = new KubeJSStages();
            } else if (QuestClustersXMod.loaded("gamestages")) {
                provider = GameStagesBridge.provider();
            }
            if (provider != null) {
                StageHelper.INSTANCE.setProviderImpl(new Refreshing(provider));
            }
        }
        if (QuestClustersXMod.loaded("gamestages")) {
            GameStagesBridge.listen();
        }
    }

    /** Re-checks the player's stage tasks right away (FTB Quests' hook for this). */
    static void refresh(Player player) {
        if (player instanceof ServerPlayer sp && sp.getServer() != null) {
            sp.getServer().execute(() -> StageTask.checkStages(sp));
        }
    }

    /** Stage changes made through FTB Library (stage rewards, commands) refresh stage tasks at once. */
    private record Refreshing(StageProvider delegate) implements StageProvider {
        @Override
        public boolean has(Player player, String stage) {
            return delegate.has(player, stage);
        }

        @Override
        public void add(ServerPlayer player, String stage) {
            delegate.add(player, stage);
            refresh(player);
        }

        @Override
        public void remove(ServerPlayer player, String stage) {
            delegate.remove(player, stage);
            refresh(player);
        }

        @Override
        public void sync(ServerPlayer player) {
            delegate.sync(player);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }
    }
}
