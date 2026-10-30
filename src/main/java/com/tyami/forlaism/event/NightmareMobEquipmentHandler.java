package com.tyami.forlaism.event;

import com.tyami.forlaism.world.NightmareHelper;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * ナイトメアモード中の敵装備強化。
 *
 * 挙動:
 *   1. スポーン時にヘルメットを必ず装備
 *   2. ランダムで他の部位も装備
 *   3. エンチャント済み
 *   4. プレイヤーを攻撃した時、防具を1部位奪う
 *   5. 死亡した敵の装備は他の敵が拾う（バニラ挙動）
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NightmareMobEquipmentHandler {

    private static final Random RANDOM = new Random();

    /** 奪う確率（攻撃1回あたり）。 */
    private static final float STEAL_CHANCE = 0.05F;

    private NightmareMobEquipmentHandler() {
    }

    // =========================================================
    // 1. スポーン時にヘルメット強制装備
    // =========================================================

    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {

        if (event.getLevel().isClientSide) return;
        if (!NightmareHelper.isActive(event.getLevel())) return;

        if (!(event.getEntity() instanceof Monster monster)) return;

        // 既にヘルメット装備してたらスキップ（村人ゾンビ対策）
        if (!monster.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;

        // ヘルメットを付与
        ItemStack helmet = createRandomHelmet();
        monster.setItemSlot(EquipmentSlot.HEAD, helmet);
        monster.setDropChance(EquipmentSlot.HEAD, 0.0F);

        // 30%で他の部位も
        if (RANDOM.nextFloat() < 0.30F) {
            monster.setItemSlot(EquipmentSlot.CHEST, createRandomChestplate());
            monster.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }
        if (RANDOM.nextFloat() < 0.20F) {
            monster.setItemSlot(EquipmentSlot.LEGS, createRandomLeggings());
            monster.setDropChance(EquipmentSlot.LEGS, 0.0F);
        }
        if (RANDOM.nextFloat() < 0.20F) {
            monster.setItemSlot(EquipmentSlot.FEET, createRandomBoots());
            monster.setDropChance(EquipmentSlot.FEET, 0.0F);
        }
    }

    // =========================================================
    // 2. 装備生成
    // =========================================================

    private static ItemStack createRandomHelmet() {
        ItemStack[] candidates = {
                new ItemStack(Items.LEATHER_HELMET),
                new ItemStack(Items.CHAINMAIL_HELMET),
                new ItemStack(Items.IRON_HELMET),
                new ItemStack(Items.GOLDEN_HELMET),
                new ItemStack(Items.DIAMOND_HELMET)
        };
        ItemStack stack = candidates[RANDOM.nextInt(candidates.length)].copy();
        enchantRandomly(stack);
        return stack;
    }

    private static ItemStack createRandomChestplate() {
        ItemStack[] candidates = {
                new ItemStack(Items.LEATHER_CHESTPLATE),
                new ItemStack(Items.CHAINMAIL_CHESTPLATE),
                new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.DIAMOND_CHESTPLATE)
        };
        ItemStack stack = candidates[RANDOM.nextInt(candidates.length)].copy();
        enchantRandomly(stack);
        return stack;
    }

    private static ItemStack createRandomLeggings() {
        ItemStack[] candidates = {
                new ItemStack(Items.LEATHER_LEGGINGS),
                new ItemStack(Items.CHAINMAIL_LEGGINGS),
                new ItemStack(Items.IRON_LEGGINGS),
                new ItemStack(Items.DIAMOND_LEGGINGS)
        };
        ItemStack stack = candidates[RANDOM.nextInt(candidates.length)].copy();
        enchantRandomly(stack);
        return stack;
    }

    private static ItemStack createRandomBoots() {
        ItemStack[] candidates = {
                new ItemStack(Items.LEATHER_BOOTS),
                new ItemStack(Items.CHAINMAIL_BOOTS),
                new ItemStack(Items.IRON_BOOTS),
                new ItemStack(Items.DIAMOND_BOOTS)
        };
        ItemStack stack = candidates[RANDOM.nextInt(candidates.length)].copy();
        enchantRandomly(stack);
        return stack;
    }

    private static void enchantRandomly(ItemStack stack) {
        // 保護系エンチャントをランダムLvで
        int protectLevel = 1 + RANDOM.nextInt(4);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, protectLevel);

        if (RANDOM.nextFloat() < 0.3F) {
            stack.enchant(Enchantments.UNBREAKING, 1 + RANDOM.nextInt(3));
        }

        if (RANDOM.nextFloat() < 0.2F) {
            stack.enchant(Enchantments.THORNS, 1 + RANDOM.nextInt(3));
        }
    }

    // =========================================================
    // 3. プレイヤーから装備を奪う
    // =========================================================

    @SubscribeEvent
    public static void onMobAttackPlayer(LivingEvent.LivingTickEvent event) {

        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide) return;
        if (!NightmareHelper.isActive(mob.level())) return;

        if (!(mob.getTarget() instanceof Player player)) return;

        // 攻撃判定: プレイヤーに接触してダメージを与えた瞬間
        if (player.hurtTime != 9) return;

        // 5% で発動
        if (RANDOM.nextFloat() >= STEAL_CHANCE) return;

        stealArmor(mob, player);
    }

    /**
     * プレイヤーの防具を1部位ランダムに奪う。
     */
    private static void stealArmor(Mob mob, Player player) {

        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        // シャッフル
        for (int i = slots.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            EquipmentSlot tmp = slots[i];
            slots[i] = slots[j];
            slots[j] = tmp;
        }

        for (EquipmentSlot slot : slots) {
            ItemStack playerStack = player.getItemBySlot(slot);
            if (playerStack.isEmpty()) continue;

            // 既に同じ部位に装備していたらスキップ
            if (!mob.getItemBySlot(slot).isEmpty()) continue;

            // 奪う
            ItemStack stolen = playerStack.copy();
            player.setItemSlot(slot, ItemStack.EMPTY);

            mob.setItemSlot(slot, stolen);
            mob.setDropChance(slot, 0.5F); // 倒せば奪い返せる

            mob.level().playSound(
                    null,
                    mob.getX(), mob.getY(), mob.getZ(),
                    net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    1.0F, 0.5F
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§4§l装備を奪われた… §7(" + slot.getName() + ")"
                    ),
                    true
            );

            return;
        }
    }
}