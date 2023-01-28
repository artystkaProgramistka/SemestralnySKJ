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

public class DatabaseNode {
    private final HashMap<String, NodeConnectionHandler> connectionHandlers;
    private final int tcpPort;
    private int key;
    private int value;

    public DatabaseNode(int tcpPort, int key, int value, List<String> connections) throws IOException {
        this.tcpPort = tcpPort;
        this.key = key;
        this.value = value;
        this.connectionHandlers = new HashMap<>();
        for (String connection : connections) {
            String[] parts = connection.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            NodeConnectionHandler handler = new NodeConnectionHandler(host, port, getMyHost(), tcpPort, true);
            connectionHandlers.put(connection, handler);
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
                        System.out.println("Terminating server");
                        out.println("OK");
                        break;
                    } else {
                        System.out.println("Sending response to client: " + response);
                        out.println(response);
                    }
                    out.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMyHost() {
        return "localhost";
    }

    public String getMyHostId() {
        return "HOST_ID:" + getMyHost() + ":" + tcpPort;
    }

    public String handleRequest(String request) {
        String[] parts = request.split(" ");
        String operation = parts[0];
        System.out.println("Handling request: " + request);
        if (operation.equals("srv__connect")) {
            String[] addr = parts[1].split(":");
            String host = addr[0];
            int port = Integer.parseInt(addr[1]);
            System.out.println("Adding new server connection: " + host + ":" + port);
            NodeConnectionHandler handler = new NodeConnectionHandler(host, port, getMyHost(), tcpPort, false);
            connectionHandlers.put(parts[1], handler);
            return "OK";
        } else if (operation.equals("srv__disconnect")) {
            connectionHandlers.remove(parts[1]);
            System.out.println("Server " + parts[1] + " has disconnected.");
            return "OK";
        } else if (operation.equals("srv__get-min")) {
            return handleGetMinOrMax("min", parts[1]);
        } else if (operation.equals("srv__get-max")) {
            return handleGetMinOrMax("max", parts[1]);
        } else if (operation.equals("srv__find-key")) {
            return handleFindKey(parts[1], parts[2]);
        } else if (operation.equals("set-value")) {
            return handleSetValue(parts[1]);
        } else if (operation.equals("get-value")) {
            return handleGetValue(parts[1]);
        } else if (operation.equals("find-key")) {
            return handleFindKey(parts[1], "");
        } else if (operation.equals("new-record")) {
            return handleNewRecord(parts[1]);
        } else if (operation.equals("get-min")) {
            return handleGetMinOrMax("min", "");
        } else if (operation.equals("get-max")) {
            return handleGetMinOrMax("max", "");
        } else if (operation.equals("terminate")) {
            for (NodeConnectionHandler handler : connectionHandlers.values()) {
                handler.disconnect();
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
            System.out.println("TODO");
            for (NodeConnectionHandler handler : connectionHandlers.values()) {
            }
            return "ERROR";
        }
    }

    private String handleFindKey(String keyString, String hostToSkip) {
        int _key = Integer.parseInt(keyString);
        if (_key == key) {
            return getMyHost() + ":" + tcpPort;
        } else {
            for (HashMap.Entry<String, NodeConnectionHandler> entry : connectionHandlers.entrySet()) {
                if (entry.getKey().equals(hostToSkip)) continue;
                String searchResult = entry.getValue().findKey(_key);
                if (!searchResult.equals("ERROR")) {
                    return searchResult;
                }
            }
        }
        return "ERROR";
    }

    private String handleNewRecord(String keyValueString) {
        String[] keyValue = keyValueString.split(":");
        key = Integer.parseInt(keyValue[0]);
        value = Integer.parseInt(keyValue[1]);
        return "OK";
    }

    private String handleGetMinOrMax(String operation, String hostToSkip) {
        int returnKey = key;
        int returnValue = value;
        for (HashMap.Entry<String, NodeConnectionHandler> entry : connectionHandlers.entrySet()) {
            if (entry.getKey().equals(hostToSkip)) continue;
            String[] keyValue = entry.getValue().getOperation(operation).split(":");
            int k = Integer.parseInt(keyValue[0]);
            int v = Integer.parseInt(keyValue[1]);
            if (operation.equals("min")) {
                if (v < returnValue) {
                    returnKey = k;
                    returnValue = k;
                }
            } else if (operation.equals("max")) {
                if (v > returnValue) {
                    returnKey = k;
                    returnValue = k;
                }
            } else {
                throw new RuntimeException();
            }
        }
        return "" + returnKey + ":" + returnValue;
    }
}
