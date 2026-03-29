package net.lerariemann.infinity.registry.var;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.world.cavern.CavernBiomeSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.source.BiomeSource;

public class BiomeRegistry {
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCE =
            DeferredRegister.create(InfinityMod.MOD_ID, RegistryKeys.BIOME_SOURCE);

    public static void register() {
        BIOME_SOURCE.register("cavern_biome_source", () -> CavernBiomeSource.CODEC);
        BIOME_SOURCE.register();
    }
}
