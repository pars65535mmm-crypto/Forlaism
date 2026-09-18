package com.tyami.forlaism.client.halo;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class HaloOfTheFirmamentani {

    public static final AnimationDefinition animation =
            AnimationDefinition.Builder
                    .withLength(1.0F)
                    .looping()
                    .addAnimation(
                            "gaienn",
                            new AnimationChannel(
                                    AnimationChannel.Targets.ROTATION,
                                    new Keyframe(
                                            0.0F,
                                            KeyframeAnimations.degreeVec(
                                                    0.0F,
                                                    0.0F,
                                                    0.0F
                                            ),
                                            AnimationChannel.Interpolations.LINEAR
                                    ),
                                    new Keyframe(
                                            1.0F,
                                            KeyframeAnimations.degreeVec(
                                                    0.0F,
                                                    90.0F,
                                                    0.0F
                                            ),
                                            AnimationChannel.Interpolations.LINEAR
                                    )
                            )
                    )
                    .build();
}