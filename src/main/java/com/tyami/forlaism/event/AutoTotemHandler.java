package com.tyami.forlaism.event;

import com.tyami.forlaism.world.PlayerClockData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * オートトーテム。
 *
 * クロック装備 + トグルONで、死亡時にトーテム効果を
 * トーテム消費なしで自動発動する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class AutoTotemHandler {

    private AutoTotemHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        // クロック装備チェック
        boolean hasClock = top.theillusivec4.curios.api.CuriosApi
                .getCuriosInventory(player)
                .map(h -> h.findFirstCurio(
                        s -> s.getItem() instanceof com.tyami.forlaism.item.MasterpieceClockItem
                ).isPresent())
                .orElse(false);

        if (!hasClock) return;

        // オートトーテムON？
        if (!PlayerClockData.isAutoTotem(player)) return;

        // 死亡キャンセル
        event.setCanceled(true);

        // トーテム効果
        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4)); // 弱い無敵: 耐性V 5秒

        ServerLevel sl = player.serverLevel();
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1.0, player.getZ(),
                100, 0.5, 1.0, 0.5, 0.3);
        sl.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40, 0.4, 0.8, 0.4, 0.1);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.5F, 1.0F);

    
    }
}