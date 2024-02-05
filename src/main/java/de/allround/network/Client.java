package de.allround.network;

import de.allround.future.Future;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class Client {
    private final Socket socket;
    private final Future<ByteBuffer> inputFuture;
    private final Future<Void> closeFuture;
    private int timeout = 25000;

    public boolean isClosed(){
        return socket.isClosed();
    }

    public Client(@NotNull Socket socket) {
        this.closeFuture = Future.create();
        inputFuture = Future.createStream();
        this.socket = socket;
        try {
            socket.setKeepAlive(true);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public Client handle(Consumer<ByteBuffer> consumer){
        inputFuture.onSuccess(consumer);
        return this;
    }

    public SocketAddress getLocalAddress(){
        return socket.getLocalSocketAddress();
    }

    public SocketAddress getRemoteAddress(){
        return socket.getRemoteSocketAddress();
    }

    Future<Void> listen(){
        if (isClosed()) return Future.failedFuture(new IOException("Client already closed!"));
        return Future.future(promise -> {
            try {
                promise.complete();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());
                long lastData = System.currentTimeMillis();
                while (socket.isConnected() && !socket.isClosed()) {
                    if (bufferedInputStream.available() > 0) {
                        lastData = System.currentTimeMillis();
                        byte[] data = bufferedInputStream.readNBytes(bufferedInputStream.available());
                        inputFuture.succeed(ByteBuffer.wrap(data));
                    } else {
                        if (System.currentTimeMillis() - lastData > this.timeout){
                            socket.close();
                            closeFuture.fail(new TimeoutException());
                        }
                    }
                }
                closeFuture.succeed();
            } catch (IOException e) {
                closeFuture.fail(e);
                promise.complete(e);
            }
        });
    }

    public Client handleClose(Consumer<Throwable> consumer){
        closeFuture.onSuccess(unused -> consumer.accept(null));
        closeFuture.onFailure(consumer);
        return this;
    }

    public Future<Void> write(byte[] data){
        if (isClosed()) return Future.failedFuture(new IOException("Client already closed!"));
        return Future.future(promise -> {
            try {
                socket.getOutputStream().write(data);
                promise.complete();
            } catch (IOException e) {
                promise.complete(e);
            }
        });
    }
}
