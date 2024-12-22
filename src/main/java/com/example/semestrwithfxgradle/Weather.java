package com.example.semestrwithfxgradle;

public class Weather {
    private WeatherMain main;
    private Wind wind;

    public WeatherMain getMain() {
        return main;
    }

    public Wind getWind() {
        return wind;
    }
}

class Wind {
    private double speed;

    public double getSpeed() {
        return speed;
    }
}

class WeatherMain {
    private double temp;
    private double humidity;
    private double feels_like;

    public double getTemp() {
        return temp;
    }

    public double getFeel() {
        return feels_like;
    }

    public double getHumidity() {
        return humidity;
    }
}

