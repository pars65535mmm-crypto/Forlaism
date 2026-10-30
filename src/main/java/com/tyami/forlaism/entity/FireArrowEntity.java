package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 火の矢の Entity。
 *
 * - ダメージ 5（getBaseDamage で固定）
 * - 着弾時に相手を燃やす
 * - 飛翔中に炎パーティクル
 */
public class FireArrowEntity extends Arrow {

    /** 火の矢のダメージ。 */
    public static final double BASE_DAMAGE = 5.0D;

    /** 着弾時の燃焼秒数。 */
    public static final int FIRE_SECONDS = 8;

    public FireArrowEntity(EntityType<? extends FireArrowEntity> type, Level level) {
        super(type, level);
    }

    public FireArrowEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.FIRE_ARROW.get(), level);
        this.setOwner(shooter);
        this.setPos(
                shooter.getX(),
                shooter.getEyeY() - 0.1D,
                shooter.getZ()
        );
    }

    public FireArrowEntity(Level level, double x, double y, double z) {
        super(ModEntityTypes.FIRE_ARROW.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    public double getBaseDamage() {
        return BASE_DAMAGE;
    }

    @Override
    public void tick() {
        super.tick();

        // 飛翔中の炎パーティクル
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.05, 0.05, 0.05, 0.0
            );
            if (this.tickCount % 3 == 0) {
                sl.sendParticles(
                        ParticleTypes.SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.05, 0.05, 0.05, 0.0
                );
            }
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);

        // 相手を燃やす
        target.setSecondsOnFire(FIRE_SECONDS);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(Items.FIRE_ARROW.get());
    }
}