package com.tyami.forlaism.screen;
import com.tyami.forlaism.block.entity.Tier2MachineBlockEntity;
import com.tyami.forlaism.registry.Blocks;
import com.tyami.forlaism.registry.MenuTypes;
import net.minecraft.network.FriendlyByteBuf; import net.minecraft.world.entity.player.Inventory; import net.minecraft.world.entity.player.Player; import net.minecraft.world.inventory.*; import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level; import net.minecraftforge.common.capabilities.ForgeCapabilities; import net.minecraftforge.items.SlotItemHandler;
public class Tier2MachineMenu extends AbstractContainerMenu {
    private final Tier2MachineBlockEntity blockEntity; private final Level level; private final ContainerData data;
    public Tier2MachineBlockEntity getBlockEntity(){return blockEntity;}
    public Tier2MachineMenu(int id, Inventory inv, FriendlyByteBuf buf){this(id,inv,(Tier2MachineBlockEntity)inv.player.level().getBlockEntity(buf.readBlockPos()),new SimpleContainerData(6));}
    public Tier2MachineMenu(int id, Inventory inv, Tier2MachineBlockEntity be, ContainerData data){super(menuType(be),id);blockEntity=be;level=inv.player.level();this.data=data; addPlayerInventory(inv);addPlayerHotbar(inv);be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h->{addSlot(new SlotItemHandler(h,0,62,35));addSlot(new SlotItemHandler(h,1,116,35));});addDataSlots(data);}
    private static MenuType<?> menuType(Tier2MachineBlockEntity be){if(be instanceof com.tyami.forlaism.block.entity.AlloyMachineBlockEntity)return MenuTypes.FORLAISM_ALLOY_MACHINE_MENU.get();if(be instanceof com.tyami.forlaism.block.entity.ConcentratorBlockEntity)return MenuTypes.FORLAISM_CONCENTRATOR_MENU.get();return MenuTypes.FORLAISM_REACTOR_MENU.get();}
    public boolean isCrafting(){return data.get(0)>0;} public int getScaledProgress(){return data.get(1)==0?0:data.get(0)*24/data.get(1);} public int getEnergyStored(){return data.get(2);} public int getMaxEnergyStored(){return data.get(3);} public int getFluidAmount(){return data.get(4);} public int getFluidCapacity(){return data.get(5);} public int getScaledEnergy(){return getMaxEnergyStored()==0?0:(int)((long)getEnergyStored()*52/getMaxEnergyStored());} public int getScaledFluid(){return getFluidCapacity()==0?0:getFluidAmount()*52/getFluidCapacity();}
    @Override public ItemStack quickMoveStack(Player p,int index){Slot s=slots.get(index);if(s==null||!s.hasItem())return ItemStack.EMPTY;ItemStack in=s.getItem(),copy=in.copy();if(index<36){if(!moveItemStackTo(in,36,38,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(in,0,36,false))return ItemStack.EMPTY;if(in.isEmpty())s.set(ItemStack.EMPTY);else s.setChanged();s.onTake(p,in);return copy;}
    @Override public boolean stillValid(Player p){return stillValid(ContainerLevelAccess.create(level,blockEntity.getBlockPos()),p,blockEntity.getBlockState().getBlock());}
    private void addPlayerInventory(Inventory i){for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(i,c+r*9+9,8+c*18,84+r*18));} private void addPlayerHotbar(Inventory i){for(int c=0;c<9;c++)addSlot(new Slot(i,c,8+c*18,142));}
}
