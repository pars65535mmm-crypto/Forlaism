package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.TrialHaloItem;
import com.tyami.forlaism.item.BathPowderItem;
import com.tyami.forlaism.item.DemiseEldMetalBowItem;
import com.tyami.forlaism.item.DreamBookItem;
import com.tyami.forlaism.item.GarbageMetalArmorMaterial;
import com.tyami.forlaism.item.GarbageMetalItem;
import com.tyami.forlaism.item.GarbageMetalItem;
import com.tyami.forlaism.item.QuantumWrenchItem;
import com.tyami.forlaism.item.GarbageMetalTier;
import com.tyami.forlaism.item.HaloOfTheAdventItem;
import com.tyami.forlaism.item.HaloOfTheFirmamentItem;
import com.tyami.forlaism.item.KyogenkakuItem;
import com.tyami.forlaism.item.MadoromuItem;
import com.tyami.forlaism.item.MugenkakuItem;
import com.tyami.forlaism.item.ForlaismWeatherControllerItem;
import com.tyami.forlaism.item.RegaliaOfTheFactotumItem;
import com.tyami.forlaism.item.SovereignScepterSwordItem;
import com.tyami.forlaism.item.StarOreBookItem;
import com.tyami.forlaism.item.TokinoStaffTier3Item;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import com.tyami.forlaism.ForalisItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class Items {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Forlaism.MOD_ID);

    // フォラリス関連
    public static final RegistryObject<Item> FORLAISM_CRYSTAL =
            ITEMS.register("forlaism_crystal",
                    () -> new com.tyami.forlaism.item.ForlaismCrystalItem(new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_CRUDE_POWDER =
            ITEMS.register("forlaism_crude_powder",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_POWDER =
            ITEMS.register("forlaism_powder",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_CULTURE_BOTTLE =
            ITEMS.register("forlaism_culture_bottle",
                    () -> new com.tyami.forlaism.item.CultureBottleItem(new Item.Properties()));

    // 鉄・鋼鉄関連
    public static final RegistryObject<Item> CRUSHED_IRON =
            ITEMS.register("crushed_iron",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ROUGH_STEEL =
            ITEMS.register("rough_steel",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STEEL_QUESTION =
            ITEMS.register("steel_question",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STEEL_INGOT =
            ITEMS.register("steel_ingot",
                    () -> new Item(new Item.Properties()));

    // 代替素材・中間素材
    public static final RegistryObject<Item> REDSTONE_QUESTION =
            ITEMS.register("redstone_question",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SAGE_STONE =
            ITEMS.register("sage_stone",
                    () -> new Item(new Item.Properties()));

    // フォラリス多結晶体関連
    public static final RegistryObject<Item> FORLAISM_POLYCRYSTAL =
            ITEMS.register("forlaism_polycrystal",
                    () -> new com.tyami.forlaism.item.PolycrystalItem(new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_POLYCRYSTAL_BUCKET =
            ITEMS.register("forlaism_polycrystal_bucket",
                    () -> new com.tyami.forlaism.item.PolycrystalBucketItem(new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_POLYCRYSTAL_SOLUTION_BUCKET =
            ITEMS.register("forlaism_polycrystal_solution_bucket",
                    () -> new com.tyami.forlaism.item.PolycrystalSolutionBucketItem(new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_POLYCRYSTAL_BOTTLE =
            ITEMS.register("forlaism_polycrystal_bottle",
                    () -> new ForalisItem(new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_POLYCRYSTAL_SOLUTION_BOTTLE =
            ITEMS.register("forlaism_polycrystal_solution_bottle",
                    () -> new ForalisItem(new Item.Properties()));

    // 刻乃杖
    public static final RegistryObject<Item> TOKINO_STAFF_TIER1 =
            ITEMS.register("tokino_staff_tier1",
                    () -> new com.tyami.forlaism.item.TokinoStaffItem(new Item.Properties().stacksTo(1)));

    // バケツ
    public static final RegistryObject<Item> FORLAISM_CULTURE_BUCKET =
            ITEMS.register("forlaism_culture_bucket",
                    () -> new BucketItem(
                            Fluids.FORLAISM_CULTURE,
                            new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)
                    ));

    public static final RegistryObject<Item> FO_BUCKET =
            ITEMS.register("fo_bucket",
                    () -> new BucketItem(
                            Fluids.FO,
                            new Item.Properties().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)
                    ));

    // ブロックアイテム
    public static final RegistryObject<Item> FORLAISM_ORE =
            ITEMS.register("forlaism_ore",
                    () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_FORLAISM_ORE =
            ITEMS.register("deepslate_forlaism_ore",
                    () -> new net.minecraft.world.item.BlockItem(Blocks.DEEPSLATE_FORLAISM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_CRYSTAL_BLOCK =
            ITEMS.register("forlaism_crystal_block",
                    () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_CRYSTAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> STEEL_BLOCK =
            ITEMS.register("steel_block",
                    () -> new net.minecraft.world.item.BlockItem(Blocks.STEEL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_HIGH_CRYSTAL_BLOCK =
            ITEMS.register("forlaism_high_crystal_block",
                    () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_HIGH_CRYSTAL_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_FUSION_MACHINE =
            ITEMS.register("forlaism_fusion_machine",
                    () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_FUSION_MACHINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> FORLAISM_INSERTER =
            ITEMS.register("forlaism_inserter",
                    () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_INSERTER.get(), new Item.Properties()));
    // Tier 2 items
    public static final RegistryObject<Item> FORLAISM_IRON_ALLOY =
            ITEMS.register("forlaism_iron_alloy",
                    () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FORLAISM_IRON_ALLOY_ROD =
            ITEMS.register("forlaism_iron_alloy_rod",
                    () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FORLAISM_CONCENTRATED_IRON_ALLOY =
            ITEMS.register("forlaism_concentrated_iron_alloy",
                    () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FORLAISM_CONCENTRATED_IRON_ALLOY_ROD =
            ITEMS.register("forlaism_concentrated_iron_alloy_rod",
                    () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FORLAISM_ORE_STAR =
            ITEMS.register("forlaism_ore_star", () -> new Item(new Item.Properties()));

// Tier 3 素材
public static final RegistryObject<Item> INCOMPLETE_FORLAISM_STAR_CORE =
        ITEMS.register(
                "incomplete_forlaism_star_core",
                () -> new Item(new Item.Properties())
        );

public static final RegistryObject<Item> FORLAISM_STAR_CORE =
        ITEMS.register(
                "forlaism_star_core",
                () -> new Item(new Item.Properties())
        );

    // Tier 2 staff
    public static final RegistryObject<Item> TOKINO_STAFF_TIER2 =
            ITEMS.register("tokino_staff_tier2",
                    () -> new com.tyami.forlaism.item.TokinoStaffTier2Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FORLAISM_ALLOY_MACHINE = ITEMS.register("forlaism_alloy_machine", () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_ALLOY_MACHINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> FORLAISM_CONCENTRATOR = ITEMS.register("forlaism_concentrator", () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_CONCENTRATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> FORLAISM_REACTOR = ITEMS.register("forlaism_reactor", () -> new net.minecraft.world.item.BlockItem(Blocks.FORLAISM_REACTOR.get(), new Item.Properties()));



public static final RegistryObject<Item> TRIAL_HALO = ITEMS.register(
        "trial_halo",
        TrialHaloItem::new
);
public static final RegistryObject<Item> HALO_OF_THE_ADVENT = ITEMS.register(
        "halo_of_the_advent",
        () -> new HaloOfTheAdventItem(new Item.Properties().stacksTo(1))
);

public static final RegistryObject<Item> HALO_OF_THE_FIRMAMENT =
        ITEMS.register(
                "halo_of_the_firmament",
                () -> new HaloOfTheFirmamentItem(new Item.Properties().stacksTo(1))
        );



        public static final RegistryObject<Item> SYNTHETIC_SMITHING_TEMPLATE =
        ITEMS.register("synthetic_smithing_template",
                () -> new Item(new Item.Properties().stacksTo(64)));

public static final RegistryObject<Item> GARBAGE_METAL =
        ITEMS.register("garbage_metal",
                () -> new GarbageMetalItem(new Item.Properties()));


public static final RegistryObject<Item> GARBAGE_METAL_SWORD =
        ITEMS.register("garbage_metal_sword",
                () -> new net.minecraft.world.item.SwordItem(
                        GarbageMetalTier.INSTANCE,
                        4,
                        -1.0F,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_AXE =
        ITEMS.register("garbage_metal_axe",
                () -> new net.minecraft.world.item.AxeItem(
                        GarbageMetalTier.INSTANCE,
                        35.0F,
                        -3.9F,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_PICKAXE =
        ITEMS.register("garbage_metal_pickaxe",
                () -> new net.minecraft.world.item.PickaxeItem(
                        GarbageMetalTier.INSTANCE,
                        1,
                        -2.8F,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_SHOVEL =
        ITEMS.register("garbage_metal_shovel",
                () -> new net.minecraft.world.item.ShovelItem(
                        GarbageMetalTier.INSTANCE,
                        1.5F,
                        -3.0F,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_HOE =
        ITEMS.register("garbage_metal_hoe",
                () -> new net.minecraft.world.item.HoeItem(
                        GarbageMetalTier.INSTANCE,
                        -3,
                        0.0F,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_HELMET =
        ITEMS.register("garbage_metal_helmet",
                () -> new net.minecraft.world.item.ArmorItem(
                        GarbageMetalArmorMaterial.INSTANCE,
                        net.minecraft.world.item.ArmorItem.Type.HELMET,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_CHESTPLATE =
        ITEMS.register("garbage_metal_chestplate",
                () -> new net.minecraft.world.item.ArmorItem(
                        GarbageMetalArmorMaterial.INSTANCE,
                        net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_LEGGINGS =
        ITEMS.register("garbage_metal_leggings",
                () -> new net.minecraft.world.item.ArmorItem(
                        GarbageMetalArmorMaterial.INSTANCE,
                        net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> GARBAGE_METAL_BOOTS =
        ITEMS.register("garbage_metal_boots",
                () -> new net.minecraft.world.item.ArmorItem(
                        GarbageMetalArmorMaterial.INSTANCE,
                        net.minecraft.world.item.ArmorItem.Type.BOOTS,
                        new Item.Properties()
                ));

public static final RegistryObject<Item> ADAMETAL =
        ITEMS.register("adametal",
                () -> new Item(new Item.Properties()));



public static final RegistryObject<Item> DEMISE_ELD_METAL_BOW = ITEMS.register(
        "demise_eld_metal_bow",
        () -> new DemiseEldMetalBowItem(
                new Item.Properties()
                        .durability(2048)
        )
);

public static final RegistryObject<Item> REGALIA_OF_THE_FACTOTUM = ITEMS.register(
        "regalia_of_the_factotum",
        () -> new RegaliaOfTheFactotumItem(new Item.Properties())
);




public static final RegistryObject<Item> SOVEREIGN_SCEPTER_SWORD =
        ITEMS.register(
                "sovereign_scepter_sword",
                () -> new SovereignScepterSwordItem(
                        new Item.Properties()
                                .stacksTo(1)
                                .durability(4096)
                )
        );


public static final RegistryObject<Item> AETHERIC_GENERATOR =

        ITEMS.register("aetheric_generator",

                () -> new net.minecraft.world.item.BlockItem(

                        Blocks.AETHERIC_GENERATOR.get(),

                        new Item.Properties()));



public static final RegistryObject<Item> FORLAISM_WEATHER_CONTROLLER =
        ITEMS.register(
                "forlaism_weather_controller",
                () -> new ForlaismWeatherControllerItem(
                        new Item.Properties().stacksTo(1)
                )
        );

public static final RegistryObject<Item> DREAM_FRAGMENT =
        ITEMS.register("dream_fragment",
                () -> new Item(
                        new Item.Properties()
                ));


public static final RegistryObject<Item> BATH_POWDER =
        ITEMS.register(
                "bath_powder",
                () -> new BathPowderItem(
                        new Item.Properties()
                )
        );

public static final RegistryObject<Item> STAR_ORE_BOOK =
        ITEMS.register("star_ore_book",
                () -> new StarOreBookItem(new Item.Properties().stacksTo(16)));


                public static final RegistryObject<Item> DREAM_BOOK =
        ITEMS.register("dream_book",
                () -> new DreamBookItem(new Item.Properties().stacksTo(16)));

                public static final RegistryObject<Item> TOKINO_STAFF_TIER3 =
        ITEMS.register("tokino_staff_tier3",
                () -> new TokinoStaffTier3Item(new Item.Properties().stacksTo(1)));


public static final RegistryObject<Item> MUGENKAKU =
        ITEMS.register("mugenkaku",
                () -> new MugenkakuItem(new Item.Properties()));

public static final RegistryObject<Item> KYOGENKAKU =
        ITEMS.register("kyogenkaku",
                () -> new KyogenkakuItem(new Item.Properties()));

public static final RegistryObject<Item> MADOROMU =
        ITEMS.register("madoromu",
                () -> new MadoromuItem(new Item.Properties()));

public static final RegistryObject<Item> ROOTER_BUNDLE =
        ITEMS.register("rooter_bundle",
                () -> new com.tyami.forlaism.item.RooterBundleItem(new Item.Properties()));
        

public static final RegistryObject<Item> TRIAL_STAFF =
        ITEMS.register("trial_staff",
                () -> new com.tyami.forlaism.item.TrialStaffItem(new Item.Properties().stacksTo(1)));
        
public static final RegistryObject<Item> NSAGE_STONE =
        ITEMS.register("nsage_stone",
                () -> new com.tyami.forlaism.item.NSageStoneItem(
                        new Item.Properties()
                ));

public static final RegistryObject<Item> MADOROMU_BLOCK =
        ITEMS.register("madoromu_block",
                () -> new net.minecraft.world.item.BlockItem(
                        Blocks.MADOROMU_BLOCK.get(),
                        new Item.Properties()
                ));

public static final RegistryObject<Item> TRUE_SAGE_STONE =
        ITEMS.register("true_sage_stone",
                () -> new com.tyami.forlaism.item.TrueSageStoneItem(
                        new Item.Properties()
                ));

public static final RegistryObject<Item> SWREQUIMET_BLADE =
        ITEMS.register("swrequimet_blade",
                () -> new com.tyami.forlaism.item.SwrequimetBladeItem(
                        new Item.Properties().stacksTo(1).fireResistant()
                ));

public static final RegistryObject<Item> NULL_SUGAR =
        ITEMS.register("null_sugar",
                () -> new com.tyami.forlaism.item.NullSugarItem(
                        new Item.Properties()
                ));
        

// クラス内に追記
public static final RegistryObject<Item> QUANTUM_TRANSFER_DEVICE_IN =
        ITEMS.register("quantum_transfer_device_in",
                () -> new net.minecraft.world.item.BlockItem(
                        Blocks.QUANTUM_TRANSFER_DEVICE_IN.get(),
                        new Item.Properties()));

public static final RegistryObject<Item> QUANTUM_TRANSFER_DEVICE_OUT =
        ITEMS.register("quantum_transfer_device_out",
                () -> new net.minecraft.world.item.BlockItem(
                        Blocks.QUANTUM_TRANSFER_DEVICE_OUT.get(),
                        new Item.Properties()));

public static final RegistryObject<Item> QUANTUM_WRENCH =
        ITEMS.register("quantum_wrench",
                () -> new QuantumWrenchItem(new Item.Properties().stacksTo(1)));

                

// ===== ナイフシリーズ =====

public static final RegistryObject<Item> KNIFE =
        ITEMS.register("knife",
                () -> new com.tyami.forlaism.item.KnifeItem(
                        new Item.Properties()
                ));

public static final RegistryObject<Item> FO_KNIFE =
        ITEMS.register("fo_knife",
                () -> new com.tyami.forlaism.item.FoKnifeItem(
                        new Item.Properties().stacksTo(1)
                ));

public static final RegistryObject<Item> ASSASSIN_KNIFE =
        ITEMS.register("a_knife",
                () -> new com.tyami.forlaism.item.AssassinKnifeItem(
                        new Item.Properties().stacksTo(1)
                ));

public static final RegistryObject<Item> R_KNIFE =
        ITEMS.register("r_knife",
                () -> new com.tyami.forlaism.item.RKnifeItem(
                        new Item.Properties().stacksTo(1)
                ));

public static final RegistryObject<Item> SC_KNIFE =
        ITEMS.register("sc_knife",
                () -> new com.tyami.forlaism.item.ScKnifeItem(
                        new Item.Properties().stacksTo(1).fireResistant()
                ));

public static final RegistryObject<Item> NEGINAIFU =
        ITEMS.register("neginaifu",
                () -> new com.tyami.forlaism.item.NeginaifuItem(
                        new Item.Properties().stacksTo(1).fireResistant()
                ));

    public static final RegistryObject<Item> DREAM_FLOOR =
            ITEMS.register("dream_floor",
                    () -> new net.minecraft.world.item.BlockItem(
                            Blocks.DREAM_FLOOR.get(),
                            new Item.Properties()
                    ));

    public static final RegistryObject<Item> BOOK_OF_SACRIFICE =
            ITEMS.register("book_of_sacrifice",
                    () -> new com.tyami.forlaism.item.BookOfSacrificeItem(
                            new Item.Properties().stacksTo(1)
                    ));

    public static final RegistryObject<Item> KNIFE_OF_SACRIFICE =
            ITEMS.register("knife_of_sacrifice",
                    () -> new com.tyami.forlaism.item.KnifeOfSacrificeItem(
                            new Item.Properties().stacksTo(1).fireResistant()
                    ));

    public static final RegistryObject<Item> INOCHI_NO_KAKERA =
            ITEMS.register("inochi_no_kakera",
                    () -> new com.tyami.forlaism.item.InochiNoKakeraItem(
                            new Item.Properties()
                    ));


// ===== 愚者・憎悪・大罪の石シリーズ =====

public static final RegistryObject<Item> STONE_OF_FOOL =
        ITEMS.register("stone_of_fool",
                () -> new Item(new Item.Properties()));

public static final RegistryObject<Item> STONE_OF_HATRED =
        ITEMS.register("stone_of_hatred",
                () -> new Item(new Item.Properties()));

public static final RegistryObject<Item> STONE_OF_SIN =
        ITEMS.register("stone_of_sin",
                () -> new Item(new Item.Properties().fireResistant()));

        }




