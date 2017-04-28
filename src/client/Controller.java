package client;

import client.main.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.HashMap;

/**
 * Created by klimb on 08.01.2017.
 */
public class Controller extends StackPane{
    private HashMap<String,Node> screens = new HashMap<>();
    public static HashMap<String,MainController> controllers = new HashMap<>();

    public void addScreen(String name, Node screen){
        screens.put(name,screen);
    }


    public boolean loadScreen(String name,String resource){
        if(screens.get(name) == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
                Parent loadScreen = (Parent) loader.load();
                ControlledScreen myScreenControler = ((ControlledScreen) loader.getController());
                if(name.equals(TempMain.MAIN_SCREEN))controllers.put(name,(MainController)loader.getController());
                myScreenControler.setScreenParent(this);
                addScreen(name, loadScreen);

                return true;
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                return false;
            }
        }
        return true;
    }

    public boolean setScreen(final String name){
        if(screens.get(name) != null){
            if(!getChildren().isEmpty()){
                getChildren().remove(0);
                getChildren().add(0,screens.get(name));
            } else {
                getChildren().add(screens.get(name));
            }
            return true;
        } else {
            System.err.println("Screens hasn't been loaded\n");
            return false;
        }
    }
    public boolean unloadScreen(String name){
        if(screens.remove(name) == null){
            System.err.println("Screen didn't exist");
            return false;
        }else{
            return true;
        }
    }
}
