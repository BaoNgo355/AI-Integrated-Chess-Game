package network;

public class LANController {

    public interface Callback {
        void onStatus(String text);
        void onGameStart(String color, NetworkPeer peer);
        void onMessage(String msg);
        void onError(String error);
        void onCancel();
    }

    private DiscoveryService discovery;
    private GameServer server;
    private GameClient client;
    private Callback callback;
    private boolean active;

    public void host(String roomCode, Callback callback) {
        stopAll();
        this.callback = callback;
        this.active = true;

        callback.onStatus("Dang tao phong...");

        discovery = new DiscoveryService();
        discovery.startHostBroadcast(roomCode);

        server = new GameServer();
        server.start(5000, new GameServer.Listener() {
            @Override
            public void onWaiting() {
                callback.onStatus("Dang cho nguoi choi ghep vao...");
            }

            @Override
            public void onConnected() {
                if (!active) return;
                discovery.stop();
                server.sendMessage("COLOR:BLACK");
                callback.onStatus("Da ket noi! Dang bat dau game...");
                callback.onGameStart("WHITE", server);
            }

            @Override
            public void onMessage(String msg) {
                if (active) callback.onMessage(msg);
            }

            @Override
            public void onDisconnected() {
                if (!active) return;
                callback.onError("Mat ket noi!");
            }

            @Override
            public void onError(String error) {
                if (!active) return;
                callback.onError("Loi: " + error);
            }
        });
    }

    public void join(String roomCode, Callback callback) {
        stopAll();
        this.callback = callback;
        this.active = true;

        callback.onStatus("Dang tim tran...");

        discovery = new DiscoveryService();
        discovery.startDiscovery(roomCode, hostIP -> {
            if (!active) return;
            callback.onStatus("Da tim thay phong! Dang ket noi...");

            client = new GameClient();
            client.connect(hostIP, 5000, new TCPPeer.Listener() {
                @Override
                public void onConnected() {
                    discovery.stop();
                    callback.onStatus("Da ket noi! Dang cho thong tin...");
                }

                @Override
                public void onMessage(String msg) {
                    if (!active) return;
                    if (msg.startsWith("COLOR:")) {
                        String color = msg.substring(6);
                        String label = color.equals("WHITE") ? "Trang" : "Den";
                        callback.onStatus("Ban cam quan " + label);
                        callback.onGameStart(color, client);
                    }
                    callback.onMessage(msg);
                }

                @Override
                public void onDisconnected() {
                    if (!active) return;
                    callback.onError("Mat ket noi!");
                }

                @Override
                public void onError(String error) {
                    if (!active) return;
                    callback.onError("Loi ket noi: " + error);
                }
            });
        });
    }

    public void cancel() {
        active = false;
        stopAll();
        if (callback != null) callback.onCancel();
    }

    private void stopAll() {
        if (discovery != null) { discovery.stop(); discovery = null; }
        if (server != null) { server.stop(); server = null; }
        if (client != null) { client.stop(); client = null; }
    }
}
