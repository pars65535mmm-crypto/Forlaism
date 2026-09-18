package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = "forlaism",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HaloOfTheFirmamentAbility {

    /*
     * =========================================================
     * 基本設定
     * =========================================================
     */

    /**
     * Javaのdouble最大値。
     */
    private static final double JAVA_MAX_VALUE =
            Double.MAX_VALUE;

    /**
     * 耐久値。
     *
     * 「残り耐久値1000」として扱う。
     */
    private static final int DURABILITY = 1000;

    /**
     * 飛行速度。
     *
     * Vanilla Creativeの0.05より高速。
     */
    private static final float FLYING_SPEED = 0.15F;

    /*
     * =========================================================
     * Attack Speed
     * =========================================================
     */

    private static final UUID ATTACK_SPEED_UUID =
            UUID.fromString(
                    "2a0e5f0d-7c91-4b43-9d3a-4c1e7f6b9201"
            );

    private static final String ATTACK_SPEED_NAME =
            "halo_of_the_firmament_attack_speed";

    /*
     * =========================================================
     * 装備状態
     * =========================================================
     *
     * 「一度装備したら次tickに必ず戻す」ため、
     * 最後に確認したHaloのItemStackを保存する。
     *
     * ワールド再起動後まで永続化する目的ではない。
     */

    private static final Map<UUID, ItemStack> LOCKED_HALOS =
            new HashMap<>();

    private HaloOfTheFirmamentAbility() {
    }

    /*
     * =========================================================
     * Haloを装備しているか
     * =========================================================
     */

    private static boolean hasHalo(Player player) {

        return CuriosApi.getCuriosInventory(player)
                .map(handler ->
                        handler.findFirstCurio(
                                stack -> stack.is(
                                        Items.HALO_OF_THE_FIRMAMENT.get()
                                )
                        ).isPresent()
                )
                .orElse(false);
    }

    /*
     * =========================================================
     * Haloを発見したら保存
     * =========================================================
     */

    private static void rememberHalo(Player player) {

        UUID uuid = player.getUUID();

        if (LOCKED_HALOS.containsKey(uuid)) {
            return;
        }

        CuriosApi.getCuriosInventory(player)
                .resolve()
                .ifPresent(handler ->

                        handler.findFirstCurio(
                                stack -> stack.is(
                                        Items.HALO_OF_THE_FIRMAMENT.get()
                                )
                        )
                        .ifPresent(result ->

                                LOCKED_HALOS.put(
                                        uuid,
                                        result.stack().copy()
                                )
                        )
                );
    }

    /*
     * =========================================================
     * Haloを強制再装備
     * =========================================================
     */

    private static void ensureHaloEquipped(Player player) {

        ItemStack halo =
                LOCKED_HALOS.get(player.getUUID());

        if (halo == null || halo.isEmpty()) {
            return;
        }

        /*
         * 既に装備されているなら何もしない。
         */
        if (hasHalo(player)) {
            return;
        }

        /*
         * Curiosのbackスロットへ再装備。
         */
        CuriosApi.getCuriosInventory(player)
                .ifPresent(handler ->

                        handler.setEquippedCurio(
                                "back",
                                0,
                                halo.copy()
                        )
                );
    }

    /*
     * =========================================================
     * Player Tick
     * =========================================================
     */

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {

        /*
         * ENDのみ。
         */
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        /*
         * サーバーのみ。
         */
        if (player.level().isClientSide) {
            return;
        }

        /*
         * =====================================================
         * 現在Haloを装備しているか確認
         * =====================================================
         */

        boolean equipped = hasHalo(player);

        /*
         * 初めて発見したらHaloを保存。
         */
        if (equipped) {
            rememberHalo(player);
        }

        /*
         * =====================================================
         * 既に記憶されているHaloを次tickに復元
         * =====================================================
         *
         * /clear等で消された場合もここに来る。
         */

        if (LOCKED_HALOS.containsKey(player.getUUID())) {
            ensureHaloEquipped(player);
        }

        /*
         * =====================================================
         * 能力は「現在Haloを装備している場合」のみ発動
         * =====================================================
         */

        if (!equipped) {
            return;
        }

        /*
         * =====================================================
         * クリエイティブ飛行
         * =====================================================
         */

        player.getAbilities().mayfly = true;
        player.getAbilities().invulnerable = true;
        player.getAbilities().setFlyingSpeed(
                FLYING_SPEED
        );
        player.onUpdateAbilities();

        /*
         * 飛行状態も維持。
         */
        if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
        }

        /*
         * =====================================================
         * 攻撃速度
         * =====================================================
         */

        applyAttackSpeed(player);

        /*
         * =====================================================
         * 常時暗視
         * =====================================================
         */

        player.addEffect(
                new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        1000,
                        0,
                        false,
                        false,
                        false
                )
        );

        /*
         * =====================================================
         * アイテムクールダウン
         * =====================================================
         */

        resetCooldowns(player);

        /*
         * =====================================================
         * FE
         * =====================================================
         */

        supplyEnergy(player);

        /*
         * =====================================================
         * 耐久値
         * =====================================================
         */

        repairAllItems(player);
    }

    /*
     * =========================================================
     * 攻撃速度
     * =========================================================
     */

    private static void applyAttackSpeed(Player player) {

        AttributeInstance attribute =
                player.getAttribute(
                        Attributes.ATTACK_SPEED
                );

        if (attribute == null) {
            return;
        }

        AttributeModifier existing =
                attribute.getModifier(
                        ATTACK_SPEED_UUID
                );

        /*
         * 初回追加。
         */
        if (existing == null) {

            attribute.addPermanentModifier(
                    new AttributeModifier(
                            ATTACK_SPEED_UUID,
                            ATTACK_SPEED_NAME,
                            JAVA_MAX_VALUE,
                            AttributeModifier.Operation.ADDITION
                    )
            );

            return;
        }

        /*
         * 値が変わっていたら再設定。
         */
        if (existing.getAmount() != JAVA_MAX_VALUE) {

            attribute.removeModifier(
                    ATTACK_SPEED_UUID
            );

            attribute.addPermanentModifier(
                    new AttributeModifier(
                            ATTACK_SPEED_UUID,
                            ATTACK_SPEED_NAME,
                            JAVA_MAX_VALUE,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }
    }

    /*
     * =========================================================
     * アイテムクールダウンを0へ
     * =========================================================
     */

    private static void resetCooldowns(Player player) {

        /*
         * 通常インベントリ
         */
        for (ItemStack stack : player.getInventory().items) {

            if (!stack.isEmpty()) {
                player.getCooldowns()
                        .removeCooldown(
                                stack.getItem()
                        );
            }
        }

        /*
         * オフハンド
         */
        for (ItemStack stack :
                player.getInventory().offhand) {

            if (!stack.isEmpty()) {
                player.getCooldowns()
                        .removeCooldown(
                                stack.getItem()
                        );
            }
        }

        /*
         * 防具
         */
        for (ItemStack stack :
                player.getInventory().armor) {

            if (!stack.isEmpty()) {
                player.getCooldowns()
                        .removeCooldown(
                                stack.getItem()
                        );
            }
        }
    }

    /*
     * =========================================================
     * 採掘速度
     * =========================================================
     *
     * 水中・空中などによる採掘速度低下を
     * 実質的に無視する。
     */

    @SubscribeEvent
    public static void onBreakSpeed(
            PlayerEvent.BreakSpeed event
    ) {

        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (!LOCKED_HALOS.containsKey(
                player.getUUID()
        )) {
            return;
        }

        /*
         * 現在Haloが実際に装備されている場合のみ。
         */
        if (!hasHalo(player)) {
            return;
        }

        /*
         * 採掘速度を最大級へ。
         */
        event.setNewSpeed(Float.MAX_VALUE);
    }

    /*
     * =========================================================
     * FE供給
     * =========================================================
     *
     * Forge Energy APIはintなので、
     * Double.MAX_VALUEをそのまま渡すことはできない。
     *
     * Integer.MAX_VALUEを要求し、
     * 対象ItemStackの容量いっぱいまで充電する。
     */

    private static void supplyEnergy(Player player) {

        /*
         * Main Hand
         */
        supplyEnergy(
                player.getMainHandItem()
        );

        /*
         * Off Hand
         */
        supplyEnergy(
                player.getOffhandItem()
        );

        /*
         * Inventory
         */
        for (ItemStack stack :
                player.getInventory().items) {

            supplyEnergy(stack);
        }

        /*
         * Armor
         */
        for (ItemStack stack :
                player.getInventory().armor) {

            supplyEnergy(stack);
        }
    }

    private static void supplyEnergy(
            ItemStack stack
    ) {

        if (stack.isEmpty()) {
            return;
        }

        stack.getCapability(
                ForgeCapabilities.ENERGY
        ).ifPresent(energy ->

                energy.receiveEnergy(
                        Integer.MAX_VALUE,
                        false
                )
        );
    }

    /*
     * =========================================================
     * 耐久値1000固定
     * =========================================================
     */

    private static void repairAllItems(Player player) {

        /*
         * Main Hand
         */
        setDurability(
                player.getMainHandItem()
        );

        /*
         * Off Hand
         */
        setDurability(
                player.getOffhandItem()
        );

        /*
         * Inventory
         */
        for (ItemStack stack :
                player.getInventory().items) {

            setDurability(stack);
        }

        /*
         * Armor
         */
        for (ItemStack stack :
                player.getInventory().armor) {

            setDurability(stack);
        }
    }

    private static void setDurability(
            ItemStack stack
    ) {

        if (stack.isEmpty()) {
            return;
        }

        /*
         * 耐久値を持たないアイテムは無視。
         */
        if (!stack.isDamageableItem()) {
            return;
        }

        int maxDamage =
                stack.getMaxDamage();

        /*
         * 最大耐久1000以下なら完全修理。
         */
        if (maxDamage <= DURABILITY) {

            if (stack.getDamageValue() != 0) {
                stack.setDamageValue(0);
            }

            return;
        }

        /*
         * 残り耐久1000になるDamageValueを計算。
         */
        int targetDamage =
                maxDamage - DURABILITY;

        if (stack.getDamageValue() != targetDamage) {

            stack.setDamageValue(
                    targetDamage
            );
        }
    }

    /*
     * =========================================================
     * 攻撃
     * =========================================================
     *
     * 通常のダメージ処理を使わない。
     *
     * setHealth(0)なので、
     *
     * Armor
     * Protection
     * 無敵時間
     * 通常のDamageSource処理
     *
     * を全部すっ飛ばす。
     */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(
            AttackEntityEvent event
    ) {

        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        /*
         * Haloを現在装備していなければ無効。
         */
        if (!hasHalo(player)) {
            return;
        }

        if (!(event.getTarget()
                instanceof LivingEntity target)) {

            return;
        }

        /*
         * Vanillaの通常攻撃をキャンセル。
         */
        event.setCanceled(true);

        /*
         * HPを0へ。
         *
         * 防御力を一切見ない。
         * 無敵時間も見ない。
         */
        target.setHealth(0.0F);

        /*
         * 死亡処理。
         */
        if (!target.isDeadOrDying()) {

            target.die(
                    target.damageSources()
                            .playerAttack(player)
            );
        }
    }
}