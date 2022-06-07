package com.example.googlefit.googlefit;

import java.util.Arrays;
import java.util.List;

/**
 * @author tuhin
 */
public class GooglefitConstant {
    public static final String HTML_BEGIN = "<!DOCTYPE><html><head></head><title>Fitness</title><body>";

    public static final String HTML_END = " </body></html>";

    public static final List<String> HEALTH_DATATYPES = Arrays.asList(
            "com.google.blood_glucose",
            "com.google.blood_pressure",
            "com.google.body.fat.percentage",
            "com.google.body.temperature",
            "com.google.cervical_mucus",
            "com.google.cervical_position",
            "com.google.heart_rate.bpm",
            "com.google.height",
            "com.google.menstruation",
            "com.google.ovulation_test",
            "com.google.oxygen_saturation",
            "com.google.sleep.segment",
            "com.google.vaginal_spotting",
            "com.google.weight");

    public static final List<String> ACTIVITY_DATATYPES = Arrays.asList(
            "com.google.activity.segment",
            "com.google.calories.bmr",
            "com.google.calories.expended",
            "com.google.cycling.pedaling.cadence",
            "com.google.cycling.pedaling.cumulative",
            "com.google.heart_minutes",
            "com.google.active_minutes",
            "com.google.power.sample",
            "com.google.step_count.cadence",
            "com.google.step_count.delta",
            "com.google.activity.exercise");

    public static final List<String> LOCATION_DATATYPES = Arrays.asList(
            "com.google.cycling.wheel_revolution.rpm",
            "com.google.cycling.wheel_revolution.cumulative",
            "com.google.distance.delta",
            "com.google.location.sample",
            "com.google.speed");

    public static final List<String> NUTRITION_DATATYPES = Arrays.asList(
            "com.google.hydration",
            "com.google.nutrition");
}
