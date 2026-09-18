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

public class TokinoStaffTier3Item extends ForalisItem {

    public static final int CAPACITY = 10000;
    public static final int CONSUMPTION_PER_USE = 300;
    public static final int DURATION_TICKS = 320; // 16秒
    public static final int SPEED_MULTIPLIER = 256;

    public TokinoStaffTier3Item(Properties properties) {
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

        // 既に加速中なら何もしない
        if (TimeAccelerationManager.isAccelerated(level, pos)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(
                        Component.literal("§cこのブロックは既に加速中です！"),
                        true
                );
            }
            return InteractionResult.PASS;
        }

        IFluidHandlerItem fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler == null) {
            return InteractionResult.PASS;
        }

        // FOが十分にあるかチェック
        FluidStack simulated = fluidHandler.drain(
                new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE),
                IFluidHandler.FluidAction.SIMULATE
        );

        if (simulated.isEmpty() || simulated.getAmount() < CONSUMPTION_PER_USE) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(
                        Component.literal("§cFOが足りません！ 必要: " + CONSUMPTION_PER_USE + "mB"),
                        true
                );
            }
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            // FOを消費
            fluidHandler.drain(
                    new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE),
                    IFluidHandler.FluidAction.EXECUTE
            );

            // 時間加速を適用（16秒間・256倍速）
            TimeAccelerationManager.accelerate(level, pos, DURATION_TICKS, SPEED_MULTIPLIER);

            // 効果音
            level.playSound(
                    null,
                    pos,
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.BLOCKS,
                    1.0F,
                    2.0F // ピッチ高めで派手に！
            );

        }

        return InteractionResult.sidedSuccess(level.isClientSide);
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
        // 紫色系（Tier3っぽい）
        return 0xFFAA00FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int amount = 0;
        IFluidHandlerItem fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler != null) {
            FluidStack fluid = fluidHandler.getFluidInTank(0);
            amount = fluid.getAmount();
        }

        tooltip.add(Component.translatable("tooltip.forlaism.tokino_staff_tier3_fo", amount, CAPACITY)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.forlaism.tokino_staff_tier3_desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.forlaism.tokino_staff_tier3_detail")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}