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
        System.out.println("Connecting to " + host + ":" + port);
        this.socket = new Socket(host, port);
        System.out.println("Connected");
        this.hostNode = hostNode;
        // TODO: send a connection request

        String response = sendMessage("connect:" + host + ":" + port);
        if (response == "ERROR") {
            System.out.println("ERROR connecting: " + host + ":" + port);
        }
    }

    private String sendMessage(String message) {
        String response = "ERROR";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            response = in.readLine();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        try {
            // Receive messages from the other connected server
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

    public void terminate() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String handleRequest(String request) {
        // TODO
        System.out.println("TODO");
        return "TODO";
    }
}