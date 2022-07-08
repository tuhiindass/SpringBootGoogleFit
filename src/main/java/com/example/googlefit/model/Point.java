package com.example.googlefit.model;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Data
@Slf4j
@ToString
public class Point {
    String name;
    String email;
    String dataTypeName;
    String startTimeDate;
    String endTimeDate;
    String modifiedTimeDate;
    String originDataSourceId;
    ArrayList<Double> value;

    public ArrayList<Double> getValue() {
        return value;
    }

    public void setValue(ArrayList<Double> value) {
        this.value = value;
    }
}
