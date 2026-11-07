package com.questclusters.visuals;

import com.questclusters.visuals.client.EntityIconSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityIconSpecTest {
    @Test
    void basicAndOptions() {
        var s = EntityIconSpec.parse("entity:minecraft:zombie[rotate=true, look=off]{IsBaby:1b}");
        assertNotNull(s);
        assertEquals("minecraft:zombie", s.entityId());
        assertTrue(s.rotate());
        assertFalse(s.lookAtCursor());
        assertEquals("{IsBaby:1b}", s.nbt());
        assertEquals("minecraft:creeper", EntityIconSpec.parse("entity:creeper").entityId(), "namespace defaults to minecraft");
    }

    @Test
    void nbtOrderAndExtraKeys() {
        var s = EntityIconSpec.parse("entity:minecraft:sheep{Color:3}[Sheared=1b, auto_rotate=yes]");
        assertEquals("{Color:3}", s.nbt());
        assertEquals(Map.of("Sheared", "1b"), s.extraNbt());
        assertTrue(s.rotate());
        var n = EntityIconSpec.parse("entity:minecraft:pig[nbt={Saddle:1b,Name:\"a,b\"}]");
        assertEquals("{Saddle:1b,Name:\"a,b\"}", n.nbt(), "commas inside braces/quotes don't split");
    }

    @Test
    void playerOptions() {
        var p = EntityIconSpec.parse("entity:minecraft:player[skin=\"Notch\", label='Hero', show_name=false, uuid=069a79f444e94726a5befca90e38aaf5]");
        assertTrue(p.isPlayer());
        assertEquals("Notch", p.skin());
        assertEquals("Hero", p.displayName());
        assertFalse(p.showName());
        assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), p.uuid());
        // player-only keys are plain NBT on other entities
        assertEquals(Map.of("skin", "x"), EntityIconSpec.parse("entity:minecraft:cow[skin=x]").extraNbt());
    }

    @Test
    void invalid() {
        assertNull(EntityIconSpec.parse("entity:"));
        assertNull(EntityIconSpec.parse("entity:Minecraft:Zombie"), "ids are lower case");
        assertNull(EntityIconSpec.parse("entity:minecraft:zombie[rotate=maybe]"));
        assertNull(EntityIconSpec.parse("entity:minecraft:zombie{unclosed"));
        assertNull(EntityIconSpec.parse("entity:minecraft:zombie junk"));
    }
}
