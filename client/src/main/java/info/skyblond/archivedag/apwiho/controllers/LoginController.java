package info.skyblond.archivedag.apwiho.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    TextField hostTextInput;
    @FXML
    Button connectButton;

    @FXML
    void chooseCAFile(ActionEvent e) {

    }

    @FXML
    void chooseCertFile(ActionEvent e) {

    }

    @FXML
    void choosePrivateKeyFile(ActionEvent e) {

    }
    
    @FXML
    void doConnect(ActionEvent e) {

    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connectButton.disableProperty().bind(
                hostTextInput.textProperty().isEmpty()
        );
    }
}
