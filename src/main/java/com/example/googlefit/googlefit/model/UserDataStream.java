package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.ListDataPointChangesResponse;
import com.google.gson.JsonObject;

public class UserDataStream extends UserDetails{
	ListDataPointChangesResponse dataStream;

	public ListDataPointChangesResponse getDataStream() {
		return dataStream;
	}

	public void setDataStream(ListDataPointChangesResponse dataStream) {
		this.dataStream = dataStream;
	}
}
