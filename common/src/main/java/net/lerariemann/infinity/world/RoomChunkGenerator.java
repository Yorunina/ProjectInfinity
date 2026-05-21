package net.lerariemann.infinity.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lerariemann.infinity.InfinityMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;
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
                    Codec.STRING.listOf().fieldOf("structure_paths").forGetter(g -> g.structurePaths),
                    Codec.INT.listOf().fieldOf("weights").forGetter(g -> g.weights)
            ).apply(instance, RoomChunkGenerator::new)
    );

    private final List<String> structurePaths;
    private final List<Integer> weights;
    private StructureTemplateManager structureManager;

    public RoomChunkGenerator(BiomeSource biomeSource, List<String> structurePaths, List<Integer> weights) {
        super(biomeSource);
        this.structurePaths = structurePaths;
        this.weights = weights;
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
        if (structureManager == null) {
            structureManager = chunkRegion.getServer().getStructureTemplateManager();
        }

        ChunkPos chunkPos = chunk.getPos();
        Random random = chunkRegion.getRandom();
        if (structurePaths.isEmpty()) return;
        int totalWeight = this.weights.stream().mapToInt(Integer::intValue).sum();
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        String structurePath = null;
        for (int i = 0; i < weights.size(); i++) {
            currentWeight += weights.get(i);
            if (randomWeight < currentWeight) {
                structurePath = structurePaths.get(i);
                break;
            }
        }
        if (structurePath == null) return;

        Identifier structureId = new Identifier(structurePath);

        StructurePlacementData placementData = new StructurePlacementData()
                .setRotation(BlockRotation.NONE)
                .setMirror(BlockMirror.NONE)
                .addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR);

        Optional<StructureTemplate> structureOptional = structureManager.getTemplate(structureId);
        if (structureOptional.isPresent()) {
            try {
                StructureTemplate structure = structureOptional.get();
                BlockPos blockPos = new BlockPos(chunkPos.getStartX(), 0, chunkPos.getStartZ());
                structure.place(chunkRegion, blockPos, blockPos, placementData, random, 0);
            } catch (Exception e) {
                InfinityMod.LOGGER.warn("Failed to place structure {}", structureId, e);
            }
        }
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

    }

    @Override
    public void populateEntities(ChunkRegion region) {
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return 64;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[world.getHeight()];
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