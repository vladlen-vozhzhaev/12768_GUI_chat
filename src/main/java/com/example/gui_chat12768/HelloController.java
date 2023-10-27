package com.example.gui_chat12768;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.SnapshotResult;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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
    private VBox messageBox;
    //private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private Label chatName;
    @FXML
    private Button inputBtn;
    private DataOutputStream out;
    private DataInputStream is;
    // private int formId;
    private int toId = 0;
    private boolean connected = false;
    private int clientId = 0;
    private File file = null;
    private Text text;
    private ImageView imageView;
    @FXML
    private void onSend(){
        JSONObject jsonObject = new JSONObject();
        if(file != null){
            try {
                FileInputStream fis = new FileInputStream(file);
                jsonObject.put("fileName", file.getName());
                jsonObject.put("fileSize", file.length());
                jsonObject.put("to_id", toId);
                out.writeUTF(jsonObject.toJSONString());
                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] buffer = new byte[1024];
                int i;
                while ((i = bis.read(buffer)) != -1){
                    out.write(buffer);
                }
                out.flush();
                file = null;
                textField.clear();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            String msg = textField.getText(); // Считываем сообщение пользователя из поля ввода
            text = new Text();
            text.setText(msg);
            messageBox.getChildren().add(text);
            //textArea.appendText(msg+"\n"); // Выводим на экран сообщение
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
            is = new DataInputStream(socket.getInputStream()); // Создаём поток ввода
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
                                int type = Integer.parseInt(jsonObject.get("type").toString());
                                boolean privateMessage = Boolean.parseBoolean(jsonObject.get("private").toString());
                                int from = Integer.parseInt(jsonObject.get("from").toString());
                                if((from == toId || from == clientId) && privateMessage){ // from - кто отправил сообщение, toId - тот с кем мы сейчас в диалоговом окне
                                    if(type == 1){
                                        renderTextMessage(msg);
                                    } else if (type == 2) {
                                        String fileName = msg.split(": ")[1];
                                        downloadFile(fileName);
                                        FileInputStream fis = new FileInputStream("files/"+fileName);
                                        renderImageMessage(fis);
                                    }
                                    //textArea.appendText(msg+"\n"); // Печатаем сообщение на экран диалога ЛС
                                }else if (!privateMessage && toId == 0){
                                    if(type == 1){
                                        renderTextMessage(msg);
                                    } else if (type == 2) {
                                        String fileName = msg.split(": ")[1];
                                        downloadFile(fileName);
                                        FileInputStream fis = new FileInputStream("files/"+fileName);
                                        renderImageMessage(fis);
                                    }
                                    //textArea.appendText(msg+"\n"); // Общий чат
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
        textField.setText("Файл: "+file.getName());
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
            messageBox.getChildren().clear();
            //textArea.clear();
            sendRequest();
        });
        usersBox.getChildren().add(userBtn); // Добавляем кнопку на экран в блок VBox
    }
    public void renderTextMessage(String msg){
        Platform.runLater(()->{
            text = new Text();
            text.setText(msg);
            messageBox.getChildren().add(text);
        });
    }
    public void renderImageMessage(FileInputStream fis){
        Platform.runLater(()->{
            imageView = new ImageView();
            Image image = new Image(fis);
            imageView.setImage(image);
            messageBox.getChildren().add(imageView);
        });
    }

    // Принимаем файл с сервера и сохраняем его на жесткий диск
    public void downloadFile(String fileName) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("getFile", fileName);
        out.writeUTF(jsonObject.toJSONString());
        while (true){
            try {
                long fileSize = Long.parseLong(is.readUTF());
                FileOutputStream fos = new FileOutputStream("files/"+fileName, true);
                byte[] buffer = new byte[1024];
                while (true){
                    is.read(buffer);
                    for(byte b : buffer){
                        fos.write(b);
                    }
                    fileSize -= 1024;
                    if(fileSize <= 0) break;
                }
                break;
            }catch (NumberFormatException e){
                System.out.println("Неверный размер");
            }
        }

    }
}