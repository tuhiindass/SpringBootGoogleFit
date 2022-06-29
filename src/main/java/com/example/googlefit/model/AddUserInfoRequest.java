package com.example.googlefit.model;


import lombok.Data;

@Data
public class AddUserInfoRequest {

    private String email;
    private String name;
    private String token;
    private String refreshToken;
    private String activity;
    private String startRange;
    private String endRange;

}
