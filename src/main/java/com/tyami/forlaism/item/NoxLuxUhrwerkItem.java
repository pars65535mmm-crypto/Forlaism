package com.tyami.forlaism.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.tyami.forlaism.damage.MoonlightDamageSource;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ノクスルクスヴェルク。
 *
 * - 攻撃力 22.5
 * - 攻撃速度 7.5
 * - 通常攻撃に月光ダメージ追加（防御貫通）
 * - スイープ時、周囲に月光ダメージ
 * - TPチャージ 0.3秒
 * - Shift長押し0.5秒 + 右クリックでレイキャストTP
 * - TP後 0.5秒無敵
 * - 追撃〈月夜〉: 15×64 = 960ダメージ ×5 + 0.7秒硬直
 */
public class NoxLuxUhrwerkItem extends SwordItem implements IAnimatedTextItem {

    // =========================================================
    // 性能定数
    // =========================================================

    public static final float ATTACK_DAMAGE = 15.0F * 1.5F; // 22.5
    public static final double ATTACK_SPEED = 3.0D * 1.5D + 3.0D; // 7.5

    /** 通常攻撃の追加月光ダメージ（防御貫通）。 */
    public static final float NORMAL_MOONLIGHT_DAMAGE = 8.0F;

    /** 追撃〈月夜〉のダメージ（TP後、64倍）。 */
    public static final float MOONLIGHT_DAMAGE = 15.0F * 64.0F; // 960

    public static final int MOONLIGHT_HITS = 5;
    public static final int MOONLIGHT_STUN_TICKS = 14;

    public static final int TP_CHARGE_TICKS = 6;
    public static final int PURSUIT_WINDOW_TICKS = 20;

    public static final double SWEEP_RADIUS = 5.0D;
    public static final float SWEEP_MOONLIGHT_DAMAGE = 12.0F;

    public static final double RAYCAST_TP_RANGE = 128.0D;
    public static final int RAYCAST_CHARGE_TICKS = 5;
    public static final int RAYCAST_COOLDOWN_TICKS = 5;

    /** TP後の無敵時間（tick）。0.25秒 = 5tick。 */
    public static final int TP_INVULNERABLE_TICKS = 5;

    // =========================================================
    // UUID
    // =========================================================

    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("a1b2c3d4-0001-0002-0003-000000000011");
    private static final UUID ATTACK_SPEED_UUID =
            UUID.fromString("a0b1c2d3-0001-0002-0003-000000000012");

