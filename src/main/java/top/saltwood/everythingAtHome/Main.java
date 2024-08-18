package top.saltwood.everythingAtHome;

import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        SocketIOProxyServer proxy = new SocketIOProxyServer();
        proxy.start();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (proxy.centerClient != null) {
                    proxy.centerClient.sendEvent("proxy-keep-alive", "I am the proxy server.");
                }
            }
        }, 0, 1000 * 60 * 3);
    }
}