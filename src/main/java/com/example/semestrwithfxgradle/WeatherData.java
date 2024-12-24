package com.example.semestrwithfxgradle;

import java.util.List;

public class WeatherData {

    public static class City {
        private String name;
        public String getName() {
            return name;
        }
    }

    public static class WeatherInfo {
        private String icon;
        private String description;
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    public static class CurrentWeatherResponse {
        private Main main;
        private Wind wind;
        private Clouds clouds;
        private List<WeatherInfo> weather;
        private String name;
        private int cod;
        public Main getMain() { return main; }
        public Wind getWind() { return wind; }
        public Clouds getClouds() { return clouds; }
        public List<WeatherInfo> getWeather() { return weather; }
        public String getName() { return name; }
        public int getCod() { return cod; }
    }

    public static class Main {
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

    public static class Wind {
        private double speed;
        private int deg;
        public double getSpeed() { return speed; }
        public int getDeg() { return deg; }
    }

    public static class Clouds {
        private int all;
        public int getAll() { return all; }
    }

    public static class ForecastResponse {
        private List<ForecastItem> list;
        private int cod;
        public List<ForecastItem> getList() { return list; }
        public int getCod() { return cod; }
    }

    public static class ForecastItem {
        private Main main;
        private List<WeatherInfo> weather;
        private String dt_txt;
        public Main getMain() { return main; }
        public List<WeatherInfo> getWeather() { return weather; }
        public String getDt_txt() { return dt_txt; }
    }

    public static class CityNotFoundException extends Exception {
        public CityNotFoundException(String message) {
            super(message);
        }
    }
}