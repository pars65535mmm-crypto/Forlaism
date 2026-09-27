package com.tyami.forlaism.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RailgunLaserPacket {

    private final Vec3 start;
    private final Vec3 end;
    private final BlockPos voidPos;
    private final int voidDuration;
    private final double voidRadius;

    public RailgunLaserPacket(Vec3 start, Vec3 end,
                              BlockPos voidPos, int voidDuration, double voidRadius) {
        this.start = start;
        this.end = end;
        this.voidPos = voidPos;
        this.voidDuration = voidDuration;
        this.voidRadius = voidRadius;
    }

    public RailgunLaserPacket(FriendlyByteBuf buf) {
        this.start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.voidPos = buf.readBlockPos();
        this.voidDuration = buf.readInt();
        this.voidRadius = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(start.x); buf.writeDouble(start.y); buf.writeDouble(start.z);
        buf.writeDouble(end.x);   buf.writeDouble(end.y);   buf.writeDouble(end.z);
        buf.writeBlockPos(voidPos);
        buf.writeInt(voidDuration);
        buf.writeDouble(voidRadius);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.tyami.forlaism.client.renderer.RailgunLaserRenderer.spawn(start, end);
                    com.tyami.forlaism.world.VoidFieldManager.spawnClient(
                            voidPos, voidDuration, voidRadius
                    );
                })
        );
        ctx.get().setPacketHandled(true);
    }
}