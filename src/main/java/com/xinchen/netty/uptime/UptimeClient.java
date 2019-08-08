package com.xinchen.netty.uptime;

/**
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 17:18
 */
public class UptimeClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));
    static final int RECONNECT_DELAY = Integer.parseInt(System.getProperty("reconnectDelay", "5"));
}
