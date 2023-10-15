package com.example.gui_chat12768;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class HelloController {
    @FXML
    private TextField textField;
    @FXML
    private TextArea textArea;

    @FXML
    private void onSendMessage(){
        String msg = textField.getText();
        textArea.appendText(msg+"\n");
        textField.clear();
    }
}