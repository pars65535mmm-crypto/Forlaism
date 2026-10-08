package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.PossibilityGlobalData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 新ワールド起動時、「可能性」フラグが立っていれば
 * プレイヤーの初期スポーン位置に「可能性」をドロップする。
 *
 * フラグはドロップした瞬間に消費される（1回きり）。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class PossibilityWorldInitHandler {

    /** 既にドロップ済みのワールドを記録（同じワールドで複数回出さない）。 */
    private static final Set<UUID> ALREADY_DROPPED = new HashSet<>();

    private PossibilityWorldInitHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // ワールド識別用のID（overworldのseed + 起動回数とかでもいいけど、簡易的に）
        // → ここでは「プレイヤーUUID + ワールドのtick数」で判定
        //   実際には「このワールドで一度もドロップしてないか」が知りたいので、
        //   ワールドのSavedDataでもいいけど、シンプルにフラグ消費で代用する。

        if (!PossibilityGlobalData.isPending()) return;

        // ログイン直後はワールド情報が揃ってない可能性があるので、次tickで実行
        ServerLevel level = player.serverLevel();
        UUID playerId = player.getUUID();

        // 一回だけ実行（同一ワールド・同一セッションで複数回出さない）
        String worldKey = player.serverLevel().getServer().getWorldData().getLevelName();
        UUID key = UUID.nameUUIDFromBytes(worldKey.getBytes());
        if (ALREADY_DROPPED.contains(key)) return;
        ALREADY_DROPPED.add(key);

        player.server.execute(() -> {
            dropPossibility(player);
        });
    }

    private static void dropPossibility(ServerPlayer player) {
        // 再チェック
        if (!PossibilityGlobalData.isPending()) return;

        ServerLevel level = player.serverLevel();

        // プレイヤーの初期スポーン位置（= ワールドスポーンではなく、プレイヤーが今いる場所）
        Vec3 pos = player.position();

        ItemStack stack = new ItemStack(Items.POSSIBILITY.get());

        ItemEntity entity = new ItemEntity(
                level,
                pos.x,
                pos.y + 0.5,
                pos.z,
                stack
        );
        entity.setNoPickUpDelay();
        entity.setGlowingTag(true);

        level.addFreshEntity(entity);

        // 演出
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.PORTAL,
                pos.x, pos.y + 1.0, pos.z,
                40,
                0.5, 0.8, 0.5,
                0.2
        );
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.x, pos.y + 1.0, pos.z,
                20,
                0.3, 0.5, 0.3,
                0.05
        );

        level.playSound(
                null,
                pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.5F, 0.5F
        );

        // プレイヤーにメッセージ
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§5どこからか、何かが落ちてきた..."),
                false
        );

        // フラグ消費
        PossibilityGlobalData.consume();
    }
}