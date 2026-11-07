#version 150
// Quest Clusters Visuals default chapter background: slowly drifting nebula with a parallax star field.
// Uniforms (same meaning as for shaders written for Certain Questing Additions):
//   size (px), scrollOffset (px, centre of the visible canvas), scrollSize (px, whole canvas), time (s), zoom.

uniform vec2 size;
uniform vec2 scrollOffset;
uniform vec2 scrollSize;
uniform float time;
uniform float zoom;

in vec2 texCoord0;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), u.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 px = texCoord0 * size;
    float z = max(zoom, 1.0) / 16.0;
    vec2 world = (px - size * 0.5) / z + scrollOffset;     // moves with the quest canvas
    vec2 q = world / 480.0 + vec2(time * 0.01, time * 0.006);
    float n = fbm(q + fbm(q * 1.7 + time * 0.02));
    vec3 deep = vec3(0.03, 0.04, 0.09);
    vec3 glow = mix(vec3(0.20, 0.10, 0.35), vec3(0.05, 0.25, 0.35), fbm(q * 0.5));
    vec3 col = mix(deep, glow, smoothstep(0.35, 0.95, n));
    // two parallax star layers
    for (int l = 1; l <= 2; l++) {
        vec2 sp = (px - size * 0.5) / (z * float(l)) + scrollOffset / float(l + 1);
        float cellSize = 24.0;
        vec2 cell = floor(sp / cellSize);
        float h = hash(cell + float(l) * 17.0);
        if (h > 0.92) {
            // one small soft star per lucky cell, at a random spot inside it
            vec2 centre = (cell + 0.2 + 0.6 * vec2(hash(cell + 3.1), hash(cell + 7.7))) * cellSize;
            float d = length(sp - centre);
            float tw = 0.6 + 0.4 * sin(time * (1.0 + h * 3.0) + h * 40.0);
            col += vec3(smoothstep(1.6, 0.0, d) * tw * (0.9 / float(l)));
        }
    }
    // soft vignette
    vec2 uv = texCoord0 - 0.5;
    col *= 1.0 - dot(uv, uv) * 0.8;
    fragColor = vec4(col, 1.0);
}
