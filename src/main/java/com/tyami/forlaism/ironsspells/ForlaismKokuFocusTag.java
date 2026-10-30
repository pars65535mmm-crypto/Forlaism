package com.tyami.forlaism.ironsspells;

import com.tyami.forlaism.Forlaism;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 刻属性のフォーカスタグ。
 * 星核をフォーカスとして使用。
 */
public class ForlaismKokuFocusTag {

    public static final TagKey<Item> KOKU_FOCUS =
            ItemTags.create(new net.minecraft.resources.ResourceLocation(Forlaism.MOD_ID, "koku_focus"));

    private ForlaismKokuFocusTag() {
    }
}