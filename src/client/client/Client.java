package client.client;

import javafx.scene.control.TextArea;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    private static Client client;
    public ObjectInputStream inputStream;
    public ObjectOutputStream outputStream;
    public TextArea chatWindow;
    public String login;
    private String ip;
    public int accessLevel;
    public String username;
    public int age;
    public int gender;
    public String status;
    public String split = "_split_";
    public Socket socket;

    private Client() {
    }

    public static Client getClient() {
        if (Client.client == null) {
            Client.client = new Client();
        }
        return Client.client;
    }

    public boolean isLogged;

    public void startUp(String ip) throws Exception {
        String[] splitIp = ip.split(":");
        socket = new Socket(InetAddress.getByName(splitIp[0]), Integer.parseInt(splitIp[1]));
        inputStream = new ObjectInputStream(socket.getInputStream());
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("started");
    }

    public String getLogin() {
        return login;
    }

    public void sendMessage(Object firstObject, Object secondObject) {
        try {
            outputStream.writeObject(firstObject);
            outputStream.writeObject(secondObject);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}