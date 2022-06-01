package com.example.googlefit.googlefit.model;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@ToString
public class Point extends UserDetails {
    String name;
    String email;
    String dataTypeName;
    String startTimeDate;
    String endTimeDate;
    String modifiedTimeDate;
    String originDataSourceId;
    Double value;
}
