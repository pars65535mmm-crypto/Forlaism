package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * カッカオカッカオ。
 *
 * - ダメージ / 攻撃速度 / 採掘速度 すべて Integer.MAX_VALUE (21億)
 * - なんでも掘れる (岩盤含む)
 * - 右クリックで視線方向に吹っ飛ぶ
 * - 持ってる間は落下ダメージ無効
 * - 殴った敵のデータをランダムにいじる（データ汚染）
 * - 掘ったブロックに幸運20相当のドロップ
 */
public class CacacaItem extends PickaxeItem implements IAnimatedTextItem {

    /** 21億。 */
    public static final int MAX_INT = Integer.MAX_VALUE;

    private static final Random RANDOM = new Random();

    /**
     * 攻撃力/攻撃速度Modifier用UUID。
     */
    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("cacacaca-caca-caca-caca-cacacacacaca");
    private static final UUID ATTACK_SPEED_UUID =
            UUID.fromString("cacacaca-caca-caca-caca-cacacacacacb");

    public CacacaItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                MAX_INT,
                -3.0F,
                properties
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    // =========================================================
    // 表示名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("カッカオカッカオ")
                .wave(3.0F, 0.5F, 0.5F)
                .gradient(0xFF8B4513, 0xFFFFAA00, 0xFFFFFFFF, 0xFF00FFFF)
                .gradientSpeed(0.8F)
                .gradientPhase(1.0F);
    }

    // =========================================================
    // 属性
    // =========================================================

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);

        if (slot != EquipmentSlot.MAINHAND) {
            return original;
        }

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        AttributeModifier>builder();

        original.forEach(builder::put);

        // 攻撃力 21億
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_UUID,
                        "cacaca_attack_damage",
                        MAX_INT,
                        AttributeModifier.Operation.ADDITION
                )
        );

        // 攻撃速度 21億
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "cacaca_attack_speed",
                        MAX_INT,
                        AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // =========================================================
    // 採掘速度 / 掘れる判定
    // =========================================================

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return MAX_INT;
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        // 岩盤でもなんでも掘れる
        return true;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // =========================================================
    // 右クリック: 視線方向に吹っ飛ぶ
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {

            // クールダウン (10 tick = 0.5秒)
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.fail(stack);
            }

            // 視線方向を取得
            var look = player.getLookAngle();

            // 吹っ飛びパワー
            double power = 3.5;
            double upBoost = 0.8;

            // 速度を上書き
            player.setDeltaMovement(
                    look.x * power,
                    look.y * power + upBoost,
                    look.z * power
            );

            // クライアントへ速度反映
            player.hurtMarked = true;

            // 落下ダメージ無効
            player.fallDistance = 0.0F;

            // 演出
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        player.getX(),
                        player.getY() + 0.5,
                        player.getZ(),
                        30,
                        0.5, 0.5, 0.5,
                        0.3
                );
                serverLevel.sendParticles(
                        ParticleTypes.CLOUD,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        15,
                        0.3, 0.1, 0.3,
                        0.1
                );
            }

            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.5F
            );

            // クールダウン
            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // =========================================================
    // ブロック破壊: 幸運20相当 + なんでも
    // =========================================================

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {

        if (player.level().isClientSide) {
            return super.onBlockStartBreak(itemstack, pos, player);
        }

        Level level = player.level();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return super.onBlockStartBreak(itemstack, pos, player);
        }

        // BlockEntityも取得
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // 通常ドロップを取得
        List<ItemStack> drops = Block.getDrops(
                state,
                (ServerLevel) level,
                pos,
                blockEntity,
                player,
                itemstack
        );

        // ドロップ無し & 岩盤等でもアイテム化
        if (drops.isEmpty()) {
            ItemStack fallback = new ItemStack(state.getBlock().asItem());
            if (!fallback.isEmpty()) {
                drops = List.of(fallback);
            }
        }

        // ブロック消去
        level.removeBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        // 幸運20相当: ドロップ数を 1 + random(0..20) 倍に膨らませる
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            int multiplier = 1 + RANDOM.nextInt(21); // 1〜21倍
            ItemStack boosted = drop.copy();
            boosted.setCount(Math.min(drop.getCount() * multiplier, 64 * 64));

            // プレイヤーに直接回収
            if (!player.getInventory().add(boosted)) {
                ItemEntity ie = new ItemEntity(
                        level,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        boosted
                );
                ie.setNoPickUpDelay();
                level.addFreshEntity(ie);
            }
        }

        return true;
    }

    // =========================================================
    // 攻撃: データ汚染
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        // 無敵時間解除
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 21億ダメージ (直接HP減算で防御貫通)
        if (!target.level().isClientSide) {
            float current = target.getHealth();
            float damage = MAX_INT;

            if (current - damage <= 0.0F) {
                target.setHealth(0.0F);
                if (!target.isDeadOrDying()) {
                    target.die(target.damageSources().playerAttack(
                            attacker instanceof Player p ? p : null
                    ));
                }
            } else {
                target.setHealth(current - damage);
            }

            // データ汚染
            corruptTargetData(target);
        }

        return true;
    }

    /**
     * 敵のデータをランダムにいじる（データ汚染）。
     * 派手演出付き。
     */
    private static void corruptTargetData(LivingEntity target) {

        CompoundTag data = target.getPersistentData();
        Level level = target.level();

        // =====================================================
        // 派手演出: 汚染発生！
        // =====================================================
        if (level instanceof ServerLevel serverLevel) {

            // 渦巻くパーティクル
            serverLevel.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    80,
                    0.6, 0.8, 0.6,
                    0.2
            );

            serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    120,
                    1.0, 1.0, 1.0,
                    0.5
            );

            serverLevel.sendParticles(
                    ParticleTypes.WARPED_SPORE,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    40,
                    0.8, 0.8, 0.8,
                    0.05
            );

            // 不快な音
            serverLevel.playSound(
                    null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SCULK_SHRIEKER_SHRIEK,
                    SoundSource.HOSTILE,
                    1.5F,
                    0.3F
            );

            serverLevel.playSound(
                    null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.HOSTILE,
                    1.0F,
                    1.5F
            );
        }

        int choice = RANDOM.nextInt(10);

        String pollutionName;

        switch (choice) {
            case 0 -> {
                var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(RANDOM.nextDouble() * 5.0);
                }
                pollutionName = "MOVEMENT_SPEED";
            }
            case 1 -> {
                var hp = target.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    double v = 1.0 + RANDOM.nextDouble() * 500.0;
                    hp.setBaseValue(v);
                    if (target.getHealth() > v) target.setHealth((float) v);
                }
                pollutionName = "MAX_HEALTH";
            }
            case 2 -> {
                var kb = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kb != null) {
                    kb.setBaseValue(RANDOM.nextDouble());
                }
                pollutionName = "KNOCKBACK_RESISTANCE";
            }
            case 3 -> {
                float scale = 0.2F + RANDOM.nextFloat() * 5.0F;
                data.putFloat("CacacaScale", scale);
                pollutionName = "SCALE";
            }
            case 4 -> {
                if (target instanceof Mob mob) {
                    mob.setNoAi(RANDOM.nextBoolean());
                }
                pollutionName = "NO_AI";
            }
            case 5 -> {
                if (RANDOM.nextBoolean()) {
                    target.setSecondsOnFire(60);
                }
                pollutionName = "FIRE";
            }
            case 6 -> {
                // 反転フラグ
                data.putBoolean("CacacaUpsideDown", RANDOM.nextBoolean());
                pollutionName = "UPSIDE_DOWN";
            }
            case 7 -> {
                // ランダムテレポート
                double dx = (RANDOM.nextDouble() - 0.5) * 20.0;
                double dz = (RANDOM.nextDouble() - 0.5) * 20.0;
                target.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
                pollutionName = "TELEPORT";
            }
            case 8 -> {
                // グロウ効果をランダム
                target.setGlowingTag(!target.hasGlowingTag());
                pollutionName = "GLOWING";
            }
            case 9 -> {
                // 周囲に伝染！
                if (level instanceof ServerLevel serverLevel) {
                    var nearby = serverLevel.getEntitiesOfClass(
                            LivingEntity.class,
                            target.getBoundingBox().inflate(5.0),
                            e -> e != target
                    );
                    for (LivingEntity e : nearby) {
                        CompoundTag ed = e.getPersistentData();
                        ed.putInt("CacacaInfected", 1);
                        e.hurt(
                                e.damageSources().magic(),
                                5.0F
                        );
                    }
                }
                pollutionName = "INFECTION";
            }
            default -> pollutionName = "UNKNOWN";
        }

        // 汚染記録
        int count = data.getInt("CacacaPollutionCount") + 1;
        data.putInt("CacacaPollutionCount", count);
        data.putString("CacacaPollution", pollutionName);
    }

    // =========================================================
    // 落下ダメージ無効（持っている間）
    // =========================================================

    /**
     * プレイヤーがこのアイテムを持っている間、落下ダメージを無効化。
     *
     * PlayerTickEvent から呼ばれる。
     */
    public static void tickJumpReset(Player player) {

        boolean holding =
                player.getMainHandItem().getItem() instanceof CacacaItem
                        || player.getOffhandItem().getItem() instanceof CacacaItem;

        if (!holding) return;

        // 落下距離リセット
        player.fallDistance = 0.0F;
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§6§lカッカオカッカオ")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§cダメージ: §f21億")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§c攻撃速度: §f21億")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§c採掘速度: §f21億")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§d能力: §fデータ汚染")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7・殴った敵のデータをランダムにいじる")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・掘ったブロックに幸運20相当")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§b右クリックで視線方向に吹っ飛ぶ")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§8カカオの如く舞い踊れ。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}