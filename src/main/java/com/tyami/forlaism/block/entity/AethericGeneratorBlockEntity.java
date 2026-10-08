package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.math.BigInteger;

/**
 * 電核機（Aetheric Generator）。
 *
 * - スロット1つ（アイテムによって発電量が変化）
 * - 内部バッファは BigInteger（理論上無限）
 * - 隣接には int ずつジャバジャバ引き出す
 * - セットしたアイテムは1秒（20tick）で消滅する（燃料消費）
 */
public class AethericGeneratorBlockEntity
        extends BlockEntity
        implements GeoBlockEntity {

    // =========================================================
    // 定数
    // =========================================================

    /** 1E+308 相当。 */
    private static final BigInteger MAX_OUTPUT =
            new BigInteger("1" + "0".repeat(308));

    /** 燃料が燃え切るまでのtick。1秒 = 20tick。 */
    public static final int BURN_TICKS = 20;

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("idle");

    private static final RawAnimation KEIZOKU =
            RawAnimation.begin().thenLoop("keizoku");

    // =========================================================
    // インベントリ
    // =========================================================

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return getOutputFor(stack.getItem()) != null;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // =========================================================
    // 内部バッファ（BigInteger）
    // =========================================================

    private BigInteger internalBuffer = BigInteger.ZERO;

    /** 残り燃焼tick。0になったら中身を消す。 */
    private int burnTicks = 0;

    private final IEnergyStorage energyStorage = new IEnergyStorage() {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;
            BigInteger req = BigInteger.valueOf(maxExtract);
            BigInteger give = internalBuffer.min(req);
            if (!simulate) {
                internalBuffer = internalBuffer.subtract(give);
                setChanged();
            }
            return give.intValue();
        }

        @Override
        public int getEnergyStored() {
            if (internalBuffer.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                return Integer.MAX_VALUE;
            }
            return internalBuffer.intValue();
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    // =========================================================
    // GeckoLib
    // =========================================================

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public AethericGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.AETHERIC_GENERATOR.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main_controller",
                0,
                this::predicate
        ));
    }

    private <E extends AethericGeneratorBlockEntity>
    PlayState predicate(AnimationState<E> state) {

        if (isRunning()) {
            state.getController().setAnimation(KEIZOKU);
        } else {
            state.getController().setAnimation(IDLE);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // =========================================================
    // 稼働判定
    // =========================================================

    public boolean isRunning() {
        return getCurrentOutput() != null;
    }

    @Nullable
    public BigInteger getCurrentOutput() {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return null;
        return getOutputFor(stack.getItem());
    }

    // =========================================================
    // 発電量テーブル
    // =========================================================

    @Nullable
    public static BigInteger getOutputFor(Item item) {

        if (item == Items.KORONEKO.get()) {
            return BigInteger.valueOf(100_000L);
        }
        if (item == Items.GAMING_MASTER_BLADE.get()) {
            return MAX_OUTPUT;
        }

        return null;
    }

    // =========================================================
    // Tick（発電）
    // =========================================================

    public static void tick(Level level, BlockPos pos, BlockState state,
                            AethericGeneratorBlockEntity be) {

        if (level.isClientSide) return;

        BigInteger output = be.getCurrentOutput();
        if (output == null) {
            // アイテムが無ければ何もしない
            be.burnTicks = 0;
            return;
        }

        // =========================================================
        // 燃料カウント開始（初回だけセット）
        // =========================================================
        if (be.burnTicks <= 0) {
            be.burnTicks = BURN_TICKS;
        }

        // 発電
        be.internalBuffer = be.internalBuffer.add(output);
        be.pushEnergy();

        // 燃焼カウントダウン
        be.burnTicks--;

        // 燃え切ったら中身を消す
        if (be.burnTicks <= 0) {
            be.itemHandler.extractItem(0, 64, false);
            be.burnTicks = 0;
            be.setChanged();

            // 消滅演出
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5,
                        pos.getY() + 1.2,
                        pos.getZ() + 0.5,
                        10,
                        0.2, 0.2, 0.2,
                        0.02
                );
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        5,
                        0.15, 0.15, 0.15,
                        0.01
                );
            }
        }

        be.setChanged();
    }

    /**
     * 隣接ブロックへ int 単位で全力プッシュ。
     */
    private void pushEnergy() {

        if (level == null) return;
        if (internalBuffer.signum() <= 0) return;

        for (Direction dir : Direction.values()) {

            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null) continue;

            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite())
                    .ifPresent(target -> {

                        while (internalBuffer.signum() > 0) {

                            int chunk = internalBuffer.min(
                                    BigInteger.valueOf(Integer.MAX_VALUE)
                            ).intValue();

                            if (chunk <= 0) break;

                            int accepted = target.receiveEnergy(chunk, false);
                            if (accepted <= 0) break;

                            internalBuffer = internalBuffer.subtract(
                                    BigInteger.valueOf(accepted)
                            );
                        }
                    });
        }
    }

    // =========================================================
    // Capability
    // =========================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side
    ) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergy = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergy.invalidate();
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putString("energy_big", internalBuffer.toString());
        tag.putInt("burn_ticks", burnTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));

        try {
            internalBuffer = new BigInteger(tag.getString("energy_big"));
        } catch (Exception e) {
            internalBuffer = BigInteger.ZERO;
        }

        burnTicks = tag.getInt("burn_ticks");
    }

    // =========================================================
    // ドロップ
    // =========================================================

    public void drops() {
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inv);
        }
    }
}