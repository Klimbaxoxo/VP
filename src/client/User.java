package client;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class User {
    private StringProperty userName;
    private StringProperty userLogin;
    private StringProperty banTime;
    private StringProperty muteTime;

    public User(String login, String name, String ban, String mute){
        this.userName = new SimpleStringProperty(name);
        this.userLogin = new SimpleStringProperty(login);
        this.banTime = new SimpleStringProperty(ban);
        this.muteTime = new SimpleStringProperty(mute);
    }
    public void setBanTime(String ban){
        this.banTime.set(ban);
    }
    public void setMuteTime(String mute){
        this.muteTime.set(mute);
    }
    public void setUserName(String name){
        this.userName.setValue(name);
    }
    public String getUserName(){
        return this.userName.get();
    }
    public void setUserLogin(String name){
        this.userLogin.setValue(name);
    }
    public String getUserLogin(){
        return this.userLogin.get();
    }
    public String getBanTime(){
        return this.banTime.get();
    }
    public String getMuteTime(){
        return this.muteTime.get();
    }
}
