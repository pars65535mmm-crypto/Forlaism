package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.SakuraBulletEntity;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

/**
 * SKR360 Inf。
 *
 * 音は「重低音のドン + 破裂音」を重ねて銃っぽく。
 */
public class SKR360InfItem extends Item implements GeoItem, IAnimatedTextItem {

    // =========================================================
    // 定数
    // =========================================================

    public static final int MAGAZINE_SIZE = 6;
    public static final float BULLET_DAMAGE = 120.0F;
    public static final float BULLET_SPEED = 4.0F;
    public static final int RECOIL_TICKS = 12;
    public static final int RELOAD_TICKS = 60;

    private static final String TAG_AMMO = "SKR360Ammo";
    private static final String TAG_RELOADING = "SKR360Reloading";
    private static final String TAG_RELOAD_END = "SKR360ReloadEnd";
    private static final String TAG_LAST_FIRE = "SKR360LastFire";

    // =========================================================
    // アニメーション定義
    // =========================================================

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("idle");

    private static final RawAnimation FIRE =
            RawAnimation.begin().thenPlay("karinoutu");

    private static final RawAnimation RELOAD =
            RawAnimation.begin().thenLoop("dorrrrrrrr");

    private static final int FIRE_ANIM_TICKS = 10;

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public SKR360InfItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // =========================================================
    // GeckoLib コントローラ
    // =========================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main",
                0,
                state -> {

                    if (!state.isMoving() && !isClient()) {
                        return state.setAndContinue(IDLE);
                    }

                    ItemStack stack = getCurrentStack();
                    if (stack == null || stack.isEmpty()) {
                        return state.setAndContinue(IDLE);
                    }

                    CompoundTag tag = stack.getTag();
                    if (tag == null) {
                        return state.setAndContinue(IDLE);
                    }

                    long now = getClientGameTime();

                    if (isReloading(tag, now)) {
                        return state.setAndContinue(RELOAD);
                    }

                    long lastFire = tag.getLong(TAG_LAST_FIRE);
                    if (now - lastFire <= FIRE_ANIM_TICKS) {
                        return state.setAndContinue(FIRE);
                    }

                    return state.setAndContinue(IDLE);
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================
    // クライアントヘルパ
    // =========================================================

    private static boolean isClient() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient();
    }

    @Nullable
    private static ItemStack getCurrentStack() {
        if (!isClient()) return null;

        var player = Minecraft.getInstance().player;
        if (player == null) return null;

        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SKR360InfItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof SKR360InfItem) {
            return off;
        }
        return null;
    }

    private static long getClientGameTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    // =========================================================
    // 右クリック: 発射
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

        if (isReloading(stack.getOrCreateTag(), sl.getGameTime())) {
            player.displayClientMessage(
                    Component.literal("§dリロード中…"),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            startReload(stack, sl.getGameTime());
            player.displayClientMessage(
                    Component.literal("§dリロード"),
                    true
            );
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        ammo--;
        setAmmo(stack, ammo);

        stack.getOrCreateTag().putLong(TAG_LAST_FIRE, sl.getGameTime());

        fire(sl, player, stack);

        player.getCooldowns().addCooldown(this, RECOIL_TICKS);

        if (ammo <= 0) {
            startReload(stack, sl.getGameTime());
            player.displayClientMessage(
                    Component.literal("リロード"),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("§d残弾: §f" + ammo + " §7/ " + MAGAZINE_SIZE),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static void fire(ServerLevel sl, Player player, ItemStack stack) {

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        SakuraBulletEntity bullet = new SakuraBulletEntity(sl, player);
        bullet.setPos(
                eye.x + look.x * 1.0,
                eye.y + look.y * 1.0 - 0.1,
                eye.z + look.z * 1.0
        );
        bullet.shoot(look, BULLET_SPEED, BULLET_DAMAGE);
        sl.addFreshEntity(bullet);

        // =========================================================
        // 重低音の「ドン」＋破裂音を重ねる
        // =========================================================

        // ① 重低音の炸裂（メイン）
        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,   // 重低音
                SoundSource.PLAYERS,
                1.8F,
                0.55F   // ← ピッチ低めでドンッ
        );

        // ② 火薬の破裂
        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.2F,
                1.8F   // 高めの破裂
        );

        // ③ 発射の鋭さ
        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS,
                1.0F,
                1.6F
        );

        // ④ 金属的な残響
        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                0.9F,
                1.4F
        );

        // 発射点の演出
        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLASH,
                eye.x + look.x * 1.2,
                eye.y + look.y * 1.2 - 0.1,
                eye.z + look.z * 1.2,
                1, 0, 0, 0, 0
        );
        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES,
                eye.x + look.x * 1.2,
                eye.y + look.y * 1.2 - 0.1,
                eye.z + look.z * 1.2,
                15, 0.2, 0.2, 0.2, 0.05
        );
        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                eye.x + look.x * 1.0,
                eye.y + look.y * 1.0 - 0.1,
                eye.z + look.z * 1.0,
                10, 0.15, 0.15, 0.15, 0.02
        );

        // 反動
        player.setDeltaMovement(
                player.getDeltaMovement().add(
                        -look.x * 0.35,
                        0.08,
                        -look.z * 0.35
                )
        );
        player.hurtMarked = true;
    }

    // =========================================================
    // リロード
    // =========================================================

    private static void startReload(ItemStack stack, long now) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_RELOADING, true);
        tag.putLong(TAG_RELOAD_END, now + RELOAD_TICKS);
    }

    private static boolean isReloading(CompoundTag tag, long now) {
        if (tag == null) return false;
        if (!tag.getBoolean(TAG_RELOADING)) return false;

        long end = tag.getLong(TAG_RELOAD_END);
        if (now >= end) {
            tag.putBoolean(TAG_RELOADING, false);
            tag.remove(TAG_RELOAD_END);
            tag.putInt(TAG_AMMO, MAGAZINE_SIZE);
            return false;
        }
        return true;
    }

    // =========================================================
    // 弾数
    // =========================================================

    public static int getAmmo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_AMMO)) {
            return MAGAZINE_SIZE;
        }
        return tag.getInt(TAG_AMMO);
    }

    private static void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt(TAG_AMMO, Math.max(0, Math.min(ammo, MAGAZINE_SIZE)));
    }

    // =========================================================
    // バー表示
    // =========================================================

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getAmmo(stack) < MAGAZINE_SIZE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getAmmo(stack) / MAGAZINE_SIZE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFFF69B4;
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
        tooltip.add(Component.literal("§d§lSAKURAの銃。")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("SKR360 Inf")
                .wave(2.5F, 0.25F, 0.50F)
                .gradient(0xFFFFB7C5, 0xFFFFFFFF, 0xFFFF69B4)
                .gradientSpeed(0.8F)
                .gradientPhase(0.6F);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.tyami.forlaism.client.SKR360InfClientExtensions());
    }
}