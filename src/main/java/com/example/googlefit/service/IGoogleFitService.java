package com.example.googlefit.service;

import com.example.googlefit.model.Point;
import com.example.googlefit.model.User;
import com.google.api.client.auth.oauth2.Credential;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IGoogleFitService {

    void saveToken(HttpServletRequest request, HttpServletResponse response, String code) throws Exception;

    Credential getCredential(HttpServletRequest request, HttpServletResponse response) throws IOException;

    void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception;

    //List<Point> getDataSetsForActivityType(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception;

    List<Point> getDataSetsForActivityType(String token,
                                           String[] activityTypes, String startDateTime, String endDateTime, String email, String name) throws Exception;

    String storeUserAllDetails(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception;

    Optional<User> getUserByEmail(String email) throws Exception;

    List<Point> getUserByEmailFitnessData(String email, String token, String activitys, String startTime, String endTime) throws Exception;
}
