#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform vec4 ColorModulator;
uniform float EffectTime;      // ナノ秒ベースで滑らかに進む時間
uniform vec4 EffectColor;      // rgb + alpha
uniform vec4 EffectParams;     // x: intensity, y: scale, z: speed, w: ringCount
uniform vec4 EffectExtra;      // x: particleCount, y: flaresEnabled, z: coreGlowEnabled, w: unused

out vec4 fragColor;

// 2D回転
vec2 rotateVec(vec2 v, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

// 美しく凛と輝く単一の「✦」（4芒星・スパークル）
float calcSingleSparkle(vec2 pos, float raySharpness, float coreSharpness) {
    // 水平・垂直の優美な光の筋
    float rayX = exp(-abs(pos.y) * raySharpness) * exp(-abs(pos.x) * 3.2);
    float rayY = exp(-abs(pos.x) * raySharpness) * exp(-abs(pos.y) * 3.2);
    
    // 中心のダイヤモンド核（✦の美しいふっくらとした菱形）
    float diamond = exp(-(abs(pos.x) + abs(pos.y)) * coreSharpness);
    
    // 中心のごく小さな眩しい核
    float pinPoint = exp(-length(pos) * 26.0);
    
    return (rayX + rayY) * 0.70 + diamond * 1.0 + pinPoint * 0.8;
}

void main() {
    // 中心を (0.0, 0.0)、半径約 1.0 に正規化
    vec2 p = (texCoord0 - vec2(0.5)) * 2.0;
    
    // スケール補正
    float scale = EffectParams.y > 0.0 ? EffectParams.y : 1.0;
    p /= scale;

    float r = length(p);

    // 四角いクアッド端でのぶつ切りを完全に防止する円形フェードマスク
    float edgeMask = smoothstep(0.95, 0.65, r);
    if (edgeMask <= 0.0) {
        discard;
    }

    // 時間と速度
    float speed = EffectParams.z > 0.0 ? EffectParams.z : 1.0;
    float time = EffectTime * speed;
    float intensity = EffectParams.x > 0.0 ? EffectParams.x : 1.6;

    // カラー（シアン〜指定色のブレンド）
    vec3 baseTint = EffectColor.rgb;
    vec3 etherealCyan = vec3(0.60, 0.90, 1.0);
    vec3 glowColor = mix(etherealCyan, baseTint, 0.6);

    // -------------------------------------------------------------
    // 1. 中央に凛と輝く唯一の「✦」（単一の4芒星スパークル）
    // -------------------------------------------------------------
    float singleSparkle = 0.0;
    if (EffectExtra.y > 0.5) {
        // ゆっくりと荘厳に回転する「✦」
        vec2 pStar = rotateVec(p, time * 0.18);
        float star = calcSingleSparkle(pStar, 24.0, 9.5);

        // 呼吸するように優雅に明滅するパルス
        float pulse = 0.85 + 0.20 * sin(time * 2.6);
        singleSparkle = star * pulse * 1.4;
    }

    // -------------------------------------------------------------
    // 2. 「✦」の背後をふんわり包み込む柔らかな光のオーラ
    // -------------------------------------------------------------
    float centerAura = 0.0;
    if (EffectExtra.z > 0.5) {
        float aura = exp(-r * 3.4) * 0.45;
        float auraPulse = 0.90 + 0.10 * sin(time * 1.8);
        centerAura = aura * auraPulse;
    }

    // -------------------------------------------------------------
    // 3. オプション（particleCount > 0 の場合のみ公転星を描画、0なら完全なし）
    // -------------------------------------------------------------
    float optionalParticles = 0.0;
    int pCount = int(clamp(EffectExtra.x, 0.0, 12.0));
    if (pCount > 0) {
        for (int i = 0; i < 12; i++) {
            if (i >= pCount) break;
            float fi = float(i);
            float orbRadius = 0.55 + 0.10 * sin(fi * 2.0 + time * 0.4);
            float dir = (mod(fi, 2.0) < 1.0) ? 1.0 : -0.85;
            float orbAngle = time * 0.8 * dir + fi * (6.2831853 / float(pCount));
            vec2 pos = vec2(cos(orbAngle), sin(orbAngle)) * orbRadius;
            float dist = length(p - pos);
            optionalParticles += exp(-dist * 18.0) * (0.7 + 0.3 * sin(time * 3.5 + fi)) * 0.8;
        }
    }

    // -------------------------------------------------------------
    // 4. オプションのリング（ringCount > 0 の場合のみ描画、0なら完全なし）
    // -------------------------------------------------------------
    float optionalRings = 0.0;
    int rings = int(clamp(EffectParams.w, 0.0, 3.0));
    if (rings >= 1) {
        optionalRings += exp(-abs(r - 0.65) * 24.0) * 0.5;
    }

    // -------------------------------------------------------------
    // 5. 合算とカラーリング
    // -------------------------------------------------------------
    float totalEnergy = (singleSparkle + centerAura + optionalParticles + optionalRings) * intensity;
    
    // 円形マスクで四角い境界を100%消滅
    totalEnergy *= edgeMask;

    // 「✦」の光の核は眩しい純白、外縁は幻想的な指定色
    vec3 pureWhite = vec3(1.0, 1.0, 1.0);
    float whiteMix = clamp(singleSparkle * 0.85, 0.0, 1.0);
    vec3 finalRgb = mix(glowColor, pureWhite, whiteMix);

    float outputAlpha = clamp(totalEnergy * EffectColor.a * vertexColor.a * ColorModulator.a, 0.0, 1.0);

    fragColor = vec4(finalRgb * totalEnergy, outputAlpha);
}
