package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Re糖分の剣。
 *
 * - 攻撃速度 20
 * - 攻撃力 342
 * - 無敵時間無視
 * - 殴った敵の全アイテム耐久を10減らす
 * - 不壊アイテムは独自の擬似耐久値(初期100)で管理
 * - 擬似耐久値が10以下の状態で殴られると耐久全開＆ドロップ
 * - Curiosスロットも対象
 * - 使用者側に耐久スナップショットを保存し、増加を検知したら即破壊＆ドロップ
 * - 敵が一撃で消えた場合は周囲10ブロックのEntityへ防御貫通2048ダメージ
 */
public class SwrequimetBladeItem extends SwordItem implements IAnimatedTextItem {

    /** 一度の攻撃で減らす耐久値。 */
    public static final int DURABILITY_DAMAGE = 10;

    /** 擬似耐久値の初期値。 */
    public static final int PSEUDO_DURABILITY_MAX = 100;

    /** 擬似耐久値がこの値以下の状態で殴られると破壊される。 */
    public static final int PSEUDO_DURABILITY_BREAK_THRESHOLD = 10;

    /** 一撃で敵が消えた場合の爆散ダメージ。 */
    public static final float AOE_DAMAGE = 2048.0F;

    /** 一撃で敵が消えた場合の爆散範囲。 */
    public static final double AOE_RADIUS = 10.0D;

    /** 擬似耐久値のNBTキー。 */
    private static final String TAG_PSEUDO_DURABILITY = "ReSugarPseudoDurability";

    /** この剣の攻撃力（Mixinから参照される）。 */
    public static final float ATTACK_DAMAGE = 342.0F;

    /**
     * 使用者UUIDごとの「前回の耐久スナップショット」。
     *
     * キー: 使用者UUID
     * 値: (ItemStack識別子 → 耐久値)
     */
    private static final Map<UUID, Map<String, Integer>> SNAPSHOTS = new HashMap<>();

