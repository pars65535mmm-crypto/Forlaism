package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.screen.FusionMachineMenu;
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

public class FusionMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int ENERGY_CAPACITY = 50000;
    public static final int MAX_ENERGY_RECEIVE = 1000;
    public static final int FLUID_CAPACITY = 10000;
    public static final int ENERGY_PER_TICK = 20;
    public static final int MAX_PROGRESS = 50; // 50 ticks = 2.5秒で1回処理
    public static final int FO_PER_OPERATION = 100; // 100 mB

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(Items.FORLAISM_POLYCRYSTAL.get());
        }
    };

    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_RECEIVE);

    private final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.FO.get();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;

    public FusionMachineBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntities.FORLAISM_FUSION_MACHINE.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> FusionMachineBlockEntity.this.progress;
                    case 1 -> MAX_PROGRESS;
                    case 2 -> FusionMachineBlockEntity.this.energyStorage.getEnergyStored();
                    case 3 -> FusionMachineBlockEntity.this.energyStorage.getMaxEnergyStored();
                    case 4 -> FusionMachineBlockEntity.this.fluidTank.getFluidAmount();
                    case 5 -> FusionMachineBlockEntity.this.fluidTank.getCapacity();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> FusionMachineBlockEntity.this.progress = value;
                    case 2 -> FusionMachineBlockEntity.this.energyStorage.setEnergy(value);
                    case 4 -> FusionMachineBlockEntity.this.fluidTank.setFluid(new FluidStack(Fluids.FO.get(), value));
                }
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.forlaism.forlaism_fusion_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FusionMachineMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyFluidHandler = LazyOptional.of(() -> fluidTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("progress", progress);
        tag = fluidTank.writeToNBT(tag);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));
        progress = tag.getInt("progress");
        fluidTank.readFromNBT(tag);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionMachineBlockEntity entity) {
        if (level.isClientSide) return;

        if (hasRecipe(entity) && hasEnoughEnergy(entity) && hasSpaceForFluid(entity)) {
            entity.energyStorage.extractEnergyInternal(ENERGY_PER_TICK, false);
            entity.progress++;
            setChanged(level, pos, state);

            if (entity.progress >= MAX_PROGRESS) {
                craftItem(entity);
            }
        } else {
            if (entity.progress > 0) {
                entity.progress = 0;
                setChanged(level, pos, state);
            }
        }
    }

    private static boolean hasRecipe(FusionMachineBlockEntity entity) {
        ItemStack input = entity.itemHandler.getStackInSlot(0);
        return input.is(Items.FORLAISM_POLYCRYSTAL.get()) && input.getCount() > 0;
    }

    private static boolean hasEnoughEnergy(FusionMachineBlockEntity entity) {
        return entity.energyStorage.getEnergyStored() >= ENERGY_PER_TICK;
    }

    private static boolean hasSpaceForFluid(FusionMachineBlockEntity entity) {
        return entity.fluidTank.getCapacity() - entity.fluidTank.getFluidAmount() >= FO_PER_OPERATION;
    }

    private static void craftItem(FusionMachineBlockEntity entity) {
        entity.itemHandler.extractItem(0, 1, false);
        entity.fluidTank.fill(new FluidStack(Fluids.FO.get(), FO_PER_OPERATION), IFluidHandler.FluidAction.EXECUTE);
        entity.progress = 0;
    }
}
