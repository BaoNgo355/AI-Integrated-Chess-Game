package network;

public interface NetworkPeer {
    void send(String msg);
    void stop();
    boolean isConnected();
}
