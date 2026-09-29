package com.tyami.forlaism.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.world.BossInstance;
import com.tyami.forlaism.world.BossManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class BossSpawnCommand {

    private BossSpawnCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(
                Commands.literal("fo")
                        .then(Commands.literal("spawn")
                                .then(Commands.literal("boss")
                                        .then(Commands.literal("kumoneko")
                                                .executes(BossSpawnCommand::spawnKumoneko)
                                        )
                                )
                        )
        );
    }

    private static int spawnKumoneko(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("§cプレイヤーのみ実行可能。"));
            return 0;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }

        // 前方3mにスポーン
        var look = player.getLookAngle().normalize();
        double x = player.getX() + look.x * 3.0D;
        double y = player.getY();
        double z = player.getZ() + look.z * 3.0D;

        BossInstance inst = BossManager.spawnBoss(level, "???", "kumorizoraneko", x, y, z);

        src.sendSuccess(
                () -> Component.literal("§5§l§f??? §5が召喚された §7(ID: " + inst.id + ")"),
                true
        );
        return 1;
    }
}