package com.tyami.forlaism.ironsspells.spell;

import com.tyami.forlaism.ironsspells.ForlaismKokuSchool;
import com.tyami.forlaism.world.EntityFreezeManager;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.Optional;

/**
 * 刻魔法「停止」。
 *
 * 対象のentityを完全停止させる。
 * レベル1: 3秒、レベル2: 5秒。
 *
 * 対象取得は自前レイキャスト方式（capabilities.magic に依存しない）。
 */
public class StopSpell extends AbstractSpell {

    /** 射程（ブロック）。 */
    public static final int RANGE = 20;

    /** エイムアシスト。 */
    public static final float AIM_ASSIST = 0.35F;

    public StopSpell() {
        this.baseManaCost = 50;
        this.manaCostPerLevel = 25;
        this.baseSpellPower = 60;     // 3秒 = 60 ticks
        this.spellPowerPerLevel = 40; // +2秒 = +40 ticks
        this.castTime = 0;            // INSTANT
    }

    @Override
    public ResourceLocation getSpellResource() {
        return new ResourceLocation("forlaism", "stop");
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return new DefaultConfig()
                .setMinRarity(SpellRarity.RARE)
                .setMaxLevel(2)
                .setSchoolResource(ForlaismKokuSchool.KOKU_ID)
                .setCooldownSeconds(10);
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public SchoolType getSchoolType() {
        return ForlaismKokuSchool.KOKU;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.BEACON_DEACTIVATE);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        // 射程20ブロックで対象がいるかチェック
        // preCastTargetHelper は TargetEntityCastData を内部で使うので、
        // ここでは単純にレイキャストだけ行って存在チェック
        var hit = Utils.raycastForEntity(level, entity, RANGE, true, AIM_ASSIST);
        if (hit instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity target
                && target.isAlive()) {
            return true;
        }

        // 対象がいない場合、警告メッセージを出す
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.irons_spellbooks.cast_error_target")
                            .withStyle(ChatFormatting.RED)
            ));
        }
        return false;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);

        if (level.isClientSide) return;

        // 自前でレイキャストして対象を取得
        var hit = Utils.raycastForEntity(level, entity, RANGE, true, AIM_ASSIST);
        if (!(hit instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof LivingEntity target)) return;
        if (!target.isAlive()) return;

        // 停止時間 = spellPower ticks
        // Lv1: 60 tick = 3秒
        // Lv2: 100 tick = 5秒
        int freezeTicks = (int) getSpellPower(spellLevel, entity);

        // Forlaism の既存凍結システムを使用
        EntityFreezeManager.freeze(target, freezeTicks);

        // 演出
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F, 0.5F);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS,
                1.0F, 0.5F);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.forlaism.stop.duration",
                                Utils.timeFromTicks(getSpellPower(spellLevel, caster) / 20.0F, 1))
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
        );
    }
}