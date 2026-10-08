package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.entity.CreeperLordEntity;
import com.tyami.forlaism.entity.CrescentSlashEntity;
import com.tyami.forlaism.entity.EliteZombieEntity;
import com.tyami.forlaism.entity.EndermanLordEntity;
import com.tyami.forlaism.entity.FactotumMinionEntity;
import com.tyami.forlaism.entity.FactotumOverlordEntity;
import com.tyami.forlaism.entity.FactotumPhantomEntity;
import com.tyami.forlaism.entity.SkeletonLordEntity;
import com.tyami.forlaism.entity.WitherSkeletonLordEntity;
import com.tyami.forlaism.entity.ZombieLordEntity;
import com.tyami.forlaism.entity.WitherSkeletonLordEntity;
import com.tyami.forlaism.entity.EndermanLordEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.tyami.forlaism.entity.CreeperLordEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.tyami.forlaism.entity.FactotumOverlordEntity;

@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Forlaism.MOD_ID);

    public static final RegistryObject<EntityType<CrescentSlashEntity>> CRESCENT_SLASH =
            ENTITY_TYPES.register("crescent_slash",
                    () -> EntityType.Builder.<CrescentSlashEntity>of(CrescentSlashEntity::new, MobCategory.MISC)
                            .sized(2.0F, 0.5F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("crescent_slash"));

    public static final RegistryObject<EntityType<FactotumMinionEntity>> FACTOTUM_MINION =
            ENTITY_TYPES.register("factotum_minion",
                    () -> EntityType.Builder.<FactotumMinionEntity>of(FactotumMinionEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.8F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("factotum_minion"));

    public static final RegistryObject<EntityType<FactotumPhantomEntity>> FACTOTUM_PHANTOM =
            ENTITY_TYPES.register("factotum_phantom",
                    () -> EntityType.Builder.<FactotumPhantomEntity>of(FactotumPhantomEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("factotum_phantom"));

public static final RegistryObject<EntityType<FactotumOverlordEntity>> FACTOTUM_OVERLORD =
        ENTITY_TYPES.register("factotum_overlord",
                () -> EntityType.Builder.<FactotumOverlordEntity>of(
                                FactotumOverlordEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(1.5F, 3.0F)
                        .clientTrackingRange(32)
                        .updateInterval(1)
                        .fireImmune()
                        .build("factotum_overlord"));

// クラス内に追加

public static final RegistryObject<EntityType<ZombieLordEntity>> ZOMBIE_LORD =
        ENTITY_TYPES.register("zombie_lord",
                () -> EntityType.Builder.<ZombieLordEntity>of(
                                ZombieLordEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(0.7F, 2.0F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("zombie_lord"));

public static final RegistryObject<EntityType<EliteZombieEntity>> ELITE_ZOMBIE =
        ENTITY_TYPES.register("elite_zombie",
                () -> EntityType.Builder.<EliteZombieEntity>of(
                                EliteZombieEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(0.6F, 1.95F)
                        .clientTrackingRange(48)
                        .updateInterval(2)
                        .build("elite_zombie"));
public static final RegistryObject<EntityType<SkeletonLordEntity>> SKELETON_LORD =
        ENTITY_TYPES.register("skeleton_lord",
                () -> EntityType.Builder.<SkeletonLordEntity>of(
                                SkeletonLordEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(0.6F, 1.99F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("skeleton_lord"));

public static final RegistryObject<EntityType<CreeperLordEntity>> CREEPER_LORD =
        ENTITY_TYPES.register("creeper_lord",
                () -> EntityType.Builder.<CreeperLordEntity>of(
                                CreeperLordEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(0.6F, 1.7F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("creeper_lord"));
public static final RegistryObject<EntityType<WitherSkeletonLordEntity>> WITHER_SKELETON_LORD =
        ENTITY_TYPES.register("wither_skeleton_lord",
                () -> EntityType.Builder.<WitherSkeletonLordEntity>of(
                                WitherSkeletonLordEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(0.7F, 2.4F)
                        .clientTrackingRange(96)
                        .updateInterval(1)
                        .fireImmune()
                        .build("wither_skeleton_lord"));

public static final RegistryObject<EntityType<EndermanLordEntity>> ENDERMAN_LORD =
        ENTITY_TYPES.register("enderman_lord",
                () -> EntityType.Builder.<EndermanLordEntity>of(
                                EndermanLordEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(0.7F, 2.9F)
                        .clientTrackingRange(128)
                        .updateInterval(1)
                        .build("enderman_lord"));

public static final RegistryObject<EntityType<com.tyami.forlaism.entity.BossCoreEntity>> BOSS_CORE =
        ENTITY_TYPES.register("boss_core",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.BossCoreEntity>of(
                                com.tyami.forlaism.entity.BossCoreEntity::new,
                                MobCategory.MISC
                        )
                        .sized(1.0F, 1.8F)
                        .clientTrackingRange(128)
                        .updateInterval(1)
                        .fireImmune()
                        .noSummon()
                        .build("boss_core"));

    public static final RegistryObject<EntityType<com.tyami.forlaism.entity.EndWardenEntity>> END_WARDEN =
            ENTITY_TYPES.register("end_warden",
                    () -> EntityType.Builder.<com.tyami.forlaism.entity.EndWardenEntity>of(
                                    com.tyami.forlaism.entity.EndWardenEntity::new,
                                    MobCategory.MONSTER
                            )
                            .sized(1.0F, 3.5F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .fireImmune()
                            .build("end_warden"));

                                public static final RegistryObject<EntityType<com.tyami.forlaism.entity.EndWardenReverseEntity>> END_WARDEN_REVERSE =
            ENTITY_TYPES.register("end_warden_reverse",
                    () -> EntityType.Builder.<com.tyami.forlaism.entity.EndWardenReverseEntity>of(
                                    com.tyami.forlaism.entity.EndWardenReverseEntity::new,
                                    MobCategory.MONSTER
                            )
                            .sized(1.0F, 3.5F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .fireImmune()
                            .build("end_warden_reverse"));
public static final RegistryObject<EntityType<com.tyami.forlaism.entity.EndWardenFinalEntity>> END_WARDEN_FINAL =
        ENTITY_TYPES.register("end_warden_final",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.EndWardenFinalEntity>of(
                                com.tyami.forlaism.entity.EndWardenFinalEntity::new,
                                MobCategory.MONSTER
                        )
                        .sized(1.2F, 4.0F)
                        .clientTrackingRange(256)
                        .updateInterval(1)
                        .fireImmune()
                        .build("end_warden_final"));

public static final RegistryObject<EntityType<com.tyami.forlaism.entity.EnergyKnifeEntity>> ENERGY_KNIFE =
        ENTITY_TYPES.register("energy_knife",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.EnergyKnifeEntity>of(
                                com.tyami.forlaism.entity.EnergyKnifeEntity::new,
                                MobCategory.MISC
                        )
                        .sized(0.3F, 0.3F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("energy_knife"));

public static final RegistryObject<EntityType<com.tyami.forlaism.entity.PrismLightOrbEntity>> PRISM_LIGHT_ORB =
        ENTITY_TYPES.register("prism_light_orb",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.PrismLightOrbEntity>of(
                                com.tyami.forlaism.entity.PrismLightOrbEntity::new,
                                MobCategory.MISC
                        )
                        .sized(0.3F, 0.3F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("prism_light_orb"));

public static final RegistryObject<EntityType<com.tyami.forlaism.entity.FraslatiaThrowEntity>> FRASLATIA_THROW =
        ENTITY_TYPES.register("fraslatia_throw",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.FraslatiaThrowEntity>of(
                                com.tyami.forlaism.entity.FraslatiaThrowEntity::new,
                                MobCategory.MISC
                        )
                        .sized(0.6F, 0.6F)
                        .clientTrackingRange(256)   // ← 視認距離を伸ばす
                        .updateInterval(1)            // ← 毎tick同期
                        .build("fraslatia_throw"));

public static final RegistryObject<EntityType<com.tyami.forlaism.entity.SakuraBulletEntity>> SAKURA_BULLET =
        ENTITY_TYPES.register("sakura_bullet",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.SakuraBulletEntity>of(
                                com.tyami.forlaism.entity.SakuraBulletEntity::new,
                                MobCategory.MISC
                        )
                        .sized(0.3F, 0.3F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("sakura_bullet"));

public static final RegistryObject<EntityType<com.tyami.forlaism.entity.HaniwaNoYariProjectile>> HANIWA_NO_YARI =
        ENTITY_TYPES.register("haniwa_no_yari",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.HaniwaNoYariProjectile>of(
                                com.tyami.forlaism.entity.HaniwaNoYariProjectile::new,
                                MobCategory.MISC
                        )
                        .sized(0.5F, 0.5F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("haniwa_no_yari"));

public static final RegistryObject<EntityType<com.tyami.forlaism.entity.IinekoreKnifeEntity>> IINEKORE_KNIFE =
        ENTITY_TYPES.register("iinekore_knife",
                () -> EntityType.Builder.<com.tyami.forlaism.entity.IinekoreKnifeEntity>of(
                                com.tyami.forlaism.entity.IinekoreKnifeEntity::new,
                                MobCategory.MISC
                        )
                        .sized(0.4F, 0.4F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build("iinekore_knife"));

    // =========================================================
    // Yatsuhanisaku: Meteor
    // =========================================================
    public static final RegistryObject<EntityType<com.tyami.forlaism.entity.MeteorEntity>> METEOR =
            ENTITY_TYPES.register("meteor",
                    () -> EntityType.Builder.<com.tyami.forlaism.entity.MeteorEntity>of(
                                    com.tyami.forlaism.entity.MeteorEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(3.0F, 3.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .fireImmune()
                            .build("meteor"));


    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(FACTOTUM_PHANTOM.get(), FactotumPhantomEntity.createAttributes().build());
        event.put(FACTOTUM_OVERLORD.get(), FactotumOverlordEntity.createAttributes().build());
        event.put(ZOMBIE_LORD.get(), ZombieLordEntity.createAttributes().build());
        event.put(ELITE_ZOMBIE.get(), EliteZombieEntity.createAttributes().build());
        event.put(SKELETON_LORD.get(), SkeletonLordEntity.createAttributes().build());
        event.put(CREEPER_LORD.get(), CreeperLordEntity.createAttributes().build());
        event.put(WITHER_SKELETON_LORD.get(), WitherSkeletonLordEntity.createAttributes().build());
        event.put(ENDERMAN_LORD.get(), EndermanLordEntity.createAttributes().build());
        event.put(BOSS_CORE.get(), com.tyami.forlaism.entity.BossCoreEntity.createAttributes().build());
                event.put(END_WARDEN.get(), com.tyami.forlaism.entity.EndWardenEntity.createAttributes().build());
                        event.put(END_WARDEN_REVERSE.get(), com.tyami.forlaism.entity.EndWardenReverseEntity.createAttributes().build());
                        event.put(END_WARDEN_FINAL.get(), com.tyami.forlaism.entity.EndWardenFinalEntity.createAttributes().build());
    }
}
