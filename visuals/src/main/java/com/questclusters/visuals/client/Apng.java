package com.questclusters.visuals.client;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Minimal APNG reader: splits an animated PNG into one standalone PNG per frame, plus frame placement, timing,
 * dispose and blend info (see the APNG spec: acTL, fcTL, fdAT chunks). Pure Java; compositing happens in
 * {@link ApngTextures}. Returns null for plain PNGs.
 */
public final class Apng {
    public static final int MAX_FRAMES = 1024;
    public static final int MAX_SIZE = 4096;
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    public static final int DISPOSE_NONE = 0, DISPOSE_BACKGROUND = 1, DISPOSE_PREVIOUS = 2;
    public static final int BLEND_SOURCE = 0, BLEND_OVER = 1;

    /** One frame as a complete PNG of its own size, placed at (x, y) on the canvas. */
    public record Frame(int x, int y, int width, int height, int delayMs, int dispose, int blend, byte[] png) {
    }

    /** @param plays 0 = loop forever */
    public record Animation(int width, int height, int plays, List<Frame> frames) {
        public int totalMs() {
            int t = 0;
            for (Frame f : frames) t += f.delayMs();
            return Math.max(1, t);
        }

        /** Frame index to show at time t (ms since start). */
        public int frameAt(long t) {
            long total = totalMs();
            if (plays > 0 && t >= total * plays) return frames.size() - 1;
            long m = t % total;
            for (int i = 0; i < frames.size(); i++) {
                m -= frames.get(i).delayMs();
                if (m < 0) return i;
            }
            return frames.size() - 1;
        }
    }

    private Apng() {
    }

    private record Chunk(String type, byte[] data) {
    }

    public static Animation parse(byte[] file) {
        if (file.length < 8) return null;
        for (int i = 0; i < 8; i++) if (file[i] != SIGNATURE[i]) return null;
        List<Chunk> chunks = new ArrayList<>();
        ByteBuffer b = ByteBuffer.wrap(file, 8, file.length - 8);
        while (b.remaining() >= 12) {
            int len = b.getInt();
            if (len < 0 || len > b.remaining() - 8) return null;
            byte[] t = new byte[4];
            b.get(t);
            byte[] d = new byte[len];
            b.get(d);
            b.getInt(); // crc
            chunks.add(new Chunk(new String(t, StandardCharsets.ISO_8859_1), d));
        }
        Chunk ihdr = chunks.isEmpty() ? null : chunks.getFirst();
        if (ihdr == null || !ihdr.type().equals("IHDR") || ihdr.data().length != 13) return null;
        int width = ByteBuffer.wrap(ihdr.data()).getInt(0), height = ByteBuffer.wrap(ihdr.data()).getInt(4);
        Chunk actl = chunks.stream().filter(c -> c.type().equals("acTL")).findFirst().orElse(null);
        if (actl == null || width <= 0 || height <= 0 || width > MAX_SIZE || height > MAX_SIZE) return null;
        int plays = ByteBuffer.wrap(actl.data()).getInt(4);

        // chunks every frame PNG needs besides its image data (palette, transparency, colour info)
        List<Chunk> shared = new ArrayList<>();
        for (Chunk c : chunks) {
            if (c.type().equals("IDAT") || c.type().equals("IEND")) break;
            if (!c.type().equals("IHDR") && !c.type().equals("acTL") && !c.type().equals("fcTL")) shared.add(c);
        }

        List<Frame> frames = new ArrayList<>();
        byte[] fctl = null;
        ByteArrayOutputStream data = null;
        for (Chunk c : chunks) {
            switch (c.type()) {
                case "fcTL" -> {
                    if (fctl != null && data != null) frames.add(frame(ihdr, shared, fctl, data.toByteArray()));
                    fctl = c.data();
                    data = new ByteArrayOutputStream();
                }
                case "IDAT" -> {
                    if (data != null) data.writeBytes(c.data()); // IDAT is the first frame only if an fcTL came before it
                }
                case "fdAT" -> {
                    if (data != null && c.data().length > 4) data.write(c.data(), 4, c.data().length - 4); // skip sequence number
                }
                default -> {
                }
            }
            if (frames.size() > MAX_FRAMES) return null;
        }
        if (fctl != null && data != null && data.size() > 0) frames.add(frame(ihdr, shared, fctl, data.toByteArray()));
        return frames.isEmpty() ? null : new Animation(width, height, plays, List.copyOf(frames));
    }

    private static Frame frame(Chunk ihdr, List<Chunk> shared, byte[] fctl, byte[] idat) {
        ByteBuffer f = ByteBuffer.wrap(fctl);
        int w = f.getInt(4), h = f.getInt(8), x = f.getInt(12), y = f.getInt(16);
        int num = f.getShort(20) & 0xFFFF, den = f.getShort(22) & 0xFFFF;
        int dispose = fctl[24], blend = fctl[25];
        int delay = Math.max(10, Math.round(1000F * num / (den == 0 ? 100 : den))); // browsers clamp tiny delays too

        byte[] hdr = ihdr.data().clone();
        ByteBuffer.wrap(hdr).putInt(0, w).putInt(4, h);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.writeBytes(SIGNATURE);
        write(png, "IHDR", hdr);
        for (Chunk c : shared) write(png, c.type(), c.data());
        write(png, "IDAT", idat);
        write(png, "IEND", new byte[0]);
        return new Frame(x, y, w, h, delay, dispose, blend, png.toByteArray());
    }

    static void write(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] t = type.getBytes(StandardCharsets.ISO_8859_1);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(t);
        out.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
