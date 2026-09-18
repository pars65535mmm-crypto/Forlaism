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

import java.util.Map;

/**
 * 搬出側。
 *
 * プールから隣接ブロックへ強制的に吐き出す。
 * 通常の insertItem / fill / receiveEnergy で入らなければ
 * setStackInSlot で上書きしてでも入れる。
 */
public class QuantumTransferDeviceOutBlockEntity extends QuantumTransferDeviceBlockEntity {

    private static final int ENERGY_PER_TICK = 1_000_000;
    private static final int FLUID_PER_TICK = 1_000_000;
    private static final int ITEM_PER_TICK = 4096;

    public QuantumTransferDeviceOutBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.QUANTUM_TRANSFER_DEVICE_OUT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, QuantumTransferDeviceOutBlockEntity be) {
        if (level.isClientSide) return;

        var pool = be.getPool();

        // まず「普通に」押し出す
        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;


            // アイテム
neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).ifPresent(handler -> {
    var entries = pool.snapshotEntries();  // 挿入順リスト
    for (var e : entries) {
        long remain = e.getValue();
        while (remain > 0) {
            int chunk = (int) Math.min(remain, ITEM_PER_TICK);
            ItemStack stack = new ItemStack(e.getKey(), chunk);
            ItemStack remainder = insertForcefully(handler, stack);
            int inserted = chunk - remainder.getCount();
            if (inserted <= 0) break;

            pool.extractItem(e.getKey(), inserted, false);
            remain -= inserted;

            if (remainder.getCount() > 0) break;
        }
    }
});

            // 液体
            neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(handler -> {
                FluidStack poolFluid = pool.getFluid();
                if (poolFluid.isEmpty()) return;

                FluidStack toSend = new FluidStack(poolFluid.getFluid(), Math.min(poolFluid.getAmount(), FLUID_PER_TICK));
                int accepted = handler.fill(toSend, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) return;

                FluidStack drained = pool.drain(accepted, false);
                handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            });

            // FE
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(energy -> {
                int stored = pool.getEnergy();
                if (stored <= 0) return;

                int toSend = Math.min(stored, ENERGY_PER_TICK);
                int accepted = energy.receiveEnergy(toSend, true);
                if (accepted <= 0) return;

                pool.extractEnergy(accepted, false);
                energy.receiveEnergy(accepted, false);
            });
        }
    }

    /**
     * 通常 insertItem で入らなければ、空きスロットに setStackInSlot で強制挿入。
     */
    private static ItemStack insertForcefully(IItemHandler handler, ItemStack stack) {
        // まず素直に
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        if (remainder.isEmpty()) return ItemStack.EMPTY;

        // 空きスロットに強制挿入
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            ItemStack cur = handler.getStackInSlot(slot);
            if (!cur.isEmpty()) continue;

            int limit = handler.getSlotLimit(slot);
            int toPut = Math.min(remainder.getCount(), limit);
            handler.insertItem(slot, new ItemStack(remainder.getItem(), toPut), false);
            remainder.shrink(toPut);
        }

        return remainder;
    }
}