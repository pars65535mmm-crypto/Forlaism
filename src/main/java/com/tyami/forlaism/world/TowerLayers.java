package com.tyami.forlaism.world;

/**
 * FakeDream の塔のレイヤーデータ。
 *
 * - STA  : 最下部 (11 × 7 × 11)
 * - NOW  : 中間   (11 × 20 × 11)  ← 24個重ねる
 * - LAST : 最上部 (11 × 15 × 11)
 *
 * 合計高さ = 7 + 20 * 24 + 15 = 502
 * 地面 Y=120 から設置すると、頂上は Y=622。
 */
public final class TowerLayers {

    private TowerLayers() {
    }

    // =========================================================
    // STA : 最下部
    // =========================================================
    public static final String[][] STA = {
        // Y0
        {
            "           ",
            " HHHHHHHHH ",
            " HIJJJ JJH ",
            " HJANHNAJH ",
            " HJBBBBBJH ",
            " HKCBEBC H ",
            " HJDBBBBJH ",
            " HLABCBAJH ",
            " HMJJ J  H ",
            " HHHHHHHHH ",
            "           ",
        },
        // Y1
        {
            "           ",
            "           ",
            "     K K   ",
            "  IFGGGFK  ",
            "   B   D   ",
            "   C  PC   ",
            "   B I F   ",
            "  OBBCBB   ",
            "       K   ",
            "           ",
            "           ",
        },
        // Y2
        {
            "           ",
            "           ",
            "     K K   ",
            "   BGGGBK  ",
            "  IB   D   ",
            "  KC   CK  ",
            "  KF  PB   ",
            "  OFBCBF   ",
            "    K  K   ",
            "           ",
            "           ",
        },
        // Y3
        {
            "           ",
            "           ",
            "     KKK   ",
            "  KFGGGBK  ",
            "   D   F   ",
            "  IC   CK  ",
            "   D   F   ",
            "  ODBCBB   ",
            "    K  K   ",
            "           ",
            "           ",
        },
        // Y4
        {
            "           ",
            "           ",
            "     KKK   ",
            "  KDDCBDK  ",
            "   B   BK  ",
            "   CP  CK  ",
            "  IB   F   ",
            "  OBDCDB   ",
            "       K   ",
            "           ",
            "           ",
        },
        // Y5
        {
            "           ",
            "           ",
            "    K KK   ",
            "  KBBCBD   ",
            "   B I BK  ",
            "   C   CK  ",
            "   B   B   ",
            "  IFBCDBK  ",
            "  N K      ",
            "           ",
            "           ",
        },
        // Y6
        {
            "           ",
            "           ",
            "      K    ",
            "  KFBCBB   ",
            "   B   B   ",
            "   C   C   ",
            "   D   B   ",
            "   BBCBB   ",
            "   P       ",
            "           ",
            "           ",
        },
    };

