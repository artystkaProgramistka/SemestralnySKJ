package my_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class NodeConnectionHandler extends Thread {

    public final String host;
    public final int port;
    private final Socket socket;

    private final DatabaseNode hostNode;

    public NodeConnectionHandler(String host, int port, DatabaseNode hostNode) throws IOException {
        this.host = host;
        this.port = port;
        this.socket = new Socket(host, port);
        this.hostNode = hostNode;

        // TODO: send a connection request
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        try {
            while (true) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String request = in.readLine();
                String response = handleRequest(request);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String handleRequest(String request) {
        // TODO
        return "TODO";
    }
}