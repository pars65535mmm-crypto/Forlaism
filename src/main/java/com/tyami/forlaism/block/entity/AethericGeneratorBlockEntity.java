package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AethericGeneratorBlockEntity
        extends BlockEntity
        implements GeoBlockEntity {

    private static final RawAnimation KEIZOKU =
            RawAnimation.begin().thenLoop("keizoku");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public AethericGeneratorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                BlockEntities.AETHERIC_GENERATOR.get(),
                pos,
                state
        );
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers
    ) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "main_controller",
                        0,
                        this::predicate
                )
        );
    }

    private <E extends AethericGeneratorBlockEntity>
    PlayState predicate(AnimationState<E> state) {

        state.getController().setAnimation(KEIZOKU);

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}