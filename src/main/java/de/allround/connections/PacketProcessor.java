package de.allround.connections;


import de.allround.protocol.datatypes.DataType;
import de.allround.protocol.packets.Packets;
import de.allround.protocol.packets.ReadablePacket;
import org.jetbrains.annotations.NotNull;

public class PacketProcessor {

    private final PlayerConnection playerConnection;
    private boolean compression; //todo: compression

    public PacketProcessor setCompression(boolean compression) {
        this.compression = compression;
        return this;
    }

    public boolean isCompression() {
        return compression;
    }

    public PacketProcessor(@NotNull PlayerConnection playerConnection) {
        this.playerConnection = playerConnection;

        playerConnection.getClient().handle(buffer -> {
            if (!compression){
                DataType.VAR_INT.read(buffer);
                int packetType = DataType.VAR_INT.read(buffer);
                ReadablePacket packet = Packets.Serverbound.get(playerConnection.getConnectionState(), packetType, buffer);
                playerConnection.handlePacket(packet);
            }
        });
    }
}
