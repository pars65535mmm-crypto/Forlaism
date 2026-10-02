package com.tyami.forlaism.client.animation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * EndWarden のアニメーション定義。
 *
 * 旧 end_wardenAnimation.java を 1.20.1 Forge 用に移植。
 * アニメ名は EndWardenAttackPhase.getAnimationName() と一致させている。
 *
 *   iddel   → IDLE / DEATH
 *   warkaa  → WALK / DIVE
 *   atttack → MELEE
 *   beeeeem → SONIC_BOOM
 */
public final class EndWardenAnimation {

    private EndWardenAnimation() {
    }

    // =========================================================
    // iddel : 待機（羽バサ + 黒玉回転）
    // =========================================================
    public static final AnimationDefinition IDLE =
            AnimationDefinition.Builder.withLength(1.0417F).looping()
                    .addAnimation("left_ribcage", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.posVec(-0.3F, -0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.posVec(-0.3F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_ribcage", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.posVec(0.4F, 0.1F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.posVec(0.3F, -0.4F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 7.5F, -2.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 5.0F, -2.5F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -15.0F, 2.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 2.5F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("hidarihane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.25F, KeyframeAnimations.degreeVec(5.2221F, 34.6575F, 9.1304F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.75F, KeyframeAnimations.degreeVec(-2.4121F, -44.9045F, 6.4031F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.9583F, KeyframeAnimations.degreeVec(-1.1377F, -6.5563F, 10.0002F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-0.218F, -2.4905F, 5.0047F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("migihhane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, -37.5F, -2.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 47.5F, -2.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 5.0F, -2.5F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("kurodama", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.2083F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -75.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.4583F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -160.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.7083F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -305.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.8333F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -417.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0417F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -585.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("kurodama", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .build();

    // =========================================================
    // warkaa : 歩行
    // =========================================================
    public static final AnimationDefinition WALK =
            AnimationDefinition.Builder.withLength(0.5417F).looping()
                    .addAnimation("head", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.2917F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.25F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_leg", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.25F, KeyframeAnimations.degreeVec(35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.375F, KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5417F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_leg", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-40.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.375F, KeyframeAnimations.degreeVec(25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5417F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("hidarihane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.0833F, KeyframeAnimations.degreeVec(0.0F, 45.0F, -12.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.2917F, KeyframeAnimations.degreeVec(-1.6575F, -12.3914F, -4.8201F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-1.8672F, -29.8832F, -4.2453F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("migihhane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 20.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.0833F, KeyframeAnimations.degreeVec(2.8605F, -37.3975F, -6.1078F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.2917F, KeyframeAnimations.degreeVec(-7.8514F, 18.7772F, -0.9517F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-5.1627F, 24.7802F, 4.4974F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .build();

    // =========================================================
    // atttack : 近接攻撃
    // =========================================================
    public static final AnimationDefinition ATTACK =
            AnimationDefinition.Builder.withLength(1.125F)
                    .addAnimation("head", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.25F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.9583F, KeyframeAnimations.degreeVec(42.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0833F, KeyframeAnimations.degreeVec(15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_tendril", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 2.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.1667F, KeyframeAnimations.degreeVec(-42.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.375F, KeyframeAnimations.degreeVec(-107.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.7083F, KeyframeAnimations.degreeVec(-150.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.8333F, KeyframeAnimations.degreeVec(-65.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.9583F, KeyframeAnimations.degreeVec(-37.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.7083F, KeyframeAnimations.degreeVec(-147.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-35.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("hidarihane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("migihhane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .build();

    // =========================================================
    // beeeeem : ソニックブーム（チャージ + 発射）
    // =========================================================
    public static final AnimationDefinition SONIC_BOOM =
            AnimationDefinition.Builder.withLength(2.0417F)
                    .addAnimation("head", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-22.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.1667F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_tendril", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_tendril", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.1667F, KeyframeAnimations.posVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_ribcage", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_ribcage", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-32.6563F, 12.5957F, 18.7734F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-77.6563F, 12.5957F, 18.7734F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-76.7146F, 24.7872F, 21.6948F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.1667F, KeyframeAnimations.degreeVec(-61.2516F, -64.2926F, -10.2612F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.7917F, KeyframeAnimations.degreeVec(-53.7516F, -64.2926F, -10.2612F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.9167F, KeyframeAnimations.degreeVec(-61.1804F, -64.4193F, 48.8286F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_arm", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(-31.8441F, -15.3579F, -23.0945F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-66.8441F, -15.3579F, -23.0945F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(-65.818F, -22.2289F, -26.2731F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.1667F, KeyframeAnimations.degreeVec(-34.9325F, 62.4497F, 35.1375F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.7917F, KeyframeAnimations.degreeVec(-19.9325F, 62.4497F, 35.1375F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.9167F, KeyframeAnimations.degreeVec(-87.2684F, 66.7573F, -83.7056F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("left_leg", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(2.8444F, -0.5147F, -6.9341F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("right_leg", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.5F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.7917F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.5F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("hidarihane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -30.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F, -80.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, -87.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.1667F, KeyframeAnimations.degreeVec(43.3838F, -72.0626F, -44.8199F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.6667F, KeyframeAnimations.degreeVec(59.8853F, -70.1991F, -63.9188F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.7917F, KeyframeAnimations.degreeVec(12.4666F, -11.4913F, -5.3807F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("hidarihane", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, -6.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.7917F, KeyframeAnimations.posVec(0.0F, 0.0F, -1.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("migihhane", new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 35.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F, 70.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 80.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.1667F, KeyframeAnimations.degreeVec(52.8322F, 77.5229F, 53.4893F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.6667F, KeyframeAnimations.degreeVec(52.3174F, 68.5285F, 55.5803F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.7917F, KeyframeAnimations.degreeVec(10.145F, 12.1978F, 3.4836F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .addAnimation("migihhane", new AnimationChannel(
                            AnimationChannel.Targets.POSITION,
                            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.3333F, KeyframeAnimations.posVec(0.0F, 0.0F, -9.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(1.7917F, KeyframeAnimations.posVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR)
                    ))
                    .build();
}