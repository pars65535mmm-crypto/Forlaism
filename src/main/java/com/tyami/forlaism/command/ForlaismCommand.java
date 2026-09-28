package com.tyami.forlaism.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.registry.Blocks;
import com.tyami.forlaism.world.MadoromuMultiblockManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public class ForlaismCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
Commands.literal("fo")
                        .then(Commands.literal("test1")
                                .executes(ForlaismCommand::executeTest1))
// ForlaismCommand に追加
.then(Commands.literal("diag")
        .executes(ForlaismCommand::executeDiag))
        
.then(Commands.literal("qqqs")
        .executes(ForlaismCommand::executeQqqs))
.then(Commands.literal("madoromi")
        .executes(ForlaismCommand::executeMadoromi))
);


    }

    /**
     * /fo test1
     *
     * プレイヤーの目の前（3マス先）にマルチブロックを自動建築。
     */
    private static int executeTest1(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("プレイヤーのみ実行できます。"));
            return 0;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }

        // プレイヤーの向き
        Direction facing = player.getDirection();
        Direction right = facing.getClockWise();

        // Fの位置 = プレイヤーの前方 3マス、地面と同じ高さ
        BlockPos fPos = player.blockPosition()
                .relative(facing, 3)
                .above(0);

        // Fの内向き = facing の逆（プレイヤー側が内側）
        Direction inward = facing.getOpposite();

        // ---- 建築開始 ----
        buildStructure(level, fPos, inward);

        source.sendSuccess(
                () -> Component.literal("§a[Forlaism] §fテスト構造を建築しました: " + fPos.toShortString()),
                true
        );
        return 1;
    }

    /**
     * Fの位置と内向き法線を基準に構造を建築。
     */
    private static void buildStructure(ServerLevel level, BlockPos fPos, Direction inward) {

        Direction right = inward.getClockWise();

        // Y1層（Fの1つ下、5x5 全部 M）
        BlockPos y1 = fPos.below();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = y1.relative(right, dx).relative(inward, -dz);
                level.setBlock(p, Blocks.MADOROMU_BLOCK.get().defaultBlockState(), 3);
            }
        }

        // Y2〜Y10層（外周のみ M、Fの位置はFブロック）
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos layerCenter = fPos.above(dy)
                    .relative(inward, 2); // 中心へ

            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {

                    boolean isEdge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    if (!isEdge) continue; // 内部は空気

                    BlockPos p = layerCenter.relative(right, dx).relative(inward, -dz);

                    if (p.equals(fPos)) {
                        // Fの位置
                        level.setBlock(p, Blocks.FORLAISM_INSERTER.get().defaultBlockState(), 3);
                    } else {
                        level.setBlock(p, Blocks.MADOROMU_BLOCK.get().defaultBlockState(), 3);
                    }
                }
            }
        }
    }



    private static int executeDiag(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();

    if (!(source.getEntity() instanceof Player player)) return 0;
    if (!(player.level() instanceof ServerLevel level)) return 0;
    

    Direction facing = player.getDirection();
    

    // プレイヤー前方3マスの「搬入機」を探す
    BlockPos fPos = null;
    for (int r = 1; r <= 10; r++) {
        BlockPos candidate = player.blockPosition().relative(facing, r);
        if (level.getBlockState(candidate).is(Blocks.FORLAISM_INSERTER.get())) {
            fPos = candidate;
            break;
        }
    }

    if (fPos == null) {
        source.sendFailure(Component.literal("§c近くに搬入機が見つかりません（前方10マス以内）"));
        return 0;
    }

    final BlockPos inserterPos = fPos;

    source.sendSuccess(() -> Component.literal("§e=== Fの周囲4方向 ==="), false);
for (Direction d : new Direction[]{
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
}) {
    BlockPos np = inserterPos.relative(d);
    String name = level.getBlockState(np).getBlock().getName().getString();
    source.sendSuccess(() -> Component.literal(
            "§7" + d.getName() + ": §f" + name
    ), false);
}

    // 内向き法線
    Direction inward = MadoromuMultiblockManager.detectInwardNormal(level, inserterPos);
    if (inward == null) {
        source.sendFailure(Component.literal("§c内向き法線を判定できません。Fの外側がM、内側が空気である必要があります。"));
        return 0;
    }

    source.sendSuccess(() -> Component.literal("§a内向き: " + inward.getName()), false);

    // 1マスずつチェック
    Direction right = inward.getClockWise();
    int errorCount = 0;

    // Y1層
    BlockPos y1 = inserterPos.below();
    for (int dx = -2; dx <= 2; dx++) {
        for (int dz = -2; dz <= 2; dz++) {
            BlockPos p = y1.relative(right, dx).relative(inward, -dz);
            if (!level.getBlockState(p).is(Blocks.MADOROMU_BLOCK.get())) {
                final int fdx = dx, fdz = dz;
                source.sendSuccess(() -> Component.literal(
                        "§c[Y1] 期待:M 実際:" + level.getBlockState(p).getBlock().getName().getString()
                                + " @ " + p.toShortString()
                                + " (dx=" + fdx + ", dz=" + fdz + ")"
                ), false);
                errorCount++;
            }
        }
    }

    // Y2〜Y10層
    for (int dy = 0; dy <= 8; dy++) {
        BlockPos layerCenter = inserterPos.above(dy).relative(inward, 2);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean isEdge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                BlockPos p = layerCenter.relative(right, dx).relative(inward, -dz);

                if (isEdge) {
                    if (p.equals(inserterPos)) continue; // Fはスキップ
                    if (!level.getBlockState(p).is(Blocks.MADOROMU_BLOCK.get())) {
                        final int fdy = dy, fdx = dx, fdz = dz;
                        source.sendSuccess(() -> Component.literal(
                                "§c[Y" + (fdy + 2) + "] 期待:M 実際:"
                                        + level.getBlockState(p).getBlock().getName().getString()
                                        + " @ " + p.toShortString()
                        ), false);
                        errorCount++;
                    }
                } else {
                    if (!level.getBlockState(p).isAir()) {
                        final int fdy = dy, fdx = dx, fdz = dz;
                        source.sendSuccess(() -> Component.literal(
                                "§c[Y" + (fdy + 2) + "] 期待:空気 実際:"
                                        + level.getBlockState(p).getBlock().getName().getString()
                                        + " @ " + p.toShortString()
                        ), false);
                        errorCount++;
                    }
                }
            }
        }
    }

    if (errorCount == 0) {
        source.sendSuccess(() -> Component.literal("§a§l✔ 構造は完璧です！"), true);
    } else {
        final int fc = errorCount;
        source.sendSuccess(() -> Component.literal("§e合計 " + fc + " 箇所の問題があります。"), true);
    }
    return 1;
}

