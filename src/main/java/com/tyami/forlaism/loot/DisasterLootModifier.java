package com.tyami.forlaism.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

/**
 * 村チェストにディザスターを 0.3% で追加する GLM。
 */
public class DisasterLootModifier extends LootModifier {

    public static final Codec<DisasterLootModifier> CODEC =
            RecordCodecBuilder.create(inst -> codecStart(inst).and(
                    inst.group(
                            ForgeRegistries.ITEMS.getCodec()
                                    .fieldOf("item").forGetter(m -> m.item),
                            Codec.FLOAT.fieldOf("chance")
                                    .forGetter(m -> m.chance)
                    )
            ).apply(inst, DisasterLootModifier::new));

    private final Item item;
    private final float chance;

    protected DisasterLootModifier(
            LootItemCondition[] conditions,
            Item item,
            float chance
    ) {
        super(conditions);
        this.item = item;
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(item));
        }
        return generatedLoot;
    }

    @Override
    public @NotNull Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}