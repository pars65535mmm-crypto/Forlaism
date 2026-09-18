package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {

    public static class Items {

        public static final TagKey<Item> TIER_3 =
                ItemTags.create(
                        new ResourceLocation(
                                Forlaism.MOD_ID,
                                "tier_3"
                        )
                );

        /** forge:tools/wrench タグ。他MODのレンチも含める。 */
        public static final TagKey<Item> WRENCH =
                ItemTags.create(
                        new ResourceLocation(
                                "forge",
                                "tools/wrench"
                        )
                );
    }
}