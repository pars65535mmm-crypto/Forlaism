package com.tyami.forlaism.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.tyami.forlaism.entity.StuckJillstlairleatEntity;
import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.JillstlairleatWaveTextPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 耐久値が-1のジルスレイト。
 *
 * 攻撃力: -1
 * 攻撃速度: -1
 * 耐久値: -1
 * 使用できない。
 * Shift+右クリックを繰り返すと、wavetextlibにより虚無的なメッセージが表示され、
 * 20回目で自ら乱回転・発光・抜け出し、地面に突き刺さる。
 */
public class BrokenJillstlairleatItem extends Item {

    public BrokenJillstlairleatItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift + 右クリックのみ判定
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            int count = tag.getInt("ClickCount") + 1;
            tag.putInt("ClickCount", count);

            if (count >= 20) {
                // 20回目: 武器が乱回転し、抜け出し、突き刺さる演出を開始
                double px = player.getX();
                double py = player.getY() + 0.8;
                double pz = player.getZ();

                StuckJillstlairleatEntity stuckEntity = new StuckJillstlairleatEntity(level, px, py, pz);
                level.addFreshEntity(stuckEntity);

                // 手からアイテムを消費
                stack.shrink(1);
                return InteractionResultHolder.consume(stack);
            }

            // 1 ~ 19回目のメッセージ取得
            String message = getClickMessage(count);
            if (player instanceof ServerPlayer sp) {
                FactotumPacketHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sp),
                        new JillstlairleatWaveTextPacket(message, 70)
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private String getClickMessage(int count) {
        return switch (count) {
            case 1 -> "ただし何も起きなかった";
            case 2 -> "やはり何も起きなかった";
            case 3, 4, 5, 6, 7, 8, 9 -> "何度繰り返しても意味はないようだ..";
            case 10 -> "..やはり意味はないようだ";
            case 11 -> "やはり何も起きなかった";
            case 12 -> "やはり何も起きない";
            case 13 -> "何も起こらない";
            case 14 -> "....";
            case 15 -> "...";
            case 16 -> "..";
            case 17 -> ".....";
            case 18 -> "...";
            case 19 -> "..";
            default -> "....";
        };
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", -1.0, AttributeModifier.Operation.ADDITION)
            );
            builder.put(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -1.0, AttributeModifier.Operation.ADDITION)
            );
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public int getDamage(ItemStack stack) {
        return -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7耐久値: -1").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§c使用できない").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
