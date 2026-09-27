package com.tyami.forlaism.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DiaryScreenOpener {

    private DiaryScreenOpener() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new DiaryScreen());
    }
}