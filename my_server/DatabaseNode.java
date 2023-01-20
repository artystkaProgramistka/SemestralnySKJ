package my_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseNode {
    private final int tcpPort;
    private final Map<Integer, Integer> keyValuePairs;
    private final List<String> connections;

    public DatabaseNode(int tcpPort, Map<Integer, Integer> keyValuePairs, List<String> connections) {
        this.tcpPort = tcpPort;
        this.keyValuePairs = keyValuePairs;
        this.connections = connections;
    }

    public static void main(String[] args) {
        int tcpPort = 0;
        Map<Integer, Integer> keyValuePairs = new HashMap<Integer, Integer>();
        List<String> connections = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-tcpport")) {
                tcpPort = Integer.parseInt(args[i + 1]);
            } else if (args[i].equals("-record")) {
                String[] record = args[i + 1].split(":");
                int key = Integer.parseInt(record[0]);
                int value = Integer.parseInt(record[1]);
                keyValuePairs.put(key, value);
            } else if (args[i].equals("-connect")) {
                connections.add(args[i + 1]);
            }
        }

        DatabaseNode node = new DatabaseNode(tcpPort, keyValuePairs, connections);
        node.start();
    }

    public void start() {
        ServerSocket serverSocket = null;
        List<Socket> connectionSockets = new ArrayList<>();
        try {
            serverSocket = new ServerSocket(tcpPort);
            for (String connection : connections) {
                String[] parts = connection.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                Socket socket = new Socket(host, port);
                connectionSockets.add(socket);
            }
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket, connectionSockets);
            }
        } catch (IOException e) {
            System.err.println("Error starting server on port " + tcpPort);
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                for (Socket socket : connectionSockets) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket");
                e.printStackTrace();
            }
        }
    }

    private void handleRequest(Socket clientSocket, List<Socket> connectionSockets) {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String request = in.readLine();
            String[] requestParts = request.split(" ");
            String operation = requestParts[0];
            if (operation.equals("set-value")) {
                int key = Integer.parseInt(requestParts[1]);
                int value = Integer.parseInt(requestParts[2]);
                keyValuePairs.put(key, value);
                out.println("OK");
            } else if (operation.equals("get-value")) {
                int key = Integer.parseInt(requestParts[1]);
                Integer value = keyValuePairs.get(key);
                if (value != null) {
                    out.println(key + ":" + value);
                } else {
                    out.println("ERROR");
                }
            } else if (operation.equals("connect")) {
                String[] connection = requestParts[1].split(":");
                String host = connection[0];
                int port = Integer.parseInt(connection[1]);
                Socket socket = new Socket(host, port);
                connectionSockets.add(socket);
            }
        } catch (IOException e) {
            System.err.println("Error handling request");
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket");
                e.printStackTrace();
            }
        }
    }

}
