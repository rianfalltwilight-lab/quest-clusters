package com.questclusters.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Disk format of a cluster. Ids are written as hex strings, like FTB Quests does, so files stay diff-friendly.
 */
public final class ClusterNbt {
    public static final int FORMAT_VERSION = 1;

    private ClusterNbt() {
    }

    public static CompoundTag write(QuestCluster c) {
        return write(c, CompoundTag::new);
    }

    /**
     * @param factory creates the compound tags; pass FTB Library's {@code SNBTCompoundTag::new} when writing files,
     *                which keeps key order stable and writes booleans as true/false
     */
    public static CompoundTag write(QuestCluster c, Supplier<CompoundTag> factory) {
        CompoundTag tag = factory.get();
        tag.putString("id", QuestCluster.idToString(c.id()));
        tag.putString("title", c.title());
        if (!c.subtitle().isEmpty()) {
            tag.putString("subtitle", c.subtitle());
        }
        tag.putString("color", String.format("#%08X", c.color()));
        tag.putBoolean("default_expanded", c.defaultExpanded());
        tag.putDouble("x", c.x());
        tag.putDouble("y", c.y());
        tag.putDouble("width", c.width());
        tag.putDouble("height", c.height());
        ListTag members = new ListTag();
        for (long m : c.members()) {
            members.add(StringTag.valueOf(QuestCluster.idToString(m)));
        }
        tag.put("members", members);
        return tag;
    }

    /** @return the cluster, or empty if the tag has no valid id */
    public static Optional<QuestCluster> read(CompoundTag tag, long chapterId) {
        long id = QuestCluster.idFromString(tag.getString("id"));
        if (id == 0L) {
            return Optional.empty();
        }
        List<Long> members = new ArrayList<>();
        ListTag list = tag.getList("members", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            long m = QuestCluster.idFromString(list.getString(i));
            if (m != 0L && !members.contains(m)) {
                members.add(m);
            }
        }
        return Optional.of(new QuestCluster(
                id,
                chapterId,
                tag.getString("title"),
                tag.getString("subtitle"),
                parseColor(tag.getString("color")),
                !tag.contains("default_expanded") || tag.getBoolean("default_expanded"),
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.contains("width", Tag.TAG_ANY_NUMERIC) ? tag.getDouble("width") : 3D,
                tag.contains("height", Tag.TAG_ANY_NUMERIC) ? tag.getDouble("height") : 1D,
                members
        ));
    }

    /** One file per chapter: {@code {version: 1, chapter: "<hex>", clusters: [...]}}. */
    public static CompoundTag writeChapterFile(long chapterId, List<QuestCluster> clusters) {
        return writeChapterFile(chapterId, clusters, CompoundTag::new);
    }

    public static CompoundTag writeChapterFile(long chapterId, List<QuestCluster> clusters, Supplier<CompoundTag> factory) {
        CompoundTag root = factory.get();
        root.putInt("version", FORMAT_VERSION);
        root.putString("chapter", QuestCluster.idToString(chapterId));
        ListTag list = new ListTag();
        for (QuestCluster c : clusters) {
            list.add(write(c, factory));
        }
        root.put("clusters", list);
        return root;
    }

    public static List<QuestCluster> readChapterFile(CompoundTag root) {
        long chapterId = QuestCluster.idFromString(root.getString("chapter"));
        if (chapterId == 0L) {
            return List.of();
        }
        List<QuestCluster> out = new ArrayList<>();
        ListTag list = root.getList("clusters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            read(list.getCompound(i), chapterId).ifPresent(out::add);
        }
        return out;
    }

    static int parseColor(String s) {
        if (s == null || s.isEmpty()) {
            return QuestCluster.DEFAULT_COLOR;
        }
        String t = s.startsWith("#") ? s.substring(1) : s;
        try {
            long v = Long.parseLong(t, 16);
            // 6-digit colours get full alpha
            return t.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
        } catch (NumberFormatException e) {
            return QuestCluster.DEFAULT_COLOR;
        }
    }
}
