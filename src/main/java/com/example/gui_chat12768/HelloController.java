package com.example.gui_chat12768;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.SnapshotResult;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

public class HelloController {
    @FXML
    private VBox usersBox;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private Label chatName;
    @FXML
    private Button inputBtn;
    private DataOutputStream out;
    // private int formId;
    private int toId = 0;
    private boolean connected = false;
    private int clientId = 0;
    private File file = null;
    @FXML
    private void onSend(){
        JSONObject jsonObject = new JSONObject();
        if(file != null){
            jsonObject.put("fileName", file.getName());
            /*out.writeUTF(jsonObject.toJSONString());
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] buffer = new byte[1024];
            int i;
            while ((i = bis.read(buffer)) != -1){
                out.write(buffer);
            }*/
        }else{
            String msg = textField.getText(); // Считываем сообщение пользователя из поля ввода
            textArea.appendText(msg+"\n"); // Выводим на экран сообщение
            textField.clear(); // Очищаем поле ввода сообщения
            jsonObject.put("message", msg); // Передаваемое сообщение
            jsonObject.put("to_id", toId); // ID получателя сообщения
            try {
                out.writeUTF(jsonObject.toJSONString()); // Отправляем сообщение на сервер
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
    public void sendRequest(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("getPrivateMessageWith", toId);
        try {
            out.writeUTF(jsonObject.toJSONString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    public void onConnect(){
        try {
            Socket socket = new Socket("127.0.0.1", 9123); // Подключаемся к серверу
            out = new DataOutputStream(socket.getOutputStream()); // Создаём поток вывода
            DataInputStream is = new DataInputStream(socket.getInputStream()); // Создаём поток ввода
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){ // Постоянно ждём информации от сервера
                        try {
                            String response = is.readUTF();// Ожидаем сообщение от сервера
                            JSONParser jsonParser = new JSONParser(); // т.к. сервер присылает сообщения в формате JSON, то для разбора нам необходим JSONParser
                            JSONObject jsonObject = (JSONObject) jsonParser.parse(response); // Превращаем сообщение от сервера в объект JSON
                            if(jsonObject.containsKey("message")){ // Если есть ключ "message", значит нам пришло сообщение
                                String msg = jsonObject.get("message").toString(); // Получаем текст сообщения
                                boolean privateMessage = Boolean.parseBoolean(jsonObject.get("private").toString());
                                int from = Integer.parseInt(jsonObject.get("from").toString());
                                if((from == toId || from == clientId) && privateMessage){ // from - кто отправил сообщение, toId - тот с кем мы сейчас в диалоговом окне
                                    textArea.appendText(msg+"\n"); // Печатаем сообщение на экран диалога ЛС
                                }else if (!privateMessage && toId == 0){
                                    textArea.appendText(msg+"\n"); // Общий чат
                                }

                            } else if (jsonObject.containsKey("users")) { // Если ключ "users", значит нам пришёл список пользователей
                                // в jsonObject = {"users": [{"id":1,  "name": "Ivan"},{"id":2, "name":"Igor"},{id:3, "name": "Oleg"}]}
                                JSONArray jsonArray = (JSONArray) jsonObject.get("users");
                                // jsonArray = [{"id":1,  "name": "Ivan"},{"id":2, "name":"Igor"},{id:3, "name": "Oleg"}]
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        usersBox.getChildren().clear();
                                        renderUserBtn("Общий чат", 0);
                                        for (int i = 0; i < jsonArray.size(); i++) {
                                            JSONObject jsonUser = (JSONObject) jsonArray.get(i); // jsonUser = {"id":1,  "name": "Ivan"}
                                            String userName = jsonUser.get("name").toString(); // "Ivan"
                                            int id = Integer.parseInt(jsonUser.get("id").toString()); // 1
                                            renderUserBtn(userName, id);
                                            if(i == jsonArray.size()-1 && !connected){
                                                clientId = id;
                                            }
                                        }
                                        connected = true;
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
    @FXML
    public void fileReceiver(){
        Stage stage = (Stage) inputBtn.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        file = fileChooser.showOpenDialog(stage);
    }

    public void renderUserBtn(String userName, int userId){
        Button userBtn = new Button(); // Создаём кнопку
        userBtn.setText(userName); // Записываем имя пользователя
        userBtn.setPrefWidth(200); // Установим ширину кнопки
        userBtn.setStyle("-fx-background-color: #AFEEEE");
        userBtn.setOnAction((event)->{ // Что делать если пользователь нажал на кнопку
            userBtn.getParent().getChildrenUnmodifiable().forEach(btn->{
                btn.setStyle("-fx-background-color: #AFEEEE");
            });
            userBtn.setStyle("-fx-background-color: #7FFFD4");
            toId = userId; // Меняем ID получателя
            chatName.setText("Чат с "+userName);
            textArea.clear();
            sendRequest();
        });
        usersBox.getChildren().add(userBtn); // Добавляем кнопку на экран в блок VBox
    }
}