package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 星核のナイフ。
 *
 * - 攻撃力 10
 * - 攻撃速度 10
 * - 耐久値 10,000,000
 * - スニークしながら殴ると自爆
 * - 自爆時 1% でネギ包丁になる
 */
public class ScKnifeItem extends SwordItem {

    private static final Random RANDOM = new Random();
    private static final float NEGI_CHANCE = 0.01F; // 1%

    public ScKnifeItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                9,      // 攻撃力 10
                10.0F,  // 攻撃速度 10相当
                properties.durability(10_000_000)
        );
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target,
                             net.minecraft.world.entity.LivingEntity attacker) {

        if (attacker instanceof Player player && player.isShiftKeyDown()
                && !player.level().isClientSide) {

            // 自爆
            player.hurt(
                    player.damageSources().explosion(player, player),
                    Float.MAX_VALUE
            );

            // 爆発演出
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.explode(
                        player,
                        player.getX(), player.getY(), player.getZ(),
                        4.0F,
                        Level.ExplosionInteraction.NONE
                );
            }

            // 1% で ネギ包丁 に変化
            if (RANDOM.nextFloat() < NEGI_CHANCE) {
                ItemStack negi = new ItemStack(Items.NEGINAIFU.get());
                if (!player.getInventory().add(negi)) {
                    player.drop(negi, false);
                }
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.displayClientMessage(
                            Component.literal("§a§lネギ包丁 §fが顕現した！"),
                            true
                    );
                }
            }

            // ナイフ自体も消費
            stack.shrink(1);
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§d星核の力が宿るナイフ。"));
        tooltip.add(Component.literal("§7スニーク攻撃で自爆する")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.literal("§71%でネギ包丁になる")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}