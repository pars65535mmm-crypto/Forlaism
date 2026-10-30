package com.tyami.forlaism.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.tyami.forlaism.world.NightmareSavedData;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /fo nightmare on|off|status
 *
 * ナイトメアモードを切り替える。
 * 権限レベル2（オペレーター）以上。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NightmareCommand {

    private NightmareCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(
                Commands.literal("fo")
                        .then(Commands.literal("nightmare")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("on")
                                        .executes(NightmareCommand::on))
                                .then(Commands.literal("off")
                                        .executes(NightmareCommand::off))
                                .then(Commands.literal("status")
                                        .executes(NightmareCommand::status))
                        )
        );
    }

    private static int on(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        NightmareSavedData.get(server).setNightmare(true);

        ctx.getSource().sendSuccess(
                () -> Component.literal("§5§lナイトメアモード: §cON")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),
                true
        );
        return 1;
    }

    private static int off(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        NightmareSavedData.get(server).setNightmare(false);

        ctx.getSource().sendSuccess(
                () -> Component.literal("§5§lナイトメアモード: §aOFF")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),
                true
        );
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        boolean active = NightmareSavedData.get(server).isNightmare();

        ctx.getSource().sendSuccess(
                () -> Component.literal("§5§lナイトメアモード: " + (active ? "§cON" : "§aOFF")),
                false
        );
        return 1;
    }
}