    // =========================================================
    // NOW : 中間 (24個重ねる)
    // =========================================================
    public static final String[][] NOW = {
        // Y0
        {
            "           ",
            "           ",
            "           ",
            "   ABCDA   ",
            "   B   A   ",
            "   C   C   ",
            "   A   A   ",
            "   AACBA   ",
            "    E      ",
            "           ",
            "           ",
        },
        // Y1
        {
            "           ",
            "           ",
            "           ",
            "   ABCDA   ",
            "   B   A   ",
            "   C   C   ",
            "   B   D   ",
            "   BACAB   ",
            "     E     ",
            "           ",
            "           ",
        },
        // Y2
        {
            "           ",
            "           ",
            "           ",
            "   AACDB   ",
            "   D   A   ",
            "   C   C   ",
            "   A   D   ",
            "   ABCAD   ",
            "      F    ",
            "           ",
            "           ",
        },
        // Y3
        {
            "           ",
            "           ",
            "           ",
            "   ADCDA   ",
            "   D   A   ",
            "   C   C   ",
            "   D   A   ",
            "   BDCAB   ",
            "       FG  ",
            "           ",
            "           ",
        },
        // Y4
        {
            "           ",
            "           ",
            "           ",
            "   BACAD   ",
            "   A   A   ",
            "   C   C   ",
            "   A   A   ",
            "   DBCBAF  ",
            "           ",
            "           ",
            "           ",
        },
        // Y5
        {
            "           ",
            "           ",
            "           ",
            "   BBCAA   ",
            "   B   D   ",
            "   C   C   ",
            "   A   AE  ",
            "   BACDA   ",
            "           ",
            "           ",
            "           ",
        },
        // Y6
        {
            "           ",
            "           ",
            "           ",
            "   AACAA   ",
            "   A   A   ",
            "   C   CF  ",
            "   D   A   ",
            "   DACAD   ",
            "           ",
            "           ",
            "           ",
        },
        // Y7
        {
            "           ",
            "           ",
            "           ",
            "   BACAA   ",
            "   D   AF  ",
            "   C   C   ",
            "   B   A   ",
            "   AACAD   ",
            "           ",
            "           ",
            "           ",
        },
        // Y8
        {
            "           ",
            "           ",
            "        H  ",
            "   DDCADF  ",
            "   A   A   ",
            "   C   C   ",
            "   B   A   ",
            "   BBCAA   ",
            "           ",
            "           ",
            "           ",
        },
        // Y9
        {
            "           ",
            "           ",
            "       E   ",
            "   AACDD   ",
            "   A   D   ",
            "   C   C   ",
            "   D   B   ",
            "   DBCBD   ",
            "           ",
            "           ",
            "           ",
        },
        // Y10
        {
            "           ",
            "           ",
            "      F    ",
            "   AACAD   ",
            "   D   A   ",
            "   C   C   ",
            "   B   A   ",
            "   AACAD   ",
            "           ",
            "           ",
            "           ",
        },
        // Y11
        {
            "           ",
            "           ",
            "     F     ",
            "   AACAA   ",
            "   A   A   ",
            "   C   C   ",
            "   D   A   ",
            "   ADCBD   ",
            "           ",
            "           ",
            "           ",
        },
        // Y12
        {
            "           ",
            "           ",
            "    E      ",
            "   DDCDD   ",
            "   D   D   ",
            "   C   C   ",
            "   A   A   ",
            "   AACBA   ",
            "           ",
            "           ",
            "           ",
        },
        // Y13
        {
            "           ",
            "           ",
            "  GE       ",
            "   AACAA   ",
            "   A   A   ",
            "   C   C   ",
            "   A   A   ",
            "   ADCBB   ",
            "           ",
            "           ",
            "           ",
        },
        // Y14
        {
            "           ",
            "           ",
            "           ",
            "  FBBCAD   ",
            "   B   D   ",
            "   C   C   ",
            "   A   A   ",
            "   ABCBD   ",
            "           ",
            "           ",
            "           ",
        },
        // Y15
        {
            "           ",
            "           ",
            "           ",
            "   AACAD   ",
            "  FD   B   ",
            "   C   C   ",
            "   A   B   ",
            "   AACDA   ",
            "           ",
            "           ",
            "           ",
        },
        // Y16
        {
            "           ",
            "           ",
            "           ",
            "   ADCDA   ",
            "   D   D   ",
            "  FC   C   ",
            "   A   D   ",
            "   AACAA   ",
            "           ",
            "           ",
            "           ",
        },
        // Y17
        {
            "           ",
            "           ",
            "           ",
            "   DDCAD   ",
            "   D   D   ",
            "   C   C   ",
            "  FD   B   ",
            "   ABCBA   ",
            "           ",
            "           ",
            "           ",
        },
        // Y18
        {
            "           ",
            "           ",
            "           ",
            "   BBCBA   ",
            "   A   B   ",
            "   C   C   ",
            "   D   A   ",
            "  EDACAA   ",
            "  G        ",
            "           ",
            "           ",
        },
        // Y19
        {
            "           ",
            "           ",
            "           ",
            "   DDCAA   ",
            "   D   B   ",
            "   C   C   ",
            "   A   D   ",
            "   ADCDA   ",
            "   E       ",
            "           ",
            "           ",
        },
    };

