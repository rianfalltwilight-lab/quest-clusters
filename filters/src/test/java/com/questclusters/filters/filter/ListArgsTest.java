package com.questclusters.filters.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListArgsTest {
    @Test
    void splitsOnTopLevelCommas() {
        assertEquals(List.of("minecraft:stone", "dirt"), ListArgs.split(" minecraft:stone , dirt ,"));
        assertEquals(List.of("minecraft:potion[minecraft:potion_contents={potion:\"a,b\",x:[1,2]}]", "minecraft:stone"),
                ListArgs.split("minecraft:potion[minecraft:potion_contents={potion:\"a,b\",x:[1,2]}],minecraft:stone"));
        assertEquals(List.of("a[b='x,]y']", "c"), ListArgs.split("a[b='x,]y'],c"));
    }

    @Test
    void percentEncodingRoundTrips() {
        String raw = "minecraft:stone[minecraft:custom_name='\"(odd) \\\\name 100%\"']";
        String enc = ListArgs.encode(raw);
        assertFalse(enc.contains("(") || enc.contains(")") || enc.contains("\\"));
        assertEquals(raw, ListArgs.decode(enc));
        assertEquals("50%x", ListArgs.decode("50%x")); // stray percent stays
    }

    @Test
    void normalisesTags() {
        assertEquals("c:ingots", ListArgs.normaliseTag("#c:ingots"));
        assertEquals("minecraft:logs", ListArgs.normaliseTag("logs"));
        assertEquals("c:storage_blocks/iron", ListArgs.normaliseTag(" #c:storage_blocks/iron "));
        assertNull(ListArgs.normaliseTag("#"));
        assertNull(ListArgs.normaliseTag("Bad Tag"));
    }
}
