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
    private final Map<Integer, Integer> keyValuePairs;
    private final List<NodeConnectionHandler> connectionHandlers;
    private final int tcpPort;

    public DatabaseNode(int tcpPort, Map<Integer, Integer> keyValuePairs, List<String> connections) {
        this.tcpPort = tcpPort;
        this.keyValuePairs = keyValuePairs;
        this.connectionHandlers = new ArrayList<>();
        for (String connection : connections) {
            String[] parts = connection.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            NodeConnectionHandler handler = new NodeConnectionHandler(host, port, this);
            connectionHandlers.add(handler);
            handler.start();
        }
    }

    public static void main(String[] args) {
        Map<Integer, Integer> keyValuePairs = new HashMap<>();
        List<String> connections = new ArrayList<>();

        int i = 0;
        int tcpPort = -1;
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-tcpport")) {
                tcpPort = Integer.parseInt(args[i + 1]);
                i += 2;
            } else if (arg.equals("-record")) {
                String[] parts = args[i + 1].split(":");
                int key = Integer.parseInt(parts[0]);
                int value = Integer.parseInt(parts[1]);
                keyValuePairs.put(key, value);
                i += 2;
            } else if (arg.equals("-connect")) {
                connections.add(args[i + 1]);
                i += 2;
            } else {
                System.err.println("Invalid argument: " + arg);
                i++;
            }
        }

        DatabaseNode node = new DatabaseNode(tcpPort, keyValuePairs, connections);
        node.start();
    }

    public void start() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(tcpPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error starting server on port " + tcpPort);
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
                for (NodeConnectionHandler handler : connectionHandlers) {
                    handler.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket");
                e.printStackTrace();
            }
        }
    }

    private void handleRequest(Socket clientSocket) {
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
                NodeConnectionHandler handler = new NodeConnectionHandler(host, port, this);
                connectionHandlers.add(handler);
                handler.start();
                out.println("OK");
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
                System.err.println("Error closing socket");
                e.printStackTrace();
            }
        }
    }

    public int getTcpPort() {
        return tcpPort;
    }
}
