package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.HaloOfTheAdventAbility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(
            method = "setScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$preventDeathScreen(
            Screen screen,
            CallbackInfo ci
    ) {

        if (!(screen instanceof DeathScreen)) {
            return;
        }

        Minecraft minecraft =
                (Minecraft) (Object) this;

        LocalPlayer player =
                minecraft.player;

        if (player == null) {
            return;
        }

        if (!HaloOfTheAdventAbility.isProtected(player)) {
            return;
        }

        ci.cancel();
    }
}