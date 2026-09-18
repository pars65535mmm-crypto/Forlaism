package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
public final class HaloOfTheAdventAbility {

    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("8f7e2c11-4a7e-4c91-9f6a-2d6b1e9a73c4");

    private static final UUID MAX_HEALTH_UUID =
            UUID.fromString("6f0b3c72-5f5a-4c0a-b6ef-9e4c6c2b8f11");

    private static final UUID ARMOR_UUID =
            UUID.fromString("bdfc0c34-8e8d-48f5-a1cb-9e1d0b7c55a2");

    private static final UUID MOVEMENT_SPEED_UUID =
            UUID.fromString("5e2e6d72-8f3a-4b7a-bd13-3c4a8c7a91e4");

    private static final double ATTACK_DAMAGE = Double.POSITIVE_INFINITY;

    /*
 * Movement efficiency:
 * movement speed itself is intentionally kept at a sane value.
 */
private static final double MOVEMENT_SPEED = 0.20D;

/*
 * Creative-like flight speed.
 */
private static final float FLYING_SPEED = 0.15F;

    /*
     * Minecraft's LivingEntity health field is float.
     * The attribute itself can still hold Double.MAX_VALUE.
     */
    public static final double ABSOLUTE_VALUE = Double.MAX_VALUE;
    public static final float SAFE_HEALTH = Float.MAX_VALUE;

    private static final String ATTACK_DAMAGE_NAME =
            "halo_of_the_advent_attack_damage";

    private static final String MAX_HEALTH_NAME =
            "halo_of_the_advent_max_health";

    private static final String ARMOR_NAME =
            "halo_of_the_advent_armor";
    private static final String MOVEMENT_SPEED_NAME =
        "halo_of_the_advent_movement_speed";



    /*
     * Latest state of every player who has awakened the halo.
     *
     * Everything is accessed from Minecraft's main thread.
     * No Entity / Level object is ever touched from another thread.
     */
    private static final Map<UUID, HaloSnapshot> SNAPSHOTS =
            new HashMap<>();

    /*
     * The actual halo stack is retained independently of the player's
     * inventory so removing/dropping it cannot destroy the ability.
     */
    private static final Map<UUID, ItemStack> LOCKED_HALOS =
            new HashMap<>();

    private HaloOfTheAdventAbility() {
    }

private static boolean hasHalo(Player player) {
    return CuriosApi.getCuriosInventory(player)
            .map(handler ->
                    handler.findFirstCurio(
                            stack -> stack.is(Items.HALO_OF_THE_ADVENT.get())
                    ).isPresent()
            )
            .orElse(false);
}

    /**
     * Called when the halo is detected for the first time.
     */
    private static void awaken(Player player) {

        UUID uuid = player.getUUID();

        if (!LOCKED_HALOS.containsKey(uuid)) {

            CuriosApi.getCuriosInventory(player)
        .resolve()
        .ifPresent(handler ->
                handler.findFirstCurio(
                        stack -> stack.is(
                                Items.HALO_OF_THE_ADVENT.get()
                        )
                )
                .ifPresent(result ->
                        LOCKED_HALOS.put(
                                uuid,
                                result.stack().copy()
                        )
                )
        );
        }

        if (!SNAPSHOTS.containsKey(uuid)) {
            SNAPSHOTS.put(
                    uuid,
                    HaloSnapshot.capture(player)
            );
        }
    }

    /**
     * The halo is checked every server tick.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        if (player.level().isClientSide) {
            return;
        }

        boolean equipped = hasHalo(player);

        /*
         * First detection:
         * once the halo has been equipped, the state is permanent.
         */
        if (equipped) {
            awaken(player);
        }

        if (!LOCKED_HALOS.containsKey(player.getUUID())) {
            return;
        }

        /*
         * Absolute equipment retention.
         */
        ensureHaloEquipped(player);

        /*
         * Ability values.
         */
        applyAttributes(player);

        /*
         * Absolute HP.
         *
         * Double.MAX_VALUE cannot be represented by LivingEntity's
         * float health field, so Float.MAX_VALUE is used for the actual
         * health value. Damage is independently blocked by the Mixin.
         */
        if (player.getHealth() != SAFE_HEALTH) {
            player.setHealth(SAFE_HEALTH);
        }

        player.getAbilities().mayfly = true;
player.getAbilities().invulnerable = true;
player.getAbilities().setFlyingSpeed(FLYING_SPEED);

/*
 * Survivalでもクリエイティブと同じように飛行可能。
 * mayflyだけでは飛行状態そのものを維持しないので、
 * isFlyingも毎tick維持する。
 */
if (!player.getAbilities().flying) {
    player.getAbilities().flying = true;
}

