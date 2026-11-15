package com.questclusters.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TexturesAnimationTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void verticalStripPlaysAllFrames() {
        var a = Textures.parseAnimation(json("{\"frametime\": 2}"), 16, 64);
        assertNotNull(a);
        assertEquals(16, a.frameHeight());
        assertArrayEquals(new int[]{0, 1, 2, 3}, a.frames());
        assertEquals(8, a.totalTicks());
        assertEquals(0, a.frameAt(0));
        assertEquals(1, a.frameAt(2 * 50));
        assertEquals(3, a.frameAt(7 * 50));
        assertEquals(0, a.frameAt(8 * 50), "loops");
    }

    @Test
    void explicitFramesAndTimes() {
        var a = Textures.parseAnimation(json("{\"frametime\": 1, \"frames\": [2, {\"index\": 0, \"time\": 5}, 1, 9]}"), 16, 48);
        assertNotNull(a);
        assertArrayEquals(new int[]{2, 0, 1}, a.frames(), "out-of-range index 9 dropped");
        assertArrayEquals(new int[]{1, 5, 1}, a.ticks());
        assertEquals(0, a.frameAt(3 * 50));
    }

    @Test
    void notAnAnimation() {
        assertNull(Textures.parseAnimation(json("{}"), 16, 16), "one frame");
        assertNull(Textures.parseAnimation(json("{\"width\": 32}"), 16, 64), "frame wider than the image");
    }

    @Test
    void horizontalStripAndGrid() {
        var strip = Textures.parseAnimation(json("{\"frametime\": 2}"), 64, 16);
        assertNotNull(strip, "a horizontal strip is animated too");
        assertEquals(4, strip.columns());
        assertEquals(16, strip.frameWidth());
        var grid = Textures.parseAnimation(json("{}"), 32, 32 * 2 / 2);
        assertNull(grid, "a square image is a single frame");
        var grid2 = Textures.parseAnimation(json("{\"width\": 16, \"height\": 16}"), 32, 32);
        assertNotNull(grid2);
        assertEquals(2, grid2.columns());
        assertEquals(4, grid2.frames().length, "2 x 2 grid, row by row");
    }

    @Test
    void oneSideGivenTakesTheOtherFromTheImage() {
        var tall = Textures.parseAnimation(json("{\"height\": 8}"), 32, 32);
        assertNotNull(tall);
        assertEquals(32, tall.frameWidth(), "width defaults to the image width, as in vanilla");
        assertEquals(4, tall.frames().length);
        var wide = Textures.parseAnimation(json("{\"width\": 8}"), 32, 32);
        assertNotNull(wide);
        assertEquals(32, wide.frameHeight(), "height defaults to the image height, as in vanilla");
        assertEquals(4, wide.frames().length);
    }

    @Test
    void interpolateBlendsTowardsTheNextFrame() {
        var a = Textures.parseAnimation(json("{\"frametime\": 4, \"interpolate\": true}"), 16, 32);
        assertNotNull(a);
        assertTrue(a.interpolate());
        assertEquals(0, a.entryAt(100));
        assertEquals(0.5F, a.progress(100), 1e-4, "2 of 4 ticks into frame 0");
    }
}