    // =========================================================
    // LAST : 最上部
    // =========================================================
    public static final String[][] LAST = {
        // Y0
        {
            "           ",
            " FEEEEFFFF ",
            " FEFEFFFEE ",
            " EEADCBAEE ",
            " EFBEEFDEF ",
            " EFCF  CFE ",
            " E DF GDEF ",
            " E DDCDBEF ",
            " E  GFFFEF ",
            " FEEEEFFFF ",
            "           ",
        },
        // Y1
        {
            "           ",
            " HHIIIHIHH ",
            " H       I ",
            " H       I ",
            " I  HIHE H ",
            " I  H  J I ",
            " I  I  F H ",
            " H  I    H ",
            " I       H ",
            " HHHIIIHHH ",
            "           ",
        },
        // Y2
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I  HIG    ",
            "    I      ",
            "    I      ",
            "    I      ",
            "           ",
            "           ",
            "           ",
        },
        // Y3
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I  EJ     ",
            "    H      ",
            "    I      ",
            "    H      ",
            "           ",
            "           ",
            "           ",
        },
        // Y4
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I         ",
            "    G      ",
            "    I      ",
            "    H      ",
            "           ",
            "           ",
            "           ",
        },
        // Y5
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I         ",
            "           ",
            "    J      ",
            "    E      ",
            "           ",
            "           ",
            "           ",
        },
        // Y6
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I         ",
            "           ",
            "           ",
            "     J     ",
            "           ",
            "           ",
            "           ",
        },
        // Y7
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I         ",
            "           ",
            "           ",
            "      GE   ",
            "           ",
            "           ",
            "           ",
        },
        // Y8
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I         ",
            "           ",
            "       G   ",
            "           ",
            "           ",
            "           ",
            "           ",
        },
        // Y9
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I     E   ",
            "       J   ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
        },
        // Y10
        {
            "           ",
            " H   I     ",
            "           ",
            "           ",
            " I    G    ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
        },
        // Y11
        {
            "           ",
            " FFFFF     ",
            " FFFFF     ",
            " FFFFF     ",
            " FFFFF     ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
        },
        // Y12
        {
            "           ",
            "           ",
            "  K        ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
        },
        // Y13
        {
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
        },
        // Y14
        {
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
            "           ",
        },
    };

    // =========================================================
    // シンボル → ブロック 変換テーブル
    // =========================================================
    // 3つの NBT で同じシンボルが違うブロックを指している場合があるので、
    // それぞれのパレットで個別に解決する。

    /** STA 用のパレット。 */
    public static net.minecraft.world.level.block.state.BlockState staSymbol(char c) {
        return switch (c) {
            case 'A' -> state("minecraft:chiseled_stone_bricks");
            case 'B' -> state("minecraft:stone_bricks");
            case 'C' -> state("forlaism:steel_block");
            case 'D' -> state("minecraft:mossy_stone_bricks");
            case 'E' -> state("minecraft:stone");
            case 'F' -> state("minecraft:cracked_stone_bricks");
            case 'G' -> state("forlaism:forlaism_glass");
            case 'H' -> slabBottom("minecraft:stone_brick_slab");
            case 'I' -> stairs("minecraft:stone_brick_stairs", "south");
            case 'J' -> pinkPetals();
            case 'K' -> vine();
            case 'L' -> wallTall("minecraft:mossy_stone_brick_wall");
            case 'M' -> state("minecraft:potted_cherry_sapling");
            case 'N' -> slabBottom("minecraft:mossy_stone_brick_slab");
            case 'O' -> wallTall("minecraft:stone_brick_wall");
            case 'P' -> stairs("minecraft:mossy_stone_brick_stairs", "south");
            case ' ' -> null;
            default -> null;
        };
    }

    /** NOW 用のパレット。 */
    public static net.minecraft.world.level.block.state.BlockState nowSymbol(char c) {
        return switch (c) {
            case 'A' -> state("minecraft:stone_bricks");
            case 'B' -> state("minecraft:cracked_stone_bricks");
            case 'C' -> state("forlaism:steel_block");
            case 'D' -> state("minecraft:mossy_stone_bricks");
            case 'E' -> stairs("minecraft:stone_brick_stairs", "east");
            case 'F' -> stairs("minecraft:mossy_stone_brick_stairs", "east");
            case 'G' -> slabTop("minecraft:mossy_stone_brick_slab");
            case 'H' -> slabTop("minecraft:stone_brick_slab");
            case ' ' -> null;
            default -> null;
        };
    }

    /** LAST 用のパレット。 */
    public static net.minecraft.world.level.block.state.BlockState lastSymbol(char c) {
        return switch (c) {
            case 'A' -> state("minecraft:cracked_stone_bricks");
            case 'B' -> state("minecraft:stone_bricks");
            case 'C' -> state("forlaism:steel_block");
            case 'D' -> state("minecraft:mossy_stone_bricks");
            case 'E' -> slabDouble("minecraft:mossy_stone_brick_slab");
            case 'F' -> slabDouble("minecraft:stone_brick_slab");
            case 'G' -> stairs("minecraft:mossy_stone_brick_stairs", "east");
            case 'H' -> wallLow("minecraft:stone_brick_wall");
            case 'I' -> wallLow("minecraft:mossy_stone_brick_wall");
            case 'J' -> stairs("minecraft:stone_brick_stairs", "north");
            case 'K' -> state("forlaism:ritual_altar");
            case ' ' -> null;
            default -> null;
        };
    }

    // =========================================================
    // ヘルパー
    // =========================================================

    private static net.minecraft.world.level.block.state.BlockState state(String id) {
        var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS
                .getValue(new net.minecraft.resources.ResourceLocation(id));
        if (block == null) return null;
        return block.defaultBlockState();
    }

    private static net.minecraft.world.level.block.state.BlockState slabBottom(String id) {
        var s = state(id);
        if (s == null) return null;
        if (s.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            s = s.setValue(
                    net.minecraft.world.level.block.SlabBlock.TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.BOTTOM
            );
        }
        if (s.hasProperty(net.minecraft.world.level.block.SlabBlock.WATERLOGGED)) {
            s = s.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED, false);
        }
        return s;
    }

    private static net.minecraft.world.level.block.state.BlockState slabTop(String id) {
        var s = state(id);
        if (s == null) return null;
        if (s.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            s = s.setValue(
                    net.minecraft.world.level.block.SlabBlock.TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.TOP
            );
        }
        if (s.hasProperty(net.minecraft.world.level.block.SlabBlock.WATERLOGGED)) {
            s = s.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED, false);
        }
        return s;
    }

    private static net.minecraft.world.level.block.state.BlockState slabDouble(String id) {
        var s = state(id);
        if (s == null) return null;
        if (s.hasProperty(net.minecraft.world.level.block.SlabBlock.TYPE)) {
            s = s.setValue(
                    net.minecraft.world.level.block.SlabBlock.TYPE,
                    net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
            );
        }
        if (s.hasProperty(net.minecraft.world.level.block.SlabBlock.WATERLOGGED)) {
            s = s.setValue(net.minecraft.world.level.block.SlabBlock.WATERLOGGED, false);
        }
        return s;
    }

    private static net.minecraft.world.level.block.state.BlockState stairs(String id, String facing) {
        var s = state(id);
        if (s == null) return null;
        if (s.hasProperty(net.minecraft.world.level.block.StairBlock.FACING)) {
            s = s.setValue(
                    net.minecraft.world.level.block.StairBlock.FACING,
                    net.minecraft.core.Direction.byName(facing)
            );
        }
        if (s.hasProperty(net.minecraft.world.level.block.StairBlock.HALF)) {
            s = s.setValue(
                    net.minecraft.world.level.block.StairBlock.HALF,
                    net.minecraft.world.level.block.state.properties.Half.BOTTOM
            );
        }
        if (s.hasProperty(net.minecraft.world.level.block.StairBlock.SHAPE)) {
            s = s.setValue(
                    net.minecraft.world.level.block.StairBlock.SHAPE,
                    net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT
            );
        }
        if (s.hasProperty(net.minecraft.world.level.block.StairBlock.WATERLOGGED)) {
            s = s.setValue(net.minecraft.world.level.block.StairBlock.WATERLOGGED, false);
        }
        return s;
    }

    private static net.minecraft.world.level.block.state.BlockState wallLow(String id) {
        var s = state(id);
        if (s == null) return null;
        s = setWallSide(s, "east", "low");
        s = setWallSide(s, "south", "low");
        s = setWallSide(s, "north", "low");
        s = setWallSide(s, "west", "none");
        if (s.hasProperty(net.minecraft.world.level.block.WallBlock.UP)) {
            s = s.setValue(net.minecraft.world.level.block.WallBlock.UP, true);
        }
        if (s.hasProperty(net.minecraft.world.level.block.WallBlock.WATERLOGGED)) {
            s = s.setValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED, false);
        }
        return s;
    }

