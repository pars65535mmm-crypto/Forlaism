package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.world.TimeAccelerationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TokinoStaffItem extends ForalisItem {

    public static final int CAPACITY = 1000;
    public static final int CONSUMPTION_PER_USE = 10;

    public TokinoStaffItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack resource) {
                return resource.getFluid() == Fluids.FO.get();
            }
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        IFluidHandlerItem fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler != null) {
            FluidStack simulated = fluidHandler.drain(new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE), IFluidHandler.FluidAction.SIMULATE);
            if (!simulated.isEmpty() && simulated.getAmount() >= CONSUMPTION_PER_USE) {
                if (!TimeAccelerationManager.isAccelerated(level, pos)) {
                    if (!level.isClientSide) {
                        fluidHandler.drain(new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE), IFluidHandler.FluidAction.EXECUTE);
                        // 16秒間 (320 ticks) 16倍速
                        TimeAccelerationManager.accelerate(level, pos, 320, 16);
                        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.5F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        IFluidHandlerItem fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler != null) {
            FluidStack fluid = fluidHandler.getFluidInTank(0);
            return Math.round(13.0F * fluid.getAmount() / (float) CAPACITY);
        }
        return 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.color(0.33F, 1.0F, 1.0F); // 水色
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        int amount = 0;
        IFluidHandlerItem fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler != null) {
            FluidStack fluid = fluidHandler.getFluidInTank(0);
            amount = fluid.getAmount();
        }

        tooltipComponents.add(Component.translatable("tooltip.forlaism.tokino_staff_fo", amount, CAPACITY).withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.forlaism.tokino_staff_desc").withStyle(ChatFormatting.GRAY));
    }
}
