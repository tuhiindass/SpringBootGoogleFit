package com.example.googlefit.model;

import lombok.Data;

@Data
public class AddUserInfoResponse {
    String access_token;
    long expires_in;
    String scope;
    String token_type;
    String id_token;
}