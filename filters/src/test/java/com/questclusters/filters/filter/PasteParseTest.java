package com.questclusters.filters.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasteParseTest {
    @Test
    void readsIdListsAndFilterStrings() throws Exception {
        var m = Class.forName("com.questclusters.filters.client.screen.ItemListScreen").getDeclaredMethod("idsIn", String.class);
        m.setAccessible(true);
        assertEquals(List.of("minecraft:stone", "minecraft:dirt", "oak_log"), m.invoke(null, "minecraft:stone, minecraft:dirt\noak_log"));
        assertEquals(List.of("a", "b", "c"), m.invoke(null, "and(smart_filter_extras:items(a,b)item(c))"));
    }
}
