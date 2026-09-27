#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform float EffectTime;
uniform float EffectAlpha;
uniform float EffectThickness;
uniform vec4  EffectColor;

out vec4 fragColor;

// =========================================================
// ノイズ
// =========================================================
float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float noise1(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), f);
}

void main() {
    float along = texCoord0.x;   // 進行方向 0(発射)..1(着弾)
    float around = texCoord0.y;  // 周方向 0..1

    // 円柱の中心からの距離 0..1
    float a = around * 2.0 - 1.0;
    float r = abs(a);

    // =========================================================
    // 1. 多層コア構造
    // =========================================================
    // 中心の白熱コア（極細）
    float core = exp(-r * r * 40.0);
    // 中間層（青）
    float mid = exp(-r * r * 8.0) * 0.9;
    // 外周の corona（広がる光）
    float corona = exp(-r * r * 2.0) * 0.5;

    // =========================================================
    // 2. 進行方向のパルス
    // =========================================================
    float life = 1.0 - along;

    // 走査線（高速で流れる）
    float scan = 0.7 + 0.3 * sin(along * 120.0 - EffectTime * 80.0);

    // 走査線2（逆方向、細かく）
    float scan2 = 0.85 + 0.15 * sin(along * 240.0 + EffectTime * 60.0);

    // =========================================================
    // 3. 電気のゆらぎ（ビーム内を走る雷）
    // =========================================================
    float wobble = noise1(along * 60.0 + EffectTime * 12.0) * 0.4 + 0.6;
    float wobble2 = noise1(along * 180.0 + EffectTime * 25.0) * 0.3 + 0.7;

    // =========================================================
    // 4. 先頭・発射点の閃光
    // =========================================================
    // 着弾点付近（along ≈ 1.0）
    float headBlast = exp(-pow((along - 0.98) * 30.0, 2.0)) * 3.0;
    // 発射点付近（along ≈ 0.0）
    float tailBlast = exp(-pow(along * 12.0, 2.0)) * 1.5;

    // =========================================================
    // 5. 断面リング（発射点側に同心円）
    // =========================================================
    // ビームの断面を切断したようなリングを進行方向に配置
    float crossRing = 0.0;
    float ringPos = fract(along * 6.0 - EffectTime * 2.0);
    float ringDist = abs(ringPos - 0.5) * 2.0;
    crossRing = exp(-pow((1.0 - ringDist) * 8.0, 2.0)) * (1.0 - r);
    crossRing *= pow(life, 2.0);

    // =========================================================
    // 6. 色収差風
    // =========================================================
    // r の位置で色相を微妙にずらす
    float chroma = r * r * 0.3;

    // =========================================================
    // 7. 色合成
    // =========================================================
    vec3 cherenkov = EffectColor.rgb;
    vec3 white = vec3(1.0);
    vec3 hot = vec3(1.0, 1.0, 1.0);

    // コア（白）→ 中間（チェレンコフ青）→ 外周（薄い青）
    vec3 color = mix(cherenkov, white, core);
    color = mix(color, cherenkov * 1.2, mid * 0.5);

    // corona は色収差風に
    color += vec3(chroma, 0.0, -chroma) * corona;

    // ホット部分（先頭）
    color = mix(color, hot, headBlast * 0.5);

    // =========================================================
    // 8. 強度合成
    // =========================================================
    float intensity = core * 3.0
                    + mid
                    + corona;

    intensity *= scan * scan2;
    intensity *= wobble * wobble2;
    intensity *= life;

    // 先頭・発射点の爆発
    intensity += headBlast;
    intensity += tailBlast;

    // 断面リング
    intensity += crossRing * 1.5;

    // 縁を柔らかく（円柱端のぶつ切り防止）
    float edgeMask = smoothstep(1.0, 0.85, r);
    intensity *= edgeMask;

    // =========================================================
    // 9. アルファ
    // =========================================================
    float alpha = intensity * EffectAlpha * vertexColor.a * ColorModulator.a;

    fragColor = vec4(color * intensity, clamp(alpha, 0.0, 1.0));
}