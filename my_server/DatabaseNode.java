package my_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DatabaseNode {
    private final List<NodeConnectionHandler> connectionHandlers;
    private final int tcpPort;
    private int key;
    private int value;

    public DatabaseNode(int tcpPort, int key, int value, List<String> connections) throws IOException {
        this.tcpPort = tcpPort;
        this.key = key;
        this.value = value;
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
        int key = -1;
        int value = -1;
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
                key = Integer.parseInt(parts[0]);
                value = Integer.parseInt(parts[1]);
                i += 2;
            } else if (arg.equals("-connect")) {
                connections.add(args[i + 1]);
                i += 2;
            } else {
                System.err.println("Invalid argument: " + arg);
                i++;
            }
        }

        DatabaseNode node = new DatabaseNode(tcpPort, key, value, connections);
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
                    System.out.println("Sending response to client: " + response);
                    out.println(response);
                    out.close();
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
        System.out.println("Handling request: " + request);
        if (operation.equals("connect")) {
            // TODO
            return "TODO";
        } else if (operation.equals("set-value")) {
            return handleSetValue(parts[1]);
        } else if (operation.equals("get-value")) {
            return handleGetValue(parts[1]);
        } else if (operation.equals("find-key")) {
            return handleFindKey(parts[1]);
        } else if (operation.equals("new-record")) {
            return handleNewRecord(parts[1]);
        } else if (operation.equals("terminate")) {
            for (NodeConnectionHandler handler : connectionHandlers) {
                handler.terminate();
            }
            return "TERMINATED";
        } else {
            return "ERROR -- Unknown operation: " + operation;
        }
        // TODO: implement other operations
    }

    private String handleSetValue(String keyValueString) {
        String[] keyValue = keyValueString.split(":");
        int _key = Integer.parseInt(keyValue[0]);
        int _value = Integer.parseInt(keyValue[1]);
        if (key != _key) {
            // TODO: query other nodes
            return "ERROR";
        }
        this.value = _value;
        return "OK";
    }

    private String handleGetValue(String keyString) {
        int _key = Integer.parseInt(keyString);
        if (_key == key) {
            return key + ":" + value;
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

    private String handleFindKey(String keyString) {
        int _key = Integer.parseInt(keyString);
        if (_key == key) {
            // TODO: get my ip
            return "localhost:" + tcpPort;
        } else {
            // TODO: query other noedes
        }
        return "ERROR";
    }

    private String handleNewRecord(String keyValueString) {
        String[] keyValue = keyValueString.split(":");
        key = Integer.parseInt(keyValue[0]);
        value = Integer.parseInt(keyValue[1]);
        return "OK";
    }

}
