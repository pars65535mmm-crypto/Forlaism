package com.tyami.forlaism.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.util.Overnull;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

/**
 * /fo overnull <targets>
 *
 * 対象を Overnull（確定即死）する。
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class OvernullCommand {

    private OvernullCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal("fo")
                        .then(Commands.literal("overnull")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument(
                                                "targets",
                                                EntityArgument.entities()
                                        )
                                        .executes(OvernullCommand::execute)
                                )
                        )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {

        CommandSourceStack source = ctx.getSource();

        Collection<? extends Entity> targets;
        try {
            targets = EntityArgument.getEntities(ctx, "targets");
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c対象の取得に失敗。"));
            return 0;
        }

        int count = 0;

        for (Entity e : targets) {

            if (!(e instanceof LivingEntity living)) continue;

            boolean ok = Overnull.execute(living);

            if (ok) {
                count++;
                source.sendSuccess(
                        () -> Component.literal(
                                "§5Overnull §f: "
                                        + living.getName().getString()
                        ),
                        true
                );
            }
        }

        if (count == 0) {
            source.sendFailure(Component.literal("§c対象がいません。"));
        }

        return count;
    }
}