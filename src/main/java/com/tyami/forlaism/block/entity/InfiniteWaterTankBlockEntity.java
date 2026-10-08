package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 無限水源タンクのBlockEntity。
 *
 * IFluidHandler を公開し、drain は常に無限に水を返す。
 * 内部タンクは持たず、常に満タン扱い。
 */
public class InfiniteWaterTankBlockEntity extends BlockEntity {

    /** 見た目用のダミータンク（容量だけ持たせる。実体は無限）。 */
    private final FluidTank visualTank = new FluidTank(Integer.MAX_VALUE) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == net.minecraft.world.level.material.Fluids.WATER;
        }

        // 常に満タン扱い
        @Override
        public FluidStack getFluid() {
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, Integer.MAX_VALUE);
        }

        @Override
        public int getFluidAmount() {
            return Integer.MAX_VALUE;
        }
    };

    /** 無限を返すIFluidHandler。 */
    private final IFluidHandler infiniteHandler = new IFluidHandler() {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, Integer.MAX_VALUE);
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid() == net.minecraft.world.level.material.Fluids.WATER;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // 受け入れない
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            if (resource.getFluid() != net.minecraft.world.level.material.Fluids.WATER) {
                return FluidStack.EMPTY;
            }
            // 要求された分を常に返す
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, resource.getAmount());
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, maxDrain);
        }
    };

    private LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.empty();

    public InfiniteWaterTankBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.INFINITE_WATER_TANK.get(), pos, state);
    }

    // =========================================================
    // Capability
    // =========================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side
    ) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluidHandler = LazyOptional.of(() -> infiniteHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluidHandler.invalidate();
    }

    // =========================================================
    // NBT（特に保存するものは無い）
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 無限なので保存不要
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 無限なので読込不要
    }

    // =========================================================
    // ドロップ
    // =========================================================

    public void drops() {
        // 中身は無限なのでアイテムとしてのドロップは無し
        SimpleContainer inv = new SimpleContainer(0);
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inv);
        }
    }
}