private static net.minecraft.world.level.block.state.BlockState wallTall(String id) {
        var s = state(id);
        if (s == null) return null;
        s = setWallSide(s, "east", "tall");
        s = setWallSide(s, "south", "none");
        s = setWallSide(s, "north", "none");
        s = setWallSide(s, "west", "none");
        if (s.hasProperty(net.minecraft.world.level.block.WallBlock.UP)) {
            s = s.setValue(net.minecraft.world.level.block.WallBlock.UP, true);
        }
        if (s.hasProperty(net.minecraft.world.level.block.WallBlock.WATERLOGGED)) {
            s = s.setValue(net.minecraft.world.level.block.WallBlock.WATERLOGGED, false);
        }
        return s;
    }

    private static net.minecraft.world.level.block.state.BlockState setWallSide(
            net.minecraft.world.level.block.state.BlockState s, String dir, String val) {
        net.minecraft.world.level.block.state.properties.WallSide side =
                switch (val) {
                    case "low" -> net.minecraft.world.level.block.state.properties.WallSide.LOW;
                    case "tall" -> net.minecraft.world.level.block.state.properties.WallSide.TALL;
                    default -> net.minecraft.world.level.block.state.properties.WallSide.NONE;
                };
        return switch (dir) {
            case "east" -> s.hasProperty(net.minecraft.world.level.block.WallBlock.EAST_WALL)
                    ? s.setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL, side) : s;
            case "south" -> s.hasProperty(net.minecraft.world.level.block.WallBlock.SOUTH_WALL)
                    ? s.setValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL, side) : s;
            case "north" -> s.hasProperty(net.minecraft.world.level.block.WallBlock.NORTH_WALL)
                    ? s.setValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL, side) : s;
            case "west" -> s.hasProperty(net.minecraft.world.level.block.WallBlock.WEST_WALL)
                    ? s.setValue(net.minecraft.world.level.block.WallBlock.WEST_WALL, side) : s;
            default -> s;
        };
    }

