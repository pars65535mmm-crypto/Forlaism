package com.tyami.forlaism.quantum;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ネットワーク番号ごとの仮想ストレージ。
 *
 * アイテムは複数種保持可能（LinkedHashMap で挿入順）。
 * 容量は実質 Integer.MAX_VALUE。
 */
public final class QuantumPool {

    public static final long MAX_ITEM_COUNT = Integer.MAX_VALUE;
    public static final int MAX_FLUID = Integer.MAX_VALUE;
    public static final int MAX_ENERGY = Integer.MAX_VALUE;

    /** アイテム: Item -> 個数（挿入順保持） */
    private final Map<Item, Long> items = new LinkedHashMap<>();

    private FluidStack fluid = FluidStack.EMPTY;
    private int energy = 0;

    // =========================================================
    // アイテム
    // =========================================================

    public synchronized ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        long current = items.getOrDefault(stack.getItem(), 0L);
        long space = MAX_ITEM_COUNT - current;
        if (space <= 0) return stack.copy();

        long toInsert = Math.min(stack.getCount(), space);
        if (!simulate) {
            items.put(stack.getItem(), current + toInsert);
        }

        ItemStack remainder = stack.copy();
        remainder.setCount((int) (stack.getCount() - toInsert));
        return remainder;
    }

    public synchronized ItemStack extractItem(Item item, int amount, boolean simulate) {
        long current = items.getOrDefault(item, 0L);
        if (current <= 0) return ItemStack.EMPTY;

        int toExtract = (int) Math.min(amount, Math.min(current, Integer.MAX_VALUE));
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (!simulate) {
            long remain = current - toExtract;
            if (remain <= 0) {
                items.remove(item);
            } else {
                items.put(item, remain);
            }
        }
        return new ItemStack(item, toExtract);
    }

    /** 挿入順を保持したスナップショット（複数種対応）。 */
    public synchronized List<Map.Entry<Item, Long>> snapshotEntries() {
        return new ArrayList<>(items.entrySet());
    }

    public synchronized Map<Item, Long> snapshotItems() {
        return new LinkedHashMap<>(items);
    }

    public synchronized boolean hasItem(Item item) {
        return items.getOrDefault(item, 0L) > 0;
    }

    public synchronized int getItemTypeCount() {
        return items.size();
    }

    // =========================================================
    // 液体
    // =========================================================

    public synchronized int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return 0;

        if (fluid.isEmpty()) {
            int toFill = Math.min(resource.getAmount(), MAX_FLUID);
            if (!simulate) fluid = new FluidStack(resource.getFluid(), toFill);
            return toFill;
        }

        if (!fluid.isFluidEqual(resource)) return 0;

        int space = MAX_FLUID - fluid.getAmount();
        if (space <= 0) return 0;

        int toFill = Math.min(resource.getAmount(), space);
        if (!simulate) fluid.setAmount(fluid.getAmount() + toFill);
        return toFill;
    }

    public synchronized FluidStack drain(int maxDrain, boolean simulate) {
        if (fluid.isEmpty()) return FluidStack.EMPTY;
        int toDrain = Math.min(maxDrain, fluid.getAmount());
        if (toDrain <= 0) return FluidStack.EMPTY;

        FluidStack result = new FluidStack(fluid.getFluid(), toDrain);
        if (!simulate) {
            fluid.setAmount(fluid.getAmount() - toDrain);
            if (fluid.getAmount() <= 0) fluid = FluidStack.EMPTY;
        }
        return result;
    }

    public synchronized FluidStack getFluid() {
        return fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
    }

    // =========================================================
    // FE
    // =========================================================

    public synchronized int receiveEnergy(int amount, boolean simulate) {
        int space = MAX_ENERGY - energy;
        if (space <= 0) return 0;
        int toReceive = Math.min(amount, space);
        if (!simulate) energy += toReceive;
        return toReceive;
    }

    public synchronized int extractEnergy(int amount, boolean simulate) {
        int toExtract = Math.min(amount, energy);
        if (toExtract <= 0) return 0;
        if (!simulate) energy -= toExtract;
        return toExtract;
    }

    public synchronized int getEnergy() {
        return energy;
    }

    // =========================================================
    // NBT
    // =========================================================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        synchronized (this) {
            ListTag itemList = new ListTag();
            for (Map.Entry<Item, Long> e : items.entrySet()) {
                if (e.getValue() <= 0) continue;
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(e.getKey());
                if (id == null) continue;
                CompoundTag t = new CompoundTag();
                t.putString("id", id.toString());
                t.putLong("count", e.getValue());
                itemList.add(t);
            }
            tag.put("Items", itemList);

            if (!fluid.isEmpty()) {
                CompoundTag ftag = new CompoundTag();
                ResourceLocation fid = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
                if (fid != null) {
                    ftag.putString("fluid", fid.toString());
                    ftag.putInt("amount", fluid.getAmount());
                    tag.put("Fluid", ftag);
                }
            }

            tag.putInt("Energy", energy);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        synchronized (this) {
            items.clear();
            fluid = FluidStack.EMPTY;
            energy = 0;

            ListTag itemList = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < itemList.size(); i++) {
                CompoundTag t = itemList.getCompound(i);
                ResourceLocation id = new ResourceLocation(t.getString("id"));
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null) {
                    long count = t.getLong("count");
                    if (count > 0) items.put(item, count);
                }
            }

            if (tag.contains("Fluid", Tag.TAG_COMPOUND)) {
                CompoundTag ftag = tag.getCompound("Fluid");
                ResourceLocation fid = new ResourceLocation(ftag.getString("fluid"));
                var f = ForgeRegistries.FLUIDS.getValue(fid);
                if (f != null) fluid = new FluidStack(f, ftag.getInt("amount"));
            }

            energy = tag.getInt("Energy");
        }
    }
}