package network;

import java.io.*;
import java.net.*;

public abstract class TCPPeer implements NetworkPeer {
    protected static final int PING_INTERVAL = 3000;
    protected static final int TIMEOUT_MS = 10000;

    protected PrintWriter out;
    protected BufferedReader in;
    protected Thread readerThread;
    protected Thread pingThread;
    protected volatile boolean running;
    protected volatile long lastMessageTime;

    protected abstract Listener getListener();

    public interface Listener {
        void onConnected();
        void onMessage(String msg);
        void onDisconnected();
        void onError(String error);
    }

    protected void startReaderThread() {
        readerThread = new Thread(() -> {
            try {
                String msg;
                while (running && (msg = in.readLine()) != null) {
                    lastMessageTime = System.currentTimeMillis();
                    if ("PING".equals(msg)) {
                        send("PONG");
                        continue;
                    }
                    if ("PONG".equals(msg)) continue;
                    getListener().onMessage(msg);
                }
            } catch (SocketTimeoutException e) {
                if (running) getListener().onError("Ket noi timeout!");
            } catch (IOException e) {
                if (running) getListener().onDisconnected();
            }
        });
        readerThread.start();
    }

    protected void startPingThread() {
        pingThread = new Thread(() -> {
            while (running) {
                try { Thread.sleep(PING_INTERVAL); } catch (InterruptedException e) { break; }
                if (!running) break;
                long idle = System.currentTimeMillis() - lastMessageTime;
                if (idle >= TIMEOUT_MS) {
                    getListener().onError("Mat ket noi (timeout)");
                    stop();
                    break;
                }
                send("PING");
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();
    }

    public void send(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush();
        }
    }

    public void stop() {
        running = false;
        if (pingThread != null) pingThread.interrupt();
        if (readerThread != null) readerThread.interrupt();
        try { if (out != null) out.close(); } catch (Exception e) {}
        try { if (in != null) in.close(); } catch (Exception e) {}
    }

    public boolean isConnected() {
        return false;
    }
}
