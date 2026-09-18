package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;

import javax.annotation.Nullable;
import java.util.List;

public class TrialStaffItem extends Item implements IMagicEffectItem {

    /** クールダウン（tick）。20 tick = 1秒 */
    private static final int COOLDOWN_TICKS = 20;

    public TrialStaffItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        return MagicEffectStyle.builder()
                .color(0x8080FFFF) // 幻想的な淡い青白〜シアンの魔法光
                .intensity(1.6f)   // 鮮烈に発光する強度
                .scale(1.0f)       // アイテムサイズにフィット
                .speed(0.8f)       // ゆったり優雅なアニメーション速度
                .ringCount(0)      // 魔法陣なし
                .particleCount(0)  // 公転粒子なし（中央に1つの✦のみ）
                .flares(true)      // 中央に凛と輝く4芒星「✦」
                .coreGlow(true)    // アイテム周囲に漂う淡いオーラ
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

            // プレイヤーの目の前にブタを出す
            var look = player.getLookAngle();
            double spawnX = player.getX() + look.x * 2.0D;
            double spawnY = player.getY();
            double spawnZ = player.getZ() + look.z * 2.0D;

            Pig pig = EntityType.PIG.create(serverLevel);
            if (pig != null) {
                pig.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0.0F);
                pig.finalizeSpawn(
                        serverLevel,
                        serverLevel.getCurrentDifficultyAt(BlockPos.containing(spawnX, spawnY, spawnZ)),
                        MobSpawnType.MOB_SUMMONED,
                        null,
                        null
                );
                serverLevel.addFreshEntity(pig);
            }

            // 音
            serverLevel.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.5F
            );

            // クールダウン
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

            player.displayClientMessage(
                    Component.literal("§d試作杖 §fが何かを呼び出した…"),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.forlaism.trial_staff.tooltip")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.forlaism.trial_staff.tooltip2")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}