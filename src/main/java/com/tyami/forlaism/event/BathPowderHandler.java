package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber
public class BathPowderHandler {

    private static final List<PendingOnsen> PENDING =
            new ArrayList<>();

    public static void scheduleOnsen(
            ServerLevel level,
            BlockPos base,
            int delay
    ) {
        PENDING.add(
                new PendingOnsen(
                        level,
                        base,
                        delay
                )
        );
    }

    @SubscribeEvent
    public static void onServerTick(
            TickEvent.ServerTickEvent event
    ) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Iterator<PendingOnsen> iterator =
                PENDING.iterator();

        while (iterator.hasNext()) {

            PendingOnsen pending =
                    iterator.next();

            pending.ticks--;

            if (pending.ticks > 0) {
                continue;
            }

            createOnsen(
                    pending.level,
                    pending.base
            );

            iterator.remove();
        }
    }

    private static void createOnsen(
            ServerLevel level,
            BlockPos base
    ) {

        /*
         * 5x5
         * 高さ2
         */

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {

                for (int y = 0; y < 2; y++) {

                    BlockPos pos =
                            base.offset(x, y, z);

                    level.setBlock(
                            pos,
                            Fluids.ONSEN
                                    .get()
                                    .defaultFluidState()
                                    .createLegacyBlock(),
                            3
                    );
                }
            }
        }
    }

    private static class PendingOnsen {

        private final ServerLevel level;
        private final BlockPos base;
        private int ticks;

        private PendingOnsen(
                ServerLevel level,
                BlockPos base,
                int ticks
        ) {
            this.level = level;
            this.base = base;
            this.ticks = ticks;
        }
    }
}