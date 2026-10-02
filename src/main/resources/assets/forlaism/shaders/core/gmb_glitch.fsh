#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform float EffectTime;
uniform float EffectAlpha;

out vec4 fragColor;

// =========================================================
// ハッシュ
// =========================================================
float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise1(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), f);
}

// =========================================================
// HSV → RGB
// =========================================================
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 uv = texCoord0;
    vec2 p = (uv - vec2(0.5)) * 2.0;
    float r = length(p);

    // 円形マスク
    float edgeMask = smoothstep(1.0, 0.75, r);
    if (edgeMask <= 0.0) discard;

    float t = EffectTime;

    // =========================================================
    // 1. 虹色（時間で流れる）
    // =========================================================
    float hue = fract(uv.x * 1.5 + uv.y * 0.5 + t * 0.35);
    vec3 rainbow = hsv2rgb(vec3(hue, 1.0, 1.0));

    // =========================================================
    // 2. ブロック状のグリッチ（横方向にズレるノイズ）
    // =========================================================
    float blockSize = 12.0;
    vec2 blockUV = floor(uv * blockSize) / blockSize;

    // ブロック単位で時間をずらす
    float blockTime = t + hash2(blockUV) * 100.0;
    float blockNoise = hash2(vec2(blockUV.y, floor(blockTime * 4.0)));

    // グリッチ発生率: 30%程度
    float glitchActive = step(0.7, blockNoise);

    // ブロックの横方向へのズレ量
    vec2 glitchedUV = uv;
    glitchedUV.x += (blockNoise - 0.5) * 0.15 * glitchActive;

    // =========================================================
    // 3. RGB分離（色収差）
    // =========================================================
    float chroma = 0.02 + 0.03 * glitchActive;
    float cr = fract(glitchedUV.x * 1.5 + glitchedUV.y * 0.5 + t * 0.35 + chroma);
    float cg = fract(glitchedUV.x * 1.5 + glitchedUV.y * 0.5 + t * 0.35);
    float cb = fract(glitchedUV.x * 1.5 + glitchedUV.y * 0.5 + t * 0.35 - chroma);

    vec3 chromaRainbow = vec3(
            hsv2rgb(vec3(cr, 1.0, 1.0)).r,
            hsv2rgb(vec3(cg, 1.0, 1.0)).g,
            hsv2rgb(vec3(cb, 1.0, 1.0)).b
    );

    vec3 color = mix(rainbow, chromaRainbow, 0.7);

    // =========================================================
    // 4. 走査線（CRT風）
    // =========================================================
    float scanline = 0.85 + 0.15 * sin(uv.y * 240.0 + t * 30.0);

    // =========================================================
    // 5. 中心の白い輝き
    // =========================================================
    float core = exp(-r * 4.5);

    // =========================================================
    // 6. 縁の発光（虹色の corona）
    // =========================================================
    float corona = exp(-pow((r - 0.85) * 6.0, 2.0));

    // =========================================================
    // 7. 時々走る「データ破損」ライン
    // =========================================================
    float corruptLine = 0.0;
    float linePos = fract(t * 0.7);
    float lineWidth = 0.02;
    if (abs(uv.y - linePos) < lineWidth) {
        corruptLine = 1.0;
    }
    // 逆向きのライン
    if (abs(uv.y - (1.0 - linePos)) < lineWidth) {
        corruptLine = 1.0;
    }

    // =========================================================
    // 8. 合成
    // =========================================================
    vec3 finalColor = color;

    // 中心の白
    finalColor = mix(finalColor, vec3(1.0), core * 0.85);

    // 縁の虹色
    finalColor += rainbow * corona * 1.2;

    // 走査線
    finalColor *= scanline;

    // 破損ラインで白飛び
    finalColor += vec3(corruptLine) * 0.8;

    // =========================================================
    // 9. アルファ
    // =========================================================
    float alpha = edgeMask * EffectAlpha * vertexColor.a * ColorModulator.a;
    alpha *= (0.75 + 0.25 * core);

    fragColor = vec4(finalColor, clamp(alpha, 0.0, 1.0));
}