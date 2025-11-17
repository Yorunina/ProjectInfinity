package net.lerariemann.infinity.platform.forge;

import net.lerariemann.infinity.util.PlatformMethods;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ForgePlatformImpl {
    public static void sendToPlayer(ServerPlayerEntity player, Identifier channel, PacketByteBuf buf) {
        ForgePlatformMethods.sendToPlayer(player, channel, buf);
    }

    public static void sendToClient(Identifier channel, PacketByteBuf buf) {
        ForgePlatformMethods.sendToClient(channel, buf);
    }

    public static void registerServerPacketReceiver(Identifier channel, PlatformMethods.ServerPacketReceiver receiver) {
        ForgePlatformMethods.registerServerPacketReceiver(channel, receiver);
    }

    public static void registerClientPacketReceiver(Identifier channel, PlatformMethods.ClientPacketReceiver receiver) {
        ForgePlatformMethods.registerClientPacketReceiver(channel, receiver);
    }
}