package net.lerariemann.infinity.platform.forge;

import net.lerariemann.infinity.util.PlatformMethods;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ForgePacketRegistry {
    private static final Map<Identifier, PlatformMethods.ServerPacketReceiver> SERVER_HANDLERS = new HashMap<>();
    private static final Map<Identifier, PlatformMethods.ClientPacketReceiver> CLIENT_HANDLERS = new HashMap<>();

    public static void registerServerHandler(Identifier channel, PlatformMethods.ServerPacketReceiver receiver) {
        SERVER_HANDLERS.put(channel, receiver);
    }

    public static void registerClientHandler(Identifier channel, PlatformMethods.ClientPacketReceiver receiver) {
        CLIENT_HANDLERS.put(channel, receiver);
    }

    public static void handleServerPacket(Identifier channel, PacketByteBuf buf, ServerPlayerEntity player) {
        PlatformMethods.ServerPacketReceiver receiver = SERVER_HANDLERS.get(channel);
        if (receiver != null) {
            receiver.receive(buf, player);
        }
    }

    public static void handleClientPacket(Identifier channel, PacketByteBuf buf) {
        PlatformMethods.ClientPacketReceiver receiver = CLIENT_HANDLERS.get(channel);
        if (receiver != null) {
            receiver.receive(buf);
        }
    }
}