package com.example.googlefit.googlefit.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataPointChangesResponse;
import com.google.api.services.fitness.model.ListDataSourcesResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface GoogleFitServiceI {

    void saveToken(String code, HttpServletRequest request, HttpServletResponse response) throws Exception;

    ListDataSourcesResponse getDetailsDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception;

    String getDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception;

    String getDataSets(HttpServletRequest request, HttpServletResponse response) throws Exception;

    List<Dataset> getDataSetsByAggregate(HttpServletRequest request, HttpServletResponse response) throws Exception;

    Credential getCredential() throws IOException;

    String getListOfDataPointChanges(HttpServletRequest request, HttpServletResponse response) throws Exception;

    ListDataPointChangesResponse getDataPointChanges(HttpServletRequest request, HttpServletResponse response, String id) throws Exception;

    String getActivityTypeList(HttpServletRequest request, HttpServletResponse response) throws Exception;

    ListDataPointChangesResponse saveAndShowActivityTypeData(HttpServletRequest request, HttpServletResponse response, String dataStreamId, String activityType) throws Exception;

    void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception;

    Dataset getDataSetsByFiltering(HttpServletRequest request, HttpServletResponse response, String id, String type)
            throws Exception;

    ListDataPointChangesResponse getDataPointChangesByFiltering(HttpServletRequest request,
                                                                HttpServletResponse response, String id, String type) throws Exception;

}
