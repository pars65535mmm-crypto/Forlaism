package com.tyami.forlaism.item;

import com.tyami.forlaism.client.FreezerClockClientExtensions;
import com.tyami.forlaism.world.WorldTimeFreezeManager;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
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

/**
 * 回る冷凍時計。
 * 10秒間長押しすることで、全世界の時間を永遠に凍結させる。
 * プレイヤー以外のあらゆる事象（機械、ブロック、エンティティ、ワールド時間、天候）が完全停止する。
 * 所持中または時間停止中、プレイヤーの死を即座に否定し蘇生させる (fakeDream風)。
 */
public class FreezerClockItem extends Item implements GeoItem, IAnimatedTextItem {

    public static final int CHARGE_TICKS = 200; // 10秒 = 200 ticks

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("idel");

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public FreezerClockItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // =========================================================
    // GeckoLib アニメーション
    // =========================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "controller",
                0,
                state -> state.setAndContinue(IDLE_ANIM)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================
    // チャージ（右クリック長押し）
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!(living instanceof Player player)) return;

        int elapsed = CHARGE_TICKS - remainingUseDuration;
        int remainingSeconds = (remainingUseDuration + 19) / 20;

        if (level.isClientSide) {
            // クライアント側での演出
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            // チクタク音
            if (elapsed % 20 == 0) {
                float pitch = 0.8F + (elapsed / 200.0F) * 0.8F;
                level.playLocalSound(x, y, z, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8F, pitch, false);
                level.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, pitch * 1.2F, false);
            }

            // チャージ進行度バー
            int progressBars = (int) ((elapsed / (float) CHARGE_TICKS) * 20);
            StringBuilder bar = new StringBuilder("§b");
            for (int i = 0; i < 20; i++) {
                if (i == progressBars) {
                    bar.append("§7");
                }
                bar.append("|");
            }

            String stateText = WorldTimeFreezeManager.isFrozen() ? "解除" : "発動";
            player.displayClientMessage(
                    Component.literal("§b§l【絶対時間凍結】" + stateText + "まで " + bar + " §f" + remainingSeconds + "秒"),
                    true
            );
        } else if (level instanceof ServerLevel sl) {
            // サーバー側パーティクル
            double angle = (elapsed * 18.0) * (Math.PI / 180.0);
            double rx = Math.cos(angle) * 1.2;
            double rz = Math.sin(angle) * 1.2;

            sl.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    player.getX() + rx,
                    player.getY() + 0.5 + (elapsed % 20) * 0.05,
                    player.getZ() + rz,
                    2, 0, 0.02, 0, 0.01
            );

            if (elapsed % 40 == 0) {
                sl.sendParticles(
                        ParticleTypes.END_ROD,
                        player.getX(),
                        player.getY() + 1.0,
                        player.getZ(),
                        5, 0.3, 0.3, 0.3, 0.02
                );
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide && living instanceof Player player && level.getServer() != null) {
            WorldTimeFreezeManager.toggleFrozen(level.getServer(), player);
        }
        return stack;
    }

    // =========================================================
    // WaveTextLib アニメーション表示
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("回る冷凍時計")
                .wave(2.8F, 0.15F, 0.45F)
                .gradient(0xFF00FFFF, 0xFFE0FFFF, 0xFF87CEFA, 0xFFFFFFFF)
                .gradientSpeed(1.5F)
                .gradientPhase(0.25F);
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§b§l【永遠の絶対時間停止】")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§7右クリックを10秒間長押しすることで世界の全時間を停止する。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7ワールドをリログしても凍結は解けない。")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("§7プレイヤー以外の機械・ブロック・Entity・ワールド全てを止める。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§6§l【即時復活 (fakeDream)】")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7所持中または時間停止中、プレイヤーの死を否定し即時蘇生する。")
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new FreezerClockClientExtensions());
    }
}
