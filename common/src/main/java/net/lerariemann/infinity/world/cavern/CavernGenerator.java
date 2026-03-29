package net.lerariemann.infinity.world.cavern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fabricators_of_create.porting_lib.tags.Tags;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

public class CavernGenerator extends ChunkGenerator {

    public static final Codec<CavernGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(CavernGenerator::getBiomeSource)
            ).apply(instance, instance.stable(CavernGenerator::new)));

    public static boolean isOreWorld = false;
    public static boolean enableBaseCommands = true;
    public static boolean glassOceanBulbs = true;
    public static BlockState baseMaterial = Blocks.DEEPSLATE.getDefaultState();

    private static List<BlockState> oreCache = null;
    
    private final OctavePerlinNoiseSampler caveNoise;
    private final OctavePerlinNoiseSampler tunnelNoise;
    private final OctavePerlinNoiseSampler oreNoise;
    private final BiomeSource cavernBiomeSource;

    public CavernGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.cavernBiomeSource = biomeSource;
        this.caveNoise = OctavePerlinNoiseSampler.create(Random.create(1111L), IntStream.rangeClosed(-3, 0).boxed().toList());
        this.tunnelNoise = OctavePerlinNoiseSampler.create(Random.create(2222L), IntStream.rangeClosed(-3, 0).boxed().toList());
        this.oreNoise = OctavePerlinNoiseSampler.create(Random.create(54321L), IntStream.rangeClosed(-3, 0).boxed().toList());
    }

    @Override protected Codec<? extends ChunkGenerator> getCodec() { return CODEC; }
    @Override public int getMinimumY() { return -64; }
    @Override public int getSeaLevel() { return 63; }
    @Override public int getWorldHeight() { return 384; }

    private static class StructData {
        int cx, cy, cz, rx, ry, rz;
        boolean isAquatic;
        StructData(int cx, int cy, int cz, int rx, int ry, int rz, boolean isAquatic) {
            this.cx = cx; this.cy = cy; this.cz = cz;
            this.rx = rx; this.ry = ry; this.rz = rz;
            this.isAquatic = isAquatic;
        }
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig randomState, StructureAccessor structures, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int minY = chunk.getBottomY();
        int maxY = chunk.getHeight();

        BlockPos.Mutable pos = new BlockPos.Mutable();
        StructureWeightSampler beardifier = StructureWeightSampler.createStructureWeightSampler(structures, chunkPos);

        List<StructData> structureCenters = new ArrayList<>();
        Map<Structure, LongSet> references = chunk.getStructureReferences();
        
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            for (long ref : entry.getValue()) {
                StructureStart start = structures.getStructureStart(ChunkSectionPos.from(new ChunkPos(ref), 0), entry.getKey(), chunk);
                if (start != null && start.hasChildren()) {
                    BlockBox box = start.getBoundingBox();
                    String name = entry.getKey().getClass().getSimpleName().toLowerCase();
                    boolean isAquatic = name.contains("ocean") || name.contains("monument") || name.contains("shipwreck");
                    
                    structureCenters.add(new StructData(
                            box.getMinX() + box.getBlockCountX() / 2, box.getMinY(), box.getMinZ() + box.getBlockCountZ() / 2,
                            box.getBlockCountX() / 2, box.getBlockCountY(), box.getBlockCountZ() / 2, isAquatic
                    ));
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                int floorOffset = (int) ((this.caveNoise.sample(worldX * 0.05, 0, worldZ * 0.05) + 1.0) * 2.0);
                int ceilOffset = (int) ((this.caveNoise.sample(worldX * 0.05, 500.0, worldZ * 0.05) + 1.0) * 2.0);
                boolean isWindow = this.tunnelNoise.sample(worldX * 0.015D, 1000.0D, worldZ * 0.015D) > 0.4D;

                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);

                    if (y <= minY + floorOffset) { chunk.setBlockState(pos, Blocks.BEDROCK.getDefaultState(), false); continue; }
                    if (y >= (maxY - 1) - ceilOffset) { chunk.setBlockState(pos, isWindow ? Blocks.BARRIER.getDefaultState() : Blocks.BEDROCK.getDefaultState(), false); continue; }

                    // --- 1. The Building Floors (Thin Shelves, Massive Rooms) ---
                    double floorWarp = this.caveNoise.sample(worldX * 0.015D, y * 0.01D, worldZ * 0.015D) * 10.0D;
                    double warpedY = y + floorWarp;
                    double floorDensity = Math.sin(warpedY * Math.PI / 20.0D); // Repeats every 40 blocks
                    
                    // Fixed: Floor is solid ONLY if density > 0.7. (Leaves massive 30-block tall open rooms)
                    boolean isCave = floorDensity <= 0.7D;

                    // --- 2. The Connecting Tunnels ---
                    double tNoise = this.tunnelNoise.sample(worldX * 0.015D, y * 0.02D, worldZ * 0.015D);
                    if (Math.abs(tNoise) < 0.06D) isCave = true; // Tunnels puncture the floors

                    double shaftNoise = this.caveNoise.sample(worldX * 0.03D, 0, worldZ * 0.03D);
                    if (shaftNoise > 0.6D) isCave = true; // Vertical drop shafts puncture the floors

                    // --- 3. Stalagmite Spiral Staircases ---
                    int cellSize = 48; // 1 pillar every 48x48 blocks roughly
                    int cellX = Math.floorDiv(worldX, cellSize);
                    int cellZ = Math.floorDiv(worldZ, cellSize);
                    long pSeed = (long)cellX * 341873128712L + (long)cellZ * 132897987541L;
                    pSeed ^= pSeed >>> 16; pSeed *= 0x2545F4914F6CDD1DL; pSeed ^= pSeed >>> 16;

                    if ((pSeed % 2) == 0) { // 50% chance for a pillar in this cell
                        int cx = cellX * cellSize + (cellSize/2) + (int)((pSeed >> 8) % 16) - 8;
                        int cz = cellZ * cellSize + (cellSize/2) + (int)((pSeed >> 16) % 16) - 8;
                        double dx = worldX - cx;
                        double dz = worldZ - cz;
                        double d2 = dx*dx + dz*dz;

                        // Tapering Math: Thick at the floor, thin in the middle
                        int modY = ((y % 40) + 40) % 40;
                        double distToFloor = Math.abs(modY - 10.0); // Center of floor is at 10
                        if (distToFloor > 20) distToFloor = 40 - distToFloor;
                        double taperBonus = ((20.0 - distToFloor) / 20.0) * 4.0; 

                        double coreR = 2.0 + taperBonus + (this.caveNoise.sample(worldX * 0.1, y * 0.1, worldZ * 0.1) * 1.5);
                        double ledgeR = 6.0 + taperBonus + (this.caveNoise.sample(worldX * 0.1, y * 0.1, worldZ * 0.1) * 1.5);

                        if (d2 < coreR * coreR) {
                            isCave = false; // Solid Pillar Core
                        } else if (d2 < ledgeR * ledgeR) {
                            double theta = Math.atan2(dz, dx);
                            double targetTheta = ((y * 0.3) % (2 * Math.PI)); 
                            if (targetTheta > Math.PI) targetTheta -= 2 * Math.PI;
                            double diff = Math.abs(theta - targetTheta);
                            if (diff > Math.PI) diff = 2 * Math.PI - diff;
                            if (diff < 1.0) { // Creates the spiral wedge!
                                isCave = false; 
                            }
                        }
                    }

                    if (y < minY + 12 || y > maxY - 12) isCave = false; 

                    double structureCarve = beardifier.sample(new DensityFunction.UnblendedNoisePos(worldX, y, worldZ));
                    if (structureCarve < -0.1D) isCave = true;   
                    else if (structureCarve > 0.1D) isCave = false; 

                    boolean isGlass = false;
                    boolean isWater = false;

                    for (StructData sd : structureCenters) {
                        if (sd.isAquatic && glassOceanBulbs) {
                            double dx = worldX - sd.cx, dz = worldZ - sd.cz, dyBase = y - sd.cy;
                            double rX = sd.rx + 30.0, rZ = sd.rz + 30.0, rY = (sd.ry / 2.0) + 30.0;
                            double domeDy = y - (sd.cy + (sd.ry / 2.0));

                            if (Math.abs(dx) > rX || Math.abs(dz) > rZ || Math.abs(domeDy) > rY) continue;
                            double domeVal = (dx * dx) / (rX * rX) + (domeDy * domeDy) / (rY * rY) + (dz * dz) / (rZ * rZ);
                            
                            if (domeVal < 1.0) {
                                if (domeVal > 0.85) isGlass = true; 
                                else isWater = true; 
                            }
                        }
                    }

                    if (isGlass) {
                        chunk.setBlockState(pos, Blocks.CYAN_STAINED_GLASS.getDefaultState(), false);
                    } else if (isWater) {
                        chunk.setBlockState(pos, Blocks.WATER.getDefaultState(), false);
                    } else if (!isCave) {
                        if (isOreWorld) {
                            double oreVal = this.oreNoise.sample(worldX * 0.15D, y * 0.15D, worldZ * 0.15D);
                            if (oreVal > 0.25D) chunk.setBlockState(pos, getVeinOre(worldX, y, worldZ), false);
                            else chunk.setBlockState(pos, baseMaterial, false);
                        } else {
                            chunk.setBlockState(pos, baseMaterial, false);
                        }
                    } else {
                        chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
                    }
                }
            }
        }

        // --- Trees and Diverse Biome Surface Pass ---
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = maxY - 2; y > minY + 2; y--) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.isOf(baseMaterial.getBlock())) {
                        BlockState above = chunk.getBlockState(pos.up());
                        if (above.isAir() || above.isOf(Blocks.WATER)) {
                            RegistryEntry<Biome> biome = this.cavernBiomeSource.getBiome((startX + x) >> 2, y >> 2, (startZ + z) >> 2, randomState.getMultiNoiseSampler());
                            
                            BlockState floorState = getSurfaceBlockForBiome(biome);
                            chunk.setBlockState(pos, floorState, false);
                            
                            if (floorState.isOf(Blocks.GRASS_BLOCK) || floorState.isOf(Blocks.MOSS_BLOCK) || floorState.isOf(Blocks.PODZOL) || floorState.isOf(Blocks.MYCELIUM)) {
                                chunk.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), false);
                                chunk.setBlockState(pos.down().down(), Blocks.DIRT.getDefaultState(), false);
                            } else if (floorState.isOf(Blocks.SAND) || floorState.isOf(Blocks.RED_SAND)) {
                                chunk.setBlockState(pos.down(), Blocks.SANDSTONE.getDefaultState(), false);
                            } else if (floorState.isOf(Blocks.TERRACOTTA)) {
                                chunk.setBlockState(pos.down(), Blocks.TERRACOTTA.getDefaultState(), false);
                            } else if (floorState.isOf(Blocks.MUD)) {
                                chunk.setBlockState(pos.down(), Blocks.MUD.getDefaultState(), false);
                            } else if (floorState.isOf(Blocks.COARSE_DIRT) || floorState.isOf(Blocks.SNOW_BLOCK)) {
                                chunk.setBlockState(pos.down(), Blocks.DIRT.getDefaultState(), false);
                            } else if (floorState.isOf(Blocks.CRIMSON_NYLIUM) || floorState.isOf(Blocks.WARPED_NYLIUM)) {
                                chunk.setBlockState(pos.down(), Blocks.NETHERRACK.getDefaultState(), false);
                            }
                        }
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    private BlockState getSurfaceBlockForBiome(RegistryEntry<Biome> biome) {
        Identifier biomeKey = biome.getKey().map(RegistryKey::getValue).orElse(null);
        if (biomeKey != null) {
            String path = biomeKey.getPath();
            if (path.contains("desert") || path.contains("beach")) return Blocks.SAND.getDefaultState();
            if (path.contains("badlands") || path.contains("mesa")) return Blocks.TERRACOTTA.getDefaultState();
            if (path.contains("snow") || path.contains("ice") || path.contains("frozen") || path.contains("peaks")) return Blocks.SNOW_BLOCK.getDefaultState();
            if (path.contains("mushroom")) return Blocks.MYCELIUM.getDefaultState();
            if (path.contains("swamp") || path.contains("mangrove")) return Blocks.MUD.getDefaultState();
            if (path.contains("savanna")) return Blocks.COARSE_DIRT.getDefaultState();
            if (path.contains("dripstone")) return Blocks.DRIPSTONE_BLOCK.getDefaultState();
            if (path.contains("crimson")) return Blocks.CRIMSON_NYLIUM.getDefaultState();
            if (path.contains("warped")) return Blocks.WARPED_NYLIUM.getDefaultState();
            if (path.contains("basalt")) return Blocks.BASALT.getDefaultState();
            if (path.contains("soul")) return Blocks.SOUL_SAND.getDefaultState();
            if (path.contains("dark_forest")) return Blocks.GRASS_BLOCK.getDefaultState();
            if (path.contains("dark") || path.contains("sculk")) return Blocks.SCULK.getDefaultState();
            if (path.contains("taiga") || path.contains("old_growth") || path.contains("spruce")) return Blocks.PODZOL.getDefaultState();
            if (path.contains("lush")) return Blocks.MOSS_BLOCK.getDefaultState();
            if (path.contains("forest") || path.contains("plains") || path.contains("jungle") || path.contains("meadow") || path.contains("cherry")) {
                return Blocks.GRASS_BLOCK.getDefaultState();
            }
        }
        return Blocks.GRASS_BLOCK.getDefaultState(); 
    }

    @Override
    public void generateFeatures(StructureWorldAccess level, Chunk chunk, StructureAccessor structureManager) {
        super.generateFeatures(level, chunk, structureManager);
        
        ChunkPos chunkPos = chunk.getPos();
        Random random = Random.create(chunkPos.toLong());
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (random.nextFloat() > 0.08f) continue; 
                
                int worldX = startX + x;
                int worldZ = startZ + z;
                
                for (int y = chunk.getBottomY() + 5; y < chunk.getHeight() - 20; y++) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = chunk.getBlockState(pos);
                    BlockState above = chunk.getBlockState(pos.up());
                    
                    boolean isSoil = state.isOf(Blocks.MOSS_BLOCK) || state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.PODZOL) || state.isOf(Blocks.DIRT) || state.isOf(Blocks.MUD) || state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.CRIMSON_NYLIUM) || state.isOf(Blocks.WARPED_NYLIUM);
                    boolean isSand = state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND) || state.isOf(Blocks.TERRACOTTA);
                    boolean isFungi = state.isOf(Blocks.MYCELIUM) || state.isOf(Blocks.CRIMSON_NYLIUM) || state.isOf(Blocks.WARPED_NYLIUM);
                    
                    if ((isSoil || isSand || isFungi) && above.isAir()) {
                        
                        RegistryEntry<Biome> biome = level.getBiome(pos);
                        Identifier biomeKey = biome.getKey().map(RegistryKey::getValue).orElse(null);
                        String path = biomeKey != null ? biomeKey.getPath() : "";

                        generateVegetation(level, pos.up(), path, random, state);
                        
                        y += 15; // Jumps up the pillar to check for the next horizontal shelf
                    }
                }
            }
        }
    }

    private void generateVegetation(StructureWorldAccess level, BlockPos pos, String biomePath, Random random, BlockState floorState) {
        if (random.nextInt(40) == 0) {
            level.setBlockState(pos, Blocks.SHROOMLIGHT.getDefaultState(), 3);
            return;
        }

        boolean isDesert = biomePath.contains("desert") || biomePath.contains("badlands") || biomePath.contains("ice") || biomePath.contains("peaks");
        float treeChance = isDesert ? 0.0f : 0.03f; 
        
        if (treeChance > 0 && random.nextFloat() < treeChance) {
            DynamicRegistryManager registries = level.getRegistryManager();
            Registry<ConfiguredFeature<?, ?>> featureRegistry = registries.get(RegistryKeys.CONFIGURED_FEATURE);
            RegistryKey<ConfiguredFeature<?, ?>> treeKey = TreeConfiguredFeatures.OAK;
            
            if (biomePath.contains("birch")) treeKey = TreeConfiguredFeatures.BIRCH;
            else if (biomePath.contains("jungle")) treeKey = TreeConfiguredFeatures.MEGA_JUNGLE_TREE;
            else if (biomePath.contains("taiga") || biomePath.contains("pine") || biomePath.contains("spruce")) treeKey = TreeConfiguredFeatures.SPRUCE;
            else if (biomePath.contains("savanna")) treeKey = TreeConfiguredFeatures.ACACIA;
            else if (biomePath.contains("dark_forest")) treeKey = TreeConfiguredFeatures.DARK_OAK;
            else if (biomePath.contains("swamp")) treeKey = TreeConfiguredFeatures.SWAMP_OAK;
            else if (biomePath.contains("cherry")) treeKey = TreeConfiguredFeatures.CHERRY;

            RegistryEntry<ConfiguredFeature<?, ?>> treeHolder = featureRegistry.getEntry(treeKey).orElse(null);
            if (treeHolder != null) {
                treeHolder.value().generate(level, this, random, pos);
                return;
            }
        }

        if (floorState.isOf(Blocks.SAND) || floorState.isOf(Blocks.RED_SAND)) {
            if (random.nextInt(15) == 0) level.setBlockState(pos, Blocks.CACTUS.getDefaultState(), 3);
            else if (random.nextInt(8) == 0) level.setBlockState(pos, Blocks.DEAD_BUSH.getDefaultState(), 3);
        } 
        else if (floorState.isOf(Blocks.MOSS_BLOCK) || floorState.isOf(Blocks.GRASS_BLOCK) || floorState.isOf(Blocks.DIRT) || floorState.isOf(Blocks.MUD)) {
            if (biomePath.contains("ocean") || biomePath.contains("beach")) {
                if (random.nextInt(12) == 0) {
                    Block[] corals = {Blocks.TUBE_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK};
                    level.setBlockState(pos, corals[random.nextInt(corals.length)].getDefaultState(), 3);
                }
            } else {
                if (random.nextInt(10) == 0) {
                    Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY};
                    level.setBlockState(pos, flowers[random.nextInt(flowers.length)].getDefaultState(), 3);
                } else if (random.nextInt(3) == 0) {
                    level.setBlockState(pos, Blocks.GRASS.getDefaultState(), 3);
                }
            }
        }
        else if (floorState.isOf(Blocks.MYCELIUM) || floorState.isOf(Blocks.PODZOL)) {
            if (random.nextInt(6) == 0) {
                level.setBlockState(pos, random.nextBoolean() ? Blocks.BROWN_MUSHROOM.getDefaultState() : Blocks.RED_MUSHROOM.getDefaultState(), 3);
            }
        }
    }

    private BlockState getVeinOre(int x, int y, int z) {
        if (oreCache == null) {
            oreCache = new ArrayList<>();
            for (RegistryEntry<Block> ore : Registries.BLOCK.getOrCreateEntryList(Tags.Blocks.ORES)) {
                oreCache.add(ore.value().getDefaultState());
            }
            if (oreCache.isEmpty()) oreCache.add(Blocks.DIAMOND_ORE.getDefaultState());
        }
        int cellX = Math.floorDiv(x, 12), cellY = Math.floorDiv(y, 12), cellZ = Math.floorDiv(z, 12);
        long seed = (long)cellX * 8951325825L + (long)cellY * 11525161L + (long)cellZ * 3129871L;
        seed ^= seed >>> 16; seed *= 0x2545F4914F6CDD1DL; seed ^= seed >>> 16;
        return oreCache.get(Math.abs((int)(seed % oreCache.size())));
    }

    @Override public void populateEntities(ChunkRegion region) {}
    @Override public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig random, Chunk chunk) {}
    @Override public void carve(ChunkRegion region, long seed, NoiseConfig random, BiomeAccess biomeManager, StructureAccessor structureManager, Chunk chunk, GenerationStep.Carver step) {}
    @Override public int getHeight(int x, int z, Heightmap.Type type, HeightLimitView heightAccessor, NoiseConfig random) { return 64; }
    @Override public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView heightAccessor, NoiseConfig random) { return new VerticalBlockSample(0, new BlockState[0]); }
    @Override public void getDebugHudText(List<String> info, NoiseConfig random, BlockPos pos) {}
}