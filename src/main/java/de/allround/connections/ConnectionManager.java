package de.allround.connections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final Map<PlayerConnection, Long> connectionKeepAliveMap = new ConcurrentHashMap<>();


    public void register(PlayerConnection playerConnection){
        connectionKeepAliveMap.put(playerConnection, System.currentTimeMillis());
    }
}
