package com.example.googlefit.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataSourcesResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IGoogleFitService {

    void saveToken(HttpServletRequest request, HttpServletResponse response, String code) throws Exception;

    Credential getCredential(HttpServletRequest request, HttpServletResponse response) throws IOException;

    void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception;

    List<Dataset> getDataSetsForActivityType(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception;
}
