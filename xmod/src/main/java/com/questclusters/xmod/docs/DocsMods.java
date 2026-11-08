package com.questclusters.xmod.docs;

import com.questclusters.xmod.QuestClustersXMod;
import dev.ftb.mods.ftblibrary.integration.docsmod.DocsModRegistry;

/**
 * Targets for FTB Quests' "show docs" image click action ({@code <mod>,<book>[,<page>[#anchor]]}).
 * GuideME is registered on both sides (its API is common), Patchouli on the client only.
 */
public final class DocsMods {
    private DocsMods() {
    }

    public static void register() {
        if (QuestClustersXMod.loaded("guideme")) {
            DocsModRegistry.INSTANCE.registerDocsMod("guideme", GuideMeDocs::open);
        }
        // Patchouli books only open on the client, and the target touches client classes
        if (QuestClustersXMod.loaded("patchouli") && net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            DocsModRegistry.INSTANCE.registerDocsMod("patchouli", PatchouliDocs::open);
        }
    }
}
