package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.util.CustomEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** Shared FE/FO/inventory implementation for Tier 2 processing machines. */
public abstract class Tier2MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 50000;
    public static final int MAX_ENERGY_RECEIVE = 1000;
    public static final int FLUID_CAPACITY = 10000;
    protected final int maxProgress;
    protected final int energyPerTick;
    protected final int fluidPerOperation;
    protected final Predicate<ItemStack> inputPredicate;
    protected final ItemStack output;
    private final String translationKey;
    protected int progress;

    protected final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == 0 && inputPredicate.test(stack);
        }
    };
    protected CustomEnergyStorage energyStorage =
        new CustomEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_RECEIVE);
    protected final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override protected void onContentsChanged() { setChanged(); }
        @Override public boolean isFluidValid(FluidStack stack) { return stack.getFluid() == Fluids.FO.get(); }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.empty();
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.empty();
    protected final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch (index) {
            case 0 -> progress; case 1 -> maxProgress; case 2 -> energyStorage.getEnergyStored();
            case 3 -> energyStorage.getMaxEnergyStored(); case 4 -> fluidTank.getFluidAmount();
            case 5 -> fluidTank.getCapacity(); default -> 0; }; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; else if (index == 2) energyStorage.setEnergy(value); }
        @Override public int getCount() { return 6; }
    };

    protected Tier2MachineBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state,
                                      Predicate<ItemStack> inputPredicate, ItemStack output, int maxProgress, int energyPerTick, int fluidPerOperation, String translationKey) {
        super(type, pos, state); this.inputPredicate = inputPredicate; this.output = output;
        this.maxProgress = maxProgress; this.energyPerTick = energyPerTick; this.fluidPerOperation = fluidPerOperation; this.translationKey = translationKey;
    }

    @Override public Component getDisplayName() { return Component.translatable(translationKey); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.tyami.forlaism.screen.Tier2MachineMenu(id, inv, this, data);
    }
    public ContainerData getData() { return data; }

public static void tick(Level level, BlockPos pos, BlockState state, Tier2MachineBlockEntity machine) {
    if (level.isClientSide) return;

    ItemStack input = machine.itemHandler.getStackInSlot(0);
    ItemStack result = machine.itemHandler.getStackInSlot(1);

    // 入力アイテムに応じた今回の加工結果
    ItemStack operationOutput = machine.getOutputForInput(input);

    boolean canOutput =
            result.isEmpty()
                    || (ItemStack.isSameItem(result, operationOutput)
                    && result.getCount() < result.getMaxStackSize());

    if (!input.isEmpty()
            && machine.inputPredicate.test(input)
            && canOutput
            && machine.fluidTank.getFluidAmount() >= machine.fluidPerOperation
            && machine.energyStorage.getEnergyStored() >= machine.energyPerTick) {

        machine.energyStorage.extractEnergyInternal(
                machine.energyPerTick,
                false
        );

        machine.progress++;

        if (machine.progress >= machine.maxProgress) {

            // 入力を1個消費
            machine.itemHandler.extractItem(0, 1, false);

            // FOを消費
            machine.fluidTank.drain(
                    machine.fluidPerOperation,
                    IFluidHandler.FluidAction.EXECUTE
            );

            // 出力
            if (result.isEmpty()) {
                machine.itemHandler.setStackInSlot(
                        1,
                        operationOutput.copy()
                );
            } else {
                result.grow(1);
            }

            machine.progress = 0;
        }

        setChanged(level, pos, state);

    } else if (machine.progress > 0) {

        machine.progress = 0;
        setChanged(level, pos, state);
    }
}

    @Override public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.ENERGY) return energyCapability.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); itemCapability = LazyOptional.of(() -> itemHandler); energyCapability = LazyOptional.of(() -> energyStorage); fluidCapability = LazyOptional.of(() -> fluidTank); }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); energyCapability.invalidate(); fluidCapability.invalidate(); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("inventory", itemHandler.serializeNBT()); tag.putInt("energy", energyStorage.getEnergyStored()); tag.putInt("progress", progress); fluidTank.writeToNBT(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); itemHandler.deserializeNBT(tag.getCompound("inventory")); energyStorage.setEnergy(tag.getInt("energy")); progress = tag.getInt("progress"); fluidTank.readFromNBT(tag); }
    public void drops() { SimpleContainer inventory = new SimpleContainer(2); inventory.setItem(0, itemHandler.getStackInSlot(0)); inventory.setItem(1, itemHandler.getStackInSlot(1)); if (level != null) Containers.dropContents(level, worldPosition, inventory); }

protected ItemStack getOutputForInput(ItemStack input) {
    return output;
}

}
