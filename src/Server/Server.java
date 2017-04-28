package Server;

import com.mysql.fabric.jdbc.FabricMySQLDriver;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.util.*;

public class Server {
    public class InnerClient{
        ObjectOutputStream out;
        String name;
        boolean isInMainChat;
        InnerClient(String name, ObjectOutputStream out){
            this.name = name;
            this.out = out;
            this.isInMainChat = true;
        }
    }
    private boolean isOn = true;
    private ArrayList<ObjectOutputStream> clientOutputStreams;
    private Connection dataBaseConnection;
    private Map<String,InnerClient> outputStreamMap;
    private Map<String,ObjectOutputStream> adminOutputMap;
    private String split = "_split_";


    public static void main(String[] args) {
        new Server().go();
    }

    public class ClientHandler implements Runnable{
        ObjectInputStream in;
        Socket clientSocket;
        ObjectOutputStream out;

         ClientHandler(Socket socket,ObjectOutputStream out){
            try{
                this.out = out;
                clientSocket = socket;
                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        public void run(){
            Object o2;
            Object o1;
            try{
                while((o1 = in.readObject()) != null){
                    o2 = in.readObject();
                    System.out.println("read two objects");
                    handleInput(o1,o2,this.out);
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private void handleInput(Object command, Object info, ObjectOutputStream out){
        String[] strings = ((String)command).split("_split_");
        System.out.println("command " + strings[0]);
        switch (strings[0].toLowerCase()){
            case "reguser":
                regUser(strings[1],info,out);
                break;
            case "login":
                logIn(strings[1],info,out);
                break;
            case "message":
                sendMessage(strings[1],info);
                break;
            case "addfriend":
                addFriendRequest(strings[1],info,out);
                break;
            case "friendrequestanswer":
                friendRequestAnswer(strings[1],info);
                break;
            case "removefriend":
                removeFriend(strings[1],info);
                break;
            case "logout":
                logOut(strings[1]);
                break;
            case "adminuserlistrequest":
                adminUserListRequest(strings[1]);
                break;
            case "setban":
                //adminSetBan(strings[1],(String)info);
                break;
            case "setmute":
                //adminSetMute(strings[1],(String)info);
                break;
            default:
                break;
        }
    }

    private void adminUserListRequest(String userLogin){
        try {
            adminOutputMap.put(userLogin,outputStreamMap.get(userLogin).out);
            PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT * FROM chatclientbase.suspended");
            ResultSet banSet = statement.executeQuery();
            HashMap<String,String> userMap = new HashMap<>();
            while(banSet.next()){
                String ban = "null", mute = "null";
                if(banSet.getDate(2) != null){
                    ban = banSet.getTimestamp(2).toString();
                }
                if(banSet.getDate(3) != null){
                    mute = banSet.getTimestamp(3).toString();
                }
                userMap.put(banSet.getString(1),ban + split + mute);
            }
            statement = dataBaseConnection.prepareStatement("SELECT userlogin, username FROM chatclientbase.userbase");
            ResultSet userBaseSet = statement.executeQuery();
            while(userBaseSet.next()){
                StringBuilder builder = new StringBuilder();
                builder.append("adminUserBaseAnswer");
                builder.append(split);
                builder.append(userBaseSet.getString(1));
                builder.append(split);
                builder.append(userBaseSet.getString(2));
                builder.append(split);
                String temp;
                if ((temp = userMap.get(userBaseSet.getString(1))) != null){
                    builder.append(temp);
                } else {
                    builder.append("null").append(split).append("null");
                }
                outputStreamMap.get(userLogin).out.writeObject(builder.toString());
                outputStreamMap.get(userLogin).out.flush();
            }
            userMap.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logOut(String userLogin){
        try {
            PreparedStatement statement =
                    dataBaseConnection.prepareStatement("SELECT userfriendlist FROM chatclientbase.userbase WHERE userlogin = '"
                            + userLogin + "';");
            ResultSet resultSet = statement.executeQuery();
            ArrayList<String> friends;
            if(resultSet.next()){
                friends = new ArrayList<>(Arrays.asList(resultSet.getString(1).split("_FL_")));
                for(String s : friends){
                    try{
                        outputStreamMap.get(s).out.writeObject("useroffline" + split + userLogin);
                        outputStreamMap.get(s).out.flush();
                    }catch (Exception ex){
                        System.err.println("failed to send useroffline");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        outputStreamMap.remove(userLogin);
        try {
            outputStreamMap.get(userLogin).out.close();
        } catch (IOException e) {}
    }
    private void delayedRequest(String userLogin,String sender, String requestType,String message) throws Exception{
        PreparedStatement statement =
                dataBaseConnection.prepareStatement("INSERT INTO chatclientbase.delayedrequest (reciever,sender,requesttype,message) " +
                        "VALUES ('" + userLogin + "', '" + sender + "', '" + requestType + "', '" + message + "')");
        statement.execute();
        statement.close();
    }
    private void removeFriend(String sender, Object receiver){
        String[] temp = ((String)receiver).split("_split_");
        try {
            ObjectOutputStream outputStream;
            String message = "removerequest_split_" + sender + "_split_" + temp[1];
            try {
                if ((outputStream = outputStreamMap.get(temp[0]).out) != null) {
                    outputStream.writeObject(message);
                    outputStream.flush();
                }
            }catch (NullPointerException ex){
                System.err.println("Failed to send removeFriendMessage");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            handleDeleteStatement(temp[0],sender);
            handleDeleteStatement(sender,temp[0]);
        }
    }
    private void handleDeleteStatement(String firstUser, String secondUser){
        try {
            PreparedStatement statement;
            ResultSet resultSet;
            String newFriends;
            statement = dataBaseConnection.prepareStatement("SELECT userfriendlist FROM chatclientbase.userbase WHERE userlogin = '" + firstUser + "'");
            resultSet = statement.executeQuery();
            if(resultSet.next()){
                ArrayList<String> friends  = new ArrayList<>(Arrays.asList(resultSet.getString(1).split("_FL_")));
                if(friends.contains(secondUser)){
                    friends.remove(secondUser);
                }
                if(friends.isEmpty()){
                    newFriends = null;
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder.append(friends.get(0));
                    for(int i = 1; i < friends.size(); i++){
                        builder.append("_FL_").append(friends.get(i));
                    }
                    newFriends = builder.toString();
                }
                statement = dataBaseConnection.prepareStatement("UPDATE chatclientbase.userbase SET userfriendlist = '" + newFriends + "' WHERE userlogin = '" + firstUser + "'");
                int q;
                q = statement.executeUpdate();
                System.out.println("Update " + firstUser + " " + q);
            }
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void friendRequestAnswer(String sender,Object result){
        String[] results = ((String)result).split("_split_");
        System.err.println("REQUEST ANSWER");
        try {
            try{
                outputStreamMap.get(results[1]).out.writeObject("friendrequestanswer_split_" + results[0] + "_split_" + sender + "_split_" +outputStreamMap.get(sender).name);
                outputStreamMap.get(results[1]).out.flush();
            }catch (Exception ex){
                System.err.println("Failed to send friendRequestAnswer");
            }
            if(results[0].equals("requestAccepted")){
                PreparedStatement statement = dataBaseConnection.prepareStatement("DELETE FROM chatclientbase.delayedrequest WHERE reciever = '" + sender + "' AND requesttype = 'addfriend' AND sender ='" + results[1] + "';");
                int i = statement.executeUpdate();
                addToFriendList(results[1],sender);
                statement.close();
            } else if(results[0].equals("requestDeclined")) {
                PreparedStatement statement = dataBaseConnection.prepareStatement("DELETE FROM chatclientbase.delayedrequest WHERE reciever = '" + sender + "' AND requesttype = 'addfriend' AND sender ='" + results[1] + "';");
                int i = statement.executeUpdate();
                statement.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFriendRequest(String sender, Object info, ObjectOutputStream answer){
        String senderName = outputStreamMap.get(sender).name;
        ObjectOutputStream out = null;
        String message = "friendRequest" + "_split_" + sender + "_split_" + senderName;
        try {
            try{
                out = outputStreamMap.get((String)info).out;
            } catch (Exception ex){
                System.err.println("Can't find outStream for " + info);
            }
            if(out != null) {
                out.writeObject(message);
                out.flush();
            } else {
                PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT 1 FROM chatclientbase.userbase WHERE userlogin = '" + sender + "'");
                ResultSet set = statement.executeQuery();
                if(set.next()){
                    delayedRequest((String)info,sender,"addfriend",message);
                } else {
                    answer.writeObject("requestFailed_split_" + info);
                    answer.flush();
                }
                statement.close();
                set.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addToFriendList(String firstUser, String secondUser){
        try{
            addToFrienListStatement(firstUser,secondUser);
            addToFrienListStatement(secondUser,firstUser);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void addToFrienListStatement(String firstUser, String secondUser) throws Exception{
        ArrayList<String> tempSplit;
        PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT userfriendlist FROM chatclientbase.userbase WHERE userlogin = '" + firstUser +"'");
        ResultSet resultSet = statement.executeQuery();
        if(resultSet.next()){
            String friendList = resultSet.getString(1);
            if(friendList != null){
                tempSplit = new ArrayList<>(Arrays.asList(friendList.split("_FL_")));
                if(!tempSplit.contains(secondUser)){
                    friendList += "_FL_" + secondUser;
                    statement = dataBaseConnection.prepareStatement("UPDATE chatclientbase.userbase SET userfriendlist = '" + friendList + "' WHERE userlogin = '" + firstUser + "'");
                    statement.executeUpdate();
                }
            } else {
                friendList = secondUser;
                statement = dataBaseConnection.prepareStatement("UPDATE chatclientbase.userbase SET userfriendlist = '" + friendList + "' WHERE userlogin = '" + firstUser + "'");
                statement.executeUpdate();
            }
        }
        statement.close();
    }
    private void regUser(String login,Object info,ObjectOutputStream out){
        /*
        Registration data string format:
        info.split strings [0] - username, [1] - password [2] - age[3] - gender
         */
        System.out.println(login);
        try {
            PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT true FROM chatclientbase.userbase WHERE userlogin = '" + login +"'");
            ResultSet resultSet = statement.executeQuery();

            if(resultSet.next()){
                try {
                    out.writeObject("reguser_split_failed_split_user already exists");
                    out.flush();
                    System.out.println("go away");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String[] strings = ((String)info).split("_split_");
                statement = dataBaseConnection.prepareStatement("INSERT INTO chatclientbase.userbase (userlogin,userpass,username,useraccesslevel,userage,usergender) values ('" + login + "','" + strings[1] + "','" + strings[0] + "',0," + Integer.parseInt(strings[2]) + "," + Integer.parseInt(strings[3]) + ")");
                statement.execute();
                statement = dataBaseConnection.prepareStatement("SELECT true FROM chatclientbase.userbase WHERE userlogin = '" + login +"' AND userpass = '" + strings[1] + "' AND username = '" + strings[0] + "'");
                resultSet = statement.executeQuery();
                if(resultSet.next()){
                    try{
                        out.writeObject("reguser_split_succeed");
                        out.flush();
                        System.out.println("welcome, bitch");
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
                outputStreamMap.put(login,new InnerClient(strings[0],out));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    private void logIn(String login,Object info,ObjectOutputStream out){
        // INFO - password
        try{
            PreparedStatement statement = dataBaseConnection.prepareStatement("SELECT useraccesslevel,username,userage,usergender,userstatus,userfriendlist FROM chatclientbase.userbase WHERE userlogin = '" + login + "' AND  userpass = '" + info + "'");
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()){
                System.out.println("LikeABawz");
                out.writeObject("login_split_succeed_split_" + resultSet.getInt(1) +
                "_split_" + resultSet.getString(2) + "_split_" + resultSet.getInt(3) + "_split_" +
                resultSet.getInt(4) + "_split_" + resultSet.getString(5) + "_split_" + resultSet.getString(6));
                out.flush();
                outputStreamMap.put(login,new InnerClient(resultSet.getString(2),out));
                String[] friends = resultSet.getString(6).split("_FL_");

                //RETURN FRIENDLIST: flist, flogin, fname, fage, fgender, fstatus, fonline

                for(String s : friends){
                    statement = dataBaseConnection.prepareStatement("SELECT username,userage,usergender,userstatus FROM chatclientbase.userbase WHERE userlogin = '" + s + "'");
                    ResultSet friendResult = statement.executeQuery();
                    String isOnline;
                    if(friendResult.next()) {

                        if (outputStreamMap.get(s) != null) {
                            isOnline = "online";
                            try{
                                outputStreamMap.get(s).out.writeObject("useronline" + split + login);
                                outputStreamMap.get(s).out.flush();
                            } catch (Exception ex){
                                System.out.println("Problem with useronline");
                            }
                        } else {
                            isOnline = "offline";
                        }
                        String builder = "flist" + split + s + split +
                                friendResult.getString(1) + split +
                                friendResult.getInt(2) + split + friendResult.getInt(3) +
                                split + friendResult.getString(4) + split + isOnline;
                        out.writeObject(builder);
                        out.flush();
                    }
                }
                statement = dataBaseConnection.prepareStatement("SELECT message FROM chatclientbase.delayedrequest WHERE reciever = '" + login + "';");
                ResultSet resultSet1 = statement.executeQuery();
                while(resultSet1.next()){
                    out.writeObject(resultSet1.getString(1));
                    out.flush();
                }
                statement = dataBaseConnection.prepareStatement("DELETE FROM chatclientbase.delayedrequest WHERE reciever = '" + login + "' AND requesttype = 'textmessage';");
                statement.executeUpdate();
            } else {
                out.writeObject("login_split_failed");
                out.flush();
                System.out.println("TryAgain");
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void sendMessage(String login,Object info){
        /*
        info: [0] - receivers list, [1] - chatName, [2] - messageType, [3] - message
        */
        System.out.println("arrived at send");
        String senderName = outputStreamMap.get(login).name;
        Object[] objects = ((String)info).split("_split_",4);
        String[] receivers = ((String)objects[0]).split("_rec_");

        System.out.println((String)objects[2]);
        switch((String)objects[2]){
            case "textmessage":
                if(((String)objects[1]).equals("mainChat")){
                    sendToMainChat(senderName,login,(String)objects[3]);
                } else {
                    sendTextMessage(senderName,login,(String)objects[1],receivers,(String)objects[3]);
                }
                System.out.println("arrived at textmsg");
                break;
        }
    }

    private void sendToMainChat(String sender, String login, String message){
        StringBuilder builder = new StringBuilder("textMessage").append(split).append("mainChat");
        builder.append(split).append(login).append(split).append(sender).append(message);
        for(String name : outputStreamMap.keySet()){
            try {
                outputStreamMap.get(name).out.writeObject(builder.toString());
                outputStreamMap.get(name).out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendTextMessage(String sender, String login, String chatName, String[] receivers,String message){
        System.out.println("arrived at sendText");
        for(String temp : receivers){
            System.out.println(temp);
            ObjectOutputStream out = null;
            try{
                out = (outputStreamMap.get(temp)).out;
            } catch (Exception ex){
                System.err.println("Can't find output stream for " + temp);
            }
            StringBuilder builder = new StringBuilder();
            builder.append("textMessage").append(split).append(chatName).append(split).append(login);
            builder.append(split).append(sender).append(split).append(message);
            if(out != null){
                try {
                    out.writeObject(builder.toString());
                    out.flush();
                    System.out.println("sended message");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    delayedRequest(temp,sender,"textmessage",builder.toString());
                    System.out.println("delayed request");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Connection getConnection() throws Exception{
        try{
            Driver driver = new FabricMySQLDriver();
            DriverManager.registerDriver(driver);
            String url = "jdbc:mysql://URL";
            String username = "NAME";
            String password = "PASS";
            Connection conn = DriverManager.getConnection(url,username,password);
            return conn;
        }catch (Exception ex){
            ex.printStackTrace();
            System.err.println("Failed to connect database");
        }
        return null;
    }

    private void createTable(){
        try {
            PreparedStatement statement = dataBaseConnection.prepareStatement("CREATE TABLE IF NOT EXISTS delayedrequest(reciever varchar(52) NOT NULL,sender varchar(52),requesttype varchar(25) ,message text NOT NULL)");
            statement.execute();
            statement = dataBaseConnection.prepareStatement("CREATE TABLE IF NOT EXISTS userbase(id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, useraccesslevel TINYINT, username varchar(52) NOT NULL, userlogin varchar(52) NOT NULL, userpass varchar(52) NOT NULL, userage TINYINT, usergender TINYINT, userstatus TINYTEXT, userfriendlist TEXT)");
            statement.execute();
            statement = dataBaseConnection.prepareStatement("CREATE TABLE IF NOT EXISTS suspended(userlogin varchar(52) NOT NULL, ban TIMESTAMP, mute TIMESTAMP)");
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void go(){
        clientOutputStreams = new ArrayList<>();
        outputStreamMap = new HashMap<>();
        adminOutputMap = new HashMap<>();
        try {
            if((dataBaseConnection = getConnection()) != null){
                System.out.println("Connection succeed");
                createTable();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try{
            ServerSocket serverSock = new ServerSocket(4242);
            while(isOn){
                Socket clientSocket = serverSock.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);

                Thread t = new Thread(new ClientHandler(clientSocket,out));
                t.start();
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
}