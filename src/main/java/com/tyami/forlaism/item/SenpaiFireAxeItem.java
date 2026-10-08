package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 先輩の消防斧。
 *
 * - 攻撃力 29 / 攻撃速度 1.7
 * - 殴った敵を 燃やし / 爆裂させ / 吹っ飛ばす
 * - 鉄ドア含む全てのドア系を一撃破壊
 * - 右クリック: 前方のドア破壊 + 敵を吹っ飛ばす
 */
public class SenpaiFireAxeItem extends AxeItem implements IAnimatedTextItem {

    /** 攻撃力 29。 */
    public static final float ATTACK_DAMAGE = 29.0F;

    /** 攻撃速度 1.7。 */
    public static final double ATTACK_SPEED = 1.7D;

    /** 右クリックのクールダウン (tick)。1秒。 */
    public static final int RIGHT_CLICK_COOLDOWN = 20;

    /** 右クリックの射程（前方何ブロックまで判定するか）。 */
    public static final double RIGHT_CLICK_RANGE = 5.0D;

    /** 右クリックの吹っ飛ばし範囲。 */
    public static final double RIGHT_CLICK_RADIUS = 4.0D;

    /** 燃焼時間（秒）。 */
    private static final int FIRE_SECONDS = 8;

    /** 打撃時の小爆発の威力。 */
    private static final float HIT_EXPLOSION_POWER = 1.5F;

    public SenpaiFireAxeItem(Properties properties) {
        // Tiers.NETHERITE, 攻撃力補正, 攻撃速度補正
        // AxeItem の攻撃力計算: base + attackDamageBonus
        // 29 にしたいので 29 - 1 - 6(netherite) = 22 … じゃなくて、
        // AxeItem(NETITE) のベースは 6.0 なので 29 にするには +23
        super(
                Tiers.NETHERITE,
                29.0F - 1.0F - Tiers.NETHERITE.getAttackDamageBonus() + 1.0F,
                1.7F, // 4.0 - 1.7 = 2.3
                properties
        );
    }

    // =========================================================
    // 通常攻撃: 燃やす + 爆裂 + 吹っ飛ばす
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (target.level().isClientSide) {
            return super.hurtEnemy(stack, target, attacker);
        }

        ServerLevel sl = (ServerLevel) target.level();

        // ---- 1. 燃やす ----
        target.setSecondsOnFire(FIRE_SECONDS);

        // ---- 2. 爆裂させる（小さな爆発） ----
        sl.explode(
                attacker,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                HIT_EXPLOSION_POWER,
                Level.ExplosionInteraction.NONE
        );

        // ---- 3. 吹っ飛ばす（ノックバック） ----
        Vec3 away = target.position().subtract(attacker.position()).normalize();
        target.knockback(
                2.5D,
                -away.x,
                -away.z
        );
        target.setDeltaMovement(
                target.getDeltaMovement().add(0, 0.5D, 0)
        );
        target.hurtMarked = true;

