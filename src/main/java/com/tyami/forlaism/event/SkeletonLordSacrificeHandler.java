package com.tyami.forlaism.event;

import com.tyami.forlaism.entity.SkeletonLordEntity;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 儀式の短剣でスケルトンを殺すと、スケルトンロードが確定召喚される。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class SkeletonLordSacrificeHandler {

    private SkeletonLordSacrificeHandler() {
    }

    @SubscribeEvent
    public static void onSkeletonDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof Skeleton skeleton)) return;
        if (skeleton instanceof SkeletonLordEntity) return;
        if (skeleton.level().isClientSide) return;

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.KNIFE_OF_SACRIFICE.get())) return;

        if (!(skeleton.level() instanceof ServerLevel sl)) return;

        SkeletonLordEntity lord = ModEntityTypes.SKELETON_LORD.get().create(sl);
        if (lord == null) return;

        lord.moveTo(
                skeleton.getX(),
                skeleton.getY(),
                skeleton.getZ(),
                skeleton.getYRot(),
                skeleton.getXRot()
        );
        lord.finalizeSpawn(
                sl,
                sl.getCurrentDifficultyAt(skeleton.blockPosition()),
                MobSpawnType.MOB_SUMMONED,
                null,
                null
        );
        lord.setTarget(player);

        sl.addFreshEntity(lord);

        player.displayClientMessage(
                Component.literal("§b§lスケルトンロードが §3儀式 §fによって顕現した…！"),
                true
        );
    }
}