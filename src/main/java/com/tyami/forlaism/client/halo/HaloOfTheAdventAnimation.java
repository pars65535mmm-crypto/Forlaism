package com.tyami.forlaism.client.halo;


import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public class HaloOfTheAdventAnimation {
	public static final AnimationDefinition animation = AnimationDefinition.Builder.withLength(4.0F).looping()
		.addAnimation("group2", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -67.5F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("group", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -45.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -135.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -180.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -225.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -270.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -315.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -360.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.addAnimation("sikaku2", new AnimationChannel(AnimationChannel.Targets.ROTATION, 
			new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 45.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 90.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 135.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 180.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 225.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 270.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 315.0F), AnimationChannel.Interpolations.LINEAR),
			new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 360.0F), AnimationChannel.Interpolations.LINEAR)
		))
		.build();
}