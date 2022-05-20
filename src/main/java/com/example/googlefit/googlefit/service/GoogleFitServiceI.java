package com.example.googlefit.googlefit.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.googlefit.googlefit.model.UserDataset;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataPointChangesResponse;
import com.google.api.services.fitness.model.ListDataSourcesResponse;

public interface GoogleFitServiceI {

	void saveToken(String code, HttpServletRequest request, HttpServletResponse response) throws Exception;

	ListDataSourcesResponse getDetailsDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception;

	String getDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception;

	UserDataset getDataSets(HttpServletRequest request, HttpServletResponse response) throws Exception;

	List<Dataset> getDataSetsByAggregate(HttpServletRequest request, HttpServletResponse response) throws Exception;

	Credential getCredential() throws IOException;

	List<ListDataPointChangesResponse> getListOfDataPointChanges(HttpServletRequest request, HttpServletResponse response) throws Exception;

	ListDataPointChangesResponse getDataPointChanges(HttpServletRequest request, HttpServletResponse response, String id) throws Exception;

	String getActivityTypeList(HttpServletRequest request, HttpServletResponse response) throws Exception;

	String saveAndShowActivityTypeData(HttpServletRequest request, HttpServletResponse response, String dataStreamId, String activityType) throws Exception;

	void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
