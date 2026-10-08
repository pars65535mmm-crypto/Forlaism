package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.MeteorEntity;
import com.tyami.forlaism.registry.ModEntityTypes;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * ヤツハニサク。
 *
 * 高い草 + 銅ブロック + ガラス で作れる、使い捨ての桜剣。
 *
 * ・攻撃力 2 / 攻撃速度 2
 * ・Shift + 右クリックで3段コンボ発動
 *   ① 周囲に桜の木を6本生やす
 *   ② 範囲10マスのentityを土葬する
 *   ③ 空に隕石を召喚（1分後に落下）
 * ・使うと壊れる
 */
public class YatsuhanisakuItem extends SwordItem implements IAnimatedTextItem {

    /** 攻撃力。 */
    public static final float ATTACK_DAMAGE = 2.0F;

    /** 攻撃速度。 */
    public static final double ATTACK_SPEED = 2.0D;

    /** 桜の本数。 */
    private static final int SAKURA_COUNT = 6;

    /** 桜を生やす半径。 */
    private static final double SAKURA_RADIUS = 6.0D;

    /** 土葬範囲。 */
    private static final double BURIAL_RADIUS = 10.0D;

    /** 隕石の召喚高度（地表から+100）。 */
    private static final int METEOR_HEIGHT = 100;

    /** 隕石の落下地点のずれ（ランダム）。 */
    private static final double METEOR_OFFSET = 3.0D;

    private static final java.util.UUID ATK_UUID =
            java.util.UUID.fromString("a8a8a8a8-1111-2222-3333-444444444401");
    private static final java.util.UUID SPD_UUID =
            java.util.UUID.fromString("a8a8a8a8-1111-2222-3333-444444444402");

    public YatsuhanisakuItem(Properties properties) {
        super(
                Tiers.STONE,
                0,
                -2.4F,
                properties.durability(1)  // ← stacksTo(1) を削除
        );
    }

    // =========================================================
    // 属性
    // =========================================================

