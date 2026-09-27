package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.world.EntityFreezeManager;
import com.tyami.forlaism.world.TimeAccelerationManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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

/**
 * 時空の杖 Tier4。
 *
 * - FO容量 100,000 mB
 * - 1回のブロック使用で 1,000 mB 消費
 * - 16秒間 (320 ticks) 1024倍速
 * - entity右クリックで10秒間の凍結（tick skip）
 */
public class TokinoStaffTier4Item extends ForalisItem {

    /** FO容量。 */
    public static final int CAPACITY = 100_000;

    /** 1回の使用で消費するFO量。 */
    public static final int CONSUMPTION_PER_USE = 1_000;

    /** 加速時間 (ticks)。16秒 = 320 ticks。 */
    public static final int ACCELERATION_DURATION = 320;

    /** 加速倍率。 */
    public static final int ACCELERATION_MULTIPLIER = 1024;

    /** 凍結時間 (ticks)。10秒 = 200 ticks。 */
    public static final int FREEZE_DURATION = 200;

    public TokinoStaffTier4Item(Properties properties) {
        super(properties.stacksTo(1));
    }

    // =========================================================
    // Fluid capability
    // =========================================================

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack resource) {
                return resource.getFluid() == Fluids.FO.get();
            }
        };
    }

    // =========================================================
    // Block: 時間加速
    // =========================================================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // 既に加速中なら弾く
        if (TimeAccelerationManager.isAccelerated(level, pos)) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(
                        Component.literal("§cこのブロックは既に加速中です！"),
                        true
                );
            }
            return InteractionResult.PASS;
        }

        IFluidHandlerItem fluidHandler = stack
                .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .orElse(null);

        if (fluidHandler == null) {
            return InteractionResult.PASS;
        }

        // FOチェック
        FluidStack simulated = fluidHandler.drain(
                new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE),
                IFluidHandler.FluidAction.SIMULATE
        );

        if (simulated.isEmpty() || simulated.getAmount() < CONSUMPTION_PER_USE) {
            if (!level.isClientSide && player != null) {
                player.displayClientMessage(
                        Component.literal("§cFOが足りません！ 必要: "
                                + CONSUMPTION_PER_USE + "mB"),
                        true
                );
            }
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            // FO消費
            fluidHandler.drain(
                    new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE),
                    IFluidHandler.FluidAction.EXECUTE
            );

            // 1024倍速で 16秒間
            TimeAccelerationManager.accelerate(
                    level,
                    pos,
                    ACCELERATION_DURATION,
                    ACCELERATION_MULTIPLIER
            );

            level.playSound(
                    null,
                    pos,
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.BLOCKS,
                    1.0F,
                    2.0F
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // =========================================================
    // Entity: 凍結（tick skip）
    // =========================================================

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        Level level = player.level();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 自分自身は凍結不可
        if (target == player) {
            return InteractionResult.PASS;
        }

        IFluidHandlerItem fluidHandler = stack
                .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .orElse(null);

        if (fluidHandler == null) {
            return InteractionResult.PASS;
        }

        // FOチェック
        FluidStack simulated = fluidHandler.drain(
                new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE),
                IFluidHandler.FluidAction.SIMULATE
        );

        if (simulated.isEmpty() || simulated.getAmount() < CONSUMPTION_PER_USE) {
            player.displayClientMessage(
                    Component.literal("§cFOが足りません！ 必要: "
                            + CONSUMPTION_PER_USE + "mB"),
                    true
            );
            return InteractionResult.PASS;
        }

        // FO消費
        fluidHandler.drain(
                new FluidStack(Fluids.FO.get(), CONSUMPTION_PER_USE),
                IFluidHandler.FluidAction.EXECUTE
        );

        // 凍結開始
        EntityFreezeManager.freeze(target, FREEZE_DURATION);

        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                0.5F
        );

        player.displayClientMessage(
                Component.literal("§d" + target.getName().getString() + " §fの時を止めた… 10秒間"),
                true
        );

        return InteractionResult.SUCCESS;
    }

    // =========================================================
    // Bar / Tooltip
    // =========================================================

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        IFluidHandlerItem h = stack
                .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .orElse(null);
        if (h == null) return 0;
        FluidStack fluid = h.getFluidInTank(0);
        return Math.round(13.0F * fluid.getAmount() / (float) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Tier4 らしく鮮やかなマゼンタ〜紫
        return 0xFFFF00FF;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);

        int amount = 0;
        IFluidHandlerItem h = stack
                .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .orElse(null);
        if (h != null) {
            amount = h.getFluidInTank(0).getAmount();
        }

        tooltip.add(Component.literal("FO: " + amount + " / " + CAPACITY + " mB")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("ブロック右クリック: 16秒間 1024倍速")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("entity右クリック: 10秒間 時間停止")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("消費: " + CONSUMPTION_PER_USE + " mB / 使用")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("時は、もはや汝の掌中にある。")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }
}