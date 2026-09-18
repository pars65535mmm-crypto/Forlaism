package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.tyami.forlaism.item.GarbageMetalItem;

public class CreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, Forlaism.MOD_ID);

    public static final RegistryObject<CreativeModeTab> FORLAISM_TAB =
            CREATIVE_MODE_TABS.register("forlaism_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("item_group.forlaism.forlaism_tab"))
                            .icon(() -> new ItemStack(Items.FORLAISM_CRYSTAL.get()))
                            .displayItems((parameters, output) -> {

                                output.accept(Items.FORLAISM_CRYSTAL.get());
                                output.accept(Items.FORLAISM_CRUDE_POWDER.get());
                                output.accept(Items.FORLAISM_POWDER.get());
                                output.accept(Items.FORLAISM_CULTURE_BOTTLE.get());
                                output.accept(Items.FORLAISM_CULTURE_BUCKET.get());

                                output.accept(Items.CRUSHED_IRON.get());
                                output.accept(Items.ROUGH_STEEL.get());
                                output.accept(Items.STEEL_QUESTION.get());
                                output.accept(Items.STEEL_INGOT.get());

                                output.accept(Items.REDSTONE_QUESTION.get());
                                output.accept(Items.SAGE_STONE.get());

                                output.accept(Items.FORLAISM_POLYCRYSTAL.get());
                                output.accept(Items.FORLAISM_POLYCRYSTAL_BUCKET.get());
                                output.accept(Items.FORLAISM_POLYCRYSTAL_SOLUTION_BUCKET.get());
                                output.accept(Items.FORLAISM_POLYCRYSTAL_BOTTLE.get());
                                output.accept(Items.FORLAISM_POLYCRYSTAL_SOLUTION_BOTTLE.get());

                                output.accept(Items.FO_BUCKET.get());
                                output.accept(Items.TOKINO_STAFF_TIER1.get());
                                output.accept(Items.FORLAISM_IRON_ALLOY.get());
                                output.accept(Items.GARBAGE_METAL.get());
                                output.accept(Items.FORLAISM_IRON_ALLOY_ROD.get());
                                output.accept(Items.FORLAISM_CONCENTRATED_IRON_ALLOY.get());
                                output.accept(Items.FORLAISM_CONCENTRATED_IRON_ALLOY_ROD.get());
                                output.accept(Items.FORLAISM_ORE_STAR.get());
                                output.accept(Items.TOKINO_STAFF_TIER2.get());

                                output.accept(Items.FORLAISM_ORE.get());
                                output.accept(Items.DEEPSLATE_FORLAISM_ORE.get());
                                output.accept(Items.FORLAISM_CRYSTAL_BLOCK.get());
                                output.accept(Items.STEEL_BLOCK.get());
                                output.accept(Items.FORLAISM_HIGH_CRYSTAL_BLOCK.get());
                                output.accept(Items.FORLAISM_FUSION_MACHINE.get());
                                output.accept(Items.FORLAISM_INSERTER.get());
                                output.accept(Items.FORLAISM_ALLOY_MACHINE.get());
                                output.accept(Items.FORLAISM_CONCENTRATOR.get());
                                output.accept(Items.FORLAISM_REACTOR.get());
                                output.accept(Items.GARBAGE_METAL_SWORD.get());
                                output.accept(Items.GARBAGE_METAL_AXE.get());
                                output.accept(Items.GARBAGE_METAL_PICKAXE.get());
                                output.accept(Items.GARBAGE_METAL_SHOVEL.get());
                                output.accept(Items.GARBAGE_METAL_HOE.get());
                                output.accept(Items.GARBAGE_METAL_HELMET.get());
                                output.accept(Items.GARBAGE_METAL_CHESTPLATE.get());
                                output.accept(Items.GARBAGE_METAL_LEGGINGS.get());
                                output.accept(Items.GARBAGE_METAL_BOOTS.get());
                                output.accept(Items.SOVEREIGN_SCEPTER_SWORD.get());
                                output.accept(Items.INCOMPLETE_FORLAISM_STAR_CORE.get());
                                output.accept(Items.FORLAISM_STAR_CORE.get());
                                output.accept(Items.FORLAISM_WEATHER_CONTROLLER.get());
                                output.accept(Items.DREAM_FRAGMENT.get());
                                output.accept(Items.BATH_POWDER.get());
                                output.accept(Items.STAR_ORE_BOOK.get());
                                output.accept(Items.DREAM_BOOK.get());
                                output.accept(Items.TOKINO_STAFF_TIER3.get());
                                output.accept(Items.MUGENKAKU.get());
                                output.accept(Items.KYOGENKAKU.get());
                                output.accept(Items.MADOROMU.get());
                                output.accept(Items.ROOTER_BUNDLE.get());
                                output.accept(Items.TRIAL_STAFF.get());
                                output.accept(Items.MADOROMU_BLOCK.get());
                                output.accept(Items.TRUE_SAGE_STONE.get());
                                output.accept(Items.NULL_SUGAR.get());
                                output.accept(Items.SWREQUIMET_BLADE.get());
                                output.accept(Items.QUANTUM_TRANSFER_DEVICE_IN.get());
                                output.accept(Items.QUANTUM_TRANSFER_DEVICE_OUT.get());
                                output.accept(Items.QUANTUM_WRENCH.get());
                                // ===== ナイフシリーズ =====
                                output.accept(Items.KNIFE.get());
                                output.accept(Items.FO_KNIFE.get());
                                output.accept(Items.ASSASSIN_KNIFE.get());
                                output.accept(Items.R_KNIFE.get());
                                output.accept(Items.SC_KNIFE.get());
                                output.accept(Items.NEGINAIFU.get());
                                output.accept(Items.BOOK_OF_SACRIFICE.get());
                                output.accept(Items.KNIFE_OF_SACRIFICE.get());
                                output.accept(Items.INOCHI_NO_KAKERA.get());
                                output.accept(Items.STONE_OF_FOOL.get());
                                output.accept(Items.STONE_OF_HATRED.get());
                                output.accept(Items.STONE_OF_SIN.get());


                                output.accept(Items.ADAMETAL.get());
                               
                            })
                            .build()
            );
}
