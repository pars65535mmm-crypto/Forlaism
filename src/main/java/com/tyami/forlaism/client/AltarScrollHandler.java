package com.tyami.forlaism.client;

import com.tyami.forlaism.block.entity.AltarBlockEntity;
import com.tyami.forlaism.network.AltarScrollPacket;
import com.tyami.forlaism.network.FactotumPacketHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

/**
 * 祭壇のカーソル移動スクロール操作。
 *
 * Xキー + スクロール → 列 (col) を動かす
 * Yキー + スクロール → 行 (row) を動かす
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class AltarScrollHandler {

    private AltarScrollHandler() {
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        BlockPos altarPos = findAltarUnderCrosshair(mc);
        if (altarPos == null) return;

        long window = mc.getWindow().getWindow();

        boolean xHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_X) == GLFW.GLFW_PRESS;
        boolean yHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Y) == GLFW.GLFW_PRESS;

        if (!xHeld && !yHeld) return;

        double scroll = event.getScrollDelta();
        int dir = scroll > 0 ? 1 : -1;

        int dRow = 0;
        int dCol = 0;

        boolean shift = mc.player.isShiftKeyDown();
        int step = shift ? 3 : 1;

        if (xHeld) {
            dCol = dir * step;
        }
        if (yHeld) {
            dRow = dir * step;
        }

        FactotumPacketHandler.CHANNEL.sendToServer(
                new AltarScrollPacket(altarPos, dRow, dCol)
        );

        event.setCanceled(true);
    }

    private static BlockPos findAltarUnderCrosshair(Minecraft mc) {

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Vec3 end = eye.add(look.scale(6.0));

        BlockHitResult hit = mc.level.clip(new ClipContext(
                eye, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos pos = hit.getBlockPos();
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (be instanceof AltarBlockEntity) {
            return pos;
        }

        return null;
    }
}