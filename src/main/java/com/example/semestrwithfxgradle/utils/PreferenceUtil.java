package com.example.semestrwithfxgradle.utils;

import java.util.prefs.Preferences;

public class PreferenceUtil {
    private static final Preferences prefs = Preferences.userNodeForPackage(PreferenceUtil.class);
    private static final String LAST_CITY_KEY = "last_city";

    public static void saveSelectedCity(String city) {
        prefs.put(LAST_CITY_KEY, city);
    }

    public static String getLastCity() {return prefs.get(LAST_CITY_KEY, "");}
}