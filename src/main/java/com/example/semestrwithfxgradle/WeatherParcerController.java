package com.example.semestrwithfxgradle;

import com.example.semestrwithfxgradle.utils.LogUtil;
import com.example.semestrwithfxgradle.utils.PreferenceUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class WeatherParcerController {

    private static final Logger logger = LogUtil.logger;
    private static final String API_KEY = "8c86e1bb36af9ce67eb25be46191ec36";

    @FXML
    private ComboBox<String> cityComboBox;

    @FXML
    private HBox imageContainer;

    @FXML
    private Label resultLabel;

    @FXML
    private VBox forecastContainer;

    private final ObservableList<String> cities = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        cityComboBox.setItems(cities);
        cityComboBox.setEditable(true);
        cityComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> handleCitySearch(newValue));
        loadLastCity();
        if (cityComboBox.getValue() == null) {
            cityComboBox.getEditor().promptTextProperty().set("Введите город...");
        }
    }

    private void handleCitySearch(String query) {
        if (query.length() > 2) {
            Thread thread = new Thread(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();
                    String url = "http://api.openweathermap.org/geo/1.0/direct?q=" + query + "&limit=5&appid=" + API_KEY;
                    Request request = new Request.Builder().url(url).build();
                    Response response = client.newCall(request).execute();
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    Gson gson = new GsonBuilder().create();
                    CityResponse[] cityResponses = gson.fromJson(responseBody, CityResponse[].class);
                    Platform.runLater(() -> {
                        cities.clear();
                        for (CityResponse cityResponse : cityResponses) {
                            cities.add(cityResponse.getName());
                        }
                    });
                } catch (IOException e) {
                    LogUtil.error("Error fetching cities", e);
                    showAlert("Ошибка при получении данных: " + e.getMessage());
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    @FXML
    private void fetchWeather() {
        String city = cityComboBox.getValue();
        if (city == null || city.trim().isEmpty()) {
            showAlert("Введите название города");
            return;
        }
        try {
            LogUtil.info("User requested weather for city: " + city);
            String url = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY + "&units=metric";
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            logger.info("API Response: {}", responseBody);

            Gson gson = new GsonBuilder().create();
            CurrentWeatherResponse weatherResponse = gson.fromJson(responseBody, CurrentWeatherResponse.class);

            if (weatherResponse == null) {
                throw new CityNotFoundException("Город не найден");
            }

            displayCurrentWeather(weatherResponse);
            fetchForecast(city); // Получаем прогноз погоды на следующие 4 дня

            saveSelectedCity(city);
        } catch (CityNotFoundException | IOException e) {
            LogUtil.error("Error fetching weather data for city: " + city, e);
            showAlert(e.getMessage());
        }
    }

    private void fetchForecast(String city) {
        try {
            String url = "http://api.openweathermap.org/data/2.5/forecast?q=" + city + "&appid=" + API_KEY + "&units=metric";
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            logger.info("API Forecast Response: {}", responseBody);

            Gson gson = new GsonBuilder().create();
            ForecastResponse forecastResponse = gson.fromJson(responseBody, ForecastResponse.class);

            if (forecastResponse == null) {
                throw new CityNotFoundException("Прогноз погоды не найден");
            }

            displayForecast(forecastResponse);

        } catch (CityNotFoundException | IOException e) {
            LogUtil.error("Error fetching forecast data for city: " + city, e);
            showAlert(e.getMessage());
        }
    }

    private void displayCurrentWeather(CurrentWeatherResponse weatherResponse) {
        if (weatherResponse == null) {
            showAlert("Не удалось получить данные о текущей погоде.");
            return;
        }

        Main main = weatherResponse.getMain();
        Wind wind = weatherResponse.getWind();
        Clouds clouds = weatherResponse.getClouds();
        List<WeatherInfo> weatherInfos = weatherResponse.getWeather();

        String sb = "Погода в " + weatherResponse.getName() + ":\n" +
                "Температура: " + main.getTemp() + "°C\n" +
                "По ощущениям: " + main.getFeels_like() + "°C\n" +
                "Максимальная температура: " + main.getTemp_max() + "°C\n" +
                "Минимальная температура: " + main.getTemp_min() + "°C\n" +
                "Влажность: " + main.getHumidity() + "%\n" +
                "Давление: " + main.getPressure() + " мм рт.ст.\n" +
                "Скорость ветра: " + wind.getSpeed() + " м/с\n" +
                "Направление ветра: " + getWindDirection(wind.getDeg()) + "\n" +
                "Облачность: " + clouds.getAll() + "%\n" +
                "Описание: " + weatherInfos.get(0).getDescription() + "\n";

        resultLabel.setText(sb);

        ImageView imageView = new ImageView(new Image("http://openweathermap.org/img/wn/" + weatherInfos.get(0).getIcon() + "@2x.png"));
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);
        imageContainer.getChildren().clear();
        imageContainer.getChildren().add(imageView);
    }

    private void displayForecast(ForecastResponse forecastResponse) {
        forecastContainer.getChildren().clear();
        for (int i = 0; i < forecastResponse.getList().size(); i += 8) {
            ForecastItem item = forecastResponse.getList().get(i);
            Main main = item.getMain();
            WeatherInfo weatherInfo = item.getWeather().get(0);

            Label label = new Label(
                    "Дата: " + item.getDt_txt() + "\n" +
                            "Статус: " + weatherInfo.getDescription() + "\n" +
                            "Температура: " + main.getTemp() + "°C\n" +
                            "Максимальная температура: " + main.getTemp_max() + "°C\n" +
                            "Минимальная температура: " + main.getTemp_min() + "°C\n"
            );

            ImageView imageView = new ImageView(new Image("http://openweathermap.org/img/wn/" + weatherInfo.getIcon() + "@2x.png"));
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);

            HBox hBox = new HBox(imageView, label);
            forecastContainer.getChildren().add(hBox);
        }
    }

    private String getWindDirection(double deg) {
        if (deg >= 337.5 || deg < 22.5) return "Северный";
        if (deg >= 22.5 && deg < 67.5) return "Северо-восточный";
        if (deg >= 67.5 && deg < 112.5) return "Восточный";
        if (deg >= 112.5 && deg < 157.5) return "Юго-восточный";
        if (deg >= 157.5 && deg < 202.5) return "Южный";
        if (deg >= 202.5 && deg < 247.5) return "Юго-западный";
        if (deg >= 247.5 && deg < 292.5) return "Западный";
        return "Северо-западный";
    }

    private void saveSelectedCity(String city) {
        PreferenceUtil.saveSelectedCity(city);
    }

    private void loadLastCity() {
        String lastCity = PreferenceUtil.getLastCity();
        if (!lastCity.isEmpty()) {
            cityComboBox.setValue(lastCity);
            fetchWeather();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    static class CityResponse {
        private String name;
        public String getName() { return name; }
    }

    static class WeatherInfo {
        private String icon;
        private String description;
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    static class CurrentWeatherResponse {
        private Main main;
        private Wind wind;
        private Clouds clouds;
        private List<WeatherInfo> weather;
        private String name;

        public Main getMain() { return main; }
        public Wind getWind() { return wind; }
        public Clouds getClouds() { return clouds; }
        public List<WeatherInfo> getWeather() { return weather; }
        public String getName() { return name; }
    }

    static class Main {
        private double temp;
        private double feels_like;
        private double temp_min;
        private double temp_max;
        private int humidity;
        private int pressure;

        public double getTemp() { return temp; }
        public double getFeels_like() { return feels_like; }
        public double getTemp_min() { return temp_min; }
        public double getTemp_max() { return temp_max; }
        public int getHumidity() { return humidity; }
        public int getPressure() { return pressure; }
    }

    static class Wind {
        private double speed;
        private int deg;

        public double getSpeed() { return speed; }
        public int getDeg() { return deg; }
    }

    static class Clouds {
        private int all;

        public int getAll() { return all; }
    }

    static class ForecastResponse {
        private List<ForecastItem> list;

        public List<ForecastItem> getList() { return list; }
    }

    static class ForecastItem {
        private Main main;
        private List<WeatherInfo> weather;
        private String dt_txt;

        public Main getMain() { return main; }
        public List<WeatherInfo> getWeather() { return weather; }
        public String getDt_txt() { return dt_txt; }
    }

    static class CityNotFoundException extends Exception {
        public CityNotFoundException(String message) {
            super(message);
        }
    }
}