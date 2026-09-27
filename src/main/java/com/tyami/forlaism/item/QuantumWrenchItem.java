package com.tyami.forlaism.item;

import com.tyami.forlaism.block.entity.WarpSenderBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 量子レンチ。
 *
 * - 量子転送機のネットワーク番号を切り替える
 * - Warp送信機の送信先を設定する
 */
public class QuantumWrenchItem extends Item {

    /** 送信元記憶UUIDのNBTキー。 */
    private static final String TAG_SENDER_POS = "WarpSenderPos";
    private static final String TAG_SENDER_DIM = "WarpSenderDim";

    public QuantumWrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        var player = context.getPlayer();
        ItemStack wrench = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        // Shift+右クリック → Warp送信元を記憶
        if (player.isShiftKeyDown() && !level.isClientSide) {
            // もしクリック先が WarpSenderBlockEntity なら送信元として記憶
            if (level.getBlockEntity(clickedPos) instanceof WarpSenderBlockEntity sender) {
                // レンチ自体に記憶
                wrench.getOrCreateTag().putLong(TAG_SENDER_POS, clickedPos.asLong());
                wrench.getOrCreateTag().putString(
                        TAG_SENDER_DIM,
                        level.dimension().location().toString()
                );

                sender.setSenderSource(clickedPos, level.dimension());

                player.displayClientMessage(
                        Component.literal("§b[Warp] §f送信元を記憶: §e" + clickedPos.toShortString()),
                        true
                );
                return InteractionResult.SUCCESS;
            }
        }

        // 通常右クリック → 記憶した送信元に「送信先」を設定
        if (!player.isShiftKeyDown() && !level.isClientSide) {
            var tag = wrench.getTag();
            if (tag != null && tag.contains(TAG_SENDER_POS)) {
                BlockPos senderPos = BlockPos.of(tag.getLong(TAG_SENDER_POS));
                String dimStr = tag.getString(TAG_SENDER_DIM);

                // 同一次元のみ
                if (!dimStr.equals(level.dimension().location().toString())) {
                    player.displayClientMessage(
                            Component.literal("§c異なる次元へは送信できません"),
                            true
                    );
                    return InteractionResult.FAIL;
                }

                if (level.getBlockEntity(senderPos) instanceof WarpSenderBlockEntity sender) {
                    sender.setTarget(clickedPos, level.dimension());

                    double dx = clickedPos.getX() - senderPos.getX();
                    double dy = clickedPos.getY() - senderPos.getY();
                    double dz = clickedPos.getZ() - senderPos.getZ();
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    player.displayClientMessage(
                            Component.literal(
                                    "§b[Warp] §f送信先を設定: §e"
                                            + clickedPos.toShortString()
                                            + " §7(距離: " + String.format("%.1f", distance) + "m)"
                            ),
                            true
                    );
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b量子転送機のネットワークを切り替える")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§7通常右クリック: §a+1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7スニーク+右クリック: §c-1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§d[Warp送信機]")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7送信機をスニーク+右クリック: §e送信元を記憶")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7別ブロックを右クリック: §e送信先を設定")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}