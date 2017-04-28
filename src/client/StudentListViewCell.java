package client;

import client.main.MainController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;

public class StudentListViewCell extends ListCell<MainController.Friend> {
    @FXML
    private Label usernameLabel;
    @FXML
    private Label userLoginLabel;
    @FXML
    private Label userGenderLabel;
    @FXML
    private Label userAgeLabel;
    @FXML
    private Label userOnlineLabel;
    @FXML
    private GridPane cellPane;
    private FXMLLoader loader;

    @Override
    protected void updateItem(MainController.Friend friend, boolean empty) {
        super.updateItem(friend, empty);

        if(empty || friend == null){
            setText(null);
            setDisable(false);
            setGraphic(null);
        } else {
            if(loader == null){
                loader = new FXMLLoader(getClass().getResource("res/listcell.fxml"));
                loader.setController(this);
                try{
                    loader.load();
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            usernameLabel.setText(friend.name + " ");
            userLoginLabel.setText("@" + friend.login + " ");
            userAgeLabel.setText("Возраст: " + friend.age);
            userGenderLabel.setText(String.valueOf(friend.gender));
            userOnlineLabel.setText(String.valueOf(friend.isOnline));
            setText(null);
            setGraphic(cellPane);
        }
    }
}
