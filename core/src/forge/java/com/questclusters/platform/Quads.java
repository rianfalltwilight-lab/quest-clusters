package com.questclusters.platform;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Immediate-mode vertices, the same calls on every version (1.21 changed the builder API):
 * {@code Quads.begin(mode, format).vertex(m, x, y, z).uv(u, v).color(argb).next() ... .draw()}.
 */
public final class Quads {
    private final BufferBuilder buf;
    private int vertices;

    private Quads(BufferBuilder buf) {
        this.buf = buf;
    }

    public static Quads begin(VertexFormat.Mode mode, VertexFormat format) {
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(mode, format);
        return new Quads(buf);
    }

    public Quads vertex(Matrix4f m, float x, float y, float z) {
        buf.vertex(m, x, y, z);
        return this;
    }

    public Quads uv(float u, float v) {
        buf.uv(u, v);
        return this;
    }

    public Quads color(int argb) {
        buf.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
        return this;
    }

    /** Ends the current vertex. */
    public Quads next() {
        buf.endVertex();
        vertices++;
        return this;
    }

    /** Draws with the current shader; nothing happens if no vertex was added. */
    public void draw() {
        var mesh = buf.end();
        if (vertices > 0) BufferUploader.drawWithShader(mesh);
    }
}
