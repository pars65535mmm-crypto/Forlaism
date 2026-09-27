package com.tyami.forlaism.event;

import com.tyami.forlaism.entity.CreeperLordEntity;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 儀式の短剣でクリーパーを殺すと、クリーパーロードが確定召喚される。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class CreeperLordSacrificeHandler {

    private CreeperLordSacrificeHandler() {
    }

    @SubscribeEvent
    public static void onCreeperDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (creeper instanceof CreeperLordEntity) return;
        if (creeper.level().isClientSide) return;

        // 攻撃者チェック
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // メインハンドが儀式の短剣か
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.KNIFE_OF_SACRIFICE.get())) return;

        if (!(creeper.level() instanceof ServerLevel sl)) return;

        // クリーパーロード召喚
        CreeperLordEntity lord = ModEntityTypes.CREEPER_LORD.get().create(sl);
        if (lord == null) return;

        lord.moveTo(
                creeper.getX(),
                creeper.getY(),
                creeper.getZ(),
                creeper.getYRot(),
                creeper.getXRot()
        );
        lord.finalizeSpawn(
                sl,
                sl.getCurrentDifficultyAt(creeper.blockPosition()),
                MobSpawnType.MOB_SUMMONED,
                null,
                null
        );
        lord.setTarget(player);

        sl.addFreshEntity(lord);

        player.displayClientMessage(
                Component.literal("§4§lクリーパーロードが §c儀式 §fによって顕現した…！"),
                true
        );
    }
}