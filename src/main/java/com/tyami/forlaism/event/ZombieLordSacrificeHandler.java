package com.tyami.forlaism.event;

import com.tyami.forlaism.entity.ZombieLordEntity;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 儀式の短剣でゾンビを殺すと、ゾンビロードが確定召喚される。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class ZombieLordSacrificeHandler {

    private ZombieLordSacrificeHandler() {
    }

    @SubscribeEvent
    public static void onZombieDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie instanceof ZombieLordEntity) return;
        if (zombie.level().isClientSide) return;

        // 攻撃者チェック
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // メインハンドが儀式の短剣か
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.KNIFE_OF_SACRIFICE.get())) return;

        if (!(zombie.level() instanceof ServerLevel sl)) return;

        // ゾンビロード召喚
        ZombieLordEntity lord = ModEntityTypes.ZOMBIE_LORD.get().create(sl);
        if (lord == null) return;

        lord.moveTo(
                zombie.getX(),
                zombie.getY(),
                zombie.getZ(),
                zombie.getYRot(),
                zombie.getXRot()
        );
        lord.finalizeSpawn(
                sl,
                sl.getCurrentDifficultyAt(zombie.blockPosition()),
                MobSpawnType.MOB_SUMMONED,
                null,
                null
        );
        lord.setTarget(player);

        sl.addFreshEntity(lord);

        player.displayClientMessage(
                Component.literal("§4§lゾンビロードが §c儀式 §fによって顕現した…！"),
                true
        );
    }
}