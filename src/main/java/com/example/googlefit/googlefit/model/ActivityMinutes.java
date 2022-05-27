package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.DataPoint;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
@ToString
public class ActivityMinutes extends UserDetails{
    String name;
    String email;
    List<DataPoint> activityMinutes;
}
