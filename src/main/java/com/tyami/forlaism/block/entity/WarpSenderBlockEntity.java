package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.block.WarpSenderBlock;
import com.tyami.forlaism.quantum.WarpDeliveryData;
import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.screen.InserterMenu;
import com.tyami.forlaism.util.CustomEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Warp送信機 BlockEntity。
 *
 * - 9スロットのアイテムインベントリ
 * - FE容量 1,000,000
 * - 送信元 / 送信先の座標を保持
 * - レッドストーン信号の立ち上がりで1スタック送信
 */
public class WarpSenderBlockEntity extends BlockEntity implements MenuProvider {

    // =========================================================
    // 定数
    // =========================================================

    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int MAX_ENERGY_RECEIVE = 10_000;

    /** 1アイテムあたりのFE消費係数（10 × 距離）。 */
    public static final int FE_PER_BLOCK = 10;

    /** 配送にかかる時間（tick）。5秒 = 100tick。 */
    public static final int DELIVERY_TIME_TICKS = 100;

    /** 発射クールダウン。 */
    public static final int COOLDOWN_TICKS = 20;

    // =========================================================
    // インベントリ / エネルギー
    // =========================================================

    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final CustomEnergyStorage energyStorage =
            new CustomEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_RECEIVE);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    // =========================================================
    // 送信元 / 送信先
    // =========================================================

    @Nullable
    private BlockPos senderSource = null;

    @Nullable
    private ResourceKey<Level> senderSourceDim = null;

    @Nullable
    private BlockPos targetPos = null;

    @Nullable
    private ResourceKey<Level> targetDim = null;

    // =========================================================
    // 状態
    // =========================================================

    private boolean wasPowered = false;
    private int cooldown = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> targetPos == null ? 0 : 1;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                energyStorage.setEnergy(value);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public WarpSenderBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.WARP_SENDER.get(), pos, state);
    }

    // =========================================================
    // 送信元 / 送信先 API
    // =========================================================

    public void setSenderSource(BlockPos pos, ResourceKey<Level> dim) {
        this.senderSource = pos.immutable();
        this.senderSourceDim = dim;
        setChanged();
    }

    public void setTarget(BlockPos pos, ResourceKey<Level> dim) {
        this.targetPos = pos.immutable();
        this.targetDim = dim;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean hasTarget() {
        return targetPos != null;
    }

    // =========================================================
    // Tick
    // =========================================================

    public static void tick(Level level, BlockPos pos, BlockState state, WarpSenderBlockEntity be) {
        if (level.isClientSide) return;

        // クールダウン
        if (be.cooldown > 0) {
            be.cooldown--;
            return;
        }

        boolean powered = state.getValue(WarpSenderBlock.POWERED);

        // 立ち上がりエッジ
        if (powered && !be.wasPowered) {
            be.launch((ServerLevel) level, pos, state);
        }

        be.wasPowered = powered;
    }

    // =========================================================
    // 送信処理
    // =========================================================

    private void launch(ServerLevel level, BlockPos pos, BlockState state) {

        // 送信先未設定
        if (targetPos == null || targetDim == null) {
            playError(level, pos, "§c送信先が設定されていません");
            return;
        }

        // 送信元未設定（このブロック自身を自動登録）
        if (senderSource == null || senderSourceDim == null) {
            senderSource = pos.immutable();
            senderSourceDim = level.dimension();
        }

        // アイテム取得（最初の非空スロット）
        int slot = findFirstNonEmptySlot();
        if (slot < 0) {
            playError(level, pos, "§c送信するアイテムがありません");
            return;
        }

        ItemStack stack = itemHandler.getStackInSlot(slot);

        // 距離計算（3次元直線距離）
        double dx = targetPos.getX() - pos.getX();
        double dy = targetPos.getY() - pos.getY();
        double dz = targetPos.getZ() - pos.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 必要FE: 10 × 距離 × スタック数
        int requiredFE = (int) Math.ceil(FE_PER_BLOCK * distance * stack.getCount());

        // FE不足チェック
        if (energyStorage.getEnergyStored() < requiredFE) {
            playError(level, pos,
                    "§cFEが足りません (必要: " + requiredFE + " FE, 現在: "
                            + energyStorage.getEnergyStored() + " FE)");
            return;
        }

        // FE消費
        energyStorage.extractEnergyInternal(requiredFE, false);

        // アイテムスタックを丸ごと取り出し（案X: スタックで飛ばす）
        ItemStack launched = itemHandler.extractItem(slot, stack.getCount(), false);

        // 演出: ゴーストItemEntityを飛ばす
        spawnGhostItem(level, pos, targetPos, launched, distance);

        // 実データ: 5秒後に到着するようスケジュール
        long arriveTick = level.getGameTime() + DELIVERY_TIME_TICKS;
        WarpDeliveryData.schedule(
                level.getServer(),
                targetPos,
                targetDim,
                launched,
                arriveTick
        );

        // 送信元演出
        level.sendParticles(
                ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                30,
                0.5, 0.5, 0.5,
                0.3
        );
        level.playSound(null, pos,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS,
                1.0F, 1.0F);

        // クールダウン
        cooldown = COOLDOWN_TICKS;
        setChanged();
    }

    /**
     * ゴーストItemEntityを飛ばす。
     *
     * - ピックアップ不可
     * - 重力なし
     * - 100tick後に消える
     */
    private void spawnGhostItem(ServerLevel level, BlockPos from, BlockPos to,
                                ItemStack stack, double distance) {

        ItemEntity ghost = new ItemEntity(
                level,
                from.getX() + 0.5,
                from.getY() + 1.0,
                from.getZ() + 0.5,
                stack.copy()
        );

        // ピックアップ不可・ノーグラビティ
        ghost.setNoGravity(true);
        ghost.setPickUpDelay(Integer.MAX_VALUE);
        ghost.setNeverPickUp();
        ghost.setNoGravity(true);

        // 速度計算: DELIVERY_TIME_TICKS で target に到達
        Vec3 delta = new Vec3(
                to.getX() - from.getX(),
                to.getY() - from.getY(),
                to.getZ() - from.getZ()
        ).scale(1.0 / DELIVERY_TIME_TICKS);

        ghost.setDeltaMovement(delta);
        ghost.hurtMarked = true;

        // ゴーストに寿命を持たせる → 100tick後に自動消滅
        ghost.getPersistentData().putBoolean("ForlaismWarpGhost", true);

        level.addFreshEntity(ghost);
    }

    private int findFirstNonEmptySlot() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void playError(ServerLevel level, BlockPos pos, String message) {
        level.playSound(null, pos,
                SoundEvents.DISPENSER_FAIL,
                SoundSource.BLOCKS,
                1.0F, 0.8F);

        level.getPlayers(p -> p.distanceToSqr(
                pos.getX(), pos.getY(), pos.getZ()) < 64)
                .forEach(p -> p.displayClientMessage(
                        Component.literal(message), true));
    }

    // =========================================================
    // MenuProvider
    // =========================================================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.forlaism.warp_sender");
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
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> cap, @Nullable Direction side) {

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());

        if (senderSource != null && senderSourceDim != null) {
            tag.putLong("senderSource", senderSource.asLong());
            tag.putString("senderSourceDim", senderSourceDim.location().toString());
        }

        if (targetPos != null && targetDim != null) {
            tag.putLong("targetPos", targetPos.asLong());
            tag.putString("targetDim", targetDim.location().toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energyStorage.setEnergy(tag.getInt("energy"));

        if (tag.contains("senderSource")) {
            senderSource = BlockPos.of(tag.getLong("senderSource"));
            senderSourceDim = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation(tag.getString("senderSourceDim"))
            );
        }

        if (tag.contains("targetPos")) {
            targetPos = BlockPos.of(tag.getLong("targetPos"));
            targetDim = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new ResourceLocation(tag.getString("targetDim"))
            );
        }
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
}