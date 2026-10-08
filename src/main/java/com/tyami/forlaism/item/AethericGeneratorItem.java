package com.tyami.forlaism.item;

import com.tyami.forlaism.block.entity.AethericGeneratorBlockEntity;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * 電核機のアイテム版。
 *
 * GeckoLib のアイテムレンダラでブロックと同じ見た目を再現する。
 * ブロックとしての機能は AethericGeneratorBlock 側。
 */
public class AethericGeneratorItem extends BlockItem implements GeoItem {

    private static final RawAnimation KEIZOKU =
            RawAnimation.begin().thenLoop("keizoku");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public AethericGeneratorItem(Block block, Properties properties) {
        super(block, properties);
    }

    // =========================================================
    // GeckoLib
    // =========================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main_controller",
                0,
                this::predicate
        ));
    }

    private <E extends AethericGeneratorItem>
    PlayState predicate(AnimationState<E> state) {
        // アイテムとして持ってる時は常時回転
        state.getController().setAnimation(KEIZOKU);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================
    // クライアント拡張
    // =========================================================

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new com.tyami.forlaism.client.AethericGeneratorItemClientExtensions());
    }
}