/**
 * /fo qqqs
 *
 * プレイヤーの足元に輪廻再転式量子融合炉を一括生成する。
 * LAYERS の構造をそのまま設置する。
 *
 * origin はプレイヤーの前方 3 マスあたりの Y-1。
 */
private static int executeQqqs(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();

    if (!(source.getEntity() instanceof Player player)) {
        source.sendFailure(Component.literal("プレイヤーのみ実行できます。"));
        return 0;
    }

    if (!(player.level() instanceof ServerLevel level)) {
        return 0;
    }

    // プレイヤーの向きから origin を決定
    Direction facing = player.getDirection();
    // プレイヤー前方3マス、y-1
    BlockPos origin = player.blockPosition()
            .relative(facing, 3)
            .below(1);

    // LAYERS を設置
    int placed = ForlaismCommand.buildReactor(level, origin);

    source.sendSuccess(
            () -> Component.literal("§a[Forlaism] §f輪廻再転式量子融合炉を生成しました: "
                    + origin.toShortString() + " (§7" + placed + " blocks§f)"),
            true
    );
    return 1;
}

/**
 * LAYERS を一括設置。設置数を返す。
 */
private static int buildReactor(ServerLevel level, BlockPos origin) {

    var LAYERS = com.tyami.forlaism.world.ForlaismReactorMultiblockManager.LAYERS;

    int count = 0;

    for (int y = 0; y < LAYERS.length; y++) {
        String[] layer = LAYERS[y];
        for (int z = 0; z < layer.length; z++) {
            String row = layer[z];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                BlockPos p = origin.offset(x, y, z);

                var state = charToBlock(c);
                if (state == null) continue;

                level.setBlock(p, state, 3);
                count++;
            }
        }
    }

    return count;
}

/**
 * LAYERS のシンボル → BlockState 変換。
 * null = 空気のままにする（設置しない）。
 */
@Nullable
private static net.minecraft.world.level.block.state.BlockState charToBlock(char c) {
    return switch (c) {
        case 'S' -> net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState();
        case 'M' -> com.tyami.forlaism.registry.Blocks.MADOROMU_BLOCK.get().defaultBlockState();
        case 'K' -> com.tyami.forlaism.registry.Blocks.FORLAISM_CRYSTAL_BLOCK.get().defaultBlockState();
        case 'T' -> com.tyami.forlaism.registry.Blocks.STEEL_BLOCK.get().defaultBlockState();
        case 'I' -> com.tyami.forlaism.registry.Blocks.FORLAISM_INSERTER.get().defaultBlockState();
        case 'C' -> blockById("mekanismgenerators:fission_reactor_casing");
        case 'G' -> blockById("mekanism:structural_glass");
        case 'E' -> blockById("mekanism:ultimate_energy_cube");
        case 'O' -> blockById("mekanism:sps_port");
        default -> null; // 空気
    };
}

@Nullable
private static net.minecraft.world.level.block.state.BlockState blockById(String id) {
    var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS
            .getValue(new net.minecraft.resources.ResourceLocation(id));
    return block == null ? null : block.defaultBlockState();
}


private static int executeMadoromi(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    if (!(source.getEntity() instanceof Player player)) {
        source.sendFailure(Component.literal("プレイヤーのみ実行できます。"));
        return 0;
    }

    // 目の前のLivingEntityを取得
    var level = player.level();
    var look = player.getLookAngle();
    var eye = player.getEyePosition();
    var end = eye.add(look.scale(10.0));

    var hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
            level, player, eye, end,
            player.getBoundingBox().expandTowards(look.scale(10.0)).inflate(1.0),
            e -> e instanceof net.minecraft.world.entity.LivingEntity && e != player
    );

    if (hitResult == null || !(hitResult.getEntity() instanceof net.minecraft.world.entity.LivingEntity target)) {
        source.sendFailure(Component.literal("対象が見つかりません。"));
        return 0;
    }

    boolean success = com.tyami.forlaism.damage.MadoromiExecution.execute(
            target,
            com.tyami.forlaism.damage.MadoromiDamageSource.of(level, player)
    );

    source.sendSuccess(
            () -> Component.literal("§d微睡ダメージ実行: §f" + target.getName().getString()
                    + " §7→ " + (success ? "§a成功" : "§c失敗")
                    + " §7(HP: " + target.getHealth() + "/" + target.getMaxHealth()
                    + " dead: " + target.isDeadOrDying() + ")"),
            true
    );
    return 1;
}

}