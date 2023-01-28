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

    public DatabaseNode(int tcpPort, Map<Integer, Integer> keyValuePairs, List<String> connections) throws IOException {
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

    public static void main(String[] args) throws IOException {
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
        // Start listening for requests from other nodes
        for (NodeConnectionHandler handler : connectionHandlers) {
            new Thread(handler).start();
        }

        // Start listening for client requests
        try {
            ServerSocket serverSocket = new ServerSocket(tcpPort);
            while (true) {
                Socket newSocket = serverSocket.accept();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(newSocket.getOutputStream(), true);
                    String request = in.readLine();
                    String response = handleRequest(request);
                    if (response == "TERMINATED") {
                        System.out.println("Terminating server on port : " + tcpPort);
                        break;
                    }
                    out.println(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String handleRequest(String request) {
        String[] parts = request.split(" ");
        String operation = parts[0];
        if (operation.equals("connect")) {
            // TODO
            return "TODO";
        } else if (operation.equals("set-value")) {
            return handleSetValue(parts[1]);
        } else if (operation.equals("get-value")) {
            return handleGetValue(parts[1]);
        } else if (operation.equals("terminate")) {
            for (NodeConnectionHandler handler : connectionHandlers) {
                handler.terminate();
            }
            return "TERMINATED";
        } else {
            return "ERROR: " + operation;
        }
        // TODO: implement other operations
    }

    private String handleSetValue(String keyValueString) {
        String[] keyValue = keyValueString.split(":");
        int key = Integer.parseInt(keyValue[0]);
        int value = Integer.parseInt(keyValue[1]);
        keyValuePairs.put(key, value);
        return "OK";
    }

    private String handleGetValue(String keyString) {
        int key = Integer.parseInt(keyString);
        if (keyValuePairs.containsKey(key)) {
            return key + ":" + keyValuePairs.get(key);
        } else {
            // Query connected nodes
            for (NodeConnectionHandler handler : connectionHandlers) {
                try {
                    // TODO: make a string read/write wrapper
                    PrintWriter out = new PrintWriter(handler.getSocket().getOutputStream(), true);

                    // TODO: fix -- this will cause recursive requests
                    // create a handler method getValue, and pass the node originally making query
                    out.println("get-value " + key);
                    BufferedReader in = new BufferedReader(new InputStreamReader(handler.getSocket().getInputStream()));
                    String response = in.readLine();
                    if (!response.equals("ERROR")) {
                        return response;
                    }
                } catch (IOException e) {
                    System.err.println("Error sending message to node at " + handler.host + ":" + handler.port);
                    e.printStackTrace();
                }
            }
            return "ERROR";
        }
    }
}
