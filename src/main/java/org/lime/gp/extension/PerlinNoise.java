package org.lime.gp.extension;

import java.util.Random;

public class PerlinNoise {
    public static final PerlinNoise STABLE = new PerlinNoise(165789);

    private float   xo, yo, zo;
    private float[] pow;
    private int[]   perm;

    public PerlinNoise(int seed) {
        pow  = new float[32];
        perm = new int[512];

        int[] permutation = new int[256];
        Random r = new Random(seed);

        for (int i = 0; i < pow.length; i++)  pow[i] = (float) Math.pow(2, i);
        for(int i = 0; i < permutation.length; i++) permutation[i] = r.nextInt(256);
        for (int i = 0; i < 256; i++) perm[256 + i] = perm[i] = permutation[i];
    }

    public void offset(float x, float y, float z) {
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }
    public float smoothNoise(float x, float y, float z, int octaves) {
        float height = 0.0f;
        for (int octave = 1; octave <= octaves; octave++)
            height += noise(x, y, z, octave);
        return height;
    }
    public float turbulentNoise(float x, float y, float z, int octaves) {
        float height = 0.0f;
        for (int octave = 1; octave <= octaves; octave++) {
            float h = noise(x, y, z, octave);
            if (h < 0.0f)
                h *= -1.0f;
            height += h;
        }
        return height;
    }
    public float noise(float x, float y, float z) {
        float fx = floor(x);
        float fy = floor(y);
        float fz = floor(z);

        int gx = (int) fx & 0xFF;
        int gy = (int) fy & 0xFF;
        int gz = (int) fz & 0xFF;

        float u = fade(x -= fx);
        float v = fade(y -= fy);
        float w = fade(z -= fz);

        int a0 = perm[gx + 0] + gy;
        int b0 = perm[gx + 1] + gy;
        int aa = perm[a0 + 0] + gz;
        int ab = perm[a0 + 1] + gz;
        int ba = perm[b0 + 0] + gz;
        int bb = perm[b0 + 1] + gz;

        float a1 = grad(perm[bb + 1], x - 1, y - 1, z - 1);
        float a2 = grad(perm[ab + 1], x - 0, y - 1, z - 1);
        float a3 = grad(perm[ba + 1], x - 1, y - 0, z - 1);
        float a4 = grad(perm[aa + 1], x - 0, y - 0, z - 1);
        float a5 = grad(perm[bb + 0], x - 1, y - 1, z - 0);
        float a6 = grad(perm[ab + 0], x - 0, y - 1, z - 0);
        float a7 = grad(perm[ba + 0], x - 1, y - 0, z - 0);
        float a8 = grad(perm[aa + 0], x - 0, y - 0, z - 0);

        float a2_1 = lerp(u, a2, a1);
        float a4_3 = lerp(u, a4, a3);
        float a6_5 = lerp(u, a6, a5);
        float a8_7 = lerp(u, a8, a7);
        float a8_5 = lerp(v, a8_7, a6_5);
        float a4_1 = lerp(v, a4_3, a2_1);
        float a8_1 = lerp(w, a8_5, a4_1);

        return a8_1;
    }

    private float noise(float x, float y, float z, int octave) {
        float p = pow[octave];
        return this.noise(x * p + this.xo, y * p + this.yo, z * p + this.zo) / p;
    }
    private float floor(float v) { return (int) v; }
    private float fade(float t) { return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f); }
    private float lerp(float t, float a, float b) { return a + t * (b - a); }
    private static float grad(int hash, float x, float y, float z) {
        int h = hash & 15;
        float u = (h < 8) ? x : y;
        float v = (h < 4) ? y : ((h == 12 || h == 14) ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