        /*
         * Snapshot is updated every tick.
         *
         * This is the state that will be restored if death somehow
         * reaches LivingEntity#die.
         */
        SNAPSHOTS.put(
                player.getUUID(),
                HaloSnapshot.capture(player)
        );
    }

    private static void ensureHaloEquipped(Player player) {

        ItemStack halo = LOCKED_HALOS.get(player.getUUID());

        if (halo == null || halo.isEmpty()) {
            return;
        }

        if (hasHalo(player)) {
            return;
        }

        /*
         * The project's Curios configuration uses the back slot.
         * Slot 0 is the first slot of that type.
         */
        CuriosApi.getCuriosInventory(player)
                .ifPresent(handler ->
                        handler.setEquippedCurio(
                                "back",
                                0,
                                halo.copy()
                        )
                );
    }

    private static void applyAttributes(Player player) {

        addOrReplaceModifier(
                player.getAttribute(Attributes.MAX_HEALTH),
                MAX_HEALTH_UUID,
                MAX_HEALTH_NAME,
                ABSOLUTE_VALUE
        );

        addOrReplaceModifier(
                player.getAttribute(Attributes.ARMOR),
                ARMOR_UUID,
                ARMOR_NAME,
                ABSOLUTE_VALUE
        );


        addOrReplaceModifier(
        player.getAttribute(Attributes.MOVEMENT_SPEED),
        MOVEMENT_SPEED_UUID,
        MOVEMENT_SPEED_NAME,
        MOVEMENT_SPEED
        );


        addOrReplaceModifier(
                player.getAttribute(Attributes.ATTACK_DAMAGE),
                ATTACK_DAMAGE_UUID,
                ATTACK_DAMAGE_NAME,
                ATTACK_DAMAGE
        );
    }

    private static void addOrReplaceModifier(
            AttributeInstance attribute,
            UUID uuid,
            String name,
            double amount
    ) {

        if (attribute == null) {
            return;
        }

        AttributeModifier existing = attribute.getModifier(uuid);

        if (existing == null) {

            attribute.addPermanentModifier(
                    new AttributeModifier(
                            uuid,
                            name,
                            amount,
                            AttributeModifier.Operation.ADDITION
                    )
            );

        } else if (existing.getAmount() != amount) {

            attribute.removeModifier(uuid);

            attribute.addPermanentModifier(
                    new AttributeModifier(
                            uuid,
                            name,
                            amount,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }
    }

    public static boolean isProtected(Player player) {
        return LOCKED_HALOS.containsKey(player.getUUID());
    }

    /**
     * Restore the latest snapshot.
     *
     * This method MUST be called from Minecraft's main thread.
     */
    public static void restoreSnapshot(Player player) {

        HaloSnapshot snapshot =
                SNAPSHOTS.get(player.getUUID());

        if (snapshot == null) {
            player.setHealth(SAFE_HEALTH);
            return;
        }

        snapshot.restore(player);

        /*
         * A restored player must not remain in a removed/dead state.
         */
        player.revive();

        player.setHealth(SAFE_HEALTH);

        ensureHaloEquipped(player);

        applyAttributes(player);

        player.getAbilities().mayfly = true;
player.getAbilities().invulnerable = true;
player.getAbilities().setFlyingSpeed(FLYING_SPEED);

/*
 * Survivalでもクリエイティブと同じように飛行可能。
 * mayflyだけでは飛行状態そのものを維持しないので、
 * isFlyingも毎tick維持する。
 */
if (!player.getAbilities().flying) {
    player.getAbilities().flying = true;
}
    }

    public static void forget(Player player) {

        UUID uuid = player.getUUID();

        SNAPSHOTS.remove(uuid);
        LOCKED_HALOS.remove(uuid);
    }

    /**
     * Player-only time-reversal snapshot.
     */
    public static final class HaloSnapshot {

        private final CompoundTag nbt;

        private HaloSnapshot(CompoundTag nbt) {
            this.nbt = nbt;
        }

        public static HaloSnapshot capture(Player player) {

            CompoundTag tag = new CompoundTag();

            /*
             * saveWithoutId stores the player's serializable state
             * without allowing the entity UUID to be replaced.
             */
            player.saveWithoutId(tag);

            return new HaloSnapshot(tag.copy());
        }

        public void restore(Player player) {

            /*
             * NBT is restored on the server thread only.
             */
            player.load(nbt.copy());

            /*
             * Do not allow the snapshot to permanently turn the player
             * into a dead/removed entity.
             */
            player.revive();

            player.setHealth(SAFE_HEALTH);

            player.clearFire();

            player.setDeltaMovement(
                    player.getDeltaMovement()
            );
        }
    }



@SubscribeEvent
public static void onLivingDeath(LivingDeathEvent event) {

    if (!(event.getEntity() instanceof Player player)) {
        return;
    }

    if (!isProtected(player)) {
        return;
    }

    event.setCanceled(true);

    restoreSnapshot(player);
}

}