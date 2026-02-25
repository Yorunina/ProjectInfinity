package net.lerariemann.infinity.registry.var;

import com.google.common.collect.ImmutableSet;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.registry.core.ModBlocks;
import net.lerariemann.infinity.util.InfinityMethods;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.poi.PointOfInterestType;

import static net.lerariemann.infinity.InfinityMod.MOD_ID;

public class ModPoi {
    public static RegistrySupplier<PointOfInterestType> NETHER_PORTAL;
    public static RegistryKey<PointOfInterestType> NETHER_PORTAL_KEY;

    public static void registerPoi() {
        InfinityMod.LOGGER.debug("Registering POI for " + InfinityMod.MOD_ID);
        if (Platform.isFabric()) registerPoiFabric();
        else registerPoiArchitectury();
        NETHER_PORTAL_KEY = RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, InfinityMethods.getId("nether_portal"));

    }

    // Deferred handling of Poi through Architectury API, works on NeoForge.
    public static void registerPoiArchitectury() {
        final DeferredRegister<PointOfInterestType> POI_TYPES = DeferredRegister.create(MOD_ID, RegistryKeys.POINT_OF_INTEREST_TYPE);
        NETHER_PORTAL = POI_TYPES.register("nether_portal", () -> new PointOfInterestType(ImmutableSet.copyOf(ModBlocks.PORTAL.get().getStateManager().getStates()), 0, 1));
        POI_TYPES.register();
    }

    // Legacy handling of Poi on Fabric, as Architectury Poi are not correctly registered.
    @ExpectPlatform
    public static void registerPoiFabric() {
        throw new AssertionError();
    }
}