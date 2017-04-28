package client.main;

import client.*;
import client.client.Client;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.net.URL;
import java.util.*;
import java.util.List;

public class MainController implements Initializable, ControlledScreen{
    private Controller myController;
    @FXML
    private Label userLabel;
    private Client client = Client.getClient();
    @FXML
    private AnchorPane chatWindow;
    @FXML
    public ListView<String> chatList;
    private HashMap<String,VBox> chatMap = new HashMap<>();
    private HashMap<String,Friend> friendMap = new HashMap<>();
    @FXML
    private Tab selectedChatTab;
    @FXML
    private TabPane tabPane;
    @FXML
    private ListView<Friend> friendList;
    private ObservableList<Friend> friendObservableList;
    private HashMap<String, String> friendRequestMap = new HashMap<>();
    @FXML
    private TextField textField;
    @FXML
    private Pane labelField;
    private List<String> friendRequestList = new ArrayList<>();
    @FXML
    private ListView<String> requestList;
    @FXML
    private MenuItem adminMenuButton;
    private HashMap<String,User> userMap = new HashMap<>();
    @FXML
    private TableColumn tableLogin;
    @FXML
    private TableColumn tableName;
    @FXML
    private TableColumn tableBan;
    @FXML
    private TableColumn tableMute;
    private ObservableList<User> usersData = FXCollections.observableArrayList();
    @FXML
    private TableView<User> adminTable;

    private void sendDeleteFriendRequest(){
        Friend removableFriend = friendList.getSelectionModel().getSelectedItem();
        Alert deleteConfirmation = new Alert(Alert.AlertType.CONFIRMATION);

        deleteConfirmation.setTitle("Удаление контакта");
        deleteConfirmation.setHeaderText("Вы уверены, что хотите удалить '" + removableFriend.name + "'(" + removableFriend.login + ")\nиз списка контактов?");
        deleteConfirmation.setContentText("Выберите действие:");

        ButtonType confirmDelete = new ButtonType("Удалить", ButtonBar.ButtonData.APPLY);
        ButtonType cancelDelete = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

        deleteConfirmation.getButtonTypes().setAll(confirmDelete,cancelDelete);
        Optional<ButtonType> result = deleteConfirmation.showAndWait();
        if(result.get() == confirmDelete){
            chatMap.remove(createChatName(client.login,removableFriend.login));
            System.out.println("sending remove message");
            client.sendMessage("removefriend" + client.split + client.login,removableFriend.login + client.split + client.username);
            System.out.println(friendList.getSelectionModel().getSelectedItem());
            friendList.getItems().remove(friendList.getSelectionModel().getSelectedItem());
            friendObservableList.remove(removableFriend);
            friendMap.remove(removableFriend.login);
            chatList.getItems().remove(createChatName(client.login,removableFriend.login));
        }
    }

    private String createChatName(String firstName, String secondName){
        if((firstName.compareTo(secondName) > 0)){
            return firstName + "_to_"  + secondName;
        } else {
            return secondName + "_to_"  + firstName;
        }
    }

    private String choose(String[] str){
        if(str[0].equals(client.login)) return str[1];
        else return str[0];
    }

    public void sendMessageButtonClick(){
        Friend chosenFriend;
        String[] chatdata;
        try {
                if ((chatdata = chatList.getSelectionModel().getSelectedItem().split("_to_")) != null) {
                    client.sendMessage("message" + client.split + client.login, client.login + "_rec_" + choose(chatdata) + client.split + chatList.getSelectionModel().getSelectedItem() + client.split + "textmessage" + client.split + " " + textField.getText());
                    textField.clear();
                    textField.requestFocus();
                } else if ((chosenFriend = friendList.getSelectionModel().getSelectedItem()) != null) {
                    client.sendMessage("message" + client.split + client.login, client.login + "_rec_" + chosenFriend.login + client.split + createChatName(client.login, chosenFriend.login) + client.split + "textmessage" + client.split + " " + textField.getText());
                    textField.clear();
                    textField.requestFocus();
                }
        } catch (Exception ex){
            System.out.println("Some null pointer");
        }
    }

    private void sendAddFriendRequest(){
        TextInputDialog addFriend = new TextInputDialog();
        addFriend.setTitle("Добавление контакта");
        addFriend.setHeaderText(null);
        addFriend.setContentText("Введите логин пользователя:");
        Optional<String> result = addFriend.showAndWait();
        result.ifPresent(login -> {
            client.sendMessage("addfriend" + client.split + client.login,login);
            friendRequestList.add(login);
        });
    }

    @Override
    public void setScreenParent(Controller screenParent) {
        myController = screenParent;
    }

