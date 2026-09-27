package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.screen.InserterMenu;
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
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InserterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int FLUID_CAPACITY = 10000;
    public static final int TRANSFER_RATE = 20; // 20 mB/tick

    // =========================================================
    // 量子融合モード用
    // =========================================================

    /** 量子融合機としての最大FE容量: int限界（約2.14 GFE） */
    public static final int QUANTUM_FE_CAPACITY = Integer.MAX_VALUE;

    /** 量子融合モード時のFE入力レート（tick毎）: int限界 */
    public static final int QUANTUM_FE_MAX_RECEIVE = Integer.MAX_VALUE;

    private boolean quantumFusionMode = false;
    /** 輪廻再転式量子融合炉モード。 */
private boolean reactorMode = false;

    private final CustomEnergyStorage quantumEnergyStorage =
            new CustomEnergyStorage(
                    QUANTUM_FE_CAPACITY,
                    QUANTUM_FE_MAX_RECEIVE,
                    QUANTUM_FE_MAX_RECEIVE
            );

    // =========================================================
    // 既存: アイテム/液体
    // =========================================================

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()
                    || stack.is(Items.TOKINO_STAFF_TIER1.get());
        }
    };

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
    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyQuantumEnergy = LazyOptional.empty();

    protected final ContainerData data;

    public InserterBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntities.FORLAISM_INSERTER.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> InserterBlockEntity.this.fluidTank.getFluidAmount();
                    case 1 -> InserterBlockEntity.this.fluidTank.getCapacity();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    InserterBlockEntity.this.fluidTank.setFluid(new FluidStack(Fluids.FO.get(), value));
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    // =========================================================
    // 量子融合モード API
    // =========================================================

    public boolean isQuantumFusionMode() {
        return quantumFusionMode;
    }

    public void setQuantumFusionMode(boolean mode) {
        this.quantumFusionMode = mode;
        setChanged();

        // クライアントへ同期
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isReactorMode() {
    return reactorMode;
}

public void setReactorMode(boolean mode) {
    this.reactorMode = mode;
    setChanged();

    if (level != null && !level.isClientSide) {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
}

    public CustomEnergyStorage getQuantumEnergyStorage() {
        return quantumEnergyStorage;
    }

    // =========================================================
    // MenuProvider
    // =========================================================

    @Override
    public Component getDisplayName() {
        // 量子融合モード時は表示名を切替
        if (quantumFusionMode) {
            return Component.translatable("block.forlaism.quantum_fusion_machine");
        }
        return Component.translatable("block.forlaism.forlaism_inserter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new InserterMenu(id, inventory, this, this.data);
    }

    // =========================================================
    // Capability
    // =========================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {

        // 量子融合モード時: Energy を公開
if ((quantumFusionMode || reactorMode) && cap == ForgeCapabilities.ENERGY) {
    return lazyQuantumEnergy.cast();
}

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
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
        lazyFluidHandler = LazyOptional.of(() -> fluidTank);
        lazyQuantumEnergy = LazyOptional.of(() -> quantumEnergyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
        lazyQuantumEnergy.invalidate();
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag = fluidTank.writeToNBT(tag);
        tag.putBoolean("QuantumFusionMode", quantumFusionMode);
        tag.putBoolean("ReactorMode", reactorMode);
        tag.putInt("QuantumFE", quantumEnergyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        fluidTank.readFromNBT(tag);
        quantumFusionMode = tag.getBoolean("QuantumFusionMode");
        reactorMode = tag.getBoolean("ReactorMode");
        quantumEnergyStorage.setEnergy(tag.getInt("QuantumFE"));
    }

    // =========================================================
    // ドロップ
    // =========================================================

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    // =========================================================
    // Tick
    // =========================================================

    public static void tick(Level level, BlockPos pos, BlockState state, InserterBlockEntity entity) {
        if (level.isClientSide) return;

        // 量子融合モード中はFO充填機能を停止（加工ロジックは後で追加）
        if (entity.quantumFusionMode) {
            // TODO: 量子融合の加工ロジック
            return;
        }

        // 通常モード: FOをアイテムに充填
        if (entity.fluidTank.getFluidAmount() > 0) {
            ItemStack stack = entity.itemHandler.getStackInSlot(0);
            if (!stack.isEmpty()) {
                IFluidHandlerItem itemFluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                if (itemFluidHandler != null) {
                    int toTransfer = Math.min(TRANSFER_RATE, entity.fluidTank.getFluidAmount());
                    int simulatedFilled = itemFluidHandler.fill(
                            new FluidStack(Fluids.FO.get(), toTransfer),
                            IFluidHandler.FluidAction.SIMULATE
                    );
                    if (simulatedFilled > 0) {
                        FluidStack drained = entity.fluidTank.drain(simulatedFilled, IFluidHandler.FluidAction.EXECUTE);
                        itemFluidHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        entity.itemHandler.setStackInSlot(0, itemFluidHandler.getContainer());
                        setChanged(level, pos, state);
                    }
                }
            }
        }
    }
}