package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.Dataset;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserDataset extends UserDetails{
	
	Dataset dataSet;

	public Dataset getDataSet() {
		return dataSet;
	}

	public void setDataSet(Dataset dataSet) {
		this.dataSet = dataSet;
	}
}
