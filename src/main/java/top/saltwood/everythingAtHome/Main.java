package top.saltwood.everythingAtHome;

public class Main {
    public static void main(String[] args) {
        SocketIOProxyServer proxy = new SocketIOProxyServer();
        proxy.start();
    }
}