        // ---- 演出 ----
        sl.sendParticles(
                ParticleTypes.FLAME,
                target.getX(), target.getY() + 1.0, target.getZ(),
                20, 0.4, 0.4, 0.4, 0.05
        );
        sl.sendParticles(
                ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 1.0, target.getZ(),
                3, 0.5, 0.5, 0.5, 0.0
        );
        sl.playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.0F, 1.2F
        );

        return super.hurtEnemy(stack, target, attacker);
    }

    // =========================================================
    // 右クリック: 前方のドア破壊 + 敵を吹っ飛ばす
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.pass(stack);
        }

        // クールダウン
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, RIGHT_CLICK_COOLDOWN);

        // =========================================================
        // 1. 前方のドアを破壊
        // =========================================================
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        boolean brokeAnyDoor = false;

        // 前方 RANGE ブロックまで、細かくレイチェック
        for (double d = 1.0; d <= RIGHT_CLICK_RANGE; d += 0.5D) {
            Vec3 p = eye.add(look.scale(d));
            BlockPos pos = BlockPos.containing(p);

            // ドアを破壊
            if (breakDoorAt(sl, pos, player)) {
                brokeAnyDoor = true;
            }

            // 上下1マスもチェック（ドアは2マス）
            if (breakDoorAt(sl, pos.above(), player)) {
                brokeAnyDoor = true;
            }
            if (breakDoorAt(sl, pos.below(), player)) {
                brokeAnyDoor = true;
            }
        }

        // =========================================================
        // 2. 前方の敵を吹っ飛ばす
        // =========================================================
        Vec3 center = eye.add(look.scale(RIGHT_CLICK_RANGE * 0.5D));
        AABB area = new AABB(
                center.x - RIGHT_CLICK_RADIUS, center.y - RIGHT_CLICK_RADIUS, center.z - RIGHT_CLICK_RADIUS,
                center.x + RIGHT_CLICK_RADIUS, center.y + RIGHT_CLICK_RADIUS, center.z + RIGHT_CLICK_RADIUS
        );

        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive()
                        && e != player
                        && !e.isSpectator()
        );

        for (LivingEntity target : targets) {
            // 視線の前方にいるか（前方180度くらい）
            Vec3 toTarget = target.position().subtract(player.position()).normalize();
            if (toTarget.dot(look) < 0.2D) continue;

            // 燃やす
            target.setSecondsOnFire(FIRE_SECONDS);

            // 吹っ飛ばす
            target.knockback(
                    3.5D,
                    -look.x,
                    -look.z
            );
            target.setDeltaMovement(
                    target.getDeltaMovement().add(0, 0.9D, 0)
            );
            target.hurtMarked = true;

            // 攻撃力分のダメージも与える
            target.invulnerableTime = 0;
            target.hurt(
                    player.damageSources().playerAttack(player),
                    (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
            );

            // 演出
            sl.sendParticles(
                    ParticleTypes.FLAME,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.05
            );
        }

        // =========================================================
        // 3. 全体演出
        // =========================================================
        // 前方へ衝撃波
        Vec3 blastCenter = eye.add(look.scale(2.0D));
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 2;
            double dx = Math.cos(angle) * 0.5;
            double dz = Math.sin(angle) * 0.5;
            sl.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    blastCenter.x, blastCenter.y, blastCenter.z,
                    1,
                    dx, 0.0, dz,
                    0.3
            );
        }

        sl.sendParticles(
                ParticleTypes.EXPLOSION,
                blastCenter.x, blastCenter.y, blastCenter.z,
                1, 0, 0, 0, 0
        );

        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS,
                1.5F, 0.8F
        );

        if (brokeAnyDoor) {
            sl.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    SoundSource.PLAYERS,
                    1.2F, 1.0F
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /**
     * 指定位置にドア系ブロックがあれば破壊する。
     *
     * 鉄ドア・木ドア・トラップドア・フェンスゲート 全部対象。
     * ドロップはさせない（消防斧で粉砕するイメージ）。
     */
    private static boolean breakDoorAt(ServerLevel level, BlockPos pos, Player player) {

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        boolean isDoor = block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock;

        if (!isDoor) {
            return false;
        }

        // 二重ドア（上半分）の場合は下半分も一緒に消す
        if (block instanceof DoorBlock) {
            if (state.hasProperty(DoorBlock.HALF)) {
                if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                    BlockPos below = pos.below();
                    if (level.getBlockState(below).getBlock() == block) {
                        level.destroyBlock(below, false, player);
                    }
                } else {
                    BlockPos above = pos.above();
                    if (level.getBlockState(above).getBlock() == block) {
                        level.destroyBlock(above, false, player);
                    }
                }
            }
        }

        // ドア本体を破壊（ドロップなし）
        level.destroyBlock(pos, false, player);

        // 破壊演出
        level.sendParticles(
                ParticleTypes.CRIT,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.1
        );

        return true;
    }

    // =========================================================
    // 表示名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("先輩の消防斧")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFFFF4400, 0xFFFFDD00, 0xFFFFFFFF, 0xFFFF4400)
                .gradientSpeed(0.8F)
                .gradientPhase(0.7F);
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
        tooltip.add(Component.literal("先輩は先行者だと知ったのは二年後のことである")
                .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}