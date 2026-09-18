package com.tyami.forlaism.util;

import net.minecraftforge.energy.EnergyStorage;

public class CustomEnergyStorage extends EnergyStorage {

    public CustomEnergyStorage(int capacity, int maxReceive) {
        super(capacity, maxReceive, 0, 0);
    }

    public CustomEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract, 0);
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, this.capacity));
    }

    public int extractEnergyInternal(int maxExtract, boolean simulate) {
        int energyExtracted = Math.min(this.energy, maxExtract);
        if (!simulate) {
            this.energy -= energyExtracted;
        }
        return energyExtracted;
    }

    public int receiveEnergyInternal(int maxReceive, boolean simulate) {
        int energyReceived = Math.min(this.capacity - this.energy, maxReceive);
        if (!simulate) {
            this.energy += energyReceived;
        }
        return energyReceived;
    }
}
