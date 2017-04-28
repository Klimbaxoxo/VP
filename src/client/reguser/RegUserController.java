package client.reguser;

import client.ControlledScreen;
import client.Controller;
import client.TempMain;
import client.client.Client;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;

import java.net.URL;
import java.util.ResourceBundle;


public class RegUserController implements Initializable, ControlledScreen {
    Controller myController;
    Client client = TempMain.client;

    Alert alert;


    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    PasswordField passwordConfirmationField;
    @FXML
    TextField userNameField;
    @FXML
    TextField serverAddressField;


    public void reguser(){
        alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка регистрации");
        alert.setHeaderText(null);
        alert.initModality(Modality.APPLICATION_MODAL);
        setUpConnection();


    }
    public boolean setUpConnection(){
        if(!loginField.getText().trim().isEmpty()){
            if(!passwordField.getText().trim().isEmpty()){
                if(!passwordConfirmationField.getText().trim().isEmpty()){
                    if(passwordField.getText().equals(passwordConfirmationField.getText())){
                        if(!userNameField.getText().trim().isEmpty()){
                            if(!serverAddressField.getText().trim().isEmpty()){
                                if(!client.isLogged){
                                    try {
                                        client.startUp(serverAddressField.getText());
                                    } catch (Exception e) {
                                        alert.setContentText("Проблема с соединением");
                                        alert.showAndWait();
                                        e.printStackTrace();
                                        return false;
                                    }
                                }
                                if(sendData()){
                                    client.login = loginField.getText();
                                    client.username = userNameField.getText();
                                    client.setIp(serverAddressField.getText());
                                    TempMain.mainContainer.loadScreen(TempMain.MAIN_SCREEN,TempMain.MAIN_SCREEN_FXML);
                                    myController.setScreen(TempMain.MAIN_SCREEN);
                                    return true;
                                } else {
                                    alert.setContentText("Пользователь с таким именем\nуже существует");
                                    alert.showAndWait();
                                    return false;
                                }
                            } else {
                                alert.setContentText("Введите адрес сервера");
                                alert.showAndWait();
                                serverAddressField.requestFocus();
                                return false;
                            }
                        } else {
                            alert.setContentText("Введите имя для отображения");
                            alert.showAndWait();
                            userNameField.requestFocus();
                            return false;
                        }
                    } else {
                        alert.setContentText("Введенный пароль не совпадает с подтверждающим");
                        alert.showAndWait();
                        passwordConfirmationField.requestFocus();
                        return false;
                    }
                } else {
                    alert.setContentText("Введите пароль для подтверждения");
                    alert.showAndWait();
                    passwordConfirmationField.requestFocus();
                    return false;
                }
            } else {
                alert.setContentText("Введите пароль");
                alert.showAndWait();
                passwordField.requestFocus();
                return false;
            }
        } else {
            alert.setContentText("Введите имя ползователя");
            alert.showAndWait();
            loginField.requestFocus();
            return false;
        }
    }
    public boolean sendData(){
        Object userData = null;
        try{
            client.sendMessage("reguser" + client.split + loginField.getText(),userNameField.getText() + client.split + passwordField.getText() + client.split +
            client.age + client.split + client.gender);
            System.out.println("sent");
            while (userData == null){
                userData = client.inputStream.readObject();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        String[] serverAnswer = ((String)userData).split(client.split);
        if(serverAnswer[1].equals("succeed")){
            return true;
        }
        return false;
    }

    //Switch to LogIn screen
    public void switchToLogIn(){
        myController.setScreen(TempMain.LOGIN_SCREEN);
    }
    //JavaFX MultiFXML handler
    @Override
    public void setScreenParent(Controller screenParent) {
        myController = screenParent;
    }
    //Useless stuff
    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
