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
}
