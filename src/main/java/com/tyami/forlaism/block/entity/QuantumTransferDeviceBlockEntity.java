package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.quantum.QuantumTransferNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 量子転送機の共通BlockEntity。
 *
 * - ネットワーク番号 (1〜255)
 * - プール参照
 * - FE/液体/アイテムのCapabilityを公開
 */
public abstract class QuantumTransferDeviceBlockEntity extends BlockEntity {

    private int network = 1;

    /** プールのCapabilityラッパー。 */
    private final PoolEnergyStorage energyStorage = new PoolEnergyStorage();
    private final PoolFluidHandler fluidHandler = new PoolFluidHandler();
    private final PoolItemHandler itemHandler = new PoolItemHandler();

    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.empty();
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();

    public QuantumTransferDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getNetwork() {
        return network;
    }

    public void setNetwork(int network) {
        this.network = QuantumTransferNetwork.clamp(network);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void cycleNetwork(int delta) {
        setNetwork(delta > 0
                ? QuantumTransferNetwork.next(network)
                : QuantumTransferNetwork.prev(network));
    }

    protected com.tyami.forlaism.quantum.QuantumPool getPool() {
        return QuantumTransferNetwork.getPool(network);
    }

    // =========================================================
    // Capability
    // =========================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        energyCap = LazyOptional.of(() -> energyStorage);
        fluidCap = LazyOptional.of(() -> fluidHandler);
        itemCap = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        fluidCap.invalidate();
        itemCap.invalidate();
    }

    // =========================================================
    // NBT / 同期
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Network", network);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        network = QuantumTransferNetwork.clamp(tag.getInt("Network"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Network", network);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            network = QuantumTransferNetwork.clamp(tag.getInt("Network"));
        }
    }

    // =========================================================
    // 内部 Capability 実装（プールへ委譲）
    // =========================================================

    private class PoolEnergyStorage implements IEnergyStorage {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return getPool().receiveEnergy(maxReceive, simulate);
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return getPool().extractEnergy(maxExtract, simulate);
        }
        @Override public int getEnergyStored() {
            return getPool().getEnergy();
        }
        @Override public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    }

    private class PoolFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public @NotNull FluidStack getFluidInTank(int tank) { return getPool().getFluid(); }
        @Override public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }
        @Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return true; }
        @Override public int fill(FluidStack resource, FluidAction action) {
            return getPool().fill(resource, action.simulate());
        }
        @Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            FluidStack cur = getPool().getFluid();
            if (cur.isEmpty() || !cur.isFluidEqual(resource)) return FluidStack.EMPTY;
            return getPool().drain(resource.getAmount(), action.simulate());
        }
        @Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return getPool().drain(maxDrain, action.simulate());
        }
    }

private class PoolItemHandler implements IItemHandler {
    @Override
    public int getSlots() {
        // 最低1、プールの種類数
        return Math.max(1, getPool().getItemTypeCount());
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        var entries = getPool().snapshotEntries();
        if (slot < 0 || slot >= entries.size()) return ItemStack.EMPTY;
        var e = entries.get(slot);
        long count = Math.min(e.getValue(), Integer.MAX_VALUE);
        return new ItemStack(e.getKey(), (int) Math.min(count, 64));
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return getPool().insertItem(stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        var entries = getPool().snapshotEntries();
        if (slot < 0 || slot >= entries.size()) return ItemStack.EMPTY;
        var e = entries.get(slot);
        return getPool().extractItem(e.getKey(), amount, simulate);
    }
}
}