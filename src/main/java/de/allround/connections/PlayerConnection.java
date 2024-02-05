package de.allround.connections;

import de.allround.future.Future;
import de.allround.misc.JSON;
import de.allround.misc.ReadPacketTypeHandlerMap;
import de.allround.network.Client;
import de.allround.protocol.ConnectionState;
import de.allround.protocol.packets.ReadablePacket;
import de.allround.protocol.packets.WritablePacket;
import de.allround.protocol.packets.handshake.server.Handshake;
import de.allround.protocol.packets.status.client.PingResponse;
import de.allround.protocol.packets.status.client.StatusResponse;
import de.allround.protocol.packets.status.server.PingRequest;
import de.allround.protocol.packets.status.server.StatusRequest;
import org.jetbrains.annotations.NotNull;

public class PlayerConnection {


    private final Client client;
    private final PacketProcessor packetProcessor;
    private final ReadPacketTypeHandlerMap packetHandlers; //todo: bessere lösung finden
    private ConnectionState connectionState = ConnectionState.HANDSHAKE;

    public Future<Void> sendPacket(@NotNull WritablePacket packet){
        System.out.println("Sending Packet: " + packet.getClass().getSimpleName() + " -> " + JSON.toJson(packet));
        return client.write(packet.write().array()).map(integer -> null);
    }

    public <PacketType extends ReadablePacket> void handlePacket(@NotNull PacketType packetType){
        System.out.println("Handling Packet: " + packetType.getClass().getSimpleName() + " -> " + JSON.toJson(packetType));
        packetHandlers.get(packetType.getClass()).forEach(packetHandler -> {
            packetHandler.handle(packetType);
        });
    }

    public PlayerConnection(Client client) {
        this.client = client;
        this.packetHandlers = new ReadPacketTypeHandlerMap();
        initDefaultPacketHandlers();
        this.packetProcessor = new PacketProcessor(this);
    }

    public void initDefaultPacketHandlers(){
        //todo
        packetHandlers.add(Handshake.class, handshake -> {
            this.connectionState = handshake.getNextState();
        });

        packetHandlers.add(StatusRequest.class, statusRequest -> {
            sendPacket(new StatusResponse(
                    "1.20.2",
                    763,
                    "§cHallo",
                    true,
                    true
            ));
        });

        packetHandlers.add(PingRequest.class, pingRequest -> {
            long payload = pingRequest.getPayload();
            sendPacket(new PingResponse(payload));
        });
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public PlayerConnection setConnectionState(ConnectionState connectionState) {
        this.connectionState = connectionState;
        return this;
    }

    public Client getClient() {
        return client;
    }

    public PacketProcessor getPacketProcessor() {
        return packetProcessor;
    }

}
