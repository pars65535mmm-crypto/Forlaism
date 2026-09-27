package com.tyami.forlaism.item;

import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * パラドックス。
 *
 * - 使用しても消費しない
 * - Dreamから元の世界（オーバーワールド）へ帰還
 * - クラフトで paradox × 1 → paradox × 2 に増殖可能
 */
public class ParadoxItem extends Item {

    /** 帰還先のY座標（オーバーワールド）。 */
    private static final double OVERWORLD_Y = 100.0D;

    public ParadoxItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {

            ServerLevel overworld = serverPlayer.server.overworld();

            // リスポーン地点があればそこへ、なければ現在座標のXZ
            BlockPos respawnPos = serverPlayer.getRespawnPosition();
            double targetX, targetZ;

            if (respawnPos != null
                    && serverPlayer.getRespawnDimension().equals(overworld.dimension())) {
                targetX = respawnPos.getX() + 0.5D;
                targetZ = respawnPos.getZ() + 0.5D;
            } else {
                targetX = serverPlayer.getX();
                targetZ = serverPlayer.getZ();
            }

            // 安全な高さを探す（簡易）
            double targetY = findSafeY(overworld, (int) targetX, (int) targetZ);

            serverPlayer.teleportTo(
                    overworld,
                    targetX,
                    targetY,
                    targetZ,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );

            serverPlayer.displayClientMessage(
                    Component.literal("§5§lパラドックス §fが世界を歪めた…"),
                    true
            );
        }

        // 消費しない
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * 安全なY座標を探す。
     */
    private static double findSafeY(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
        // 上から下へ走査
        for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); y--) {
            pos.setY(y);
            if (!level.getBlockState(pos).isAir()) {
                return y + 1.0D;
            }
        }
        return OVERWORLD_Y;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§5因果を歪める石。")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§7使用すると元の世界へ帰還する。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8消費されない。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}