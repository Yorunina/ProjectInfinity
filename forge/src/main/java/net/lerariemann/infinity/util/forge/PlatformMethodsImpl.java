package net.lerariemann.infinity.util.forge;

import dev.architectury.registry.registries.RegistrySupplier;
import io.netty.buffer.Unpooled;
import net.lerariemann.infinity.item.forge.StarOfLangItemForge;
import net.lerariemann.infinity.item.StarOfLangItem;
import net.lerariemann.infinity.util.PlatformMethods;
import net.lerariemann.infinity.platform.forge.ForgePlatformImpl;
import net.lerariemann.infinity.util.InfinityMethods;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * See {@link PlatformMethods} for usages.
 */
@SuppressWarnings("unused")
public class PlatformMethodsImpl {

    public static PacketByteBuf createPacketByteBufs() {
        return new PacketByteBuf(Unpooled.buffer());
    }

    public static void onWorldLoad(Object mixin, ServerWorld world) {
        MinecraftServer server = world.getServer();
        server.forgeGetWorldMap().put(world.getRegistryKey(),world);
        server.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(world));
    }

    public static void unfreeze(Registry<?> registry) {
        SimpleRegistry<?> writableRegistry = (SimpleRegistry<?>) registry;
        writableRegistry.unfreeze();
    }

    public static void freeze(Registry<?> registry) {
        registry.freeze();
    }

    //Optional, requires Item Group API.
    public static <T extends Item> void addAfter(RegistrySupplier<T> supplier, RegistryKey<ItemGroup> group, Item item) {
        // Forge implementation would use different approach
    }

    public static Path getRootConfigPath() {
        return ModLoadingContext.get().getActiveContainer().getModInfo().getOwningFile().getFile().findResource("config");
    }

    public static Path getConfigPath() {
        return Path.of(FMLPaths.CONFIGDIR.get() + "/infinity");
    }

    public static TagKey<Item> createItemTag(String id) {
        return ItemTags.create(InfinityMethods.getId(id));
    }
    public static TagKey<Block> createBlockTag(String id) {
        return BlockTags.create(InfinityMethods.getId(id));
    }

    public static void registerFlammableBlock(RegistrySupplier<Block> block, int burn, int spread) {
        // Forge implementation would use different approach
    }

    public static Function<Item.Settings, ? extends StarOfLangItem> getStarOfLangConstructor() {
        return StarOfLangItemForge::new;
    }

    // 网络相关方法实现
    public static void sendToPlayer(ServerPlayerEntity player, Identifier channel, PacketByteBuf buf) {
        ForgePlatformImpl.sendToPlayer(player, channel, buf);
    }

    public static void sendToClient(Identifier channel, PacketByteBuf buf) {
        ForgePlatformImpl.sendToClient(channel, buf);
    }

    public static void registerServerPacketReceiver(Identifier channel, PlatformMethods.ServerPacketReceiver receiver) {
        ForgePlatformImpl.registerServerPacketReceiver(channel, receiver);
    }

    public static void registerClientPacketReceiver(Identifier channel, PlatformMethods.ClientPacketReceiver receiver) {
        ForgePlatformImpl.registerClientPacketReceiver(channel, receiver);
    }
}
