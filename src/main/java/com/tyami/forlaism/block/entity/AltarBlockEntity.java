package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.block.AltarBlock;
import com.tyami.forlaism.recipe.AltarRecipe;
import com.tyami.forlaism.recipe.AltarRecipes;
import com.tyami.forlaism.registry.BlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 儀式祭壇の BlockEntity。
 *
 * 9x9 = 81 スロットにアイテムを保持し、
 * レシピが成立すると儀式アニメーションを経てクラフトする。
 *
 * カーソルシステム:
 *   X + スクロール → 列移動
 *   Y + スクロール → 行移動
 */
public class AltarBlockEntity extends BlockEntity {

    /** 総スロット数。9x9。 */
    public static final int GRID = 9;
    public static final int SLOTS = GRID * GRID;

    /** 各スロットの相対オフセット（ブロック中心基準、Yは浮遊高さ）。 */
    public static final float[][] SLOT_OFFSETS = buildOffsets();

    /** 儀式にかける総tick数。 */
    public static final int RITUAL_DURATION = 120; // 6秒

    // =========================================================
    // 状態
    // =========================================================

    /** スロットに置かれたアイテム。 */
    private final ItemStack[] items = new ItemStack[SLOTS];

    /** 現在のレシピ（あれば）。 */
    @Nullable
    private AltarRecipe activeRecipe;

    /** 儀式進行中のtick。0 = 停止。 */
    private int ritualTick = 0;

    /** 完成済みの出力アイテム（演出中に表示）。 */
    private ItemStack pendingOutput = ItemStack.EMPTY;

    /** 完成アイテムが出てきた後の保持tick。 */
    private int outputHoldTick = 0;

    /** カーソル位置（現在配置/回収対象のマス）。初期値は中央。 */
    private int cursorRow = 4;
    private int cursorCol = 4;

