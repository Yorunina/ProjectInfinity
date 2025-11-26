package net.lerariemann.infinity.registry.var.rules;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;

public class CubeMaterial {
    public enum Rule implements MaterialRules.MaterialRule {
        INSTANCE;
        public static final CodecHolder<Rule> CODEC = CodecHolder.of(MapCodec.unit(INSTANCE));

        @Override
        public CodecHolder<? extends MaterialRules.MaterialRule> codec() {
            return CODEC;
        }

        @Override
        public MaterialRules.BlockStateRule apply(MaterialRules.MaterialRuleContext materialRuleContext) {
            return new Cube();
        }
    }

    public static class Cube implements MaterialRules.BlockStateRule {
        static final BlockState wall = Blocks.OBSIDIAN.getDefaultState();
        static final BlockState floor = Blocks.OBSIDIAN.getDefaultState();
        static final BlockState ceiling = Blocks.OBSIDIAN.getDefaultState();
        static final BlockState air = Blocks.AIR.getDefaultState();
        static final BlockState light = Blocks.GLOWSTONE.getDefaultState();

        @Override
        public BlockState tryApply(int i, int j, int k) {
            int roomSize = 16;

            int localX = i % roomSize;
            int localY = j % roomSize;
            int localZ = k % roomSize;

            // 处理负数坐标
            if (i < 0) localX = (roomSize + (i % roomSize)) % roomSize;
            if (j < 0) localY = (roomSize + (j % roomSize)) % roomSize;
            if (k < 0) localZ = (roomSize + (k % roomSize)) % roomSize;

            // 房间边界（墙壁、地板、天花板）
            if (localX == 0 || localX == roomSize - 1 ||
                    localZ == 0 || localZ == roomSize - 1) {
                return wall;
            }

            if (localY == 0) {
                return floor;
            }

            if (localY == roomSize - 1) {
                return ceiling;
            }

            // 房间中心放置光源
            if (localX == roomSize / 2 && localY == roomSize / 2 && localZ == roomSize / 2) {
                return light;
            }

            return air;
        }
    }
}
