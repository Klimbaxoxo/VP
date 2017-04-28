package client;

import client.client.*;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TempMain extends Application {
    public static Client client = Client.getClient();
    public static final String MAIN_SCREEN = "main";
    public static final String MAIN_SCREEN_FXML = "main/mainpage.fxml";
    public static final String LOGIN_SCREEN = "login";
    private static final String LOGIN_SCREEN_FXML = "login/login.fxml";
    public static final String REGUSER_SCREEN = "reguser";
    public static final String REGUSER_SCREEN_FXML = "reguser/reguser.fxml";
    public static Controller mainContainer;
    private static Group root;

    public void start(Stage primaryStage){
        mainContainer = new Controller();
        mainContainer.loadScreen(TempMain.LOGIN_SCREEN,TempMain.LOGIN_SCREEN_FXML);
        mainContainer.setScreen(TempMain.LOGIN_SCREEN);

        root = new Group();
        root.getChildren().addAll(mainContainer);
        Scene scene = new Scene(root,785,590);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

    }
    public void stop(){
        System.out.println("Stage is closing");
        client.sendMessage("logout" + client.split + client.login,null);
    }
}
