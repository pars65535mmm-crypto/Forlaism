package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 儀式の短剣で村人を刺した時の処理。
 *
 * - Dreamディメンション内で村人を儀式の短剣で殴る
 *   → イノチノカケラをドロップ
 * - 村人を殺してしまうと、プレイヤーも死ぬ
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class SacrificeKnifeHandler {

    private SacrificeKnifeHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onVillagerHurt(LivingHurtEvent event) {

        // 対象は村人のみ
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        // 攻撃者がプレイヤーか
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Dreamディメンション内か
        if (!player.level().dimension().equals(DreamDimension.DREAM_LEVEL)) {
            return;
        }

        // メインハンドが儀式の短剣か
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.KNIFE_OF_SACRIFICE.get())) {
            return;
        }

        // ダメージ後のHPを予測
        float newHealth = villager.getHealth() - event.getAmount();

        // =========================================================
        // 殺してしまった場合 → プレイヤーも死ぬ
        // =========================================================
        if (newHealth <= 0.0F) {

            // 村人のHPを1に固定して殺させない
            event.setAmount(Math.max(0.0F, villager.getHealth() - 1.0F));

            // プレイヤー即死
            player.hurt(
                    player.damageSources().genericKill(),
                    Float.MAX_VALUE
            );

            player.displayClientMessage(
                    Component.literal("§4§l村人を殺めた… 汝の命も共に…"),
                    true
            );

            return;
        }

        // =========================================================
        // 殺さなかった場合 → イノチノカケラをドロップ
        // =========================================================
        if (player.level() instanceof ServerLevel serverLevel) {

            ItemStack fragment = new ItemStack(Items.INOCHI_NO_KAKERA.get(), 1);

            Vec3 pos = villager.position().add(0, 0.5, 0);

            ItemEntity itemEntity = new ItemEntity(
                    serverLevel,
                    pos.x, pos.y, pos.z,
                    fragment
            );
            itemEntity.setNoPickUpDelay();

            serverLevel.addFreshEntity(itemEntity);
        }

        // 短剣の耐久は減らさないので何もしない
    }
}