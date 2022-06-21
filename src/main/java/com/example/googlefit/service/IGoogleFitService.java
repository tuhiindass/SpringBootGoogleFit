package com.example.googlefit.service;

import com.example.googlefit.model.Point;
import com.google.api.client.auth.oauth2.Credential;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface IGoogleFitService {

    void saveToken(HttpServletRequest request, HttpServletResponse response, String code) throws Exception;

    Credential getCredential(HttpServletRequest request, HttpServletResponse response) throws IOException;

    void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception;

    List<Point> getDataSetsForActivityType(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception;

    String storeUserAllDetails(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception;
}
