package net.lerariemann.infinity.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lerariemann.infinity.InfinityMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;

import java.util.Optional;

import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class RoomChunkGenerator extends ChunkGenerator {
    public static final Codec<RoomChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    Codec.STRING.listOf().fieldOf("structure_paths").forGetter(g -> g.structurePaths)
            ).apply(instance, RoomChunkGenerator::new)
    );

    private final List<String> structurePaths;
    private StructureTemplateManager structureManager;

    public RoomChunkGenerator(BiomeSource biomeSource, List<String> structurePaths) {
        super(biomeSource);
        this.structurePaths = structurePaths;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public StructurePlacementCalculator createStructurePlacementCalculator(RegistryWrapper<StructureSet> structureSetRegistry, NoiseConfig noiseConfig, long seed) {
        return StructurePlacementCalculator.create(noiseConfig, seed, this.biomeSource, Stream.empty());
    }


    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // 这里我们将在区块生成时加载结构
        if (structureManager == null) {
            structureManager = chunkRegion.getServer().getStructureTemplateManager();
        }

        ChunkPos chunkPos = chunk.getPos();
        Random random = Random.create(seed);
        // 从预定义的结构列表中随机选择一个
        if (!structurePaths.isEmpty()) {
            String structurePath = structurePaths.get(random.nextInt(structurePaths.size()));
            Identifier structureId = new Identifier(structurePath);

            // 创建结构放置配置
            StructurePlacementData placementData = new StructurePlacementData()
                    .setRotation(BlockRotation.NONE)
                    .setMirror(BlockMirror.NONE)
                    .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);

            // 加载结构
            Optional<StructureTemplate> structureOptional = structureManager.getTemplate(structureId);
            if (structureOptional.isPresent()) {
                StructureTemplate structure = structureOptional.get();
                BlockPos blockPos = new BlockPos(chunkPos.getStartX(), 0, chunkPos.getStartZ());
                structure.place(chunkRegion, blockPos, blockPos, placementData, random, 0);
            } else {
                placeFallbackRoom(chunk, chunkPos);
            }
        }
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

    }

    @Override
    public void populateEntities(ChunkRegion region) {

    }

    private void placeFallbackRoom(Chunk chunk, ChunkPos chunkPos) {
        // 放置一个简单的房间作为后备方案
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        // 创建一个16x16x16的简单房间
        int height = 16;

        // 放置地板
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                mutable.set(x, 0, z);
                chunk.setBlockState(mutable, Blocks.STONE.getDefaultState(), false);

                // 放置天花板
                mutable.set(x, height - 1, z);
                chunk.setBlockState(mutable, Blocks.STONE.getDefaultState(), false);

                // 放置墙壁
                if (x == 0 || x == 15 || z == 0 || z == 15) {
                    for (int y = 1; y < height - 1; y++) {
                        mutable.set(x, y, z);
                        chunk.setBlockState(mutable, Blocks.STONE.getDefaultState(), false);
                    }
                }
            }
        }

        // 在中心放置光源
        mutable.set(8, height - 2, 8);
        chunk.setBlockState(mutable, Blocks.TORCH.getDefaultState(), false);
    }


    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return 64; // 默认高度
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[world.getHeight()];
        // 初始化为空气方块
        for (int i = 0; i < states.length; i++) {
            states[i] = Blocks.AIR.getDefaultState();
        }
        return new VerticalBlockSample(world.getBottomY(), states);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
    }

    @Override
    public int getWorldHeight() {
        return 256;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 64;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }
}