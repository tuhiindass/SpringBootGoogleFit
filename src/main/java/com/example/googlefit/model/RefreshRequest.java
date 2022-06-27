package com.example.googlefit.model;

import lombok.Data;

@Data
public class RefreshRequest {
    String client_id;
    String client_secret;
    String refresh_token;
    String grant_type;
}
