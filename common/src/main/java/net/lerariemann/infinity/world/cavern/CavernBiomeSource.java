package net.lerariemann.infinity.world.cavern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;


import java.util.stream.Stream;

public class CavernBiomeSource extends BiomeSource {
    public static final Codec<CavernBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryCodecs.entryList(RegistryKeys.BIOME)
                            .fieldOf("biomes")
                            .forGetter(CavernBiomeSource::getCavernBiomes)
            ).apply(instance, instance.stable(CavernBiomeSource::new)));

    private final RegistryEntryList<Biome> cavernBiomes;

    public CavernBiomeSource(RegistryEntryList<Biome> biomes) {
        this.cavernBiomes = biomes;
    }

    public RegistryEntryList<Biome> getCavernBiomes() {
        return this.cavernBiomes;
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return this.cavernBiomes.stream();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler sampler) {
        // Creates a strict 256x256 block grid of randomized biomes
        int scale = 64;
        int cellX = Math.floorDiv(x, scale);
        int cellZ = Math.floorDiv(z, scale);

        long seed = (long)cellX * 341873128712L + (long)cellZ * 132897987541L;
        seed ^= seed >>> 16; seed *= 0x2545F4914F6CDD1DL; seed ^= seed >>> 16;

        int index = Math.abs((int)(seed % this.cavernBiomes.size()));
        return this.cavernBiomes.get(index);
    }
}