package network;

import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscoveryService {
    public static final int DISCOVERY_PORT = 5001;
    public static final int DISCOVERY_TIMEOUT = 15000;

    private Thread broadcastThread;
    private Thread listenThread;
    private Thread scanThread;
    private DatagramSocket broadcastSocket;
    private DatagramSocket listenSocket;
    private volatile boolean running;

    public void startHostBroadcast(String roomCode) {
        running = true;
        String msg = "CHESS_HOST:1:" + roomCode;

        broadcastThread = new Thread(() -> {
            try {
                broadcastSocket = new DatagramSocket();
                broadcastSocket.setBroadcast(true);
                byte[] buf = msg.getBytes();
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");

                while (running) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcast, DISCOVERY_PORT);
                    try { broadcastSocket.send(packet); } catch (Exception ignored) {}
                    try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    public void startDiscovery(String roomCode, java.util.function.Consumer<String> onHostFound) {
        running = true;
        String prefix = "CHESS_HOST:1:" + roomCode;
        AtomicBoolean found = new AtomicBoolean(false);

        listenThread = new Thread(() -> {
            try {
                listenSocket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
                listenSocket.setBroadcast(true);
                listenSocket.setSoTimeout(3000);
                byte[] buf = new byte[1024];

                long startTime = System.currentTimeMillis();

                while (running && System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        listenSocket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength()).trim();
                        if (received.equals(prefix)) {
                            String hostIP = packet.getAddress().getHostAddress();
                            found.set(true);
                            onHostFound.accept(hostIP);
                            return;
                        }
                    } catch (SocketTimeoutException e) {
                        // Timeout normal, check running flag
                    } catch (Exception ignored) {}
                }

                if (!found.get()) {
                    scanSubnet(roomCode, onHostFound, found, startTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void scanSubnet(String roomCode, java.util.function.Consumer<String> onHostFound,
                            AtomicBoolean found, long startTime) {
        scanThread = new Thread(() -> {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                byte[] ipBytes = localHost.getAddress();
                if (ipBytes.length != 4) return;

                String prefix = "CHESS_HOST:1:" + roomCode;
                DatagramSocket scanSocket = new DatagramSocket();
                scanSocket.setSoTimeout(1000);

                for (int i = 1; i <= 254 && running && !found.get()
                     && System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT; i++) {
                    ipBytes[3] = (byte) i;
                    String targetIP = InetAddress.getByAddress(ipBytes).getHostAddress();

                    byte[] buf = prefix.getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length,
                        InetAddress.getByName(targetIP), DISCOVERY_PORT);
                    try {
                        scanSocket.send(packet);
                    } catch (Exception ignored) {}
                }

                // Listen for responses during scan
                byte[] recvBuf = new byte[1024];
                long scanStart = System.currentTimeMillis();
                while (running && !found.get()
                     && System.currentTimeMillis() - scanStart < 5000) {
                    try {
                        DatagramPacket response = new DatagramPacket(recvBuf, recvBuf.length);
                        scanSocket.receive(response);
                        String received = new String(response.getData(), 0, response.getLength()).trim();
                        if (received.equals(prefix)) {
                            found.set(true);
                            onHostFound.accept(response.getAddress().getHostAddress());
                        }
                    } catch (SocketTimeoutException e) {
                        break;
                    } catch (Exception ignored) {}
                }

                scanSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        scanThread.setDaemon(true);
        scanThread.start();
    }

    public void stop() {
        running = false;
        if (broadcastSocket != null && !broadcastSocket.isClosed()) broadcastSocket.close();
        if (listenSocket != null && !listenSocket.isClosed()) listenSocket.close();
        if (broadcastThread != null) broadcastThread.interrupt();
        if (listenThread != null) listenThread.interrupt();
        if (scanThread != null) scanThread.interrupt();
    }
}
