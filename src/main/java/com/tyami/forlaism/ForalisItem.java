package com.tyami.forlaism;

import com.tyami.forlaism.client.ForalisGradient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ForalisItem extends Item {

    public ForalisItem(Properties properties) {
        super(properties);
    }

    public ForlaismRarity getForlaismRarity() {
        return ForlaismRarity.FORALIS;
    }

    @Override
    public Component getName(ItemStack stack) {

        if (getForlaismRarity() == ForlaismRarity.FORALIS) {

            // 翻訳キーから通常のアイテム名を取得
            String name = Component.translatable(
                    this.getDescriptionId(stack)
            ).getString();

            return ForalisGradient.create(name);
        }

        return super.getName(stack);
    }
}