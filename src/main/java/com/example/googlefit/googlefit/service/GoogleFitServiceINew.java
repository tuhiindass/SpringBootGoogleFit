package com.example.googlefit.googlefit.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataSourcesResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface GoogleFitServiceINew {

    void saveToken(String code, HttpServletRequest request, HttpServletResponse response) throws Exception;

    Credential getCredential() throws IOException;

    void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception;


    ListDataSourcesResponse getDetailsDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception;

    Dataset getDataSetsByFiltering(HttpServletRequest request, HttpServletResponse response, String id, String type,
                                   String startDateTime, String endDateTime) throws Exception;

    List<Dataset> getDataSetsForActivityType(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception;
}
