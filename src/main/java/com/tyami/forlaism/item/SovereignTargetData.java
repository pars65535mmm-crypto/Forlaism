package com.tyami.forlaism.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class SovereignTargetData {

    private static final String TARGET_UUID = "SovereignTargetUUID";
    private static final String TARGET_TYPE = "SovereignTargetType";

    private SovereignTargetData() {
    }

    /**
     * 攻撃した敵をItemStackのNBTへ保存。
     */
    public static void saveTarget(
        ItemStack stack,
        LivingEntity target
) {
    CompoundTag tag = stack.getOrCreateTag();

    tag.putUUID(
            TARGET_UUID,
            target.getUUID()
    );
}

    /**
     * 保存した敵を現在のLevelから検索。
     */
    public static LivingEntity getTarget(
            ItemStack stack,
            net.minecraft.world.level.Level level
    ) {

        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return null;
        }

        if (!tag.hasUUID(TARGET_UUID)) {
            return null;
        }

        UUID uuid = tag.getUUID(TARGET_UUID);

        Entity entity;

        if (level instanceof ServerLevel serverLevel) {
            entity = serverLevel.getEntity(uuid);
        } else {
            entity = level.getPlayerByUUID(uuid);
        }

        if (entity instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    /**
     * 保存対象を消す。
     */
    public static void clearTarget(ItemStack stack) {

        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(TARGET_UUID);
        tag.remove(TARGET_TYPE);
    }

    /**
     * 保存対象が存在するか。
     */
    public static boolean hasTarget(ItemStack stack) {

        CompoundTag tag = stack.getTag();

        return tag != null
                && tag.hasUUID(TARGET_UUID);
    }
}