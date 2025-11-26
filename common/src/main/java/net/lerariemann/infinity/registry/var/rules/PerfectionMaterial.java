package net.lerariemann.infinity.registry.var.rules;

import com.mojang.serialization.MapCodec;
import net.lerariemann.infinity.util.registry.RuleUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;

public class PerfectionMaterial {
    public enum Rule implements MaterialRules.MaterialRule {
        INSTANCE;
        public static final CodecHolder<Rule> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

        @Override
        public CodecHolder<? extends MaterialRules.MaterialRule> codec() {
            return CODEC;
        }

        @Override
        public MaterialRules.BlockStateRule apply(MaterialRules.MaterialRuleContext materialRuleContext) {
            return new Perfection();
        }
    }

    public static class Perfection implements MaterialRules.BlockStateRule {
        static final BlockState cobblestone = Blocks.COBBLESTONE.getDefaultState();
        static final BlockState lightNorth = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH);
        static final BlockState lightSouth = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH);
        static final BlockState lightEast = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST);
        static final BlockState lightWest = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST);
        static final BlockState glass = Blocks.GLASS.getDefaultState();
        static final BlockState air = Blocks.AIR.getDefaultState();

        @Override
        public BlockState tryApply(int i, int j, int k) {
            int x = RuleUtils.normalize(i, 10);
            int y = j - 50;
            int z = RuleUtils.normalize(k, 10);
            if (y == -2) return Blocks.BEDROCK.getDefaultState();
            switch (y) {
                case -1 -> {
                    return cobblestone;
                }
                case 4 -> {
                    //Skylights
                    if ((z == 7 || z == 6 || z == 0 || z == 9) && (x == 0 || x == 9 || x == 2 || x == 3)) return glass;
                    return cobblestone;
                }
                case 3 -> {
                    // Crossroad overhang, North/South
                    if (z == 2 || z == 3 || z == 4) {
                        return cobblestone;
                    }
                    //Crossroad torch - South (North facing)
                    else if (z == 1) {
                        if (x == 1) {
                            return lightNorth;
                        }
                    }
                    //Crossroad torch - North (South facing)
                    else if (z == 5) {
                        if (x == 1) {
                            return lightSouth;
                        }
                    }
                    // Crossroad overhang, East/West
                    if (x == 7 || x == 6 || x == 5) {
                        return cobblestone;
                    }
                    //Crossroad torch - West (East facing)
                    else if (x == 8) {
                        if (z == 8) {
                            return lightEast;
                        }
                    }
                    //Crossroad torch - East (West facing)
                    else if (x == 4) {
                        if (z == 8) {
                            return lightWest;
                        }
                    }
                    return air;
                }
                case 0, 1, 2 -> {
                    //Crossroad walls, East/West
                    if (x == 7 || x == 6 || x == 5) {
                        if (z == 7 || z == 8 || z == 9) {
                            return air;
                        }
                        return cobblestone;
                    }
                    //Crossroad walls, North/South
                    if (z == 2 || z == 3 || z == 4 || z == 12 || z == 13 || z == 14) {
                        if (x == 0 || x == 2 || x == 1) {
                            return air;
                        }
                        return cobblestone;
                    }

                    return air;
                }
                default -> {
                    return air;
                }
            }
        }
    }

}
