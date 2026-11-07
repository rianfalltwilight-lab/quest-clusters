package com.questclusters.visuals;

import com.questclusters.visuals.client.Apng;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

class ApngTest {
    /** Raw RGBA scanlines (filter 0) for a solid colour, deflated. */
    static byte[] idat(int w, int h, int argb) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        for (int y = 0; y < h; y++) {
            raw.write(0);
            for (int x = 0; x < w; x++) {
                raw.write((argb >> 16) & 255); raw.write((argb >> 8) & 255); raw.write(argb & 255); raw.write((argb >>> 24) & 255);
            }
        }
        Deflater d = new Deflater();
        d.setInput(raw.toByteArray());
        d.finish();
        byte[] buf = new byte[4096];
        int n = d.deflate(buf);
        byte[] out = new byte[n];
        System.arraycopy(buf, 0, out, 0, n);
        return out;
    }

    static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws Exception {
        Method m = Apng.class.getDeclaredMethod("write", ByteArrayOutputStream.class, String.class, byte[].class);
        m.setAccessible(true);
        m.invoke(null, out, type, data);
    }

    static byte[] fctl(int seq, int w, int h, int x, int y, int delayNum, int delayDen, int dispose, int blend) {
        return ByteBuffer.allocate(26).putInt(seq).putInt(w).putInt(h).putInt(x).putInt(y)
                .putShort((short) delayNum).putShort((short) delayDen).put((byte) dispose).put((byte) blend).array();
    }

    static byte[] twoFrameApng() throws Exception {
        ByteArrayOutputStream f = new ByteArrayOutputStream();
        f.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        chunk(f, "IHDR", ByteBuffer.allocate(13).putInt(4).putInt(4).put((byte) 8).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0).array());
        chunk(f, "acTL", ByteBuffer.allocate(8).putInt(2).putInt(0).array());
        chunk(f, "fcTL", fctl(0, 4, 4, 0, 0, 1, 10, Apng.DISPOSE_NONE, Apng.BLEND_SOURCE));
        chunk(f, "IDAT", idat(4, 4, 0xFFFF0000));
        chunk(f, "fcTL", fctl(1, 2, 2, 1, 1, 30, 100, Apng.DISPOSE_BACKGROUND, Apng.BLEND_OVER));
        byte[] d = idat(2, 2, 0xFF00FF00);
        chunk(f, "fdAT", ByteBuffer.allocate(4 + d.length).putInt(2).put(d).array());
        chunk(f, "IEND", new byte[0]);
        return f.toByteArray();
    }

    @Test
    void splitsFramesIntoValidPngs() throws Exception {
        Apng.Animation a = Apng.parse(twoFrameApng());
        assertNotNull(a);
        assertEquals(4, a.width());
        assertEquals(2, a.frames().size());
        assertEquals(0, a.plays());
        var f1 = a.frames().get(1);
        assertEquals(1, f1.x());
        assertEquals(2, f1.width());
        assertEquals(300, f1.delayMs());
        assertEquals(Apng.BLEND_OVER, f1.blend());
        BufferedImage img0 = ImageIO.read(new ByteArrayInputStream(a.frames().get(0).png()));
        BufferedImage img1 = ImageIO.read(new ByteArrayInputStream(f1.png()));
        assertEquals(0xFFFF0000, img0.getRGB(3, 3));
        assertEquals(0xFF00FF00, img1.getRGB(0, 0));
        assertEquals(2, img1.getWidth());
    }

    @Test
    void timing() throws Exception {
        Apng.Animation a = Apng.parse(twoFrameApng()); // 100 ms + 300 ms, looping
        assertEquals(400, a.totalMs());
        assertEquals(0, a.frameAt(50));
        assertEquals(1, a.frameAt(150));
        assertEquals(0, a.frameAt(420));
        Apng.Animation once = new Apng.Animation(4, 4, 1, a.frames());
        assertEquals(1, once.frameAt(10_000), "finite plays stop on the last frame");
    }

    @Test
    void plainPngIsNotAnimated() throws Exception {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        assertNull(Apng.parse(out.toByteArray()));
        assertNull(Apng.parse(new byte[]{1, 2, 3}));
    }
}
