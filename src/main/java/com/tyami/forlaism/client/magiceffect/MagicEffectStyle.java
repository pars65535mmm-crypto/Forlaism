package com.tyami.forlaism.client.magiceffect;

import java.util.Objects;

/**
 * 幻想的な魔法アイテムエフェクトの描画スタイルを定義するクラス。
 */
public final class MagicEffectStyle {

    // デフォルト値
    public static final int DEFAULT_COLOR = 0x80E8FFFF; // 半透明の淡い青白い光
    public static final float DEFAULT_INTENSITY = 1.0f;
    public static final float DEFAULT_SCALE = 1.0f;
    public static final float DEFAULT_SPEED = 1.0f;
    public static final int DEFAULT_RING_COUNT = 3;
    public static final int DEFAULT_PARTICLE_COUNT = 8;
    public static final boolean DEFAULT_FLARES = true;
    public static final boolean DEFAULT_CORE_GLOW = true;

    private final boolean enabled;
    private final int color;
    private final float intensity;
    private final float scale;
    private final float speed;
    private final int ringCount;
    private final int particleCount;
    private final boolean flares;
    private final boolean coreGlow;

    private MagicEffectStyle(Builder builder) {
        this.enabled = builder.enabled;
        this.color = builder.color;
        this.intensity = builder.intensity;
        this.scale = builder.scale;
        this.speed = builder.speed;
        this.ringCount = builder.ringCount;
        this.particleCount = builder.particleCount;
        this.flares = builder.flares;
        this.coreGlow = builder.coreGlow;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MagicEffectStyle createDefault() {
        return builder().build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getColor() {
        return color;
    }

    public float getIntensity() {
        return intensity;
    }

    public float getScale() {
        return scale;
    }

    public float getSpeed() {
        return speed;
    }

    public int getRingCount() {
        return ringCount;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public boolean hasFlares() {
        return flares;
    }

    public boolean hasCoreGlow() {
        return coreGlow;
    }

    // RGBAを [0.0, 1.0] の float 配列として取得
    public float[] getColorComponents() {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a <= 0.0f) a = 1.0f; // 0ならアルファ1.0として扱う
        return new float[]{r, g, b, a};
    }

    public static class Builder {
        private boolean enabled = true;
        private int color = DEFAULT_COLOR;
        private float intensity = DEFAULT_INTENSITY;
        private float scale = DEFAULT_SCALE;
        private float speed = DEFAULT_SPEED;
        private int ringCount = DEFAULT_RING_COUNT;
        private int particleCount = DEFAULT_PARTICLE_COUNT;
        private boolean flares = DEFAULT_FLARES;
        private boolean coreGlow = DEFAULT_CORE_GLOW;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder intensity(float intensity) {
            this.intensity = intensity;
            return this;
        }

        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder speed(float speed) {
            this.speed = speed;
            return this;
        }

        public Builder ringCount(int ringCount) {
            this.ringCount = Math.max(0, ringCount);
            return this;
        }

        public Builder particleCount(int particleCount) {
            this.particleCount = Math.max(0, particleCount);
            return this;
        }

        public Builder flares(boolean flares) {
            this.flares = flares;
            return this;
        }

        public Builder coreGlow(boolean coreGlow) {
            this.coreGlow = coreGlow;
            return this;
        }

        public MagicEffectStyle build() {
            return new MagicEffectStyle(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MagicEffectStyle that)) return false;
        return enabled == that.enabled &&
                color == that.color &&
                Float.compare(that.intensity, intensity) == 0 &&
                Float.compare(that.scale, scale) == 0 &&
                Float.compare(that.speed, speed) == 0 &&
                ringCount == that.ringCount &&
                particleCount == that.particleCount &&
                flares == that.flares &&
                coreGlow == that.coreGlow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, color, intensity, scale, speed, ringCount, particleCount, flares, coreGlow);
    }
}