    // =========================================================
    // プレイヤーごとの状態
    // =========================================================

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        UUID recordedEntityId = null;
        int shiftHoldTicks = 0;
        long pursuitExpireTick = 0L;
        boolean wasShiftDown = false;
        int raycastCooldown = 0;
    }

    // =========================================================
    // コンストラクタ
    // =========================================================

    public NoxLuxUhrwerkItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                0,
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性
    // =========================================================

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {

        Multimap<Attribute, AttributeModifier> original =
                super.getDefaultAttributeModifiers(slot);

        if (slot != EquipmentSlot.MAINHAND) {
            return original;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder =
                ImmutableMultimap.builder();

        original.forEach(builder::put);

        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_UUID,
                        "noxluxuhrwerk_attack",
                        ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADDITION
                )
        );

        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "noxluxuhrwerk_speed",
                        ATTACK_SPEED,
                        AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // =========================================================
    // 通常攻撃（スイープ + 月光貫通）
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (!attacker.level().isClientSide
                && attacker.level() instanceof ServerLevel sl) {

            // =========================================================
            // 通常攻撃に月光ダメージ追加（防御貫通）
            // =========================================================
            target.invulnerableTime = 0;
            target.hurtTime = 0;
            target.hurt(
                    MoonlightDamageSource.of(sl, attacker),
                    NORMAL_MOONLIGHT_DAMAGE
            );

            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    6, 0.3, 0.5, 0.3, 0.04
            );

            // =========================================================
            // スイープ（周囲に月光）
            // =========================================================
            doEnhancedSweep(sl, attacker, target);
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    private void doEnhancedSweep(ServerLevel sl, LivingEntity attacker, LivingEntity mainTarget) {

        AABB area = attacker.getBoundingBox().inflate(SWEEP_RADIUS);

        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive()
                        && e != attacker
                        && !e.isSpectator()
                        && !e.getUUID().equals(mainTarget.getUUID())
        );

        for (LivingEntity t : targets) {
            t.invulnerableTime = 0;
            t.hurtTime = 0;
            t.hurt(
                    MoonlightDamageSource.of(sl, attacker),
                    SWEEP_MOONLIGHT_DAMAGE
            );

            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    t.getX(),
                    t.getY() + t.getBbHeight() * 0.5,
                    t.getZ(),
                    8, 0.4, 0.6, 0.4, 0.05
            );
        }

        sl.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                attacker.getX(),
                attacker.getY() + 1.0,
                attacker.getZ(),
                15,
                SWEEP_RADIUS, 0.8, SWEEP_RADIUS,
                0.1
        );
    }

    // =========================================================
    // 右クリック: レイキャストTP
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.pass(stack);
        }

        State state = STATES.computeIfAbsent(sp.getUUID(), k -> new State());

        // クールダウン中は不可（メッセージなし）
        if (state.raycastCooldown > 0) {
            return InteractionResultHolder.fail(stack);
        }

        // Shift押してなければ何もしない
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        // Shift押し続け時間チェック（メッセージなし、静かにfail）
        if (state.shiftHoldTicks < RAYCAST_CHARGE_TICKS) {
            return InteractionResultHolder.fail(stack);
        }

        performRaycastTeleport(sp, sl, state);
        state.raycastCooldown = RAYCAST_COOLDOWN_TICKS;

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // =========================================================
    // 毎tick処理
    // =========================================================

    public static void tickPlayer(ServerPlayer player) {

        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof NoxLuxUhrwerkItem)) {
            STATES.remove(player.getUUID());
            return;
        }

        State state = STATES.computeIfAbsent(player.getUUID(), k -> new State());

        if (state.raycastCooldown > 0) {
            state.raycastCooldown--;
        }

        boolean shiftDown = player.isShiftKeyDown();

        if (shiftDown && !state.wasShiftDown) {
            LivingEntity lookTarget = findLookTarget(player, 64.0D);
            if (lookTarget != null) {
                state.recordedEntityId = lookTarget.getUUID();
                state.shiftHoldTicks = 0;
            } else {
                state.recordedEntityId = null;
                state.shiftHoldTicks = 0;
            }
        }

        if (shiftDown) {
            state.shiftHoldTicks++;
        } else {
            state.shiftHoldTicks = 0;
        }

        if (shiftDown && state.recordedEntityId != null) {
            if (state.shiftHoldTicks >= TP_CHARGE_TICKS) {
                performEntityTeleport(player, state);
                state.shiftHoldTicks = 0;
            }
        }

        state.wasShiftDown = shiftDown;
    }

    // =========================================================
    // Entity背後TP
    // =========================================================

    private static void performEntityTeleport(ServerPlayer player, State state) {

        if (!(player.level() instanceof ServerLevel level)) return;

        var entity = level.getEntity(state.recordedEntityId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            state.recordedEntityId = null;
            return;
        }

        Vec3 look = living.getLookAngle().normalize();
        Vec3 behind = living.position().subtract(look.scale(2.0));

        level.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40, 0.5, 1.0, 0.5, 0.3
        );

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F, 1.2F
        );

        player.teleportTo(behind.x, behind.y, behind.z);
        lookAt(player, living);

        level.sendParticles(
                ParticleTypes.PORTAL,
                behind.x, behind.y + 1.0, behind.z,
                40, 0.5, 1.0, 0.5, 0.3
        );
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                behind.x, behind.y + 1.0, behind.z,
                15, 0.4, 0.6, 0.4, 0.05
        );

        // =========================================================
        // TP後 0.5秒無敵
        // =========================================================
        applyTpInvulnerability(player);

        state.pursuitExpireTick = level.getGameTime() + PURSUIT_WINDOW_TICKS;
    }

    // =========================================================
    // レイキャストTP
    // =========================================================

    private static void performRaycastTeleport(ServerPlayer player, ServerLevel level, State state) {

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(RAYCAST_TP_RANGE));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 destination;

        if (hit.getType() == HitResult.Type.BLOCK) {
            destination = hit.getLocation().subtract(look.scale(0.5));
        } else {
            destination = end;
        }

        level.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40, 0.5, 1.0, 0.5, 0.3
        );

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F, 1.4F
        );

        player.teleportTo(destination.x, destination.y, destination.z);

        level.sendParticles(
                ParticleTypes.PORTAL,
                destination.x, destination.y + 1.0, destination.z,
                40, 0.5, 1.0, 0.5, 0.3
        );
        level.sendParticles(
                ParticleTypes.END_ROD,
                destination.x, destination.y + 1.0, destination.z,
                20, 0.4, 0.6, 0.4, 0.05
        );

        level.playSound(
                null,
                destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F, 1.2F
        );

        // =========================================================
        // TP後 0.5秒無敵
        // =========================================================
        applyTpInvulnerability(player);
    }

    // =========================================================
    // TP後 0.5秒無敵
    // =========================================================

    private static void applyTpInvulnerability(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                TP_INVULNERABLE_TICKS,
                4,        // 耐性V（amplifier 4）
                false,
                false,
                false
        ));
    }

    // =========================================================
    // 視線先Entity検索
    // =========================================================

    @Nullable
    private static LivingEntity findLookTarget(ServerPlayer player, double maxDist) {

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDist));

        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(maxDist))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eye,
                end,
                box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    // =========================================================
    // 視線ロック
    // =========================================================

    private static void lookAt(ServerPlayer player, LivingEntity target) {

        double dx = target.getX() - player.getX();
        double dy = target.getEyeY() - player.getEyeY();
        double dz = target.getZ() - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, horizontal) * (180.0 / Math.PI));

        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);

        player.connection.teleport(
                player.getX(), player.getY(), player.getZ(),
                yaw, pitch
        );
    }

    // =========================================================
    // 攻撃時の追撃〈月夜〉
    // =========================================================

    public static void onAttack(ServerPlayer player, LivingEntity target) {

        if (!(player.getMainHandItem().getItem() instanceof NoxLuxUhrwerkItem)) return;

        State state = STATES.get(player.getUUID());
        if (state == null) return;

        if (!(player.level() instanceof ServerLevel level)) return;

        if (level.getGameTime() > state.pursuitExpireTick) return;

        if (state.recordedEntityId == null
                || !state.recordedEntityId.equals(target.getUUID())) {
            return;
        }

        // 追撃〈月夜〉: 15×64 = 960ダメージ×5
        for (int i = 0; i < MOONLIGHT_HITS; i++) {
            target.invulnerableTime = 0;
            target.hurtTime = 0;

            target.hurt(
                    MoonlightDamageSource.of(level, player),
                    MOONLIGHT_DAMAGE
            );

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    8, 0.4, 0.6, 0.4, 0.05
            );
        }

        applyStun(target, MOONLIGHT_STUN_TICKS);

        level.sendParticles(
                ParticleTypes.FLASH,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                1, 0, 0, 0, 0
        );
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                25, 0.6, 0.8, 0.6, 0.1
        );

        level.playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                1.2F, 1.8F
        );

        state.pursuitExpireTick = 0L;
        state.recordedEntityId = null;
    }

    // =========================================================
    // 硬直
    // =========================================================

    private static final Map<UUID, Long> STUN_MAP = new HashMap<>();

    private static void applyStun(LivingEntity target, int ticks) {
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }

        STUN_MAP.put(target.getUUID(), target.level().getGameTime() + ticks);
    }

    public static void tickStun(ServerLevel level) {
        long now = level.getGameTime();

        STUN_MAP.entrySet().removeIf(entry -> {
            if (entry.getValue() > now) return false;

            UUID uuid = entry.getKey();
            var e = level.getEntity(uuid);
            if (e instanceof LivingEntity living) {
                if (living instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                living.setDeltaMovement(0, 0, 0);
            }
            return true;
        });
    }

    // =========================================================
    // 表示名（アニメーション）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ノクスルクスヴェルク")
                .glow(0x60AA00FF, 1);
    }

    // =========================================================
    // Tooltip（タイプライター）
    // =========================================================

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        if (lineIndex == 1) {
            return AnimatedText.of("夜に沈んでいた、だって4000年も待ったんだから。")
                    .typewriter(2, 20);
        }

        return null;
    }


}