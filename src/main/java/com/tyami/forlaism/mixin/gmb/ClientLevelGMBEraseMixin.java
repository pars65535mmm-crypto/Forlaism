package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * クライアント側: 消去済みEntityは追加された瞬間に削除。
 */
@OnlyIn(Dist.CLIENT)
@Mixin(value = ClientLevel.class, priority = 30000)
public abstract class ClientLevelGMBEraseMixin {

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbClientBlockAdd(int id, Entity entity, CallbackInfo ci) {
        if (entity == null) return;
        if (GMBEraseRegistry.isErasedClient(entity.getUUID())) {
            entity.remove(Entity.RemovalReason.KILLED);
            ci.cancel();
        }
    }
}