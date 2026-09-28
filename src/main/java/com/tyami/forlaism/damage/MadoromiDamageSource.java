package com.tyami.forlaism.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 微睡ダメージ。
 *
 * - 光輪（Halo of the Advent / Firmament）の防御を正面から突破する
 * - 光輪のクラスは一切参照しない
 * - 純粋に hurt() を呼ぶだけで光輪装備者を殺せる
 *
 * 実処理は LivingEntityMadoromiMixin 側で行う。
 */
public final class MadoromiDamageSource {

    public static final ResourceKey<DamageType> MADOROMI =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "madoromi")
            );

    private MadoromiDamageSource() {
    }

    public static DamageSource of(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(MADOROMI),
                attacker
        );
    }

    /**
     * 現在のスレッドで微睡ダメージの「致死処理中」かどうか。
     *
     * Mixin 側で hurt を突破した後、
     * setHealth / die の防御も同様に突破するために使う。
     */
    private static final ThreadLocal<Boolean> EXECUTING =
            ThreadLocal.withInitial(() -> false);

    public static boolean isExecuting() {
        return EXECUTING.get();
    }

    public static void beginExecution() {
        EXECUTING.set(true);
    }

    public static void endExecution() {
        EXECUTING.set(false);
    }
}