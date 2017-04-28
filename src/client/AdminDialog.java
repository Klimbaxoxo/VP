package client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;


public class AdminDialog {
    private Stage stage;
    private Scene scene;
    private FXMLLoader newLoader = new FXMLLoader(getClass().getResource("res/shietFxml.fxml"));
    public AdminDialog(){
        try {
            scene = new Scene(newLoader.load(),600,400);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
