package network;

import java.io.*;
import java.net.*;

public class GameClient extends TCPPeer {

    private Socket socket;
    private Thread connectThread;
    private Listener listener;

    @Override
    protected TCPPeer.Listener getListener() { return listener; }

    public void connect(String ip, int port, Listener listener) {
        this.listener = listener;
        this.running = true;

        connectThread = new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 5000);
                socket.setSoTimeout(TIMEOUT_MS);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                lastMessageTime = System.currentTimeMillis();
                listener.onConnected();

                startPingThread();
                startReaderThread();

            } catch (SocketTimeoutException e) {
                if (running) listener.onError("Ket noi timeout!");
            } catch (IOException e) {
                if (running) listener.onError(e.getMessage());
            }
        });
        connectThread.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (connectThread != null) connectThread.interrupt();
        try { if (socket != null) socket.close(); } catch (Exception e) {}
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void sendMessage(String msg) {
        send(msg);
    }
}
