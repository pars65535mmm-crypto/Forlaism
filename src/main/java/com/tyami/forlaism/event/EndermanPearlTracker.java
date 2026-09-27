package com.tyami.forlaism.event;

import com.tyami.forlaism.entity.EndermanLordEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * プレイヤーがエンダーパールを使った瞬間、
 * 周囲64マス以内のエンダーマンロードを激怒させる。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class EndermanPearlTracker {

    private static final double WRATH_RADIUS = 64.0D;

    private EndermanPearlTracker() {
    }

    @SubscribeEvent
    public static void onPearlUse(PlayerInteractEvent.RightClickItem event) {

        Player player = event.getEntity();

        if (player.level().isClientSide) return;

        // エンダーパール以外は無視
        if (!event.getItemStack().is(Items.ENDER_PEARL)) return;

        // 周囲のエンダーマンロードを激怒
        List<EndermanLordEntity> lords = player.level().getEntitiesOfClass(
                EndermanLordEntity.class,
                new AABB(player.blockPosition()).inflate(WRATH_RADIUS),
                EndermanLordEntity::isAlive
        );

        for (EndermanLordEntity lord : lords) {
            lord.triggerWrath(player);
        }
    }
}