    // =========================================================
    // コンストラクタ
    // =========================================================

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.RITUAL_ALTAR.get(), pos, state);
        for (int i = 0; i < SLOTS; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    // =========================================================
    // スロット操作
    // =========================================================

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SLOTS) return ItemStack.EMPTY;
        return items[slot];
    }

    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) return;
        items[slot] = stack;
        setChanged();
        sync();
    }

    /** 最初の空きスロットを探す。 */
    public int findEmptySlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (items[i].isEmpty()) return i;
        }
        return -1;
    }

    /** 最初の非空スロットを探す（取り出し用）。 */
    public int findFilledSlot() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!items[i].isEmpty()) return i;
        }
        return -1;
    }

    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    /** 全スロットを空にする。 */
    public void clearAll() {
        for (int i = 0; i < SLOTS; i++) {
            items[i] = ItemStack.EMPTY;
        }
        setChanged();
        sync();
    }

    // =========================================================
    // カーソル
    // =========================================================

    public int getCursorRow() {
        return cursorRow;
    }

    public int getCursorCol() {
        return cursorCol;
    }

    public void setCursor(int row, int col) {
        this.cursorRow = Math.max(0, Math.min(GRID - 1, row));
        this.cursorCol = Math.max(0, Math.min(GRID - 1, col));
        setChanged();
        sync();
    }

    /** カーソル移動（deltaを加算してラップさせる）。 */
    public void moveCursor(int dRow, int dCol) {
        int r = cursorRow + dRow;
        int c = cursorCol + dCol;
        r = (r % GRID + GRID) % GRID;
        c = (c % GRID + GRID) % GRID;
        setCursor(r, c);
    }

    /** カーソルのスロットindex。 */
    public int getCursorSlot() {
        return cursorRow * GRID + cursorCol;
    }

    /** カーソル位置のアイテム。 */
    public ItemStack getCursorItem() {
        return getItem(getCursorSlot());
    }

    /** カーソル位置にアイテムを置く。成功したらtrue。 */
    public boolean placeAtCursor(ItemStack stack) {
        int slot = getCursorSlot();
        if (!items[slot].isEmpty()) return false;

        ItemStack copy = stack.copy();
        copy.setCount(1);
        items[slot] = copy;

        setChanged();
        sync();
        return true;
    }

    /** カーソル位置からアイテムを取り出す。 */
    public ItemStack takeFromCursor() {
        int slot = getCursorSlot();
        ItemStack s = items[slot];
        if (s.isEmpty()) return ItemStack.EMPTY;

        items[slot] = ItemStack.EMPTY;
        setChanged();
        sync();
        return s;
    }

    // =========================================================
    // レシピチェック
    // =========================================================

    @Nullable
    private AltarRecipe findMatchingRecipe() {
        for (AltarRecipe recipe : AltarRecipes.getAll()) {
            if (recipe.matches(items)) {
                return recipe;
            }
        }
        return null;
    }

    // =========================================================
    // Tick
    // =========================================================

    public static void tick(Level level, BlockPos pos, BlockState state, AltarBlockEntity be) {
        // カーソルパーティクルは両側で出す
        be.spawnCursorParticles(level, pos);

        if (level.isClientSide) {
            if (be.ritualTick > 0) be.ritualTick++;
            if (be.outputHoldTick > 0) be.outputHoldTick--;
            return;
        }

        // =========================================================
        // 儀式中
        // =========================================================
        if (be.ritualTick > 0) {
            be.ritualTick++;
            be.spawnRitualEffects((ServerLevel) level, pos);

            if (be.ritualTick >= RITUAL_DURATION + 20) {
                be.completeRitual((ServerLevel) level, pos);
            }
            return;
        }

        // =========================================================
        // 完成品ホールド中
        // =========================================================
        if (be.outputHoldTick > 0) {
            be.outputHoldTick--;
            return;
        }

        // =========================================================
        // 発動判定
        // =========================================================
        if (!be.isEmpty() && be.ritualTick == 0 && be.outputHoldTick == 0) {
            AltarRecipe recipe = be.findMatchingRecipe();
            if (recipe != null) {
                be.startRitual(recipe, (ServerLevel) level, pos);
            }
        }
    }

    // =========================================================
    // カーソル見える化
    // =========================================================

    private void spawnCursorParticles(Level level, BlockPos pos) {

        // 儀式中は出さない
        if (ritualTick > 0) return;

        float[] off = SLOT_OFFSETS[getCursorSlot()];
        double cx = pos.getX() + 0.5 + off[0];
        double cy = pos.getY() + off[1] + 0.15;
        double cz = pos.getZ() + 0.5 + off[2];

        double t = (level.getGameTime() % 40) / 40.0;
        double pulse = Math.sin(t * Math.PI) * 0.15;

        level.addParticle(
                ParticleTypes.END_ROD,
                cx, cy + pulse, cz,
                0, 0, 0
        );

        if (level.getGameTime() % 10 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2;
                double rx = Math.cos(angle) * 0.08;
                double rz = Math.sin(angle) * 0.08;
                level.addParticle(
                        ParticleTypes.END_ROD,
                        cx + rx, cy + pulse, cz + rz,
                        0, 0, 0
                );
            }
        }
    }

    // =========================================================
    // 儀式開始
    // =========================================================

    private void startRitual(AltarRecipe recipe, ServerLevel level, BlockPos pos) {
        this.activeRecipe = recipe;
        this.ritualTick = 1;

        level.playSound(null, pos,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 1.5F, 1.0F);

        level.playSound(null, pos,
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.BLOCKS, 1.0F, 1.5F);

        setChanged();
        sync();
    }

    // =========================================================
    // 儀式演出
    // =========================================================

    private void spawnRitualEffects(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.5;
        double cz = pos.getZ() + 0.5;

        float progress = Math.min(1.0F, ritualTick / (float) RITUAL_DURATION);

        // Phase 1: 螺旋収束
        if (progress < 0.5F) {
            for (int i = 0; i < 6; i++) {
                double angle = (ritualTick * 0.4 + i * (Math.PI * 2 / 6));
                double radius = 3.0 * (1.0 - progress * 2.0);
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                double y = cy + Math.sin(ritualTick * 0.3 + i) * 0.5;

                level.sendParticles(
                        ParticleTypes.ENCHANT,
                        x, y, z,
                        2, 0.05, 0.05, 0.05, 0.02);
            }
        }
        // Phase 2: ぎゅっ
        else if (progress < 0.75F) {
            float p = (progress - 0.5F) / 0.25F;
            double radius = 1.0 * (1.0 - p);

            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2 + ritualTick * 0.5;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        x, cy, z,
                        1, 0, 0, 0, 0);
            }

            if (ritualTick % 3 == 0) {
                level.sendParticles(
                        ParticleTypes.FLASH,
                        cx, cy, cz,
                        1, 0, 0, 0, 0);
            }
        }
        // Phase 3: 逆回転
        else {
            if (pendingOutput.isEmpty() && activeRecipe != null) {
                pendingOutput = activeRecipe.getOutput().copy();
            }

            float p = (progress - 0.75F) / 0.25F;

            for (int i = 0; i < 12; i++) {
                double angle = -(ritualTick * 0.8) + i * (Math.PI * 2 / 12);
                double radius = 0.8 * (1.0 - p * 0.5);
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        x, cy + Math.sin(ritualTick * 0.5) * 0.3, z,
                        1, 0, 0.02, 0, 0);
            }

            level.sendParticles(
                    ParticleTypes.FIREWORK,
                    cx, cy, cz,
                    3, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // =========================================================
    // 儀式完了
    // =========================================================

    private void completeRitual(ServerLevel level, BlockPos pos) {
        if (activeRecipe == null) {
            ritualTick = 0;
            return;
        }

        for (int i = 0; i < SLOTS; i++) {
            items[i] = ItemStack.EMPTY;
        }

        pendingOutput = activeRecipe.getOutput().copy();
        outputHoldTick = 100;

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.5;
        double cz = pos.getZ() + 0.5;

        level.playSound(null, pos,
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.BLOCKS, 1.5F, 1.2F);

        level.playSound(null, pos,
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                SoundSource.BLOCKS, 1.5F, 1.0F);

        for (double y = 0; y < 5; y += 0.2) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    cx, cy + y, cz,
                    2, 0.2, 0, 0.2, 0.05);
        }

        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                cx, cy, cz,
                1, 0, 0, 0, 0);

        activeRecipe = null;
        ritualTick = 0;
        setChanged();
        sync();
    }

    // =========================================================
    // 取り出し
    // =========================================================

    @Nullable
    public ItemStack extractOutput() {
        if (pendingOutput.isEmpty()) return null;
        ItemStack out = pendingOutput.copy();
        pendingOutput = ItemStack.EMPTY;
        outputHoldTick = 0;
        setChanged();
        sync();
        return out;
    }

    public boolean hasOutput() {
        return !pendingOutput.isEmpty();
    }

    public ItemStack peekOutput() {
        return pendingOutput;
    }

    public float getRitualProgress(float partialTick) {
        if (ritualTick <= 0) return 0.0F;
        return Math.min(1.0F, (ritualTick + partialTick) / (float) RITUAL_DURATION);
    }

    public boolean isCrafting() {
        return ritualTick > 0;
    }

    // =========================================================
    // 同期
    // =========================================================

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveClientData(tag);
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
        if (tag != null) loadClientData(tag);
    }

    private void saveClientData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            CompoundTag t = new CompoundTag();
            t.putByte("Slot", (byte) i);
            items[i].save(t);
            list.add(t);
        }
        tag.put("Items", list);

        tag.putInt("RitualTick", ritualTick);
        tag.putInt("OutputHoldTick", outputHoldTick);
        tag.putInt("CursorRow", cursorRow);
        tag.putInt("CursorCol", cursorCol);

        if (!pendingOutput.isEmpty()) {
            CompoundTag o = new CompoundTag();
            pendingOutput.save(o);
            tag.put("PendingOutput", o);
        }
    }

    private void loadClientData(CompoundTag tag) {
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < SLOTS; i++) items[i] = ItemStack.EMPTY;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int slot = t.getByte("Slot") & 0xFF;
            if (slot < SLOTS) items[slot] = ItemStack.of(t);
        }
        ritualTick = tag.getInt("RitualTick");
        outputHoldTick = tag.getInt("OutputHoldTick");
        cursorRow = tag.contains("CursorRow") ? tag.getInt("CursorRow") : 4;
        cursorCol = tag.contains("CursorCol") ? tag.getInt("CursorCol") : 4;
        pendingOutput = tag.contains("PendingOutput")
                ? ItemStack.of(tag.getCompound("PendingOutput"))
                : ItemStack.EMPTY;
    }

    // =========================================================
    // 永続NBT
    // =========================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveClientData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadClientData(tag);
    }

    // =========================================================
    // ドロップ
    // =========================================================

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(SLOTS);
        for (int i = 0; i < SLOTS; i++) inv.setItem(i, items[i]);
        Containers.dropContents(level, worldPosition, inv);

        if (!pendingOutput.isEmpty()) {
            Containers.dropItemStack(level,
                    worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    pendingOutput);
        }
    }

    // =========================================================
    // スロット位置
    // =========================================================

    private static float[][] buildOffsets() {
        float[][] out = new float[SLOTS][3];
        float step = 0.11F;
        int idx = 0;
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                float x = (col - (GRID - 1) / 2.0F) * step;
                float z = (row - (GRID - 1) / 2.0F) * step;
                out[idx][0] = x;
                out[idx][1] = 1.2F;
                out[idx][2] = z;
                idx++;
            }
        }
        return out;
    }
}