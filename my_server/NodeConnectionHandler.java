package my_server;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.lang.Thread.sleep;

class NodeConnectionHandler {

    public final String myHost;
    public final String host;
    public final int port;
    public final int myPort;

    public NodeConnectionHandler(String host, int port, String myHost, int myPort, boolean notify) {
        this.host = host;
        this.port = port;
        this.myHost = myHost;
        this.myPort = myPort;
        System.out.println("Connecting to " + host + ":" + port);
        System.out.println("Connected");

        if (notify) {
            String response = sendMessage("srv__connect " + myHost + ":" + myPort);
            if (!response.equals("OK")) {
                System.out.println("ERROR connecting: " + host + ":" + port + " -- got response: |" + response + "|");
            }
        }
    }

    private String sendMessage(String message) {
        Socket socket;
        System.out.println("Sending message: \"" + message + "\" to " + host + ":" + port);
        while (true) {
            try {
                socket = new Socket(host, port);
                break;
            } catch (ConnectException e) {
                throw new RuntimeException(e);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String response = "ERROR";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            response = in.readLine();
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Got response: \"" + response + "\" from " + host + ":" + port);
        return response;
    }

    public void disconnect() {
        System.out.println("Sending disconnect message to: " + host + ":" + port);
        String resp = sendMessage("srv__disconnect " + myHost + ":" + myPort);
        if (!resp.equals("OK")) {
            throw new RuntimeException();
        }
    }

    public String getOperation(String operation) {
        return sendMessage("srv__get-" + operation + " " + myHost + ":" + myPort);
    }

    public String findKey(int key) {
        return sendMessage("srv__find-key " + key + " " + myHost + ":" + myPort);
    }
}