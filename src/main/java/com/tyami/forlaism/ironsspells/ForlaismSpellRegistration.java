package com.tyami.forlaism.ironsspells;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.ironsspells.spell.StopSpell;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForlaismSpellRegistration {

    public static final ResourceLocation STOP_ID =
            new ResourceLocation(Forlaism.MOD_ID, "stop");

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        // Iron's Spells の呪文レジストリ以外は無視
        if (!event.getRegistryKey().equals(SpellRegistry.SPELL_REGISTRY_KEY)) {
            return;
        }

        var registry = event.getForgeRegistry();
        if (registry == null) return;

        if (!registry.containsKey(STOP_ID)) {
            registry.register(STOP_ID, new StopSpell());
            System.out.println("[Forlaism] Stop spell registered.");
        }
    }
}