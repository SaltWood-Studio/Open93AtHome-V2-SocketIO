package top.saltwood.everythingAtHome;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.ClientOperations;
import com.corundumstudio.socketio.Configuration;

import java.io.FileInputStream;
import java.util.Map;
import java.util.UUID;

public class SocketIOProxyServer {
    protected final com.corundumstudio.socketio.SocketIOServer ioServer;
    public com.corundumstudio.socketio.SocketIOClient centerClient = null;

    public SocketIOProxyServer() {
        this(9300);
    }

    public SocketIOProxyServer(int socketioPort) {
        this("0.0.0.0", socketioPort);
    }

    public SocketIOProxyServer(String host, int socketioPort) {
        // Configuration for the server
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(socketioPort);

        // Create a new SocketIOServer instance
        this.ioServer = new com.corundumstudio.socketio.SocketIOServer(config);

        this.addListeners();
    }

    public void proxyWrapper(com.corundumstudio.socketio.SocketIOClient client, Object data,
                             com.corundumstudio.socketio.AckRequest ackRequest,
                             String eventName){
        if (this.centerClient == null) {
            if (ackRequest.isAckRequested()) {
                client.sendEvent("message", "Center server is down. Please try again later.");
                client.disconnect();
            }
        } else {
            Object waiter = new Object();
            synchronized (waiter) { // 确保在 synchronized 块中调用 wait()
                this.centerClient.sendEvent(eventName, new AckCallback<>(Object.class) {
                    @Override
                    public void onSuccess(Object o) {
                        if (ackRequest != null) ackRequest.sendAckData(o);
                    }
                }, data, client.getSessionId());
                try {
                    waiter.wait(10000); // 等待 10 秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 恢复中断状态
                    // 处理被中断的情况
                }
            }
        }
    }

    private void addListeners() {
        this.ioServer.addConnectListener(client -> {
            client.sendEvent("message", "Welcome to Open93@Home! You can find us at https://github.com/SaltWood-Studio.");
            System.out.println("Client connected: " + client.getSessionId());
            this.proxyWrapper(client, client.getHandshakeData().getAuthToken(), null, "cluster-connect");
        });

        // Event for receiving message from client
        this.ioServer.addEventListener("enable", Object.class, (client, data, ackRequest) -> {
            this.proxyWrapper(client, data, ackRequest, "enable");
        });

        this.ioServer.addEventListener("center-disconnect-cluster", Object.class, (client, data, ackRequest) -> {
            this.ioServer.getAllClients()
                    .stream()
                    .filter(c -> c.getSessionId().equals(UUID.fromString(((Map<String, String>) data).get("sessionId"))))
                    .forEach(c -> c.disconnect());
        });

        this.ioServer.addEventListener("center-inject", Object.class, (client, data, ackRequest) -> {
            String handshakeSign;
            try (FileInputStream fis = new FileInputStream("./handshake")) {
                handshakeSign = new String(fis.readAllBytes());
            }
            Map<String, Object> map = (Map<String, Object>) data;
            String requestData = (String) map.get("handshake");
            if (requestData.equals(handshakeSign)) {
                this.centerClient = client;
            }
            else {
                System.out.println("Center server authentication invalid.");
            }
        });

        // Event for receiving message from client
        this.ioServer.addEventListener("disable", Object.class, (client, data, ackRequest) -> {
            this.proxyWrapper(client, data, ackRequest, "disable");
        });

        // Event for receiving message from client
        this.ioServer.addEventListener("keep-alive", Object.class, (client, data, ackRequest) -> {
            this.proxyWrapper(client, data, ackRequest, "keep-alive");
        });

        // Event for client disconnect
        this.ioServer.addDisconnectListener(client -> {
            this.proxyWrapper(client, new Object(), null, "cluster-disconnect");
        });
    }

    public void start() {
        // Start the server
        ioServer.start();
        System.out.println("Socket.IO proxy server started on port " + this.ioServer.getConfiguration().getPort());
    }

    public void stop() {
        // Stop the server
        ioServer.stop();
        System.out.println("Socket.IO proxy server stopped");
    }

    public void disconnectAll() {
        ioServer.getAllClients().forEach(ClientOperations::disconnect);
    }
}
