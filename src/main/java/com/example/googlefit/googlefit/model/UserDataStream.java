package com.example.googlefit.googlefit.model;

import com.google.gson.JsonObject;

public class UserDataStream extends UserDetails{
	JsonObject dataStream;

	public JsonObject getDataStream() {
		return dataStream;
	}

	public void setDataStream(JsonObject dataStream) {
		this.dataStream = dataStream;
	}
}
