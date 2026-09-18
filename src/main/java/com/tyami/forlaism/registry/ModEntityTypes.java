package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.entity.CrescentSlashEntity;
import com.tyami.forlaism.entity.FactotumMinionEntity;
import com.tyami.forlaism.entity.FactotumPhantomEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(FACTOTUM_PHANTOM.get(), FactotumPhantomEntity.createAttributes().build());
    }
}
