package net.lerariemann.infinity.platform.forge;

import net.lerariemann.infinity.util.PlatformMethods;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ForgePlatformMethods {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(
        new Identifier("infinity", "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    static {
        // 注册所有数据包
        int id = 0;
        
        PACKET_HANDLER.registerMessage(id++,
            ForgePacketWrapper.class,
            ForgePacketWrapper::encode,
            ForgePacketWrapper::decode,
            ForgePacketWrapper::handle
        );
    }

    public static void sendToPlayer(ServerPlayerEntity player, Identifier channel, PacketByteBuf buf) {
        PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), 
            new ForgePacketWrapper(channel, buf));
    }

    public static void sendToClient(Identifier channel, PacketByteBuf buf) {
        PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), 
            new ForgePacketWrapper(channel, buf));
    }

    public static void registerServerPacketReceiver(Identifier channel, PlatformMethods.ServerPacketReceiver receiver) {
        // 在Forge中，这通常通过网络通道处理
        ForgePacketRegistry.registerServerHandler(channel, receiver);
    }

    public static void registerClientPacketReceiver(Identifier channel, PlatformMethods.ClientPacketReceiver receiver) {
        // 在Forge中，这通常通过网络通道处理
        ForgePacketRegistry.registerClientHandler(channel, receiver);
    }

    public static class ForgePacketWrapper {
        private final Identifier channel;
        private final PacketByteBuf data;

        public ForgePacketWrapper(Identifier channel, PacketByteBuf data) {
            this.channel = channel;
            this.data = data;
        }

        public static void encode(ForgePacketWrapper msg, PacketByteBuf buf) {
            buf.writeIdentifier(msg.channel);
            buf.writeBytes(msg.data);
        }

        public static ForgePacketWrapper decode(PacketByteBuf buf) {
            Identifier channel = buf.readIdentifier();
            PacketByteBuf data = new PacketByteBuf(buf.copy());
            return new ForgePacketWrapper(channel, data);
        }

        public static void handle(ForgePacketWrapper msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                    ForgePacketRegistry.handleServerPacket(msg.channel, msg.data, ctx.get().getSender());
                } else {
                    ForgePacketRegistry.handleClientPacket(msg.channel, msg.data);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}