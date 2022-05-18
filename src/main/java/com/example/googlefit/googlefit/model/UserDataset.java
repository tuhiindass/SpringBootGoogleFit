package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.DataSource;
import com.google.api.services.fitness.model.Dataset;
import com.google.gson.JsonObject;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class UserDataset {
	String name;
	String email;
	List<Dataset> _lDataSet;
	List<DataSource> _lDataSource;
	JsonObject dataStream;
	
	public JsonObject getDataStream() {
		return dataStream;
	}
	public void setDataStream(JsonObject dataStream) {
		this.dataStream = dataStream;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public List<Dataset> get_lDataSet() {
		return _lDataSet;
	}
	public void set_lDataSet(List<Dataset> _lDataSet) {
		this._lDataSet = _lDataSet;
	}
	public List<DataSource> get_lDataSource() {
		return _lDataSource;
	}
	public void set_lDataSource(List<DataSource> _lDataSource) {
		this._lDataSource = _lDataSource;
	}
	
	
}
