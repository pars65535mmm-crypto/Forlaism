package com.tyami.forlaism.screen;
import com.tyami.forlaism.block.entity.PhotonMachineBlockEntity;
import com.tyami.forlaism.registry.MenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
public class PhotonMachineMenu extends AbstractContainerMenu {
    private final PhotonMachineBlockEntity be; private final ContainerData data;
    public PhotonMachineMenu(int id, Inventory inv, FriendlyByteBuf buf) { this(id, inv, (PhotonMachineBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6)); }
    public PhotonMachineMenu(int id, Inventory inv, PhotonMachineBlockEntity be, ContainerData data) { super(MenuTypes.PHOTON_MACHINE_MENU.get(), id); this.be=be; this.data=data; int slotX=be.getKind()==PhotonMachineBlockEntity.Kind.COLLECTOR?110:80; int slotY=be.getKind()==PhotonMachineBlockEntity.Kind.COLLECTOR?29:35; addSlot(new SlotItemHandler(be.getInventory(),0,slotX,slotY)); for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,7+c*18,83+r*18)); for(int c=0;c<9;c++)addSlot(new Slot(inv,c,7+c*18,141)); addDataSlots(data); }
    public PhotonMachineBlockEntity getBlockEntity(){return be;} public int getProgress(){return data.get(0);} public int getMaxProgress(){return data.get(1);} public int getEnergy(){return data.get(2);} public int getMaxEnergy(){return data.get(3);} public int getPhotons(){return data.get(4);} public int getMaxPhotons(){return data.get(5);}
    @Override public ItemStack quickMoveStack(Player p,int index){Slot s=slots.get(index); if(!s.hasItem())return ItemStack.EMPTY; ItemStack copy=s.getItem().copy(); if(index==0){if(!moveItemStackTo(s.getItem(),1,37,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(s.getItem(),0,1,false))return ItemStack.EMPTY; if(s.getItem().isEmpty())s.set(ItemStack.EMPTY); else s.setChanged(); return copy;}
    @Override public boolean stillValid(Player p){return stillValid(ContainerLevelAccess.create(p.level(),be.getBlockPos()),p,be.getBlockState().getBlock());}
}
