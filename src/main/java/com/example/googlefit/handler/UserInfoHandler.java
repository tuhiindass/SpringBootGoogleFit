package com.example.googlefit.handler;

import com.example.googlefit.model.AddUserInfoRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class UserInfoHandler {
    @Value("${api.userinfo.baseurl}")
    String url;

    RestTemplate restTemplate = new RestTemplate();

    public void addUserInfo(AddUserInfoRequest infoRequest, String action) {
        //creating URL
        UriComponentsBuilder uribuilder = UriComponentsBuilder.fromUriString(url + "/user_info")
                .queryParam("action", action)
                .queryParam("name", infoRequest.getName());

        //Calling API
        ResponseEntity<String> response = restTemplate
                .postForEntity(uribuilder.build().toUri(), infoRequest, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("User Added/updated successfully " + response.getBody());
        } else {
            log.error("Error occured " + response.getBody());
        }

    }

    /*public void getUserInfo(String action,String name){
        UriComponentsBuilder uribuilder = UriComponentsBuilder.fromUriString(url+"/user_info")
                .queryParam("action", action)
                .queryParam("name", name.replace(" ","-"));

        //Calling API
        ResponseEntity<String> response = restTemplate
                .getForEntity(uribuilder.build().toUri(), String.class);

        if(response.getStatusCode()== HttpStatus.OK){
            log.info("User details received successfully "+response.getBody());
        }else {
            log.error("Error occured "+response.getBody());
        }
    }*/
}
