package net.lerariemann.infinity.registry.var;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.world.cavern.CavernGenerator;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class GeneratorRegistry {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(InfinityMod.MOD_ID, RegistryKeys.CHUNK_GENERATOR);

    public static void register() {
        CHUNK_GENERATORS.register("cavern", () -> CavernGenerator.CODEC);
        CHUNK_GENERATORS.register();
    }
}