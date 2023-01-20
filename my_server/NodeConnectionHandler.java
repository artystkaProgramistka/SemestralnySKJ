package my_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NodeConnectionHandler extends Thread {
    private final String host;
    private final int port;
    private final DatabaseNode node;
    private Socket socket;

    public NodeConnectionHandler(String host, int port, DatabaseNode node) {
        this.host = host;
        this.port = port;
        this.node = node;
    }

    public void run() {
        try {
            socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("connect " + node.getTcpPort());
            String response = in.readLine();
            if (!response.equals("OK")) {
                System.err.println("Error connecting to node at " + host + ":" + port);
            }
        } catch (IOException e) {
            System.err.println("Error connecting to node at " + host + ":" + port);
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    public Socket getSocket() {
        return socket;
    }
}