package com.tyami.forlaism.item;

import com.tyami.forlaism.world.SavedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 保存したEntityを復活させるヘルパー。
 */
public final class SavedEntityRestorer {

    private SavedEntityRestorer() {
    }

    /**
     * 視線先のEntityを保存する。
     */
    public static boolean saveLookedAtEntity(ServerPlayer player) {
        Entity target = findLookedAtEntity(player, 64.0D);
        if (target == null) {
            return false;
        }

        // 保存NBT（UUIDは含めない）
        CompoundTag nbt = new CompoundTag();
        target.saveWithoutId(nbt);

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (typeId == null) return false;

        SavedEntityData.Entry entry = new SavedEntityData.Entry(
                java.util.UUID.randomUUID(),
                target.getName().getString(),
                typeId.toString(),
                nbt,
                System.currentTimeMillis()
        );

        SavedEntityData.get(player.server).add(entry);

        // 演出
        ServerLevel sl = player.serverLevel();
        sl.sendParticles(
                ParticleTypes.END_ROD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                30, 0.5, 0.5, 0.5, 0.1
        );
        sl.sendParticles(
                ParticleTypes.ENCHANT,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.6, 0.6, 0.6, 0.05
        );
        sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENDER_CHEST_CLOSE, SoundSource.PLAYERS, 1.0F, 1.5F);

        return true;
    }

    /**
     * 保存済みEntityを復活させる。
     */
    public static boolean restore(ServerPlayer player, java.util.UUID entryId) {
        SavedEntityData data = SavedEntityData.get(player.server);
        SavedEntityData.Entry entry = data.get(entryId);

        if (entry == null) return false;

        ResourceLocation typeId = ResourceLocation.tryParse(entry.entityTypeId);
        if (typeId == null) return false;

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(typeId);
        if (type == null) return false;

        ServerLevel sl = player.serverLevel();

        // プレイヤーの視線先5mに出現
        Vec3 look = player.getLookAngle().normalize();
        Vec3 spawnPos = player.position().add(look.scale(5.0D));

        Entity entity = type.create(sl);
        if (entity == null) return false;

        entity.load(entry.entityNbt.copy());
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.setUUID(java.util.UUID.randomUUID());
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;

        // 残り体力チェック（死んでたら最大の半分で復活）
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            if (living.getHealth() <= 0.0F) {
                living.setHealth(living.getMaxHealth());
            }
        }

        sl.addFreshEntity(entity);

        // 演出
        sl.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                spawnPos.x, spawnPos.y + 1.0, spawnPos.z,
                60, 0.5, 1.0, 0.5, 0.2
        );
        sl.sendParticles(
                ParticleTypes.FLASH,
                spawnPos.x, spawnPos.y + 1.0, spawnPos.z,
                1, 0, 0, 0, 0
        );
        sl.sendParticles(
                ParticleTypes.END_ROD,
                spawnPos.x, spawnPos.y + 1.0, spawnPos.z,
                40, 0.8, 1.2, 0.8, 0.1
        );
        sl.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 0.7F);
        sl.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 1.5F);

        return true;
    }

    /**
     * 視線先のEntityを取得。
     */
    private static Entity findLookedAtEntity(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eye,
                end,
                player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D),
                e -> e instanceof net.minecraft.world.entity.LivingEntity && e != player
        );

        return hit != null ? hit.getEntity() : null;
    }
}