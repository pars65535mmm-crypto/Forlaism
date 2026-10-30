package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 木の矢の Entity。
 *
 * バニラの弓は createArrow で setBaseDamage しても
 * releaseUsing で上書きされるため、getBaseDamage() を
 * オーバーライドして常にダメージ 3 を返す。
 */
public class WoodArrowEntity extends Arrow {

    /** 木の矢のダメージ。 */
    public static final double BASE_DAMAGE = 3.0D;

    public WoodArrowEntity(EntityType<? extends WoodArrowEntity> type, Level level) {
        super(type, level);
    }

    public WoodArrowEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.WOOD_ARROW.get(), level);
        this.setOwner(shooter);
        this.setPos(
                shooter.getX(),
                shooter.getEyeY() - 0.1D,
                shooter.getZ()
        );
    }

    public WoodArrowEntity(Level level, double x, double y, double z) {
        super(ModEntityTypes.WOOD_ARROW.get(), level);
        this.setPos(x, y, z);
    }

    /**
     * ダメージを常に 3.0 に固定。
     *
     * バニラの弓は releaseUsing で setBaseDamage するが、
     * getBaseDamage が 3.0 を返す限り、ダメージ計算は 3.0 になる。
     */
    @Override
    public double getBaseDamage() {
        return BASE_DAMAGE;
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(Items.WOOD_ARROW.get());
    }
}