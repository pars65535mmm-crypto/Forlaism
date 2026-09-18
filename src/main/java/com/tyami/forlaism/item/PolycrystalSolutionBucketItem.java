package com.tyami.forlaism.item;

import com.tyami.forlaism.ForlaismRarity;
import com.tyami.forlaism.client.ForalisGradient;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.registry.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;

public class PolycrystalSolutionBucketItem extends BucketItem {

    public PolycrystalSolutionBucketItem(Properties properties) {
        super(Fluids.FORLAISM_POLYCRYSTAL_SOLUTION, properties.stacksTo(1).craftRemainder(null));
    }

    public ForlaismRarity getForlaismRarity() {
        return ForlaismRarity.FORALIS;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getForlaismRarity() == ForlaismRarity.FORALIS) {
            String name = Component.translatable(this.getDescriptionId(stack)).getString();
            return ForalisGradient.create(name);
        }
        return super.getName(stack);
    }

    public static ItemStack getEmptySuccessItem(ItemStack stack, Player player) {
        return !player.getAbilities().instabuild ? new ItemStack(Items.FORLAISM_POLYCRYSTAL_BUCKET.get()) : stack;
    }
}
