package com.tyami.forlaism.client.gmb;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * GMB の能力を集約するクラス。
 *
 * 【設計】
 *   - 攻撃時: onHit()
 *   - 使用時: onUse()
 *   - Tooltip: describeFeatures()
 *
 *   これらは Feature のリストを走査して実行する。
 *   新しい能力を追加したいときは
 *       GMBAbility.registerFeature(...)
 *   を呼ぶだけでOK。
 */
public final class GMBAbility {

    // =========================================================
    // Feature 定義
    // =========================================================

    /** 攻撃時に走る Feature。 */
    public interface OnHitFeature {
        void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker);
        String describe();
    }

    /** 右クリック時に走る Feature。 */
    public interface OnUseFeature {
        void onUse(ItemStack stack, Player player);
        String describe();
    }

    private static final List<OnHitFeature> ON_HIT = new ArrayList<>();
    private static final List<OnUseFeature> ON_USE = new ArrayList<>();

    private GMBAbility() {
    }

    // =========================================================
    // 登録 API（あとで拡張する用）
    // =========================================================

    public static void registerOnHit(OnHitFeature feature) {
        ON_HIT.add(feature);
    }

    public static void registerOnUse(OnUseFeature feature) {
        ON_USE.add(feature);
    }

    // =========================================================
    // 実行
    // =========================================================

    public static void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        for (OnHitFeature f : ON_HIT) {
            try {
                f.onHit(stack, target, attacker);
            } catch (Throwable t) {
                System.err.println("[GMB] onHit feature failed: " + t);
            }
        }
    }

    public static void onUse(ItemStack stack, Player player) {
        for (OnUseFeature f : ON_USE) {
            try {
                f.onUse(stack, player);
            } catch (Throwable t) {
                System.err.println("[GMB] onUse feature failed: " + t);
            }
        }
    }

    // =========================================================
    // Tooltip
    // =========================================================

    public static List<String> describeFeatures(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        for (OnHitFeature f : ON_HIT) lines.add("§d・" + f.describe());
        for (OnUseFeature f : ON_USE) lines.add("§b・" + f.describe());
        return lines;
    }

    // =========================================================
    // =========================================================
    // デフォルト機能（最初から入ってる）
    // =========================================================
    // =========================================================

        static {

        // =========================================================
        // 1. 絶対即死
        // =========================================================
        registerOnHit(new OnHitFeature() {
            @Override
            public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                target.invulnerableTime = 0;
                target.hurtTime = 0;
                target.setHealth(0.0F);
                if (!target.isDeadOrDying()) {
                    target.die(target.damageSources().playerAttack(
                            attacker instanceof Player p ? p : null));
                }
            }
            @Override
            public String describe() {
                return "絶対即死（無敵貫通）";
            }
        });

        // =========================================================
        // 2. 範囲伝染（10ブロック）
        // =========================================================
        registerOnHit(new OnHitFeature() {
            @Override
            public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                if (!(attacker.level() instanceof ServerLevel sl)) return;
                AABB area = attacker.getBoundingBox().inflate(10.0);
                var nearby = sl.getEntitiesOfClass(
                        LivingEntity.class, area,
                        e -> e != attacker && e != target
                                && e.isAlive() && !e.isSpectator()
                );
                for (LivingEntity e : nearby) {
                    e.invulnerableTime = 0;
                    e.hurtTime = 0;
                    e.setHealth(0.0F);
                    if (!e.isDeadOrDying()) {
                        e.die(e.damageSources().playerAttack(
                                attacker instanceof Player p ? p : null));
                    }
                }
            }
            @Override
            public String describe() {
                return "範囲伝染（半径10m）";
            }
        });

        // =========================================================
        // 3. 体力吸収
        // =========================================================
        registerOnHit(new OnHitFeature() {
            @Override
            public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
                if (!(attacker instanceof Player p)) return;
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 5.0F));
            }
            @Override
            public String describe() {
                return "体力吸収（命中時 +5HP）";
            }
        });

        // =========================================================
        // 4. 右クリック: 世界消去（半径128m 全Entity抹消 + 再スポーン阻止）
        // =========================================================
        registerOnUse(new OnUseFeature() {
            @Override
            public void onUse(ItemStack stack, Player player) {
                if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
                if (player.getCooldowns().isOnCooldown(
                        com.tyami.forlaism.registry.Items.GAMING_MASTER_BLADE.get())) {
                    return;
                }

                com.tyami.forlaism.annihilation.GMBAnnihilation.annihilateAround(sp);

                player.getCooldowns().addCooldown(
                        com.tyami.forlaism.registry.Items.GAMING_MASTER_BLADE.get(),
                        60
                );
            }
            @Override
            public String describe() {
                return "右クリック: 世界消去（半径128m 完全抹消）";
            }
        });
    }
}