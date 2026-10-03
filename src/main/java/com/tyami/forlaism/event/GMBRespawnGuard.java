package com.tyami.forlaism.event;

import com.tyami.forlaism.annihilation.GMBAnnihilation;
import com.tyami.forlaism.annihilation.GMBEraseRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** UUIDを変えて復活する未知のEntity/Manager型に対する汎用封印。 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class GMBRespawnGuard {
    private GMBRespawnGuard() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        if (GMBEraseRegistry.isSuppressedReplacement(level, entity, level.getGameTime())) {
            event.setCanceled(true);
            GMBAnnihilation.forceRemove(level, entity);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        Entity source = event.getSource().getEntity();
        if (source == null || !(source.level() instanceof ServerLevel level)) return;
        if (GMBEraseRegistry.isSuppressedReplacement(level, source, level.getGameTime())) {
            event.setCanceled(true);
            return;
        }
        if (source instanceof net.minecraft.world.entity.projectile.Projectile projectile
                && projectile.getOwner() != null
                && GMBEraseRegistry.isSuppressedReplacement(level, projectile.getOwner(), level.getGameTime())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (GMBEraseRegistry.isSuppressedReplacement(level, entity, level.getGameTime())) {
                    GMBAnnihilation.erase(level, entity, null, false);
                }
            }
        }
    }
}
