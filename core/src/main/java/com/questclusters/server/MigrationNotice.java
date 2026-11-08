package com.questclusters.server;

import com.questclusters.api.QuestBookHooks;
import com.questclusters.migration.Migration;
import com.questclusters.migration.MigrationRegistry;
import com.questclusters.migration.MigrationRunner;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftbquests.integration.PermissionsHelper;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tells the server's admins in game what a migration run did: what was imported from removed addons, what failed
 * (it is retried next start), and where the backup is. Kept in {@code quests/quest_clusters/migration_notice.snbt}
 * until the first player who may edit quests joins (or the singleplayer owner), so a restart does not lose it.
 */
public final class MigrationNotice implements QuestBookHooks.Listener {
    public static final MigrationNotice INSTANCE = new MigrationNotice();
    static final String FILE = "migration_notice.snbt";

    private MigrationNotice() {
    }

    static Path path(Path questsFolder) {
        return questsFolder.resolve("quest_clusters").resolve(FILE);
    }

    /** After a run that did something: replaces any older, unseen notice. */
    public static void record(Path questsFolder, MigrationRunner.Result result) {
        if (result == null || !result.didSomething()) {
            return;
        }
        SNBTCompoundTag t = new SNBTCompoundTag();
        t.put("applied", list(result.applied()));
        t.put("failed", list(result.failed()));
        if (result.backup() != null) {
            t.putString("backup", result.backup().getFileName().toString());
        }
        try {
            Files.createDirectories(path(questsFolder).getParent());
            SNBT.write(path(questsFolder), t);
        } catch (IOException e) {
            com.questclusters.QuestClusters.LOGGER.warn("Could not store the migration notice: {}", e.toString());
        }
    }

    private static ListTag list(List<String> ids) {
        ListTag l = new ListTag();
        ids.forEach(id -> l.add(StringTag.valueOf(id)));
        return l;
    }

    static boolean isAdmin(ServerPlayer player) {
        return PermissionsHelper.hasEditorPermission(player) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    @Override
    public void login(ServerPlayer player) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null || file.getFolder() == null || !isAdmin(player)) {
            return;
        }
        Path p = path(file.getFolder());
        if (!Files.exists(p)) {
            return;
        }
        CompoundTag t = SNBT.read(p);
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
        if (t != null) {
            messages(t).forEach(player::sendSystemMessage);
        }
    }

    static List<Component> messages(CompoundTag t) {
        List<Component> out = new java.util.ArrayList<>();
        out.add(Component.translatable("quest_clusters.migrate.notice.title").withStyle(ChatFormatting.GOLD));
        ListTag applied = t.getList("applied", Tag.TAG_STRING);
        for (int i = 0; i < applied.size(); i++) {
            out.add(Component.literal("  ✔ ").withStyle(ChatFormatting.GREEN).append(Component.literal(describe(applied.getString(i))).withStyle(ChatFormatting.WHITE)));
        }
        ListTag failed = t.getList("failed", Tag.TAG_STRING);
        for (int i = 0; i < failed.size(); i++) {
            out.add(Component.literal("  ✘ ").withStyle(ChatFormatting.RED).append(Component.literal(describe(failed.getString(i))).withStyle(ChatFormatting.WHITE)));
        }
        if (!failed.isEmpty()) {
            out.add(Component.translatable("quest_clusters.migrate.notice.failed").withStyle(ChatFormatting.RED));
        }
        if (t.contains("backup")) {
            out.add(Component.translatable("quest_clusters.migrate.backup", t.getString("backup")).withStyle(ChatFormatting.GRAY));
        }
        String cmd = "/questclusters migrate status";
        out.add(Component.translatable("quest_clusters.migrate.notice.details",
                Component.literal(cmd).withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(cmd)))))
                .withStyle(ChatFormatting.GRAY));
        return out;
    }

    private static String describe(String id) {
        for (Migration m : MigrationRegistry.all()) {
            if (m.id().equals(id)) {
                return m.description();
            }
        }
        return id;
    }
}
