package application;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Lightweight UDP helper for sending/receiving short text messages (STATE / INPUT snapshots).
 * Designed as a minimal drop-in companion to the existing TCP NetworkManager.
 */
public class UdpNetworkManager {
    private DatagramSocket socket;
    private InetAddress remoteAddr;
    private int remotePort;
    private Thread recvThread;
    private volatile boolean running = false;

    private UdpNetworkManager() {}

    public static UdpNetworkManager createServer(int bindPort) throws Exception {
        UdpNetworkManager nm = new UdpNetworkManager();
        nm.socket = new DatagramSocket(bindPort);
        return nm;
    }

    public static UdpNetworkManager createClient(String host, int port) throws Exception {
        UdpNetworkManager nm = new UdpNetworkManager();
        nm.socket = new DatagramSocket();
        nm.remoteAddr = InetAddress.getByName(host);
        nm.remotePort = port;
        return nm;
    }

    public int getLocalPort() {
        return socket == null ? -1 : socket.getLocalPort();
    }

    public void setRemote(InetAddress addr, int port) {
        this.remoteAddr = addr; this.remotePort = port;
    }

    public void sendSnapshot(String msg) {
        send(msg);
    }

    public void sendInput(String msg) {
        send(msg);
    }

    private synchronized void send(String msg) {
        if (socket == null || remoteAddr == null) return;
        try {
            byte[] data = msg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket p = new DatagramPacket(data, data.length, remoteAddr, remotePort);
            socket.send(p);
        } catch (Exception e) {
            System.out.println("[UdpNetworkManager] send failed: " + e.getMessage());
        }
    }

    /**
     * Start a background receiver which invokes onMessage for each received packet.
     */
    public void startReceiver(Consumer<String> onMessage) {
        if (socket == null) throw new IllegalStateException("Socket not initialized");
        if (recvThread != null && recvThread.isAlive()) return;
        running = true;
        recvThread = new Thread(() -> {
            byte[] buf = new byte[4096];
            while (running) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    int len = Math.min(p.getLength(), buf.length);
                    String s = new String(p.getData(), 0, len, StandardCharsets.UTF_8);
                    // remember remote peer if not known
                    if (remoteAddr == null) {
                        remoteAddr = p.getAddress();
                        remotePort = p.getPort();
                    }
                    onMessage.accept(s);
                } catch (Exception e) {
                    if (running) System.out.println("[UdpNetworkManager] recv error: " + e.getMessage());
                }
            }
        });
        recvThread.setDaemon(true);
        recvThread.start();
    }

    public void close() {
        running = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
