package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ディメンショナルブレイカー。
 *
 * 第四の壁を切り裂く鍵。
 * dream_floor を右クリックすると次元のカケラを落とす。
 *
 * dream_floor は消えず、その場に復活する。
 */
public class DimensionalBreakerItem extends SwordItem {

    /** 1回の破壊で得られる次元のカケラの数。 */
    public static final int SHARD_COUNT = 3;

    public DimensionalBreakerItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                5,
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 右クリック: dream_floor を叩くとカケラ入手
    // =========================================================

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);

        // dream_floor 以外は通常処理
        if (!state.is(com.tyami.forlaism.registry.Blocks.DREAM_FLOOR.get())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }

        // =========================================================
        // 第四の壁を切り裂く！
        // =========================================================

        // 次元のカケラを3個ドロップ
        for (int i = 0; i < SHARD_COUNT; i++) {
            ItemStack shard = new ItemStack(Items.DIMENSION_SHARD.get());

            ItemEntity itemEntity = new ItemEntity(
                    sl,
                    pos.getX() + 0.5 + (sl.random.nextDouble() - 0.5) * 0.5,
                    pos.getY() + 1.0,
                    pos.getZ() + 0.5 + (sl.random.nextDouble() - 0.5) * 0.5,
                    shard
            );
            itemEntity.setNoPickUpDelay();
            sl.addFreshEntity(itemEntity);
        }

        // =========================================================
        // 演出
        // =========================================================

        sl.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS,
                1.5F, 0.5F
        );

        sl.playSound(
                null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS,
                1.2F, 0.7F
        );

        sl.sendParticles(
                ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                40, 0.5, 0.5, 0.5, 0.3
        );

        sl.sendParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.05
        );

        sl.sendParticles(
                ParticleTypes.FLASH,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1, 0, 0, 0, 0
        );

        player.displayClientMessage(
                Component.literal("§5§l虚空が §d裂けた…")
                        .withStyle(ChatFormatting.DARK_PURPLE),
                true
        );

        return InteractionResult.SUCCESS;
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
        tooltip.add(Component.literal("§5§l壁を切り裂く剣。")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}