package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = "forlaism",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HaloOfTheFirmamentHarvestAbility {

    /*
     * =========================================================
     * 最後に倒したMobの種類
     * =========================================================
     *
     * Player UUID
     *     ↓
     * Entity type
     *
     * という形で保存する。
     *
     * 例:
     *
     * Zombie
     * Skeleton
     * Creeper
     * Ender Dragon
     * ...
     */

    private static final Map<UUID, String> LAST_KILLED_MOB =
            new HashMap<>();

    private HaloOfTheFirmamentHarvestAbility() {
    }

    /*
     * =========================================================
     * Halo判定
     * =========================================================
     */

    private static boolean hasHalo(Player player) {

        return CuriosApi.getCuriosInventory(player)
                .map(handler ->
                        handler.findFirstCurio(
                                stack -> stack.is(
                                        Items.HALO_OF_THE_FIRMAMENT.get()
                                )
                        ).isPresent()
                )
                .orElse(false);
    }

    /*
     * =========================================================
     * Mob死亡
     * =========================================================
     */

    @SubscribeEvent
    public static void onLivingDeath(
            LivingDeathEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        /*
         * Killerを取得。
         *
         * getKillCredit()ではなく、
         * damage sourceからプレイヤーを取得する。
         */
        Entity attacker =
                event.getSource().getEntity();

        if (!(attacker instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (!hasHalo(player)) {
            return;
        }

        /*
         * Mobの種類を保存。
         *
         * ResourceLocation形式:
         *
         * minecraft:zombie
         * minecraft:skeleton
         * minecraft:creeper
         *
         * modded mobなら
         *
         * example:custom_mob
         *
         */
        String entityType =
                entity.getType()
                        .builtInRegistryHolder()
                        .key()
                        .location()
                        .toString();

        LAST_KILLED_MOB.put(
                player.getUUID(),
                entityType
        );
    }

    /*
     * =========================================================
     * ブロック破壊
     * =========================================================
     *
     * Halo装備者なら、
     *
     * 「採掘できるか？」
     *
     * の判定を実質無視する。
     */

    @SubscribeEvent
    public static void onBlockBreak(
            BlockEvent.BreakEvent event
    ) {

        Player player =
                event.getPlayer();

        if (player == null) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        if (!hasHalo(player)) {
            return;
        }

        /*
         * ここでキャンセルすると、
         * Vanilla側の破壊処理を止めてしまう。
         *
         * なので「採掘可能判定」だけを
         * Mixin側で突破する方式にする。
         *
         * このイベントでは何もしない。
         */
    }

    /*
     * =========================================================
     * API
     * =========================================================
     *
     * 最後に倒したMobのIDを取得する。
     */

    public static String getLastKilledMob(
            Player player
    ) {

        return LAST_KILLED_MOB.get(
                player.getUUID()
        );
    }

    /*
     * =========================================================
     * リセット
     * =========================================================
     */

    public static void clear(Player player) {

        LAST_KILLED_MOB.remove(
                player.getUUID()
        );
    }
}