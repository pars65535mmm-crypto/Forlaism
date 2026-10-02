package com.tyami.forlaism.item;

import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;
import com.tyami.forlaism.entity.EndWardenReverseEntity;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 終理の召喚符（リバース）。
 *
 * クラフトした瞬間に EndWardenReverseEntity を召喚する触媒。
 * 召喚符自体は消滅する。
 *
 * 見た目は同じ syoukannkei テクスチャを使い回し、
 * 赤いオーラをマジックエフェクトで表現する。
 */
public class EndWardenSummonerReverseItem extends Item implements IMagicEffectItem {

    /** 赤いオーラ（控えめ）。 */
    public static final MagicEffectStyle EFFECT_STYLE = MagicEffectStyle.builder()
            .color(0x80FF3333)   // 半透明の赤
            .intensity(1.4f)
            .scale(1.05f)
            .speed(0.7f)
            .ringCount(0)
            .particleCount(0)
            .flares(true)
            .coreGlow(true)
            .build();

    public EndWardenSummonerReverseItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        return EFFECT_STYLE;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§4§l終理の召喚符・叛逆")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§cクラフトした瞬間、終理の叛逆が顕現する。")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§8真なる終焉は、ここから始まる。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /**
     * プレイヤーの前方3ブロックに EndWardenReverseEntity を召喚する。
     */
    public static void summonEndWardenReverse(ServerLevel level, ServerPlayer player) {

        var look = player.getLookAngle().normalize();

        double spawnX = player.getX() + look.x * 3.0D;
        double spawnY = player.getY();
        double spawnZ = player.getZ() + look.z * 3.0D;

        EndWardenReverseEntity warden = ModEntityTypes.END_WARDEN_REVERSE.get().create(level);
        if (warden == null) {
            player.displayClientMessage(
                    Component.literal("§c終理の叛逆に失敗した…"),
                    true
            );
            return;
        }

        warden.moveTo(
                spawnX, spawnY, spawnZ,
                player.getYRot() + 180.0F,
                0.0F
        );

        level.addFreshEntity(warden);

        // 演出（通常版より派手）
        level.playSound(
                null,
                spawnX, spawnY, spawnZ,
                SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE,
                3.0F,
                0.5F
        );
        level.playSound(
                null,
                spawnX, spawnY, spawnZ,
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE,
                2.0F,
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
                ParticleTypes.SOUL_FIRE_FLAME,
                spawnX, spawnY + 1.0, spawnZ,
                120,
                2.0, 2.5, 2.0,
                0.2
        );
        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                spawnX, spawnY + 1.0, spawnZ,
                30,
                1.5, 2.0, 1.5,
                0.0
        );

        player.displayClientMessage(
                Component.literal("§4§l終理の叛逆が顕現した…")
                        .withStyle(ChatFormatting.DARK_RED),
                true
        );
    }
}