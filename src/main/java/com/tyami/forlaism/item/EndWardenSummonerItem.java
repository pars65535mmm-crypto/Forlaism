package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.EndWardenEntity;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 終理の召喚符。
 *
 * クラフトした瞬間に EndWarden を召喚する触媒。
 * インベントリに入った瞬間（アイテム取得イベント）で召喚され、
 * 召喚符自体は消滅する。
 *
 * このアイテム単体では何の機能も持たない。
 */
public class EndWardenSummonerItem extends Item {

    public EndWardenSummonerItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§5§l終理の召喚符")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§7クラフトした瞬間、終理が顕現する。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8これは召喚の触媒であり、物品ではない。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // =========================================================
    // 召喚処理（外部から呼ぶ想定）
    // =========================================================

    /**
     * プレイヤーの前方3ブロックに EndWarden を召喚する。
     */
    public static void summonEndWarden(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {

        // プレイヤーの視線方向を取得
        var look = player.getLookAngle().normalize();

        // 召喚位置（前方3ブロック）
        double spawnX = player.getX() + look.x * 3.0D;
        double spawnY = player.getY();
        double spawnZ = player.getZ() + look.z * 3.0D;

        // エンティティ生成
        EndWardenEntity warden = ModEntityTypes.END_WARDEN.get().create(level);
        if (warden == null) {
            player.displayClientMessage(
                    Component.literal("§c終理の召喚に失敗した…"),
                    true
            );
            return;
        }

        // 位置・向きを設定
        warden.moveTo(
                spawnX, spawnY, spawnZ,
                player.getYRot() + 180.0F,
                0.0F
        );

        // スポーン
        level.addFreshEntity(warden);

        // 演出
        level.playSound(
                null,
                spawnX, spawnY, spawnZ,
                SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE,
                3.0F,
                0.6F
        );

        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                spawnX, spawnY + 1.0, spawnZ,
                1,
                0, 0, 0,
                0
        );

        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                spawnX, spawnY + 1.0, spawnZ,
                80,
                1.5, 2.0, 1.5,
                0.15
        );

        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                spawnX, spawnY + 1.0, spawnZ,
                20,
                1.0, 1.5, 1.0,
                0.0
        );

        // メッセージ
        player.displayClientMessage(
                Component.literal("§5§l終理が顕現した…")
                        .withStyle(ChatFormatting.DARK_PURPLE),
                true
        );
    }
}