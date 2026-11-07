package com.questclusters.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClusterNbtTest {
    @Test
    void chapterFileRoundTrip() {
        QuestCluster a = new QuestCluster(0x1234ABCDL, 0x55L, "矿石处理", "Ore processing", 0x80FF0000, false,
                12.5, -3, 4, 1.5, List.of(1L, 0x7FFFFFFFFFFFFFFFL));
        QuestCluster b = new QuestCluster(0x99L, 0x55L, "B", "", QuestCluster.DEFAULT_COLOR, true, 0, 0, 3, 1, List.of(7L));

        CompoundTag tag = ClusterNbt.writeChapterFile(0x55L, List.of(a, b));
        assertEquals(List.of(a, b), ClusterNbt.readChapterFile(tag));
    }

    @Test
    void missingDefaultExpandedMeansExpanded() {
        CompoundTag t = new CompoundTag();
        t.putString("id", "01");
        t.putString("title", "x");
        assertEquals(true, ClusterNbt.read(t, 5L).orElseThrow().defaultExpanded());
    }
}
