package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RooterBundleItem extends Item {

    public RooterBundleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    private static final Random RANDOM = new Random();

    private static final int STRUCTURE_ROLLS = 10;
    private static final int DROP_ROLLS = 5;
    private static final int ALL_ROLLS = 1;

    /*
     * =========================================================
     * LootDataManager から LootTable の ResourceLocation 一覧を取得
     * =========================================================
     *
     * 1.20.1 の LootDataManager は
     *
     *   private ImmutableMultimap<LootDataType<?>, ResourceLocation> typeKeys;
     *
     * を持っている。
     * SRG名は "f_278478_" 、Mojang名は "typeKeys" 。
     */
    private static Field cachedTypeKeysField = null;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<ResourceLocation> getLootTableIds(LootDataManager manager) {

        if (manager == null) return List.of();

        try {
            if (cachedTypeKeysField == null) {
                for (String name : new String[]{"f_278478_", "typeKeys"}) {
                    try {
                        Field f = LootDataManager.class.getDeclaredField(name);
                        f.setAccessible(true);
                        cachedTypeKeysField = f;
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }

            if (cachedTypeKeysField == null) {
                System.out.println("[RooterBundle] typeKeys field not found!");
                return List.of();
            }

            Object value = cachedTypeKeysField.get(manager);
            if (value instanceof com.google.common.collect.Multimap rawMultimap) {

                List<ResourceLocation> result = new ArrayList<>();
                for (Object key : rawMultimap.keySet()) {
                    if (key instanceof LootDataType<?> type && type == LootDataType.TABLE) {
                        for (Object v : rawMultimap.get(key)) {
                            if (v instanceof ResourceLocation id) {
                                result.add(id);
                            }
                        }
                        break;
                    }
                }
                return result;
            }
        } catch (Exception e) {
            System.out.println("[RooterBundle] reflection error: " + e);
        }

        return List.of();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

            List<ResourceLocation> allIds = getLootTableIds(serverLevel.getServer().getLootData());
            System.out.println("[RooterBundle] total LootTables = " + allIds.size());

            rollFiltered(serverLevel, player, allIds, FilterMode.STRUCTURE, STRUCTURE_ROLLS);
            rollFiltered(serverLevel, player, allIds, FilterMode.DROP, DROP_ROLLS);
            rollFiltered(serverLevel, player, allIds, FilterMode.ALL, ALL_ROLLS);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            serverLevel.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENDER_CHEST_OPEN,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.2F
            );

            player.displayClientMessage(
                    Component.literal("§dルーターバンドル §fがルートを開いた…"),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private enum FilterMode {
        STRUCTURE,
        DROP,
        ALL
    }

    private static void rollFiltered(ServerLevel level, Player player,
                                     List<ResourceLocation> allIds,
                                     FilterMode mode, int times) {

        if (allIds.isEmpty()) return;

        List<ResourceLocation> candidates = new ArrayList<>();

        for (ResourceLocation id : allIds) {
            String path = id.getPath();

            switch (mode) {
                case STRUCTURE -> {
                    if (path.startsWith("chests/") || path.contains("/chests/")) {
                        candidates.add(id);
                    }
                }
                case DROP -> {
                    if (path.startsWith("entities/") || path.contains("/entities/")) {
                        candidates.add(id);
                    }
                }
                case ALL -> candidates.add(id);
            }
        }

        System.out.println("[RooterBundle] mode=" + mode + " candidates=" + candidates.size());

        if (candidates.isEmpty()) return;

        BlockPos pos = player.blockPosition();
        Vec3 dropPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);

        for (int i = 0; i < times; i++) {

            ResourceLocation chosenId = candidates.get(RANDOM.nextInt(candidates.size()));
            LootTable table = level.getServer().getLootData().getLootTable(chosenId);

            if (table == null || table == LootTable.EMPTY) {
                continue;
            }

            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, dropPos)
                    .withParameter(LootContextParams.THIS_ENTITY, player)
                    .withParameter(LootContextParams.KILLER_ENTITY, player)
                    .withParameter(
                            LootContextParams.DAMAGE_SOURCE,
                            level.damageSources().generic()
                    )
                    .withLuck(player.getLuck())
                    .create(LootContextParamSets.ENTITY);

            List<ItemStack> drops;
            try {
                drops = table.getRandomItems(params);
            } catch (Exception e) {
                continue;
            }

            for (ItemStack drop : drops) {
                if (drop.isEmpty()) continue;

                ItemEntity itemEntity = new ItemEntity(
                        level,
                        dropPos.x + (RANDOM.nextDouble() - 0.5) * 0.8,
                        dropPos.y,
                        dropPos.z + (RANDOM.nextDouble() - 0.5) * 0.8,
                        drop
                );
                itemEntity.setNoPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.forlaism.rooter_bundle.tooltip")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.forlaism.rooter_bundle.tooltip2")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.forlaism.rooter_bundle.tooltip3")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}