    public SwrequimetBladeItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                342 - 1, // SwordItemは+1されるので、合計342になるように
                -2.4F,   // 攻撃速度はAttributeModifierで上書き
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("Re糖分の剣")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFFFFC0CB, 0xFFFFFFFF, 0xFFFF69B4)
                .gradientSpeed(0.6F)
                .gradientPhase(1.0F);
    }

    // =========================================================
    // 攻撃
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        // 1. 無敵時間を無視して確実にダメージ
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 2. 敵の全装備品の耐久を削る
        drainTargetEquipment(target);

        // 3. 使用者側のスナップショットを確認（増加検知）
        if (attacker instanceof Player player) {
            checkAndDestroyRestoredItems(player);
        }

        // 4. 一撃で敵が消えたかを確認
        if (!target.isAlive() || target.isDeadOrDying()) {
            detonateAround(attacker);
        }

        return true;
    }

    // =========================================================
    // 敵の装備品の耐久を削る
    // =========================================================

    private void drainTargetEquipment(LivingEntity target) {

        // 全EquipmentSlot
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipment = target.getItemBySlot(slot);
            if (!equipment.isEmpty()) {
                drainItemDurability(equipment, target);
            }
        }

        // メインハンド/オフハンドはgetItemBySlotで取れるが念のため
        drainItemDurability(target.getMainHandItem(), target);
        drainItemDurability(target.getOffhandItem(), target);

        // Curiosスロット
        drainCuriosItems(target);
    }

    /**
     * Curiosスロットのアイテムを取得して耐久を削る。
     */
    private void drainCuriosItems(LivingEntity target) {
        try {
            top.theillusivec4.curios.api.CuriosApi
                    .getCuriosInventory(target)
                    .ifPresent(handler -> {
                        handler.getCurios().forEach((slotType, slotHandler) -> {
                            var stacks = slotHandler.getStacks();
                            for (int i = 0; i < stacks.getSlots(); i++) {
                                ItemStack curiosStack = stacks.getStackInSlot(i);
                                if (!curiosStack.isEmpty()) {
                                    drainItemDurability(curiosStack, target);
                                }
                            }
                        });
                    });
        } catch (Throwable ignored) {
            // Curios未導入環境では無視
        }
    }

    /**
     * 1つのItemStackの耐久を削る。
     */
    private void drainItemDurability(ItemStack stack, LivingEntity owner) {

        if (stack.isEmpty()) return;

        // ---- 壊れないアイテム（不壊 or 耐久なし） ----
        if (!stack.isDamageableItem() || stack.getItem().canBeDepleted() == false) {
            handlePseudoDurability(stack, owner);
            return;
        }

        // ---- 通常の耐久値 ----
        int currentDamage = stack.getDamageValue();
        int maxDamage = stack.getMaxDamage();
        int remaining = maxDamage - currentDamage;

        // 耐久が10以下なら問答無用で全開＆ドロップ
        if (remaining <= PSEUDO_DURABILITY_BREAK_THRESHOLD) {
            breakAndDrop(stack, owner);
            return;
        }

        // 10減らす
        int newDamage = Math.min(maxDamage, currentDamage + DURABILITY_DAMAGE);
        stack.setDamageValue(newDamage);

        // 壊れた場合はドロップ
        if (newDamage >= maxDamage) {
            breakAndDrop(stack, owner);
        }
    }

    /**
     * 擬似耐久値の処理（不壊アイテム用）。
     */
    private void handlePseudoDurability(ItemStack stack, LivingEntity owner) {

        CompoundTag tag = stack.getOrCreateTag();

        // 初期化
        if (!tag.contains(TAG_PSEUDO_DURABILITY)) {
            tag.putInt(TAG_PSEUDO_DURABILITY, PSEUDO_DURABILITY_MAX);
        }

        int pseudo = tag.getInt(TAG_PSEUDO_DURABILITY);

        // 擬似耐久値が10以下なら破壊＆ドロップ
        if (pseudo <= PSEUDO_DURABILITY_BREAK_THRESHOLD) {
            breakAndDrop(stack, owner);
            return;
        }

        // 10減らす
        tag.putInt(TAG_PSEUDO_DURABILITY, Math.max(0, pseudo - DURABILITY_DAMAGE));
    }

    /**
     * アイテムを破壊してドロップさせる。
     */
    private void breakAndDrop(ItemStack stack, LivingEntity owner) {

        if (stack.isEmpty()) return;

        Level level = owner.level();
        if (level.isClientSide) return;

        ItemStack drop = stack.copy();
        drop.setCount(1);

        // 装備スロットを空にする
        stack.setCount(0);

        // ドロップ
        if (level instanceof ServerLevel serverLevel) {
            Vec3 pos = owner.position().add(0, owner.getBbHeight() * 0.5, 0);
            ItemEntity itemEntity = new ItemEntity(
                    serverLevel,
                    pos.x, pos.y, pos.z,
                    drop
            );
            itemEntity.setNoPickUpDelay();
            serverLevel.addFreshEntity(itemEntity);
        }
    }

    // =========================================================
    // 使用者側のスナップショット確認
    // =========================================================

    /**
     * 使用者の全アイテムの耐久をスナップショットし、
     * 前回より「増加」していたら即破壊＆ドロップする。
     */
    private void checkAndDestroyRestoredItems(Player player) {

        Map<String, Integer> previous = SNAPSHOTS.computeIfAbsent(
                player.getUUID(),
                k -> new HashMap<>()
        );

        Map<String, Integer> current = new HashMap<>();
        Map<String, ItemStack> stackMap = new HashMap<>();

        // 全アイテムを収集
        collectPlayerItems(player, current, stackMap);

        // 前回と比較
        for (Map.Entry<String, Integer> entry : previous.entrySet()) {

            String key = entry.getKey();
            int prevValue = entry.getValue();

            if (!current.containsKey(key)) continue;

            int nowValue = current.get(key);

            // 耐久が「増加」していたら破壊
            if (nowValue > prevValue) {
                ItemStack stack = stackMap.get(key);
                if (stack != null && !stack.isEmpty()) {
                    breakAndDrop(stack, player);
                }
            }
        }

        // スナップショット更新
        SNAPSHOTS.put(player.getUUID(), current);
    }

    /**
     * プレイヤーの全アイテムの耐久値をマップに収集。
     */
    private void collectPlayerItems(
            Player player,
            Map<String, Integer> out,
            Map<String, ItemStack> stackMap
    ) {
        // インベントリ
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            recordStack(stack, "inv_" + i, out, stackMap);
        }

        // 防具
        int armorIndex = 0;
        for (ItemStack stack : player.getInventory().armor) {
            recordStack(stack, "armor_" + armorIndex, out, stackMap);
            armorIndex++;
        }

        // オフハンド
        recordStack(player.getOffhandItem(), "offhand", out, stackMap);

        // Curiosスロット
        try {
            top.theillusivec4.curios.api.CuriosApi
                    .getCuriosInventory(player)
                    .ifPresent(handler -> {
                        handler.getCurios().forEach((slotType, slotHandler) -> {
                            var stacks = slotHandler.getStacks();
                            for (int i = 0; i < stacks.getSlots(); i++) {
                                recordStack(
                                        stacks.getStackInSlot(i),
                                        "curio_" + slotType + "_" + i,
                                        out,
                                        stackMap
                                );
                            }
                        });
                    });
        } catch (Throwable ignored) {
        }
    }

    private void recordStack(
            ItemStack stack,
            String key,
            Map<String, Integer> out,
            Map<String, ItemStack> stackMap
    ) {
        if (stack.isEmpty()) return;

        int durability;

        if (stack.isDamageableItem() && stack.getItem().canBeDepleted()) {
            durability = stack.getMaxDamage() - stack.getDamageValue();
        } else {
            // 不壊アイテムは擬似耐久値
            CompoundTag tag = stack.getTag();
            durability = (tag != null && tag.contains(TAG_PSEUDO_DURABILITY))
                    ? tag.getInt(TAG_PSEUDO_DURABILITY)
                    : PSEUDO_DURABILITY_MAX;
        }

        out.put(key, durability);
        stackMap.put(key, stack);
    }

    // =========================================================
    // 周囲爆散
    // =========================================================

    private void detonateAround(LivingEntity attacker) {

        if (attacker == null) return;
        Level level = attacker.level();
        if (level.isClientSide) return;

        AABB area = attacker.getBoundingBox().inflate(AOE_RADIUS);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive()
                        && e != attacker
                        && !e.isSpectator()
        );

        for (LivingEntity target : targets) {
            // 防御貫通ダメージ
            target.invulnerableTime = 0;
            target.hurt(
                    attacker.damageSources().fellOutOfWorld(),
                    AOE_DAMAGE
            );
        }
    }

    // =========================================================
    // ツールチップ
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§d攻撃力 342 / 攻撃速度 20").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§c無敵時間無視").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§6相手の全装備の耐久を §e10 §6削る").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7不壊アイテムは §b擬似耐久値 §7で管理").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§4耐久が蘇った瞬間、それは砕け散る").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false; // 剣自体は壊れない
    }

    // =========================================================
    // 攻撃属性（攻撃速度20）
    // =========================================================

    private static final UUID ATTACK_SPEED_UUID =
            UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f23456789012");

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier>
    getDefaultAttributeModifiers(EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);

        if (slot != EquipmentSlot.MAINHAND) {
            return original;
        }

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier>builder();

        original.forEach(builder::put);

        // 攻撃速度 20
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "ReSugar blade attack speed",
                        20.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // 一撃で死んだ場合のフック（攻撃側で処理）
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return super.onLeftClickEntity(stack, player, entity);
    }
}