private static net.minecraft.world.level.block.state.BlockState pinkPetals() {
        var s = state("minecraft:pink_petals");
        if (s == null) return null;
        if (s.hasProperty(net.minecraft.world.level.block.PinkPetalsBlock.FACING)) {
            s = s.setValue(
                    net.minecraft.world.level.block.PinkPetalsBlock.FACING,
                    net.minecraft.core.Direction.SOUTH
            );
        }
        if (s.hasProperty(net.minecraft.world.level.block.PinkPetalsBlock.AMOUNT)) {
            s = s.setValue(net.minecraft.world.level.block.PinkPetalsBlock.AMOUNT, 4);
        }
        return s;
    }

    private static net.minecraft.world.level.block.state.BlockState vine() {
        var s = state("minecraft:vine");
        if (s == null) return null;
        if (s.hasProperty(net.minecraft.world.level.block.VineBlock.EAST)) {
            s = s.setValue(net.minecraft.world.level.block.VineBlock.EAST, true);
        }
        if (s.hasProperty(net.minecraft.world.level.block.VineBlock.SOUTH)) {
            s = s.setValue(net.minecraft.world.level.block.VineBlock.SOUTH, false);
        }
        if (s.hasProperty(net.minecraft.world.level.block.VineBlock.NORTH)) {
            s = s.setValue(net.minecraft.world.level.block.VineBlock.NORTH, false);
        }
        if (s.hasProperty(net.minecraft.world.level.block.VineBlock.WEST)) {
            s = s.setValue(net.minecraft.world.level.block.VineBlock.WEST, false);
        }
        if (s.hasProperty(net.minecraft.world.level.block.VineBlock.UP)) {
            s = s.setValue(net.minecraft.world.level.block.VineBlock.UP, false);
        }
        return s;
    }
}