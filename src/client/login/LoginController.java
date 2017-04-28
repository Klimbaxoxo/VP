package client.login;

import client.ControlledScreen;
import client.Controller;
import client.TempMain;
import client.client.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;


public class LoginController implements Initializable, ControlledScreen{
    @FXML
    TextField userLoginField;
    @FXML
    PasswordField userPasswordField;
    @FXML
    TextField serverAddressField;
    @FXML
    Button logInButton;
    @FXML
    Button regUser;
    Controller myController;
    Client client = TempMain.client;
    public static String friendList;
    

    // FXML Controller
    public LoginController(){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try{
            //loader.load();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    //Button click handler
    public void logInButtonClick(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка авторизации");
        alert.setHeaderText(null);
        alert.initModality(Modality.APPLICATION_MODAL);
        if(!(userLoginField.getText().trim().isEmpty())){
            if(!(userPasswordField.getText().trim().isEmpty())){
                if(!(serverAddressField.getText().trim().isEmpty())){
                    if(!client.isLogged){
                        try {
                            client.startUp(serverAddressField.getText());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    client.login = userLoginField.getText();
                    if(!sendData()){
                        alert.setContentText("Введены неверные данные\nПроверьте правильность имени пользователя и пароля");
                        alert.showAndWait();
                    } else {
                        TempMain.mainContainer.loadScreen(TempMain.MAIN_SCREEN,TempMain.MAIN_SCREEN_FXML);
                        myController.setScreen(TempMain.MAIN_SCREEN);
                    }
                } else {
                    alert.setContentText("Введите адрес сервера");
                    serverAddressField.requestFocus();
                    alert.showAndWait();
                }
            } else {
                alert.setContentText("Введите пароль");
                userPasswordField.requestFocus();
                alert.showAndWait();
            }
        } else {
            alert.setContentText("Введите логин");
            userLoginField.requestFocus();
            alert.showAndWait();
        }
    }

    //Switch to registration window
    public void regUserClick(){
        TempMain.mainContainer.loadScreen(TempMain.REGUSER_SCREEN,TempMain.REGUSER_SCREEN_FXML);
        myController.setScreen(TempMain.REGUSER_SCREEN);
    }

    //LogIn info sender
    public boolean sendData(){
        Object userData = null;
        try {
            client.sendMessage("login" + client.split + client.login,userPasswordField.getText());
            System.out.println("sent");
            while (userData == null){
                userData = client.inputStream.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] serverAnswer = ((String)userData).split(client.split,8);
        if(serverAnswer[1].equals("succeed")){
            client.accessLevel = Integer.parseInt(serverAnswer[2]);
            client.username = serverAnswer[3];
            client.age = Integer.parseInt(serverAnswer[4]);
            client.gender = Integer.parseInt(serverAnswer[5]);
            client.status = serverAnswer[6];
            System.out.println((String)userData);
            friendList = serverAnswer[7];
            return true;
        }
        return false;
    }





    //JavaFX MultiFXML handler
    @Override
    public void setScreenParent(Controller screenParent) {
        myController = screenParent;
    }

    //Useless
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

}
