package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.PhotonChemicals;
import com.tyami.forlaism.util.CustomEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.common.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Photon machines. Photon amounts are stored as microEph (1000 microEph = 1 mEph). */
public class PhotonMachineBlockEntity extends BlockEntity implements MenuProvider {
    public enum Kind { COLLECTOR, DECAY, BLASTER, CONDENSER }
    public static final long MICRO_PER_MEPH = 1000L;
    private final Kind kind;
    private long photons;
    /** Amount currently mirrored into the Mekanism tank, in mEph transport units. */
    private long mirroredGasAmount;
    private int progress;
    private final CustomEnergyStorage energyStorage;
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isValidInput(stack); }
        @Override public int getSlotLimit(int slot) { return kind == Kind.BLASTER || kind == Kind.CONDENSER ? 1 : 64; }
    };
    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();
    private LazyOptional<IGasHandler> gasCap = LazyOptional.empty();
    private final IGasTank gasTank;
    private final IGasHandler gasHandler;
    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch (index) {
            case 0 -> progress; case 1 -> getMaxProgress(); case 2 -> energyStorage.getEnergyStored();
            case 3 -> energyStorage.getMaxEnergyStored();
            // Photon values can exceed an int (up to 999 MEph), so send a 0..1000 fill ratio.
            case 4 -> (int)Math.min(1000L, Math.round(photons * 1000.0 / getPhotonCapacity()));
            case 5 -> 1000; default -> 0; }; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; else if (index == 2) energyStorage.setEnergy(value); }
        @Override public int getCount() { return 6; }
    };
    public PhotonMachineBlockEntity(BlockPos pos, BlockState state, Kind kind) {
        super(BlockEntities.PHOTON_MACHINE.get(), pos, state); this.kind = resolveKind(state, kind);
        this.energyStorage = new CustomEnergyStorage(getEnergyCapacity(), Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.gasTank = ChemicalTankBuilder.GAS.create(gasCapacity(), gas -> gas == PhotonChemicals.PHOTON.get(), this::onGasChanged);
        this.gasHandler = new IGasHandler() {
            @Override public int getTanks() { return 1; }
            @Override public GasStack getChemicalInTank(int tank) { return tank == 0 ? gasTank.getStack() : GasStack.EMPTY; }
            @Override public void setChemicalInTank(int tank, GasStack stack) { if (tank == 0) gasTank.setStack(stack); }
            @Override public long getTankCapacity(int tank) { return tank == 0 ? gasTank.getCapacity() : 0; }
            @Override public boolean isValid(int tank, GasStack stack) { return tank == 0 && gasTank.isValid(stack); }
            @Override public GasStack insertChemical(int tank, GasStack stack, Action action) { return tank == 0 ? gasTank.insert(stack, action, mekanism.api.AutomationType.EXTERNAL) : stack; }
            @Override public GasStack extractChemical(int tank, long amount, Action action) { return tank == 0 ? gasTank.extract(amount, action, mekanism.api.AutomationType.EXTERNAL) : GasStack.EMPTY; }
        };
    }
    private void onGasChanged() { setChanged(); syncClient(); }
    private static Kind resolveKind(BlockState state, Kind fallback) {
        if (state.is(com.tyami.forlaism.registry.Blocks.PHOTON_DECAY_CHAMBER.get())) return Kind.DECAY;
        if (state.is(com.tyami.forlaism.registry.Blocks.PHOTON_BLASTER_INFUSER.get())) return Kind.BLASTER;
        if (state.is(com.tyami.forlaism.registry.Blocks.PHOTON_CONDENSER.get())) return Kind.CONDENSER;
        return fallback;
    }
    public Kind getKind() { return kind; }
    public ContainerData getData() { return data; }
    public ItemStackHandler getInventory() { return inventory; }
    public long getPhotons() { return photons; }
    public long getPhotonCapacity() { return switch (kind) { case COLLECTOR -> 3_000L; case DECAY -> 10_000_000_000_000L; case BLASTER -> 2_500_000_000L; case CONDENSER -> 999_000_000_000_000L; }; }
    private long gasCapacity() { return Math.max(1L, (getPhotonCapacity() + MICRO_PER_MEPH - 1L) / MICRO_PER_MEPH); }
    private int getEnergyCapacity() { return switch (kind) { case COLLECTOR -> 200_000; case DECAY -> 3_000_000; case BLASTER -> 560_000; case CONDENSER -> 5_000_000; }; }
    public int getMaxProgress() { if (kind == Kind.DECAY) return Math.max(1, (int)Math.ceil(value(inventory.getStackInSlot(0)) * 6.02 / 1000.0)); return switch (kind) { case BLASTER -> 10; case CONDENSER -> 50; default -> 1; }; }
    private boolean isValidInput(ItemStack stack) {
        return switch (kind) {
            case COLLECTOR -> false;
            case DECAY -> isCrystal(stack);
            case BLASTER -> stack.is(Items.KAIRO_SOTAI.get()) || isBlasterCircuit(stack);
            case CONDENSER -> isCondenserCrystal(stack);
        };
    }
    private static boolean isCrystal(ItemStack s) { return s.is(Items.PHOTON_TINY_CRYSTAL.get()) || s.is(Items.PHOTON_SMALL_CRYSTAL.get()) || s.is(Items.PHOTON_CRYSTAL.get()) || s.is(Items.PHOTON_HIGH_CRYSTAL.get()) || s.is(Items.PHOTON_HIGH_PURITY_CRYSTAL.get()) || s.is(Items.PHOTON_MAXIMAL_HIGH_PURITY_CRYSTAL.get()) || s.is(Items.PHOTON_CRITICAL_CRYSTAL.get()); }
    private static boolean isCondenserCrystal(ItemStack s) { return isCrystal(s) && !s.is(Items.PHOTON_CRITICAL_CRYSTAL.get()); }
    private static boolean isCircuit(ItemStack s) { return isBlasterCircuit(s) || s.is(Items.HEAVY_PHOTON_CIRCUIT.get()); }
    private static boolean isBlasterCircuit(ItemStack s) { return s.is(Items.GENERAL_CIRCUIT.get()) || s.is(Items.GENERAL_PHOTON_CIRCUIT.get()) || s.is(Items.PHOTON_CIRCUIT.get()) || s.is(Items.ADVANCED_PHOTON_CIRCUIT.get()) || s.is(Items.REFINED_PHOTON_CIRCUIT.get()) || s.is(Items.HIGH_DENSITY_PHOTON_CIRCUIT.get()); }
    private static long value(ItemStack s) { if (s.is(Items.PHOTON_TINY_CRYSTAL.get())) return 3_000L; if (s.is(Items.PHOTON_SMALL_CRYSTAL.get())) return 10_000L; if (s.is(Items.PHOTON_CRYSTAL.get())) return 100_000L; if (s.is(Items.PHOTON_HIGH_CRYSTAL.get())) return 1_000_000L; if (s.is(Items.PHOTON_HIGH_PURITY_CRYSTAL.get())) return 1_000_000_000L; if (s.is(Items.PHOTON_MAXIMAL_HIGH_PURITY_CRYSTAL.get())) return 1_000_000_000_000L; if (s.is(Items.PHOTON_CRITICAL_CRYSTAL.get())) return 1_000_000_000_000_000L; return 0; }
    private static Item crystal(long amount) { if (amount >= 1_000_000_000_000_000L) return Items.PHOTON_CRITICAL_CRYSTAL.get(); if (amount >= 1_000_000_000_000L) return Items.PHOTON_MAXIMAL_HIGH_PURITY_CRYSTAL.get(); if (amount >= 1_000_000_000L) return Items.PHOTON_HIGH_PURITY_CRYSTAL.get(); if (amount >= 1_000_000L) return Items.PHOTON_HIGH_CRYSTAL.get(); if (amount >= 100_000L) return Items.PHOTON_CRYSTAL.get(); if (amount >= 10_000L) return Items.PHOTON_SMALL_CRYSTAL.get(); return Items.PHOTON_TINY_CRYSTAL.get(); }
    public static void tick(Level level, BlockPos pos, BlockState state, PhotonMachineBlockEntity be) {
        if (level.isClientSide) return;
        be.acceptTransportedPhotons();
        if (be.kind == Kind.COLLECTOR) {
            int light = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos.above());
            int cost = light * 10;
            if (light > 0 && be.energyStorage.getEnergyStored() >= cost && be.photons < be.getPhotonCapacity()) { be.energyStorage.extractEnergyInternal(cost, false); be.photons = Math.min(be.getPhotonCapacity(), be.photons + light); }
            ItemStack output = be.inventory.getStackInSlot(0);
            if (be.photons >= 3_000L && (output.isEmpty() || output.is(Items.PHOTON_TINY_CRYSTAL.get())) && output.getCount() < 64) {
                int amount = (int)Math.min(be.photons / 3_000L, 64L - output.getCount());
                be.photons -= amount * 3_000L;
                if (output.isEmpty()) be.inventory.setStackInSlot(0, new ItemStack(Items.PHOTON_TINY_CRYSTAL.get(), amount));
                else output.grow(amount);
            }
            be.syncGas(); be.setChanged(); return;
        }
        ItemStack in = be.inventory.getStackInSlot(0); if (in.isEmpty()) { be.progress = 0; be.syncGas(); return; }
        if (be.kind == Kind.BLASTER && !be.isValidInput(in)) { be.progress = 0; be.syncGas(); return; }
        if (be.kind == Kind.CONDENSER && !be.isValidInput(in)) { be.progress = 0; be.syncGas(); return; }
        if (be.kind == Kind.DECAY && value(in) / 100L > 1_000_000L) {
            be.inventory.setStackInSlot(0, ItemStack.EMPTY);
            Containers.dropItemStack(level, pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, in);
            be.syncGas(); return;
        }
        long required = switch (be.kind) { case DECAY -> 0; case BLASTER -> be.blasterCost(in); case CONDENSER -> be.condenserCost(in); default -> 0; };
        int energy = switch (be.kind) { case DECAY -> (int)Math.min(Integer.MAX_VALUE, Math.max(1, Math.ceil(value(in) * 2.02 / 1000.0))); case BLASTER -> 10_000; case CONDENSER -> 50_000; default -> 0; };
        if ((be.progress == 0 && required > be.photons) || be.energyStorage.getEnergyStored() < energy) { be.syncGas(); return; }
        // Photon is the recipe input, not a per-tick drain. Reserve the complete
        // amount when processing starts so a 10-tick recipe cannot consume only
        // the first few ticks of photon and then stall halfway through.
        if (be.progress == 0) be.photons -= required;
        be.energyStorage.extractEnergyInternal(energy, false); be.progress++;
        if (be.progress >= be.getMaxProgress()) { be.finish(in); be.progress = 0; }
        be.syncGas(); be.setChanged();
    }
    /** Mekanism's gas amount is mEph; the machine keeps the finer microEph remainder internally. */
    private void syncGas() {
        long amount = Math.min(gasCapacity(), Math.max(0L, photons / MICRO_PER_MEPH));
        gasTank.setStack(amount <= 0 ? GasStack.EMPTY : new GasStack(PhotonChemicals.PHOTON.get(), amount));
        mirroredGasAmount = amount;
        syncClient();
    }
    private void acceptTransportedPhotons() {
        long transported = gasTank.getStored();
        long delta = transported - mirroredGasAmount;
        if (delta != 0) {
            photons = Math.max(0L, Math.min(getPhotonCapacity(), photons + delta * MICRO_PER_MEPH));
            mirroredGasAmount = transported;
        }
    }
    private long blasterCost(ItemStack in) {
        if (in.is(Items.GENERAL_CIRCUIT.get())) return 3_000L;                    // 3 mEph
        if (in.is(Items.GENERAL_PHOTON_CIRCUIT.get())) return 10_000L;            // 10 mEph
        if (in.is(Items.PHOTON_CIRCUIT.get())) return 1_000_000L;                 // 1 Eph
        if (in.is(Items.ADVANCED_PHOTON_CIRCUIT.get())) return 125_000_000L;     // 125 Eph
        if (in.is(Items.REFINED_PHOTON_CIRCUIT.get())) return 1_300_000_000L;     // 1300 Eph
        if (in.is(Items.HIGH_DENSITY_PHOTON_CIRCUIT.get())) return 2_500_000_000L;// 2500 Eph
        return 3_000L;
    }
    private void pushPhotonsToNeighbors() {
        if (level == null || photons <= 0) return;
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof PhotonMachineBlockEntity other && other.photons < other.getPhotonCapacity()) {
                long moved = Math.min(photons, other.getPhotonCapacity() - other.photons);
                photons -= moved; other.photons += moved; other.syncGas(); other.setChanged();
                if (photons == 0) break;
            }
        }
    }
    private long condenserCost(ItemStack in) {
        if (in.is(Items.PHOTON_TINY_CRYSTAL.get())) return 7_000L;                    // 7 mEph
        if (in.is(Items.PHOTON_SMALL_CRYSTAL.get())) return 90_000L;                  // 90 mEph
        if (in.is(Items.PHOTON_CRYSTAL.get())) return 900_000L;                       // 900 mEph
        if (in.is(Items.PHOTON_HIGH_CRYSTAL.get())) return 999_000_000L;              // 999 Eph
        if (in.is(Items.PHOTON_HIGH_PURITY_CRYSTAL.get())) return 999_000_000_000L;   // 999 KEph
        if (in.is(Items.PHOTON_MAXIMAL_HIGH_PURITY_CRYSTAL.get())) return 999_000_000_000_000L; // 999 MEph
        return 0L;
    }
    private void finish(ItemStack in) { if (kind == Kind.DECAY) { long gain = Math.min(1_000_000L, value(in) / 100); photons = Math.min(getPhotonCapacity(), photons + value(in) + gain); inventory.extractItem(0, 1, false); } else if (kind == Kind.CONDENSER) { long next = value(in) + condenserCost(in); inventory.setStackInSlot(0, new ItemStack(crystal(next))); } else if (kind == Kind.BLASTER) { inventory.setStackInSlot(0, new ItemStack(nextCircuit(in))); } }
    private Item nextCircuit(ItemStack s) { if (s.is(Items.GENERAL_CIRCUIT.get())) return Items.GENERAL_PHOTON_CIRCUIT.get(); if (s.is(Items.GENERAL_PHOTON_CIRCUIT.get())) return Items.PHOTON_CIRCUIT.get(); if (s.is(Items.PHOTON_CIRCUIT.get())) return Items.ADVANCED_PHOTON_CIRCUIT.get(); if (s.is(Items.ADVANCED_PHOTON_CIRCUIT.get())) return Items.REFINED_PHOTON_CIRCUIT.get(); if (s.is(Items.REFINED_PHOTON_CIRCUIT.get())) return Items.HIGH_DENSITY_PHOTON_CIRCUIT.get(); return Items.HEAVY_PHOTON_CIRCUIT.get(); }
    @Override public Component getDisplayName() { return Component.translatable(switch (kind) { case COLLECTOR -> "block.forlaism.photon_collector"; case DECAY -> "block.forlaism.photon_decay_chamber"; case BLASTER -> "block.forlaism.photon_blaster_infuser"; case CONDENSER -> "block.forlaism.photon_condenser"; }); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new com.tyami.forlaism.screen.PhotonMachineMenu(id, inv, this, data); }
    @Override public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) { if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast(); if (cap == ForgeCapabilities.ENERGY) return energyCap.cast(); if (cap == Capabilities.GAS_HANDLER) { if (level != null && level.isClientSide) return super.getCapability(cap, side); return gasCap.cast(); } return super.getCapability(cap, side); }
    @Override public void onLoad() { super.onLoad(); itemCap = LazyOptional.of(() -> inventory); energyCap = LazyOptional.of(() -> energyStorage); gasCap = LazyOptional.of(() -> gasHandler); }
    private void syncClient() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag() { CompoundTag tag = super.getUpdateTag(); tag.putLong("photons", photons); tag.put("inventory", inventory.serializeNBT()); tag.putInt("progress", progress); return tag; }
    @Override public void handleUpdateTag(CompoundTag tag) { super.handleUpdateTag(tag); if (tag.contains("photons")) photons = tag.getLong("photons"); if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory")); if (tag.contains("progress")) progress = tag.getInt("progress"); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); energyCap.invalidate(); gasCap.invalidate(); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("inventory", inventory.serializeNBT()); tag.putLong("photons", photons); tag.putInt("energy", energyStorage.getEnergyStored()); tag.putInt("progress", progress); tag.put("gas", gasTank.serializeNBT()); }
    @Override public void load(CompoundTag tag) { super.load(tag); inventory.deserializeNBT(tag.getCompound("inventory")); photons = tag.getLong("photons"); energyStorage.setEnergy(tag.getInt("energy")); progress = tag.getInt("progress"); if (tag.contains("gas")) gasTank.deserializeNBT(tag.getCompound("gas")); mirroredGasAmount = gasTank.getStored(); if (!tag.contains("photons")) photons = mirroredGasAmount * MICRO_PER_MEPH; syncGas(); }
    public void dropContents() { SimpleContainer c = new SimpleContainer(1); c.setItem(0, inventory.getStackInSlot(0)); Containers.dropContents(level, worldPosition, c); }
}
