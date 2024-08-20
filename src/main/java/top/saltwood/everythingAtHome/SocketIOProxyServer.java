package top.saltwood.everythingAtHome;

import com.alibaba.fastjson2.JSON;
import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.ClientOperations;
import com.corundumstudio.socketio.Configuration;
import com.fasterxml.jackson.annotation.JsonCreator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SocketIOProxyServer {
    protected final com.corundumstudio.socketio.SocketIOServer ioServer;
    public com.corundumstudio.socketio.SocketIOClient centerClient = null;
    protected FileWatcher watcher;

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
        this.watcher = new FileWatcher();

        this.addListeners();
    }

    public void proxyWrapper(com.corundumstudio.socketio.SocketIOClient client, Object data,
                             com.corundumstudio.socketio.AckRequest ackRequest,
                             String eventName){
        String sign;
        synchronized (watcher.handshakeString) {
            sign = watcher.handshakeString;
        }
        Request request = new Request.Builder()
                .url("http://locahost:65012/93AtHome/socketio/" + eventName + "?sign=" + sign)
                .addHeader("User-Agent", "93@Home-socket.io/1.0.0")
                .method("POST", RequestBody.create(JSON.toJSONString(new HashMap<Object, Object>() {
                    {
                        put("sessionId", client.getSessionId().toString());
                        put("data", data);
                    }
                }).getBytes()))
                .build();
        OkHttpClient c = new OkHttpClient();
        Response response = null;
        try {
            response = c.newCall(request).execute();
            if (!response.isSuccessful()) client.disconnect();
            String json = response.body().byteString().toString();
            Object res = JSON.parse(json);
            if (ackRequest.isAckRequested() && !(eventName.equals("connect") || eventName.equals("disconnect"))) ackRequest.sendAckData(res);
        } catch (IOException ignore) { }
    }

    private void addListeners() {
        this.ioServer.addConnectListener(client -> {
            client.sendEvent("message", "Welcome to Open93@Home! You can find us at https://github.com/SaltWood-Studio.");
            System.out.println("Client connected: " + client.getSessionId());
            this.proxyWrapper(client, client.getHandshakeData().getAuthToken(), null, "connect");
        });

        // Event for receiving message from client
        this.ioServer.addEventListener("enable", Object.class, (client, data, ackRequest) -> {
            this.proxyWrapper(client, data, ackRequest, "enable");
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
            this.proxyWrapper(client, new Object(), null, "disconnect");
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
