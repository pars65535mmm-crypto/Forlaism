package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

/**
 * 搬入側。
 *
 * 隣接ブロックの IItemHandler / IFluidHandler / IEnergyStorage から
 * 吸い出してプールへ入れる。
 */
public class QuantumTransferDeviceInBlockEntity extends QuantumTransferDeviceBlockEntity {

    private static final int ENERGY_PER_TICK = 1_000_000;
    private static final int FLUID_PER_TICK = 1_000_000;
    private static final int ITEM_PER_TICK = 4096;

    public QuantumTransferDeviceInBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.QUANTUM_TRANSFER_DEVICE_IN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, QuantumTransferDeviceInBlockEntity be) {
        if (level.isClientSide) return;

        var pool = be.getPool();

        // 6方向すべての隣接ブロックを舐める
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            // アイテム
            neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(handler -> {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack simulated = handler.extractItem(slot, ITEM_PER_TICK, true);
                    if (simulated.isEmpty()) continue;

                    ItemStack remainder = pool.insertItem(simulated, true);
                    int canMove = simulated.getCount() - remainder.getCount();
                    if (canMove <= 0) continue;

                    ItemStack extracted = handler.extractItem(slot, canMove, false);
                    pool.insertItem(extracted, false);
                }
            });

            // 液体
            neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(handler -> {
                FluidStack simulated = handler.drain(FLUID_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
                if (simulated.isEmpty()) return;

                int accepted = pool.fill(simulated, true);
                if (accepted <= 0) return;

                FluidStack drained = handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                pool.fill(drained, false);
            });

            // FE
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(energy -> {
                int simulated = energy.extractEnergy(ENERGY_PER_TICK, true);
                if (simulated <= 0) return;

                int accepted = pool.receiveEnergy(simulated, true);
                if (accepted <= 0) return;

                energy.extractEnergy(accepted, false);
                pool.receiveEnergy(accepted, false);
            });
        }
    }
}