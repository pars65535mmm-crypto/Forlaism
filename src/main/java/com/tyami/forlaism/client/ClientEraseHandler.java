package com.tyami.forlaism.client;

import com.tyami.forlaism.erase.EraseRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * クライアント側で抹消済みEntityを定期的に除去する。
 *
 * サーバーから削除パケットが来るはずだが、
 * MOD側の復活処理対策として念のため。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientEraseHandler {

    private ClientEraseHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // 5tick毎にチェック
        if (mc.level.getGameTime() % 5 != 0) return;

        // クライアント側キャッシュに登録されているUUIDのEntityを除去
        mc.level.entitiesForRendering().forEach(entity -> {
            if (EraseRegistry.isErasedClient(entity.getUUID())) {
                entity.remove(Entity.RemovalReason.KILLED);
            }
        });
    }
}