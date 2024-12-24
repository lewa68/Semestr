package com.example.semestrwithfxgradle;

import com.example.semestrwithfxgradle.utils.LogUtil;
import com.example.semestrwithfxgradle.utils.PreferenceUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.stream.Collectors;

public class WeatherParcerController {

    private static final Logger logger = LogUtil.logger;
    private static final String API_KEY = "8c86e1bb36af9ce67eb25be46191ec36";
    private static final String CITIES_URL = "http://bulk.openweathermap.org/sample/city.list.json.gz";
    private static final Path CITIES_CACHE_FILE = Paths.get("src", "main", "resources", "cities_cache.json");

    @FXML
    private ComboBox<String> cityComboBox;
    @FXML
    private HBox imageContainer;
    @FXML
    private Label resultLabel;
    @FXML
    private VBox forecastContainer;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;

    private final ObservableList<String> allCities = FXCollections.observableArrayList();
    private FilteredList<String> filteredCities;

    @FXML
    private void initialize() {
        logger.info("Initializing WeatherParcerController");
        filteredCities = new FilteredList<>(allCities, s -> true);
        cityComboBox.setPrefWidth(200);
        cityComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
            }
        });
        cityComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "Введите город..." : item);
            }
        });
        cityComboBox.setEditable(true);
        cityComboBox.setItems(filteredCities);
        cityComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> filterCities(newValue.toLowerCase(Locale.ROOT)));
        loadCities();
        loadLastCity();
    }

    private void loadCities() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                updateMessage("Загрузка списка городов...");
                updateProgress(-1, -1);
                if (Files.exists(CITIES_CACHE_FILE)) {
                    try (BufferedReader reader = Files.newBufferedReader(CITIES_CACHE_FILE)) {
                        Gson gson = new Gson();
                        Type type = new TypeToken<List<WeatherData.City>>() {}.getType();
                        List<WeatherData.City> citiesList = gson.fromJson(reader, type);
                        List<String> cityNames = citiesList.stream().map(WeatherData.City::getName).distinct().toList();
                        Platform.runLater(() -> allCities.addAll(cityNames));
                        logger.info("Список городов успешно загружен из кэша.");
                        return null;
                    } catch (IOException e) {
                        LogUtil.error("Ошибка при чтении кэша городов", e);
                    }
                }
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(CITIES_URL).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        LogUtil.error("Failed to fetch cities list. HTTP code: " + response.code(), null);
                        Platform.runLater(() -> showAlert("Ошибка при загрузке списка городов: HTTP код " + response.code()));
                        return null;
                    }
                    InputStream inputStream = Objects.requireNonNull(response.body()).byteStream();
                    LogUtil.info("Файл city.list.json.gz успешно загружен.");
                    GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream));
                    StringBuilder contentBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        contentBuilder.append(line);
                    }
                    String responseBody = contentBuilder.toString();
                    LogUtil.debug("Содержимое файла city.list.json:\n{}", responseBody.substring(0, Math.min(responseBody.length(), 1000)));
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<WeatherData.City>>() {}.getType();
                    List<WeatherData.City> citiesList = gson.fromJson(responseBody, type);
                    List<String> cityNames = citiesList.stream().map(WeatherData.City::getName).distinct().toList();
                    Platform.runLater(() -> allCities.addAll(cityNames));
                    try (BufferedWriter writer = Files.newBufferedWriter(CITIES_CACHE_FILE)) {
                        gson.toJson(citiesList, writer);
                        logger.info("Список городов успешно сохранён в кэш.");
                    } catch (IOException e) {
                        LogUtil.error("Ошибка при сохранении кэша городов", e);
                    }
                } catch (IOException e) {
                    LogUtil.error("Error fetching cities list", e);
                    Platform.runLater(() -> showAlert("Ошибка при загрузке списка городов: " + e.getMessage()));
                } finally {
                    updateProgress(1, 1);
                    updateMessage("Готово!");
                }
                return null;
            }
        };
        task.setOnRunning(event -> {
            progressBar.setVisible(true);
            progressLabel.setVisible(true);
        });
        task.setOnSucceeded(event -> {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
        });
        task.setOnFailed(event -> {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
        });
        new Thread(task).start();
    }

    private void filterCities(String query) {
        logger.info("Filtering cities with query: {}", query);
        if (query == null || query.isEmpty()) {
            filteredCities.setPredicate(s -> true);
            cityComboBox.getItems().setAll(allCities);
        } else {
            filteredCities.setPredicate(s -> s.toLowerCase(Locale.ROOT).startsWith(query));
            List<String> limitedSuggestions = filteredCities.stream().limit(5).collect(Collectors.toList());
            cityComboBox.getItems().setAll(limitedSuggestions);
        }
        logger.info("Filtered cities: {}", cityComboBox.getItems());
    }

    @FXML
    private void fetchWeather() {
        String city = cityComboBox.getEditor().getText().trim();
        if (city.isEmpty()) {
            return;
        }
        try {
            LogUtil.info("User requested weather for city: " + city);
            String url = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY + "&units=metric";
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                LogUtil.error("Failed to fetch weather data for city: " + city + ". HTTP code: " + response.code(), null);
                showAlert("Ошибка при получении данных о погоде. Возможно, город не существует.");
                return;
            }
            String responseBody = Objects.requireNonNull(response.body()).string();
            logger.info("API Response: {}", responseBody);
            Gson gson = new Gson();
            WeatherData.CurrentWeatherResponse weatherResponse = gson.fromJson(responseBody, WeatherData.CurrentWeatherResponse.class);
            if (weatherResponse == null || weatherResponse.getCod() != 200) {
                throw new WeatherData.CityNotFoundException("Город не найден");
            }
            displayCurrentWeather(weatherResponse);
            fetchForecast(city);
            saveSelectedCity(city);
        } catch (WeatherData.CityNotFoundException | IOException e) {
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
            if (!response.isSuccessful()) {
                LogUtil.error("Failed to fetch forecast data for city: " + city + ". HTTP code: " + response.code(), null);
                showAlert("Ошибка при получении прогноза погоды. Возможно, город не существует.");
                return;
            }
            String responseBody = Objects.requireNonNull(response.body()).string();
            logger.info("API Forecast Response: {}", responseBody);
            Gson gson = new Gson();
            WeatherData.ForecastResponse forecastResponse = gson.fromJson(responseBody, WeatherData.ForecastResponse.class);
            if (forecastResponse == null || forecastResponse.getCod() != 200) {
                throw new WeatherData.CityNotFoundException("Прогноз погоды не найден");
            }
            displayForecast(forecastResponse);
        } catch (WeatherData.CityNotFoundException | IOException e) {
            LogUtil.error("Error fetching forecast data for city: " + city, e);
            showAlert(e.getMessage());
        }
    }

    private void displayCurrentWeather(WeatherData.CurrentWeatherResponse weatherResponse) {
        if (weatherResponse == null) {
            showAlert("Не удалось получить данные о текущей погоде.");
            return;
        }
        WeatherData.Main main = weatherResponse.getMain();
        WeatherData.Wind wind = weatherResponse.getWind();
        WeatherData.Clouds clouds = weatherResponse.getClouds();
        List<WeatherData.WeatherInfo> weatherInfos = weatherResponse.getWeather();
        String weatherInfo = "Погода в " + weatherResponse.getName() + ":\n" +
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
        resultLabel.setText(weatherInfo);
        ImageView imageView = new ImageView(new Image("http://openweathermap.org/img/wn/" + weatherInfos.get(0).getIcon() + "@2x.png"));
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);
        imageContainer.getChildren().clear();
        imageContainer.getChildren().add(imageView);
    }

    private void displayForecast(WeatherData.ForecastResponse forecastResponse) {
        forecastContainer.getChildren().clear();
        if (forecastResponse == null || forecastResponse.getList() == null || forecastResponse.getList().isEmpty()) {
            logger.error("Прогноз погоды не содержит данных.");
            showAlert("Не удалось получить прогноз погоды.");
            return;
        }
        logger.info("Количество записей в прогнозе погоды: {}", forecastResponse.getList().size());

        for (int i = 0; i < forecastResponse.getList().size(); i += 8) {
            WeatherData.ForecastItem item = forecastResponse.getList().get(i);
            if (item == null || item.getMain() == null || item.getWeather() == null || item.getWeather().isEmpty()) {
                logger.error("Недостаточно данных для записи прогноза погоды.");
                continue;
            }
            WeatherData.Main main = item.getMain();
            WeatherData.WeatherInfo weatherInfo = item.getWeather().get(0);
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
            cityComboBox.getEditor().setText(lastCity); // Устанавливаем текст в редакторе
            fetchWeather();
        } else {
            cityComboBox.getEditor().promptTextProperty().set("Введите город...");
        }
    }
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}