    @Override
    public com.google.common.collect.Multimap<
            net.minecraft.world.entity.ai.attributes.Attribute,
            net.minecraft.world.entity.ai.attributes.AttributeModifier>
    getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);
        if (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) return original;

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier>builder();

        original.forEach(builder::put);

        // 攻撃力 2
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ATK_UUID,
                        "yatsuhanisaku_attack",
                        ATTACK_DAMAGE - 1.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        // 攻撃速度 2.0
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        SPD_UUID,
                        "yatsuhanisaku_speed",
                        ATTACK_SPEED - 4.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // =========================================================
    // 表示名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ヤツハニサク")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFFFFB7C5, 0xFFFFFFFF, 0xFFFF69B4, 0xFFFFB7C5)
                .gradientSpeed(0.8F)
                .gradientPhase(0.6F);
    }

    // =========================================================
    // 右クリック（Shift で発動）
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift押してないなら何もしない
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.pass(stack);
        }

        // =========================================================
        // 3段コンボ発動！
        // =========================================================

        // ① 桜の木を6本生やす
        summonSakuraTrees(sl, player);

        // ② 範囲10マスのentityを土葬
        buryEntities(sl, player);

        // ③ 隕石を召喚
        summonMeteor(sl, player);

        // =========================================================
        // 演出
        // =========================================================
        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS,
                2.0F, 0.5F
        );

        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS,
                2.0F, 0.6F
        );

        player.displayClientMessage(
                Component.literal("§d§l八重に咲き、土に還り、天より裁く。"),
                true
        );

        // =========================================================
        // 剣は壊れる
        // =========================================================
        stack.hurtAndBreak(
                9999,
                player,
                p -> p.broadcastBreakEvent(hand)
        );

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // =========================================================
    // ① 桜の木を6本
    // =========================================================

    private void summonSakuraTrees(ServerLevel sl, Player player) {

        var configuredRegistry = sl.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE);

        Optional<ConfiguredFeature<?, ?>> treeFeature =
                configuredRegistry.getOptional(
                        new ResourceLocation("minecraft", "trees_cherry")
                );

        if (treeFeature.isEmpty()) {
            System.out.println("[Yatsuhanisaku] trees_cherry not found!");
            return;
        }

        RandomSource random = sl.getRandom();

        // 円形に6本配置
        for (int i = 0; i < SAKURA_COUNT; i++) {
            double angle = (i / (double) SAKURA_COUNT) * Math.PI * 2.0;

            double dx = Math.cos(angle) * SAKURA_RADIUS;
            double dz = Math.sin(angle) * SAKURA_RADIUS;

            BlockPos basePos = BlockPos.containing(
                    player.getX() + dx,
                    player.getY(),
                    player.getZ() + dz
            );

            // 地表の高さを取得
            BlockPos surfacePos = sl.getHeightmapPos(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    basePos
            );

            // 桜を生やす
            treeFeature.get().place(
                    sl,
                    sl.getChunkSource().getGenerator(),
                    random,
                    surfacePos
            );
        }

        System.out.println("[Yatsuhanisaku] " + SAKURA_COUNT + " cherry trees spawned.");
    }

    // =========================================================
    // ② 土葬
    // =========================================================

    private void buryEntities(ServerLevel sl, Player player) {

        Vec3 center = player.position();

        AABB area = new AABB(
                center.x - BURIAL_RADIUS, center.y - BURIAL_RADIUS, center.z - BURIAL_RADIUS,
                center.x + BURIAL_RADIUS, center.y + BURIAL_RADIUS, center.z + BURIAL_RADIUS
        );

        List<Entity> targets = sl.getEntities(
                player,
                area,
                e -> e instanceof LivingEntity
                        && e.isAlive()
                        && e != player
        );

        int buried = 0;

        for (Entity target : targets) {

            // =========================================================
            // 地面に埋める
            // =========================================================
            LivingEntity living = (LivingEntity) target;

            // 窒息ダメージで即死させる
            living.setAirSupply(-20);

            // 土ブロックで包む
            BlockPos targetPos = target.blockPosition();

            for (int dy = 0; dy < 3; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {

                        BlockPos pos = targetPos.offset(dx, dy, dz);

                        // 対象の本体位置はスキップ
                        if (dx == 0 && dz == 0 && dy == 1) continue;

                        // 既存のブロックが空気または置換可能なら土を置く
                        if (sl.getBlockState(pos).isAir()) {
                            sl.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                        }
                    }
                }
            }

            // 窒息ダメージを即座に与える（保険）
            living.hurt(
                    sl.damageSources().inWall(),
                    100.0F
            );

            buried++;
        }

        System.out.println("[Yatsuhanisaku] Buried " + buried + " entities.");
    }

    // =========================================================
    // ③ 隕石召喚
    // =========================================================

    private void summonMeteor(ServerLevel sl, Player player) {

        // =========================================================
        // 隕石の落下地点を計算
        // =========================================================
        double dx = (sl.getRandom().nextDouble() - 0.5) * METEOR_OFFSET * 2;
        double dz = (sl.getRandom().nextDouble() - 0.5) * METEOR_OFFSET * 2;

        BlockPos targetPos = BlockPos.containing(
                player.getX() + dx,
                player.getY(),
                player.getZ() + dz
        );

        // 地表を取得
        BlockPos surfacePos = sl.getHeightmapPos(
                Heightmap.Types.WORLD_SURFACE_WG,
                targetPos
        );

        // =========================================================
        // 隕石生成（上空 +100）
        // =========================================================
        MeteorEntity meteor = new MeteorEntity(
                sl,
                surfacePos.getX() + 0.5,
                surfacePos.getY() + METEOR_HEIGHT,
                surfacePos.getZ() + 0.5
        );

        sl.addFreshEntity(meteor);

        player.displayClientMessage(
                Component.literal("§c§l……空が、割れる。"),
                true
        );

        System.out.println("[Yatsuhanisaku] Meteor summoned at Y="
                + (surfacePos.getY() + METEOR_HEIGHT));
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
        tooltip.add(Component.literal("§d§l八重に咲き、土に還り、天より裁く。")
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}