#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform float EffectTime;
uniform float EffectAlpha;

out vec4 fragColor;

// =========================================================
// ノイズ
// =========================================================
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p *= 2.02;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 uv = texCoord0;

    // 時間
    float t = EffectTime * 0.15;

    // =========================================================
    // 1. オーロラの帯（横長のカーテン）
    // =========================================================
    // ゆっくり流れるノイズ
    float curtain1 = fbm(vec2(uv.x * 3.0 + t, uv.y * 2.0 + t * 0.5));
    float curtain2 = fbm(vec2(uv.x * 5.0 - t * 0.7, uv.y * 3.0));
    float curtain3 = fbm(vec2(uv.x * 7.0 + t * 1.3, uv.y * 4.0 - t * 0.3));

    float curtain = curtain1 * 0.5 + curtain2 * 0.3 + curtain3 * 0.2;

    // 縦方向の減衰（上に行くほど消える）
    float vertFade = smoothstep(0.0, 0.4, uv.y) * smoothstep(1.0, 0.6, uv.y);

    // =========================================================
    // 2. 縦縞（オーロラ特有のカーテン構造）
    // =========================================================
    float stripes = sin(uv.x * 40.0 + fbm(vec2(uv.x * 2.0, t)) * 5.0) * 0.5 + 0.5;
    stripes = pow(stripes, 2.0);

    // =========================================================
    // 3. 合成
    // =========================================================
    float aurora = curtain * vertFade * (0.7 + 0.3 * stripes);

    // =========================================================
    // 4. 色（緑ベース + 上端に紫）
    // =========================================================
    vec3 green = vec3(0.30, 1.00, 0.50);
    vec3 teal = vec3(0.20, 0.90, 0.80);
    vec3 purple = vec3(0.60, 0.30, 0.90);

    // 下は緑、中は青緑、上は紫
    vec3 color = mix(green, teal, smoothstep(0.0, 0.5, uv.y));
    color = mix(color, purple, smoothstep(0.6, 1.0, uv.y));

    // =========================================================
    // 5. アルファ
    // =========================================================
    float alpha = aurora * EffectAlpha * vertexColor.a;

    fragColor = vec4(color * aurora, clamp(alpha, 0.0, 1.0));
}