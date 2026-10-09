package com.tyami.forlaism;

import com.tyami.forlaism.registry.*;
import com.tyami.forlaism.screen.FusionMachineScreen;
import com.tyami.forlaism.screen.InserterScreen;
import com.tyami.forlaism.screen.Tier2MachineScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import com.tyami.forlaism.client.renderer.EndWardenFinalRenderer;
import com.tyami.forlaism.client.model.EndWardenModel;
import com.tyami.forlaism.client.renderer.EndWardenRenderer;
import com.tyami.forlaism.client.renderer.AethericGeneratorRenderer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.tyami.forlaism.client.halo.HaloOfTheAdventModel;
import com.tyami.forlaism.client.halo.HaloOfTheAdventRenderer;
import com.tyami.forlaism.client.halo.HaloOfTheFirmament;
import com.tyami.forlaism.client.halo.HaloOfTheFirmamentRenderer;
import net.minecraft.client.Minecraft;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import net.minecraftforge.client.event.EntityRenderersEvent;

import com.tyami.forlaism.client.renderer.CrescentSlashRenderer;
import com.tyami.forlaism.client.renderer.EndWardenFinalRenderer;
import com.tyami.forlaism.client.renderer.FactotumMinionRenderer;
import com.tyami.forlaism.client.renderer.FactotumOverlordRenderer;
import com.tyami.forlaism.client.renderer.FactotumPhantomRenderer;
import com.tyami.watelib.WaveTextRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import com.tyami.watelib.text.AnimatedText;
import net.minecraftforge.data.event.GatherDataEvent;
import com.tyami.forlaism.client.renderer.FactotumOverlordRenderer;

@Mod(Forlaism.MOD_ID)
public class Forlaism {

    public static final String MOD_ID = "forlaism";

    public Forlaism() {

        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        Items.ITEMS.register(modEventBus);
        com.tyami.forlaism.registry.ModEnchantments.ENCHANTMENTS.register(modEventBus);
        Blocks.BLOCKS.register(modEventBus);
        Fluids.FLUIDS.register(modEventBus);
        FluidTypes.FLUID_TYPES.register(modEventBus);
        BlockEntities.BLOCK_ENTITIES.register(modEventBus);
        MenuTypes.MENUS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        CreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        com.tyami.forlaism.registry.ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);
        

        

        com.tyami.forlaism.network.FactotumPacketHandler.register();
        com.tyami.forlaism.registry.ModPotions.POTIONS.register(modEventBus);
        com.tyami.forlaism.magic.MagicCircleSummoner.registerNetwork();

        modEventBus.addListener(this::commonSetup);
        


        System.out.println("[Forlaism] Forlaism has been loaded!");
    // サーバー起動/停止フック
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        com.tyami.forlaism.quantum.QuantumTransferNetwork.setServer(event.getServer());
        com.tyami.forlaism.world.PossibilityGlobalData.isPending();
    }



@Mod.EventBusSubscriber(
        modid = MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public static class ClientModEvents {

    @SubscribeEvent
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event) {

        event.registerLayerDefinition(
                HaloOfTheAdventModel.LAYER_LOCATION,
                HaloOfTheAdventModel::createBodyLayer
        );
        event.registerLayerDefinition(
                HaloOfTheFirmament.LAYER_LOCATION,
                HaloOfTheFirmament::createBodyLayer
        );
                event.registerLayerDefinition(
                EndWardenModel.LAYER_LOCATION,
                EndWardenModel::createBodyLayer
        );
        event.registerLayerDefinition(
        com.tyami.forlaism.client.model.IinekoreKnifeModel.LAYER_LOCATION,
        com.tyami.forlaism.client.model.IinekoreKnifeModel::createBodyLayer
);
        
}

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.CRESCENT_SLASH.get(), CrescentSlashRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FACTOTUM_MINION.get(), FactotumMinionRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FACTOTUM_PHANTOM.get(), FactotumPhantomRenderer::new);
        event.registerEntityRenderer(
        ModEntityTypes.FACTOTUM_OVERLORD.get(),
        FactotumOverlordRenderer::new
);
        event.registerEntityRenderer(
                ModEntityTypes.END_WARDEN.get(),
                EndWardenRenderer::new
        );
        event.registerBlockEntityRenderer(
        BlockEntities.AETHERIC_GENERATOR.get(),
        AethericGeneratorRenderer::new
        );
        event.registerBlockEntityRenderer(
                BlockEntities.RITUAL_ALTAR.get(),
                com.tyami.forlaism.client.renderer.AltarBlockEntityRenderer::new
        );
        event.registerEntityRenderer(
            ModEntityTypes.ZOMBIE_LORD.get(),
            net.minecraft.client.renderer.entity.ZombieRenderer::new
    );
    event.registerEntityRenderer(
            ModEntityTypes.ELITE_ZOMBIE.get(),
            net.minecraft.client.renderer.entity.ZombieRenderer::new
    );
    event.registerEntityRenderer(
        ModEntityTypes.SKELETON_LORD.get(),
        net.minecraft.client.renderer.entity.SkeletonRenderer::new
);
event.registerEntityRenderer(
                ModEntityTypes.END_WARDEN_REVERSE.get(),
                EndWardenRenderer::new
);
        event.registerEntityRenderer(
                ModEntityTypes.SAKURA_BULLET.get(),
                com.tyami.forlaism.client.renderer.SakuraBulletRenderer::new
        );
