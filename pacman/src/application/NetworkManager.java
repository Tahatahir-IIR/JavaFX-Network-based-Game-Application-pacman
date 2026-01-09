package application;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;

public class NetworkManager {
    private ServerSocket server;
    private Socket socket;
    private Thread acceptThread;

    // For client
    private Socket clientSocket;

    // UDP managers (optional, for latency compensation)
    private UdpNetworkManager udpHost;
    private UdpNetworkManager udpClient;

    private NetworkManager() {}

    public static NetworkManager createServer() throws Exception {
        NetworkManager nm = new NetworkManager();
        nm.server = new ServerSocket(0); // random free port
        nm.acceptThread = new Thread(() -> {
            try {
                nm.socket = nm.server.accept();
            } catch (Exception e) { /* ignore */ }
        });
        nm.acceptThread.setDaemon(true);
        nm.acceptThread.start();
        return nm;
    }

    public String getRoomCode() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        int tcpPort = server.getLocalPort();
        String raw = ip + ":" + tcpPort;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes("UTF-8"));
    }

    public void initUdpHost(int udpPort) throws Exception {
        udpHost = UdpNetworkManager.createServer(udpPort);
    }

    public void initUdpClient(String host, int udpPort) throws Exception {
        udpClient = UdpNetworkManager.createClient(host, udpPort);
    }

    public UdpNetworkManager getUdpHost() { return udpHost; }
    public UdpNetworkManager getUdpClient() { return udpClient; }

    public static NetworkManager connectTo(String code) throws Exception {
        String decoded = new String(Base64.getUrlDecoder().decode(code), "UTF-8");
        String[] parts = decoded.split(":" );
        if (parts.length != 2) throw new IllegalArgumentException("Invalid code");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        NetworkManager nm = new NetworkManager();
        nm.clientSocket = new Socket(ip, port);
        return nm;
    }

    // Host: wait for client connect and get its socket
    public Socket waitForClient() throws Exception {
        if (socket != null) return socket;
        if (acceptThread != null) {
            // wait until accept completes (blocking) to ensure host gets the client socket
            acceptThread.join();
        }
        return socket;
    }

    // Host-side send helper (send to the connected client)
    public synchronized void sendToClient(String msg) {
        try {
            if (socket == null) return;
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(msg);
            out.flush();
        } catch (Exception e) {
            System.out.println("[NetworkManager] sendToClient failed: " + e.getMessage());
        }
    }

    // Client-side send helper (send to host)
    public synchronized void sendToHost(String msg) {
        try {
            if (clientSocket == null) return;
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            out.println(msg);
            out.flush();
        } catch (Exception e) {
            System.out.println("[NetworkManager] sendToHost failed: " + e.getMessage());
        }
    }

    // Client: get socket
    public Socket getClientSocket() { return clientSocket; }

    public void close() {
        try { if (socket != null) socket.close(); } catch (Exception ignored){}
        try { if (server != null) server.close(); } catch (Exception ignored){}
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception ignored){}
        try { if (udpHost != null) udpHost.close(); } catch (Exception ignored){}
        try { if (udpClient != null) udpClient.close(); } catch (Exception ignored){}
    }
}
