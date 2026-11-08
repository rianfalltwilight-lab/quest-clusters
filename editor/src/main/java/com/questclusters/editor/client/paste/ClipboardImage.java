package com.questclusters.editor.client.paste;

import com.questclusters.editor.QuestClustersEditor;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Reads an image from the system clipboard. Windows: native clipboard (DIB) through JNA; macOS: osascript;
 * Linux: wl-paste or xclip. Returns null if there is no image (or it cannot be read).
 */
public final class ClipboardImage {
    private static final long MAX_BYTES = 256L << 20;

    /** For the dev self-test: an image to return instead of reading the system clipboard. */
    public static BufferedImage testOverride;

    private ClipboardImage() {
    }

    public static BufferedImage read() {
        if (testOverride != null) return testOverride;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) return Windows.read();
            if (os.contains("mac")) return mac();
            return linux();
        } catch (Throwable t) {
            QuestClustersEditor.LOGGER.debug("No clipboard image: {}", t.toString());
            return null;
        }
    }

    private static BufferedImage decode(byte[] data) throws IOException {
        return data == null || data.length == 0 ? null : ImageIO.read(new ByteArrayInputStream(data));
    }

    // ------------------------------------------------------------------ macOS

    private static BufferedImage mac() throws Exception {
        Path tmp = Files.createTempFile("qc_clipboard", ".img");
        try {
            for (String type : List.of("«class PNGf»", "«class TIFF»")) {
                String p = tmp.toAbsolutePath().toString().replace("\"", "\\\"");
                Process proc = new ProcessBuilder("osascript",
                        "-e", "set fh to open for access (POSIX file \"" + p + "\") with write permission",
                        "-e", "try",
                        "-e", "set eof fh to 0",
                        "-e", "write (the clipboard as " + type + ") to fh",
                        "-e", "on error e",
                        "-e", "close access fh",
                        "-e", "error e",
                        "-e", "end try",
                        "-e", "close access fh").redirectErrorStream(true).start();
                proc.getInputStream().readAllBytes();
                if (proc.waitFor(5, TimeUnit.SECONDS) && proc.exitValue() == 0 && Files.size(tmp) > 0 && Files.size(tmp) < MAX_BYTES) {
                    BufferedImage img = decode(Files.readAllBytes(tmp));
                    if (img != null) return img;
                }
            }
            return null;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ------------------------------------------------------------------ Linux

    private static BufferedImage linux() throws Exception {
        for (List<String> cmd : List.of(List.of("wl-paste", "--no-newline", "--type", "image/png"),
                List.of("xclip", "-selection", "clipboard", "-t", "image/png", "-o"))) {
            try {
                Process proc = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).start();
                byte[] data;
                try (InputStream in = proc.getInputStream()) {
                    data = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, MAX_BYTES));
                }
                if (proc.waitFor(5, TimeUnit.SECONDS) && proc.exitValue() == 0) {
                    BufferedImage img = decode(data);
                    if (img != null) return img;
                }
            } catch (IOException notInstalled) {
                // try the next tool
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Windows

    private static final class Windows {
        interface User32 extends Library {
            boolean OpenClipboard(Pointer owner);

            boolean CloseClipboard();

            boolean IsClipboardFormatAvailable(int format);

            Pointer GetClipboardData(int format);
        }

        interface Kernel32 extends Library {
            Pointer GlobalLock(Pointer mem);

            boolean GlobalUnlock(Pointer mem);

            long GlobalSize(Pointer mem);
        }

        private static final int CF_DIB = 8, CF_DIBV5 = 17;

        static BufferedImage read() throws IOException {
            User32 user32 = Native.load("user32", User32.class);
            Kernel32 kernel32 = Native.load("kernel32", Kernel32.class);
            int format = user32.IsClipboardFormatAvailable(CF_DIB) ? CF_DIB : user32.IsClipboardFormatAvailable(CF_DIBV5) ? CF_DIBV5 : 0;
            if (format == 0 || !user32.OpenClipboard(null)) return null;
            byte[] dib;
            try {
                Pointer handle = user32.GetClipboardData(format);
                if (handle == null) return null;
                long size = kernel32.GlobalSize(handle);
                if (size <= 40 || size > MAX_BYTES) return null;
                Pointer p = kernel32.GlobalLock(handle);
                if (p == null) return null;
                try {
                    dib = p.getByteArray(0, (int) size);
                } finally {
                    kernel32.GlobalUnlock(handle);
                }
            } finally {
                user32.CloseClipboard();
            }
            return decode(bmp(dib));
        }

        /** A DIB plus the 14-byte BMP file header makes a .bmp file ImageIO can read. */
        static byte[] bmp(byte[] dib) {
            ByteBuffer b = ByteBuffer.wrap(dib).order(ByteOrder.LITTLE_ENDIAN);
            int headerSize = b.getInt(0), bitCount = b.getShort(14) & 0xFFFF, compression = b.getInt(16), colorsUsed = b.getInt(32);
            int masks = compression == 3 && headerSize == 40 ? 12 : 0; // BI_BITFIELDS with a plain header
            int palette = bitCount <= 8 ? (colorsUsed == 0 ? 1 << bitCount : colorsUsed) * 4 : 0;
            int offset = 14 + headerSize + masks + palette;
            ByteArrayOutputStream out = new ByteArrayOutputStream(dib.length + 14);
            ByteBuffer h = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
            h.put((byte) 'B').put((byte) 'M').putInt(dib.length + 14).putInt(0).putInt(offset);
            out.writeBytes(h.array());
            out.writeBytes(dib);
            return out.toByteArray();
        }
    }
}
