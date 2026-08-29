package network;

import java.io.*;
import java.net.*;

public class GameServer extends TCPPeer {

    public interface Listener extends TCPPeer.Listener {
        void onWaiting();
    }

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Thread acceptThread;
    private Listener listener;

    @Override
    protected TCPPeer.Listener getListener() { return listener; }

    public void start(int port, Listener listener) {
        this.listener = listener;
        this.running = true;

        acceptThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                listener.onWaiting();
                clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(TIMEOUT_MS);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                lastMessageTime = System.currentTimeMillis();
                listener.onConnected();

                startPingThread();
                startReaderThread();

            } catch (IOException e) {
                if (running) listener.onError(e.getMessage());
            }
        });
        acceptThread.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (acceptThread != null) acceptThread.interrupt();
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception e) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) {}
    }

    @Override
    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed();
    }

    public void sendMessage(String msg) {
        send(msg);
    }
}
