package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.ChickenCannonProjectileEntity;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * チキンキャノン。
 */
public class ChickenCannonItem extends Item implements GeoItem, IAnimatedTextItem {

    /** 発射速度。 */
    public static final double LAUNCH_SPEED = 2.0D;

    /** クールダウン (tick)。1秒 = 20tick。 */
    public static final int COOLDOWN_TICKS = 20;

    // =========================================================
    // GeckoLib
    // =========================================================

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public ChickenCannonItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .durability(500)
                .rarity(Rarity.RARE));

        // GeckoLibの同期アニメ登録（サーバー→クライアント）
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main",
                0,
                state -> state.setAndContinue(IDLE)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================
    // クライアントレンダラー
    // =========================================================

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private com.tyami.forlaism.client.renderer.ChickenCannonRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new com.tyami.forlaism.client.renderer.ChickenCannonRenderer();
                }
                return renderer;
            }
        });
    }

    // =========================================================
    // 表示名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("チキンキャノン")
                .gradient(0xFFFFAA00, 0xFFFFFFFF)
                .gradientSpeed(0.8F)
                .gradientPhase(0.5F);
    }

    // =========================================================
    // 右クリック
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 spawnPos = eye.add(look.scale(0.8)).add(0, -0.1, 0);

        ChickenCannonProjectileEntity chicken = new ChickenCannonProjectileEntity(sl, player);
        chicken.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        chicken.shoot(look, LAUNCH_SPEED);

        sl.addFreshEntity(chicken);

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CHICKEN_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.5F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.5F);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        stack.hurtAndBreak(1, player,
                p -> p.broadcastBreakEvent(hand));

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§6Re Chicken Cannon")
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}