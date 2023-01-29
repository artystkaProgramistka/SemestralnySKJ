package my_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DatabaseNode {
    private final HashMap<String, NodeConnectionHandler> connectionHandlers;
    private final int tcpPort;
    private int key;
    private int value;

    private ArrayList<Thread> threads;

    // needed to avoid cycles in the network requests
    private int nodeTaskIdCounter = 0;
    private HashSet<String> doneTaskIds;

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
        doneTaskIds = new HashSet<>();
        threads = new ArrayList<>();
    }

    public String getNewTaskId() {
        return "TASK-" + nodeTaskIdCounter++ + ":" + getMyHost() + ":" + tcpPort;
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
                System.out.println(">>>>>>> new socket accepted");
                if (handleNewSocket(newSocket)) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("END");
    }

    private boolean handleNewSocket(Socket newSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
            PrintWriter out = new PrintWriter(newSocket.getOutputStream(), true);
            String request = in.readLine();
            String response = handleRequest(request, newSocket, in, out);
            if (response == "TERMINATED") {
                System.out.println("Terminating server");
                out.println("OK");
                out.close();
                in.close();
                return true;
            } else if (response == "ASYNC") {
                return false;
            } else {
                System.out.println("Sending response to client: " + response);
                out.println(response);
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getMyHost() {
        return "localhost";
    }

    public String handleRequest(String request, Socket newSocket, BufferedReader in, PrintWriter out) {
        String[] parts = request.split(" ");
        String operation = parts[0];
        System.out.println("Handling request: " + request);
        if (operation.equals("terminate")) {
            for (NodeConnectionHandler handler : connectionHandlers.values()) {
                handler.disconnect();
            }
            return "TERMINATED";
        } else if (operation.equals("srv__connect")) {
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
        }
        Thread t = new Thread(() -> {
            String response = handleAsyncRequest(parts, operation, newSocket);
            System.out.println("Sending async response: " + response);
            out.println(response);
            out.close();
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        t.start();
        threads.add(t);
        // try {
        //     t.wait();
        // } catch (InterruptedException e) {
        //     throw new RuntimeException(e);
        // }
        return "ASYNC";
    }

    private String handleAsyncRequest(String[] parts, String operation, Socket newSocket) {
        if (operation.equals("srv__get-min")) {
            return handleGetMinOrMax(parts[1], "min", parts[2]);
        } else if (operation.equals("srv__get-max")) {
            return handleGetMinOrMax(parts[1], "max", parts[2]);
        } else if (operation.equals("srv__find-key")) {
            return handleFindKey(parts[1], parts[2], parts[3]);
        } else if (operation.equals("srv__set-value")) {
            return handleSetValue(parts[1], parts[2], parts[3]);
        } else if (operation.equals("set-value")) {
            return handleSetValue(getNewTaskId(), parts[1], "");
        } else if (operation.equals("get-value")) {
            return handleGetValue(getNewTaskId(), parts[1], "");
        } else if (operation.equals("find-key")) {
            return handleFindKey(getNewTaskId(), parts[1], "");
        } else if (operation.equals("new-record")) {
            return handleNewRecord(parts[1]);
        } else if (operation.equals("get-min")) {
            return handleGetMinOrMax(getNewTaskId(), "min", "");
        } else if (operation.equals("get-max")) {
            return handleGetMinOrMax(getNewTaskId(), "max", "");
        } else {
            return "ERROR -- Unknown operation: " + operation;
        }
    }

    private String handleSetValue(String taskId, String keyValueString, String parentNode) {
        if (doneTaskIds.contains(taskId)) return "ERROR";
        doneTaskIds.add(taskId);

        String[] keyValue = keyValueString.split(":");
        int _key = Integer.parseInt(keyValue[0]);
        int _value = Integer.parseInt(keyValue[1]);
        if (key == _key) {
            this.value = _value;
            return "OK";
        }

        for (HashMap.Entry<String, NodeConnectionHandler> entry : connectionHandlers.entrySet()) {
            if (entry.getKey().equals(parentNode)) continue;
            String result = entry.getValue().setValue(taskId, _key, _value);
            if (result.equals("ERROR")) continue;
            if (result.equals("OK")) {
                return "OK";
            }
        }
        return "ERROR";
    }

    private String handleGetValue(String taskId, String keyString, String parentNode) {
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

    private String handleFindKey(String taskId, String keyString, String parentNode) {
        if (doneTaskIds.contains(taskId)) return "ERROR";
        doneTaskIds.add(taskId);
        int _key = Integer.parseInt(keyString);
        if (_key == key) {
            return getMyHost() + ":" + tcpPort;
        } else {
            for (HashMap.Entry<String, NodeConnectionHandler> entry : connectionHandlers.entrySet()) {
                if (entry.getKey().equals(parentNode)) continue;
                String searchResult = entry.getValue().findKey(taskId, _key);
                if (searchResult.equals("ERROR")) continue;
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

    private String handleGetMinOrMax(String taskId, String operation, String parentNode) {
        if (doneTaskIds.contains(taskId)) return "ERROR";
        doneTaskIds.add(taskId);
        int returnKey = key;
        int returnValue = value;
        for (HashMap.Entry<String, NodeConnectionHandler> entry : connectionHandlers.entrySet()) {
            if (entry.getKey().equals(parentNode)) continue;
            String[] keyValue = entry.getValue().getOperation(taskId, operation).split(":");
            if (keyValue[0].equals("ERROR")) continue;
            int k = Integer.parseInt(keyValue[0]);
            int v = Integer.parseInt(keyValue[1]);
            if (operation.equals("min")) {
                if (v < returnValue) {
                    returnKey = k;
                    returnValue = v;
                }
            } else if (operation.equals("max")) {
                if (v > returnValue) {
                    returnKey = k;
                    returnValue = v;
                }
            } else {
                throw new RuntimeException();
            }
        }
        return "" + returnKey + ":" + returnValue;
    }
}