event.registerEntityRenderer(
        ModEntityTypes.END_WARDEN_FINAL.get(),
        EndWardenFinalRenderer::new  // ← 専用レンダラ
);
event.registerEntityRenderer(
        ModEntityTypes.CREEPER_LORD.get(),
        net.minecraft.client.renderer.entity.CreeperRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.WITHER_SKELETON_LORD.get(),
        net.minecraft.client.renderer.entity.WitherSkeletonRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.ENDERMAN_LORD.get(),
        net.minecraft.client.renderer.entity.EndermanRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.BOSS_CORE.get(),
        com.tyami.forlaism.client.renderer.BossCoreEmptyRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.ENERGY_KNIFE.get(),
        com.tyami.forlaism.client.renderer.EnergyKnifeRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.PRISM_LIGHT_ORB.get(),
        com.tyami.forlaism.client.renderer.PrismLightOrbRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.FRASLATIA_THROW.get(),
        com.tyami.forlaism.client.renderer.FraslatiaThrowRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.HANIWA_NO_YARI.get(),
        com.tyami.forlaism.client.renderer.HaniwaNoYariRenderer::new
);
event.registerEntityRenderer(
        ModEntityTypes.IINEKORE_KNIFE.get(),
        com.tyami.forlaism.client.renderer.IinekoreKnifeRenderer::new
);
        event.registerEntityRenderer(
                ModEntityTypes.METEOR.get(),
                com.tyami.forlaism.client.renderer.MeteorRenderer::new
        );


        
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

        WaveTextRegistry.registerItemName(
        Items.FORLAISM_STAR_CORE.get(),
        stack -> AnimatedText.of("フォラリス星核")
.wave(2.5f, 0.05f, 0.5f) // 上下の揺れ（ちょっとゆっくり）
            .gradient(0xFF98FB98, 0xFFAFEEEE, 0xFFE0FFFF) // さっきの淡いカラー
            .gradientSpeed(2.5f)   // スピードを大幅アップ（シャカシャカ動かす）
            .gradientPhase(0.15f)  // 位相のズレを小さくして、色が溶け合うようにする
);

            MenuScreens.register(
                    MenuTypes.FORLAISM_FUSION_MACHINE_MENU.get(),
                    FusionMachineScreen::new
            );

            MenuScreens.register(
                    MenuTypes.FORLAISM_INSERTER_MENU.get(),
                    InserterScreen::new
            );

            MenuScreens.register(
                    MenuTypes.FORLAISM_ALLOY_MACHINE_MENU.get(),
                    Tier2MachineScreen::new
            );

            MenuScreens.register(
                    MenuTypes.FORLAISM_CONCENTRATOR_MENU.get(),
                    Tier2MachineScreen::new
            );
            

            MenuScreens.register(
                    MenuTypes.FORLAISM_REACTOR_MENU.get(),
                    Tier2MachineScreen::new
            );

                        net.minecraft.client.renderer.item.ItemProperties.register(
                    Items.DEMISE_ELD_METAL_BOW.get(),
                    new net.minecraft.resources.ResourceLocation(
                            "pull"
                    ),
                    (stack, level, entity, seed) -> {

                        if (entity == null) {
                            return 0.0F;
                        }

                        int used =
                                stack.getUseDuration()
                                        - entity.getUseItemRemainingTicks();

                        return Math.min(
                                1.0F,
                                used / 30.0F
                        );
                    }
            );

ItemProperties.register(
        Items.SOVEREIGN_SCEPTER_SWORD.get(),
        new ResourceLocation(
                Forlaism.MOD_ID,
                "sovereign_animation"
        ),
        (stack, level, entity, seed) -> {

            if (level == null) {
                return 0.0F;
            }

            /*
             * 8フレーム
             *
             * 0
             * 1
             * 2
             * ...
             * 7
             * 0
             */
            int frame =
                    (int)(level.getGameTime() % 8);

            return frame / 8.0F;
        }
);

            net.minecraft.client.renderer.item.ItemProperties.register(
                    Items.DEMISE_ELD_METAL_BOW.get(),
                    new net.minecraft.resources.ResourceLocation(
                            "pulling"
                    ),
                    (stack, level, entity, seed) ->
                            entity != null
                                    && entity.isUsingItem()
                                    && entity.getUseItem() == stack
                                    ? 1.0F
                                    : 0.0F
            );

            CuriosRendererRegistry.register(
                    Items.HALO_OF_THE_ADVENT.get(),
                    () -> new HaloOfTheAdventRenderer(
                            new HaloOfTheAdventModel<>(
                                    Minecraft.getInstance()
                                            .getEntityModels()
                                            .bakeLayer(
                                                    HaloOfTheAdventModel.LAYER_LOCATION
                                            )
                            )
                    )
            );



            CuriosRendererRegistry.register(
        Items.HALO_OF_THE_FIRMAMENT.get(),
        () -> new HaloOfTheFirmamentRenderer(
                new HaloOfTheFirmament<>(
                        Minecraft.getInstance()
                                .getEntityModels()
                                .bakeLayer(
                                        HaloOfTheFirmament.LAYER_LOCATION
                                )
                )
        )
);
        });
    }
}




private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        com.tyami.forlaism.quantum.QuantumFusionRecipes.bootstrap();
        com.tyami.forlaism.quantum.ReactorRecipes.bootstrap();
        com.tyami.forlaism.recipe.ModBrewingRecipes.registerRecipes();
        com.tyami.forlaism.recipe.AltarRecipes.bootstrap();
    });
}



}
