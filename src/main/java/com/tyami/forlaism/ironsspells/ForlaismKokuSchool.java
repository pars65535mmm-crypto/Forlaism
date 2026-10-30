package com.tyami.forlaism.ironsspells;

import com.tyami.forlaism.Forlaism;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;


@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForlaismKokuSchool {

    public static final ResourceLocation KOKU_ID =
            new ResourceLocation(Forlaism.MOD_ID, "koku");

    public static SchoolType KOKU;

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        // Iron's Spells の学派レジストリ以外は無視
        if (!event.getRegistryKey().equals(SchoolRegistry.SCHOOL_REGISTRY_KEY)) {
            return;
        }

        var registry = event.getForgeRegistry();
        if (registry == null) return;

        if (registry.containsKey(KOKU_ID)) {
           KOKU = (SchoolType) registry.getValue(KOKU_ID);
            return;
        }
        ResourceLocation iconPath = new ResourceLocation(Forlaism.MOD_ID, "gui/school_icons/clock");

        KOKU = new SchoolType(
                KOKU_ID,
                
                ForlaismKokuFocusTag.KOKU_FOCUS,
                Component.translatable("school.forlaism.koku")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                LazyOptional.of(ForlaismKokuAttribute.KOKU_SPELL_POWER::get),
                LazyOptional.of(ForlaismKokuAttribute.KOKU_MAGIC_RESIST::get),
                LazyOptional.of(() -> SoundEvents.ENCHANTMENT_TABLE_USE),
                ForlaismKokuDamageType.KOKU_MAGIC
        );

        registry.register(KOKU_ID, KOKU);
        System.out.println("[Forlaism] Koku school registered.");
    }
}