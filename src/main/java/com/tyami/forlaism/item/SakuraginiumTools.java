package com.tyami.forlaism.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;

/**
 * サクラギニウムのツール群（剣以外）。
 *
 * 全部 Tier = SakuraginiumTier / 耐久 101761。
 */
public final class SakuraginiumTools {

    private SakuraginiumTools() {
    }

    public static class Pickaxe extends PickaxeItem {
        public Pickaxe() {
            super(
                    SakuraginiumTier.INSTANCE,
                    1,
                    2.8F,
                    new Item.Properties()
                            .durability(SakuraginiumTier.DURABILITY)
                            .fireResistant()
            );
        }
    }

    public static class Axe extends AxeItem {
        public Axe() {
            super(
                    SakuraginiumTier.INSTANCE,
                    5.0F,
                    3.0F,
                    new Item.Properties()
                            .durability(SakuraginiumTier.DURABILITY)
                            .fireResistant()
            );
        }
    }

    public static class Shovel extends ShovelItem {
        public Shovel() {
            super(
                    SakuraginiumTier.INSTANCE,
                    1.5F,
                    3.0F,
                    new Item.Properties()
                            .durability(SakuraginiumTier.DURABILITY)
                            .fireResistant()
            );
        }
    }

    public static class Hoe extends HoeItem {
        public Hoe() {
            super(
                    SakuraginiumTier.INSTANCE,
                    4,
                    0.0F,
                    new Item.Properties()
                            .durability(SakuraginiumTier.DURABILITY)
                            .fireResistant()
            );
        }
    }
}