#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform float EffectTime;
uniform float EffectAlpha;
uniform float EffectRadius;
uniform vec4  EffectColor;

out vec4 fragColor;

// =========================================================
// ノイズ
// =========================================================
float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i + vec3(0,0,0)), hash(i + vec3(1,0,0)), f.x),
            mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
        mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
            mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y),
        f.z
    );
}

float fbm(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 6; i++) {
        v += a * noise(p);
        p *= 2.02;
        a *= 0.5;
    }
    return v;
}

// =========================================================
// 2D回転
// =========================================================
vec2 rot(vec2 p, float a) {
    float s = sin(a), c = cos(a);
    return vec2(p.x * c - p.y * s, p.x * s + p.y * c);
}

void main() {
    float u = texCoord0.x;
    float v = texCoord0.y;

    float theta = u * 6.2831853;
    float phi   = v * 3.1415926;

    vec3 spherePos = vec3(
        sin(phi) * cos(theta),
        cos(phi),
        sin(phi) * sin(theta)
    );

    float edge = sin(phi);
    edge = pow(edge, 0.5);

    float t = EffectTime;

    // =========================================================
    // 1. 重力レンズ風の歪み
    //    球面座標を少し曲げる
    // =========================================================
    float distort = fbm(spherePos * 4.0 + vec3(0.0, t * 0.2, 0.0)) - 0.5;
    spherePos += vec3(distort * 0.06);

    // =========================================================
    // 2. 多層渦（3層を重ねる）
    // =========================================================
    // 時計回り渦
    vec3 p1 = rot(spherePos.xz, t * 0.6).x == 0.0
        ? spherePos
        : vec3(rot(spherePos.xz, t * 0.6).x, spherePos.y, rot(spherePos.xz, t * 0.6).y);
    vec3 n1 = p1 * 3.5 + vec3(0.0, t * 0.3, 0.0);

    // 反時計回り渦
    vec2 r2 = rot(spherePos.xz, -t * 0.9);
    vec3 p2 = vec3(r2.x, spherePos.y, r2.y);
    vec3 n2 = p2 * 6.0 - vec3(t * 0.4, 0.0, t * 0.5);

    // 放射状
    vec3 n3 = spherePos * 10.0 + vec3(sin(t * 1.5), cos(t * 1.2), sin(t * 1.8));

    float nebula = fbm(n1) * 0.45 + fbm(n2) * 0.35 + fbm(n3) * 0.20;

    // =========================================================
    // 3. 事象の地平面（縁の歪み）
    // =========================================================
    // 球体のへりを強調して、外側ほど波打たせる
    float horizon = pow(1.0 - edge, 2.5);
    horizon += 0.15 * sin(theta * 8.0 + t * 2.0) * (1.0 - edge);

    // =========================================================
    // 4. 降着円盤（中心を囲む光の渦）
    // =========================================================
    float disk = smoothstep(0.3, 0.75, nebula) * edge;
    disk = pow(disk, 1.5);

    // 渦の筋
    float swirlLines = sin(theta * 6.0 + t * 3.0 + nebula * 8.0) * 0.5 + 0.5;
    swirlLines *= pow(edge, 1.8);

    // =========================================================
    // 5. 中心の特異点
    // =========================================================
    float centerDist = length(spherePos.xy) + length(spherePos.yz) * 0.3;
    float singularity = 1.0 - smoothstep(0.0, 0.30, centerDist);

    // =========================================================
    // 6. パルス（脈打つ）
    // =========================================================
    float pulse = 0.85 + 0.15 * sin(t * 4.0);

    // =========================================================
    // 7. 色合成
    // =========================================================
    vec3 deepBlack = vec3(0.0, 0.0, 0.02);
    vec3 cherenkov = EffectColor.rgb;
    vec3 hotCore = vec3(1.0, 1.0, 1.0);

    // 基本は深い黒
    vec3 color = deepBlack;

    // 渦の青
    color = mix(color, cherenkov * 0.8, disk * 1.2);

    // 降着円盤の筋（明るい）
    color += cherenkov * swirlLines * 1.5 * pulse;

    // 事象の地平面（縁の光）
    color += cherenkov * horizon * 2.0 * pulse;

    // ホットコア（渦の中心付近だけ白く）
    float hotZone = smoothstep(0.55, 0.85, nebula) * edge;
    color = mix(color, hotCore * 1.5, hotZone * 0.7);

    // 特異点（真っ黒で塗りつぶす）
    color = mix(color, vec3(0.0), singularity);

    // =========================================================
    // 8. アルファ
    // =========================================================
    float alpha = (0.35 + 0.65 * edge) * EffectAlpha;
    alpha *= (0.6 + 0.4 * nebula);
    alpha *= (0.8 + 0.2 * pulse);
    alpha *= vertexColor.a * ColorModulator.a;

    // 特異点は完全に不透明な黒
    alpha = mix(alpha, EffectAlpha, singularity);

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}