    private void chatMapInit(){
        for (Friend friend : friendList.getItems()){
            VBox tempBox = createChatWindow();
            chatMap.put(createChatName(client.login,friend.login),tempBox);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatMapInit();
        friendListEvent();
        setFriendListContext();
        chatListEvent();
        requestListEvent();
        if(client.accessLevel != 3){
            adminMenuButton.setDisable(true);
        } else {
            adminMenuButton.setOnAction(e -> adminPanelInit());
        }
        friendObservableList = FXCollections.observableArrayList();
        friendList.setItems(friendObservableList);
        friendList.setCellFactory(newFriendList -> new StudentListViewCell());

        InputTask task = new InputTask(this);
        task.setOnSucceeded(e -> {
            handleInput(task.getValue());
            task.restart();
        });
        task.start();
        userLabel.setText(client.username + "\n(" + client.login + ")");
    }

    private void adminPanelInit(){
        try{
            ObservableList<Friend> friendObservableListOne = friendObservableList;
            client.sendMessage("adminUserListRequest" + client.split + client.login,client.accessLevel);
            Stage adminPanelStage = new Stage();
            adminPanelStage.setTitle("Панель администрирования");
            adminPanelStage.setResizable(false);
            FXMLLoader adminPanelLoader = new FXMLLoader(getClass().getResource("../res/adminPanel.fxml"));
            adminPanelLoader.setController(this);
            Scene adminPaneScene = new Scene(adminPanelLoader.load());

            tableLogin.setCellValueFactory(new PropertyValueFactory<User,String>("userLogin"));
            tableName.setCellValueFactory(new PropertyValueFactory<User,SimpleStringProperty>("userName"));
            tableBan.setCellValueFactory(new PropertyValueFactory<User,SimpleStringProperty>("banTime"));
            tableMute.setCellValueFactory(new PropertyValueFactory<User,SimpleStringProperty>("muteTime"));

            friendObservableList = friendObservableListOne;
            friendList.setItems(friendObservableList);
            System.out.println(friendObservableList.isEmpty());
            adminPanelStage.setScene(adminPaneScene);
            setAdminTableContextMenu();
            adminPanelStage.show();
            adminPanelStage.setOnCloseRequest(e -> adminMenuButton.setDisable(false));
            adminMenuButton.setDisable(true);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setAdminTableContextMenu(){
        ContextMenu adminTableContextMenu = new ContextMenu();
        adminTableContextMenu.setOnAutoHide(e -> adminTable.getSelectionModel().clearSelection());

        MenuItem banButton = new MenuItem("Заблокировать пользователя");
        MenuItem muteButton = new MenuItem("Заблокировать доступ в основной чат");
        MenuItem unBanButton = new MenuItem("Разблокировать пользователя");
        MenuItem unMuteButton = new MenuItem("Разблокировать доступ в основной чат");

        unMuteButton.setOnAction(e -> adminTableUnMute());
        unBanButton.setOnAction(e -> adminTableUnBan());
        muteButton.setOnAction(e -> adminTableMute());
        banButton.setOnAction(e -> adminTableBan());

        adminTableContextMenu.getItems().addAll(banButton,muteButton,unBanButton,unMuteButton);
        adminTable.setContextMenu(adminTableContextMenu);
        adminTableEvent();
    }

    private void adminTableBan(){}
    private void adminTableMute(){}

    private void setFriendListContext(){
        ContextMenu friendListContext = new ContextMenu();
        MenuItem deleteButton = new MenuItem("Удалить контакт");
        MenuItem addButton = new MenuItem("Добавить контакт");

        deleteButton.setOnAction(e -> sendDeleteFriendRequest());
        addButton.setOnAction(e -> sendAddFriendRequest());

        friendListContext.getItems().addAll(deleteButton,addButton);
        friendListContext.setOnAutoHide(e -> friendList.getSelectionModel().clearSelection());
        friendList.setContextMenu(friendListContext);
    }

    private void adminTableUnMute(){}
    private void adminTableUnBan(){}

    private void requestListEvent(){
        requestList.setDisable(true);
        requestList.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> friendRequestHandle(newValue,friendRequestMap.get(newValue))));
    }

    private void adminTableEvent(){
        adminTable.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
            if(adminTable.getSelectionModel().getSelectedItem() != null){
                if(t.getButton() == MouseButton.SECONDARY){
                    adminTable.getContextMenu().show(adminTable,t.getScreenX(),t.getScreenY());
                }
            }
        });
    }

    private void friendListExit(){
        friendList.getSelectionModel().clearSelection();
    }

    private void chatListEvent(){
        chatList.getSelectionModel().selectedItemProperty().addListener((v,oldValue,newValue) ->{
            chatWindow.getChildren().clear();
            System.out.println("VALUE " + newValue);
            chatWindow.getChildren().add(chatMap.get(newValue));
            tabPane.getSelectionModel().select(1);
        });
    }

    private void friendListEvent(){
        friendList.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
            if(friendList.getSelectionModel().getSelectedItem() == null){
                friendList.getContextMenu().getItems().get(0).setDisable(true);
                friendListExit();
            } else {
                friendList.getContextMenu().getItems().get(0).setDisable(false);
            }
            if (t.getButton() == MouseButton.SECONDARY) {
                friendList.getContextMenu().show(friendList,t.getScreenX(),t.getScreenY());
            } else {
                    if(friendList.getSelectionModel().getSelectedItem() != null){
                        Friend newValue = friendList.getSelectionModel().getSelectedItem();
                        VBox temp;
                        String chatName = createChatName(newValue.login,client.login);
                        if(chatList.getItems().contains(chatName)){
                            temp = chatMap.get(chatName);
                            chatList.getSelectionModel().select(chatName);
                            chatWindow.getChildren().clear();
                            chatWindow.getChildren().add(temp);
                            tabPane.getSelectionModel().select(1);
                            temp.requestFocus();
                            textField.requestFocus();
                        } else {
                            temp = chatMap.get(chatName);
                            chatList.getItems().add(chatName);
                            chatList.getSelectionModel().select(chatName);
                            chatWindow.getChildren().clear();
                            chatWindow.getChildren().add(temp);
                            tabPane.getSelectionModel().select(1);
                            temp.requestFocus();
                            textField.requestFocus();
                        }
                    }
            }
        });

    }

    private void handleInput(Object receivedObject){
        String[] receivedMessage = ((String)receivedObject).split(client.split);
        System.out.println((String)receivedObject);

        switch (receivedMessage[0]){
            case "textMessage":
                handleTextMessage(receivedMessage);
                break;
            case "requestFailed":
                requestFailed(receivedMessage[1]);
                break;
            case "friendRequest":
                requestHandle(receivedMessage[1],receivedMessage[2]);
                break;
            case "friendrequestanswer":
                friendRequestResult(receivedMessage);
                break;
            case "removerequest":
                System.out.println("removeFriend");
                System.out.println(receivedMessage[1]);
                removeFriend(receivedMessage[1]);
                break;
            case "flist":
                flistHandle(receivedMessage);
                break;
            case "useroffline":
                userStatus(receivedMessage[1],"offline");
                break;
            case "useronline":
                userStatus(receivedMessage[1],"online");
                break;
            case "adminUserBaseAnswer":
                adminUserBaseAnswer(receivedMessage);
                break;
            default: break;
        }
    }

    private void adminUserBaseAnswer(String[] answer){
        User tempUser = new User(answer[1],answer[2],answer[3],answer[4]);
        usersData.add(tempUser);
        adminTable.getItems().add(tempUser);
        userMap.put(answer[1],tempUser);
    }

    private void userStatus(String userLogin, String status){
        System.out.println("changing status to " + status);
        int ind = friendObservableList.indexOf(friendMap.get(userLogin));
        friendObservableList.remove(ind);
        friendMap.get(userLogin).isOnline = status;
        friendObservableList.add(ind,friendMap.get(userLogin));
    }

    private void flistHandle(String[] receivedMessage){
        Friend friend = new Friend(receivedMessage[2],receivedMessage[1]);
        friend.age = Integer.parseInt(receivedMessage[3]);
        friend.gender = Integer.parseInt(receivedMessage[4]);
        friend.isOnline = receivedMessage[6];
        friendObservableList.add(friend);
        chatMap.put(createChatName(client.login,receivedMessage[1]),createChatWindow());
        friendMap.put(receivedMessage[1],friend);
    }

    private void removeFriend(String friend){
        friendList.getItems().remove(friendMap.get(friend));
        friendObservableList.remove(friendMap.get(friend));
        System.out.println(friendMap.get(friend));
        friendMap.remove(friend);
        chatMap.remove(createChatName(client.login,friend));
        chatList.getItems().remove(createChatName(client.login,friend));
    }

    private void friendRequestResult(String[] result){
        switch (result[1]){
            case "requestAccepted":
                System.out.println("Request result: " + result[2] + result[3]);
                if(friendMap.get(result[2]) == null && friendRequestList.contains(result[2])){
                    Friend friend = new Friend(result[3],result[2]);
                    friendObservableList.add(friend);
                    chatMap.put(createChatName(client.login,result[2]),createChatWindow());
                    friendMap.put(result[2],friend);
                    friendRequestList.remove(result[2]);
                }
            default:break;
        }
    }

    private void requestHandle(String userLogin, String userName){
        requestList.setDisable(false);
        friendRequestMap.put(userLogin,userName);
        requestList.getItems().add(userLogin);
    }

    private void friendRequestHandle(String userLogin, String userName){
        Alert friendRequestAlert = new Alert(Alert.AlertType.CONFIRMATION);
        friendRequestAlert.setTitle("Заявка на добавление в контакты");
        friendRequestAlert.setHeaderText("Пользователь " + userName + " (" + userLogin + ") хочет добавить\nвас в список контактов");
        friendRequestAlert.setContentText("Выберите действие:");

        ButtonType acceptFriend = new ButtonType("Принять");
        ButtonType chooseLater = new ButtonType("Позже");
        ButtonType declineFriend = new ButtonType("Отклонить", ButtonBar.ButtonData.CANCEL_CLOSE);

        friendRequestAlert.getButtonTypes().setAll(acceptFriend,chooseLater,declineFriend);

        Optional<ButtonType> result = friendRequestAlert.showAndWait();
        if(result.get() == acceptFriend){
            if(friendMap.get(userLogin) == null){
                Friend friend = new Friend(userName,userLogin);
                friendObservableList.add(friend);
                friendMap.put(userLogin,friend);
                chatMap.put(createChatName(userLogin,client.login),createChatWindow());
                client.sendMessage("friendrequestanswer" + client.split + client.login,"requestAccepted" + client.split + userLogin + client.split + client.username);
                requestList.getItems().remove(userLogin);
                requestList.getSelectionModel().clearSelection();
                if(requestList.getItems().isEmpty())requestList.setDisable(true);
            }
        } else if(result.get() == chooseLater){
            requestList.getSelectionModel().clearSelection();
        } else if(result.get() == declineFriend){
            client.sendMessage("friendrequestanswer" + client.split + client.login, "requestDeclined" + client.split + userLogin + client.split + client.username);
            requestList.getItems().remove(userLogin);
            requestList.getSelectionModel().clearSelection();
            if (requestList.getItems().isEmpty()) requestList.setDisable(true);
        }
    }

    private void requestFailed(String login){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Ошибка добавления контакта!");
        alert.setContentText("Пользователь "+ login + "не в сети");
        alert.showAndWait();
    }

    private void handleTextMessage(String[] receivedMessage){
        VBox tempVBox;
            if ((tempVBox = this.chatMap.get(receivedMessage[1])) == null) {
                tempVBox = createChatWindow();
                this.chatMap.put(receivedMessage[1], tempVBox);
                if (!chatList.getItems().contains(receivedMessage[1])) {
                    this.chatList.getItems().add(receivedMessage[1]);
                }
                tempVBox.setFillWidth(true);
                tempVBox.getChildren().add(newMessage(receivedMessage[2], receivedMessage[3], receivedMessage[4]));
                ScrollPane tempBoxScroller = new ScrollPane(tempVBox);
                tempBoxScroller.setMinSize(556, 500);
                tempBoxScroller.setMaxSize(556, 500);
                chatWindow.getChildren().add(tempBoxScroller);
            } else {
                if (!chatList.getItems().contains(receivedMessage[1])) {
                    this.chatList.getItems().add(receivedMessage[1]);
                }
                tempVBox.getChildren().add(newMessage(receivedMessage[2], receivedMessage[3], receivedMessage[4]));
            }
    }

    private VBox createChatWindow(){
        VBox newVBox = new VBox();
        newVBox.setMinSize(540,500);
        return newVBox;
    }

    private Pane newMessage(String senderLogin,String senderName, String message){
        Pane messagePane = new Pane();
        messagePane.setMinSize(540,30);
        if(senderLogin.equals(client.login)){
            messagePane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            messagePane.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
        Label messageField = new Label();
        messageField.setWrapText(true);
        messageField.setText(senderName + "\n" + message);
        messageField.setMinSize(30,30);
        messageField.setMaxWidth(500);
        messageField.setStyle("-fx-background-color: coral");
        messagePane.getChildren().add(messageField);
        return messagePane;
    }

    private class InputTask extends Service<String>{
        MainController mainController;
        InputTask(MainController controller){
            this.mainController = controller;
        }
        @Override
        protected Task<String> createTask() {
            return new Task<String>() {
                @Override
                protected String call() throws Exception{
                    Object obj1;
                    try {
                        if ((obj1 = client.inputStream.readObject()) != null) {
                            return (String)obj1;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        }
    }

    public class Friend{
        public String name;
        public String login;
        public int gender;
        public int age;
        public String isOnline = "default";

        Friend(String name, String login){
            this.login = login;
            this.name = name;
        }
    }
}