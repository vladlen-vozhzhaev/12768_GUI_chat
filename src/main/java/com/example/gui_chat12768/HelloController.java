package com.example.gui_chat12768;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.SnapshotResult;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class HelloController {
    @FXML
    private VBox usersBox;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    private DataOutputStream out;
    // private int formId;
    private int toId = 0;
    @FXML
    private void onSend(){
        String msg = textField.getText(); // Считываем сообщение пользователя из поля ввода
        textArea.appendText(msg+"\n"); // Выводим на экран сообщение
        textField.clear(); // Очищаем поле ввода сообщения
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", msg); // Передаваемое сообщение
        jsonObject.put("to_id", toId); // ID получателя сообщения
        try {
            out.writeUTF(jsonObject.toJSONString()); // Отправляем сообщение на сервер
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    private void onConnect(){
        try {
            Socket socket = new Socket("127.0.0.1", 9123);
            out = new DataOutputStream(socket.getOutputStream());
            DataInputStream is = new DataInputStream(socket.getInputStream());
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            String response = is.readUTF();// Ожидаем сообщение от сервера
                            JSONParser jsonParser = new JSONParser();
                            JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
                            if(jsonObject.containsKey("message")){
                                String msg = jsonObject.get("message").toString();
                                textArea.appendText(msg+"\n");
                            } else if (jsonObject.containsKey("users")) {
                                JSONArray jsonArray = (JSONArray) jsonObject.get("users");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        usersBox.getChildren().clear();
                                        for (int i = 0; i < jsonArray.size(); i++) {
                                            JSONObject jsonUser = (JSONObject) jsonArray.get(i);
                                            String userName = jsonUser.get("name").toString();
                                            int id = Integer.parseInt(jsonUser.get("id").toString());
                                            Button userBtn = new Button();
                                            userBtn.setText(userName);
                                            userBtn.setPrefWidth(200);
                                            userBtn.setOnAction((event)->{
                                                toId = id;
                                            });
                                            usersBox.getChildren().add(userBtn);
                                        }
                                    }
                                });

                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            });
            thread.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}