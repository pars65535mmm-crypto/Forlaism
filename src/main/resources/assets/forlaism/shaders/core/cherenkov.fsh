#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform float EffectTime;
uniform float EffectAlpha;
uniform vec4 EffectColor;

out vec4 fragColor;

void main() {
    // 中心からの相対座標 (-1 ～ 1)
    vec2 p = (texCoord0 - 0.5) * 2.0;
    float r = length(p);

    // 円形マスク（外側を透明に）
    if (r > 1.0) {
        discard;
    }
    float circleMask = smoothstep(1.0, 0.85, r);

    // =========================================================
    // 回転角
    // =========================================================
    float angle = atan(p.y, p.x);
    float rotated = angle + EffectTime * 0.15;

    // =========================================================
    // 1. メインの光輪（中心から外側に広がるリング）
    // =========================================================
    float ringRadius = 0.55;
    float ringWidth = 0.08;
    float ring = exp(-pow((r - ringRadius) / ringWidth, 2.0));

    // =========================================================
    // 2. 逆回転するセカンドリング
    // =========================================================
    float ring2Radius = 0.30;
    float ring2Width = 0.05;
    float ring2 = exp(-pow((r - ring2Radius) / ring2Width, 2.0)) * 0.7;

    // =========================================================
    // 3. 光線（放射状のスポーク）
    // =========================================================
    float spokes = 0.0;
    int spokeCount = 12;
    for (int i = 0; i < 12; i++) {
        if (i >= spokeCount) break;
        float spokeAngle = (float(i) / float(spokeCount)) * 6.2831853;
        float delta = abs(mod(rotated - spokeAngle + 3.14159, 6.2831853) - 3.14159);
        float spoke = exp(-delta * 30.0) * smoothstep(0.0, 0.4, r) * smoothstep(1.0, 0.6, r);
        spokes += spoke;
    }
    spokes *= 0.4;

    // =========================================================
    // 4. 中心のグロー
    // =========================================================
    float core = exp(-r * 5.0) * 1.5;

    // =========================================================
    // 5. 渦巻きパターン（動きを強調）
    // =========================================================
    float spiral = sin(rotated * 3.0 + r * 12.0 - EffectTime * 0.8) * 0.5 + 0.5;
    spiral *= exp(-r * 2.0) * 0.3;

    // =========================================================
    // 合成
    // =========================================================
    float total = (ring + ring2 + spokes + core + spiral) * circleMask;

    // チェレンコフ青（コアだけ白く）
    vec3 cherenkovBlue = EffectColor.rgb;
    vec3 white = vec3(1.0);
    float whiteMix = clamp(core * 0.7, 0.0, 1.0);
    vec3 color = mix(cherenkovBlue, white, whiteMix);

    float alpha = clamp(total * EffectAlpha * vertexColor.a, 0.0, 1.0);

    // 加算合成 (事前にブレンド設定済み)
    fragColor = vec4(color * total, alpha);
}