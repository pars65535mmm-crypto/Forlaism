package com.tyami.forlaism.event;

import com.tyami.forlaism.entity.EndermanLordEntity;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 儀式の短剣でエンダーマンを殺すと、エンダーマンロードが確定召喚される。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class EndermanLordSacrificeHandler {

    private EndermanLordSacrificeHandler() {
    }

    @SubscribeEvent
    public static void onEndermanDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof EnderMan enderman)) return;
        if (enderman instanceof EndermanLordEntity) return;
        if (enderman.level().isClientSide) return;

        // 攻撃者チェック
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // メインハンドが儀式の短剣か
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.KNIFE_OF_SACRIFICE.get())) return;

        if (!(enderman.level() instanceof ServerLevel sl)) return;

        // エンダーマンロード召喚
        EndermanLordEntity lord = ModEntityTypes.ENDERMAN_LORD.get().create(sl);
        if (lord == null) return;

        lord.moveTo(
                enderman.getX(),
                enderman.getY(),
                enderman.getZ(),
                enderman.getYRot(),
                enderman.getXRot()
        );
        lord.finalizeSpawn(
                sl,
                sl.getCurrentDifficultyAt(enderman.blockPosition()),
                MobSpawnType.MOB_SUMMONED,
                null,
                null
        );
        lord.setTarget(player);

        sl.addFreshEntity(lord);

        player.displayClientMessage(
                Component.literal("§5§lエンダーマンロードが §d儀式 §fによって顕現した…！"),
                true
        );
    }
}