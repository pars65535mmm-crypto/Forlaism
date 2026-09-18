package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ForlaismWeatherControllerItem extends Item {

    private static final String WEATHER_MODE_TAG = "WeatherMode";

    // 0 = 晴天
    // 1 = 雨天
    // 2 = 雷雨

    public ForlaismWeatherControllerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack controller = player.getItemInHand(hand);

        // Shift + 右クリック
        if (player.isShiftKeyDown()) {

            int mode = getWeatherMode(controller);

            mode++;

            if (mode >= 3) {
                mode = 0;
            }

            setWeatherMode(controller, mode);

            if (!level.isClientSide) {
                showWeatherMode(player, mode);
            }

            return InteractionResultHolder.sidedSuccess(
                    controller,
                    level.isClientSide()
            );
        }

        // 通常右クリック
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

            ItemStack starCore = player.getOffhandItem();

            // 左手に星核が必要
            if (!starCore.is(Items.FORLAISM_STAR_CORE.get())) {

                player.displayClientMessage(
                        Component.literal("§c左手にフォラリス星核が必要です。"),
                        true
                );

                return InteractionResultHolder.fail(controller);
            }

            int mode = getWeatherMode(controller);

            applyWeather(serverLevel, mode);

            // 星核を1個消費
            if (!player.getAbilities().instabuild) {
                starCore.shrink(1);
            }

            showActivatedMessage(player, mode);
        }

        return InteractionResultHolder.sidedSuccess(
                controller,
                level.isClientSide()
        );
    }

    private static int getWeatherMode(ItemStack stack) {
        return stack.getOrCreateTag().getInt(WEATHER_MODE_TAG);
    }

    private static void setWeatherMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt(WEATHER_MODE_TAG, mode);
    }

    private static void showWeatherMode(Player player, int mode) {

        switch (mode) {

            case 0 -> player.displayClientMessage(
                    Component.literal("§e天候選択: §f晴天"),
                    true
            );

            case 1 -> player.displayClientMessage(
                    Component.literal("§b天候選択: §f雨天"),
                    true
            );

            case 2 -> player.displayClientMessage(
                    Component.literal("§5天候選択: §f雷雨"),
                    true
            );
        }
    }

    private static void showActivatedMessage(Player player, int mode) {

        switch (mode) {

            case 0 -> player.displayClientMessage(
                    Component.literal("§eフォラリス星核を消費し、天候を晴天へ変更しました。"),
                    true
            );

            case 1 -> player.displayClientMessage(
                    Component.literal("§bフォラリス星核を消費し、天候を雨天へ変更しました。"),
                    true
            );

            case 2 -> player.displayClientMessage(
                    Component.literal("§5フォラリス星核を消費し、天候を雷雨へ変更しました。"),
                    true
            );
        }
    }

    private static void applyWeather(ServerLevel level, int mode) {

        switch (mode) {

            // 晴天
            case 0 -> level.setWeatherParameters(
                    12000,
                    0,
                    false,
                    false
            );

            // 雨天
            case 1 -> level.setWeatherParameters(
                    0,
                    6000,
                    true,
                    false
            );

            // 雷雨
            case 2 -> level.setWeatherParameters(
                    0,
                    6000,
                    true,
                    true
            );
        }
    }
}