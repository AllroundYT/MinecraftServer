package de.allround.network;

import de.allround.future.Future;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Server {
    private final ServerSocket serverSocket;
    private final Future<Client> clientFuture;
    private final Future<Client> clientDisconnectFuture;
    private final List<Client> connectedClients;

    public Server handleClient(Consumer<Client> consumer){
        clientFuture.onSuccess(consumer);
        return this;
    }

    public Server handleDisconnect(Consumer<Client> consumer){
        clientDisconnectFuture.onSuccess(consumer);
        return this;
    }

    public List<Client> getConnectedClients() {
        return connectedClients;
    }

    public SocketAddress getLocalAddress(){
        return serverSocket.getLocalSocketAddress();
    }

    public Server() {
        this.clientFuture = Future.createStream();
        this.clientDisconnectFuture = Future.createStream();
        try {
            this.connectedClients = new ArrayList<>();
            this.serverSocket = new ServerSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Future<Void> bind(String host, int port){
        return Future.future(promise -> {
            try {
                serverSocket.bind(new InetSocketAddress(host,port));
                startAcceptingClients().onSuccess(unused -> promise.complete());
            } catch (IOException e) {
                promise.complete(e);
            }
            startAcceptingClients();
        });
    }

    @NotNull Future<Void> startAcceptingClients(){
        return Future.future(promise -> {
            try {
                promise.complete();
                while (serverSocket.isBound() && !serverSocket.isClosed()) {
                    Client client = new Client(serverSocket.accept());
                    client.listen().onFailure(unused -> {
                        this.connectedClients.remove(client);
                        clientDisconnectFuture.succeed(client);
                    }).onSuccess(unused -> {
                        connectedClients.add(client);
                        clientFuture.succeed(client);
                    });
                }
            } catch (IOException e) {
                promise.complete(e);
            }
        });
    }
}
