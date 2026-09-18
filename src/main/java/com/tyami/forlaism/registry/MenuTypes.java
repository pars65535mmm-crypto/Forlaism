package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.screen.FusionMachineMenu;
import com.tyami.forlaism.screen.InserterMenu;
import com.tyami.forlaism.screen.Tier2MachineMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Forlaism.MOD_ID);

    public static final RegistryObject<MenuType<FusionMachineMenu>> FORLAISM_FUSION_MACHINE_MENU =
            registerMenuType("forlaism_fusion_machine_menu", FusionMachineMenu::new);

    public static final RegistryObject<MenuType<InserterMenu>> FORLAISM_INSERTER_MENU =
            registerMenuType("forlaism_inserter_menu", InserterMenu::new);
    public static final RegistryObject<MenuType<Tier2MachineMenu>> FORLAISM_ALLOY_MACHINE_MENU = registerMenuType("forlaism_alloy_machine_menu", Tier2MachineMenu::new);
    public static final RegistryObject<MenuType<Tier2MachineMenu>> FORLAISM_CONCENTRATOR_MENU = registerMenuType("forlaism_concentrator_menu", Tier2MachineMenu::new);
    public static final RegistryObject<MenuType<Tier2MachineMenu>> FORLAISM_REACTOR_MENU = registerMenuType("forlaism_reactor_menu", Tier2MachineMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }
}
