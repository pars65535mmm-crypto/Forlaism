package com.tyami.forlaism.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class NeginaifuItem extends SwordItem {

    /** 永続敵対破棄タグのキー。MobPacifiedMixin と揃える。 */
    public static final String PACIFIED_TAG = "ForlaismPacified";

    public NeginaifuItem(Properties properties) {
        super(
                Tiers.DIAMOND,
                3,      // 攻撃力 4
                -2.8F,  // 攻撃速度 1.2
                properties.durability(125_003_250)
        );
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        // =========================================================
        // 1. まず即座にターゲットを消す
        //    （Mixinが効く前にAIが先行するのを防止）
        // =========================================================
        if (target instanceof Mob mob) {

            // 攻撃対象を消す
            mob.setTarget(null);

            // 殴り返し履歴もクリア
            mob.setLastHurtByMob(null);

            if (attacker instanceof Player player) {
                mob.setLastHurtByPlayer(null);
                mob.setLastHurtByMob(null);
                player.setLastHurtMob(null);
                player.setLastHurtByMob(null);
            }

            // =========================================================
            // 2. 永続敵対破棄タグを付与
            //    これ以降 setTarget が Mixin でブロックされる
            // =========================================================
            mob.getPersistentData().putBoolean(PACIFIED_TAG, true);
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§aネギを刻むための包丁。"));
        tooltip.add(Component.literal("§7殴った敵の敵対を永続で破棄する")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}