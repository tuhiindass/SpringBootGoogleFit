package com.example.googlefit.googlefit.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.example.googlefit.googlefit.model.UserDataset;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataPointChangesResponse;
import com.google.api.services.fitness.model.ListDataSourcesResponse;

public interface GoogleFitServiceI {

	void googleSignIn(HttpServletResponse response) throws Exception;

	void saveToken(String code) throws Exception;

	ListDataSourcesResponse getDetailsDataSources() throws Exception;

	String getDataSources() throws Exception;

	UserDataset getDataSets() throws Exception;

	List<Dataset> getDataSetsByAggregate() throws Exception;

	Credential getCredential() throws IOException;

	List<ListDataPointChangesResponse> getListOfDataPointChanges() throws Exception;

	ListDataPointChangesResponse getDataPointChanges(String id) throws Exception;

	String getActivityTypeList() throws Exception;

	String saveAndShowActivityTypeData(String dataStreamId, String activityType) throws Exception;

}
