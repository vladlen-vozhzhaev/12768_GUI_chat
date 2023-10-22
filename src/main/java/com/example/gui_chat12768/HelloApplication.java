package com.example.gui_chat12768;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Загружаем шаблон приложения
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        // Создаём сцену, в которую помещаем шаблон, и назначем ширину и высоту
        Scene scene = new Scene(fxmlLoader.load(), 500, 400);
        stage.setTitle("Сетевой чат"); // Заголовок окна
        stage.setScene(scene); // Размещаем сцену в окне
        stage.setOnCloseRequest((event)->{
            System.exit(0); // Полностью закрываем приложение и потоки
        }); // Обработка действий при нажатии на красный крестик
        stage.show(); // Отображаем результат
        HelloController helloController = fxmlLoader.getController(); // Получаем объекь контроллера
        helloController.onConnect(); // Вызываем метод подключения к серверу
    }

    public static void main(String[] args) {
        launch();
    }
}