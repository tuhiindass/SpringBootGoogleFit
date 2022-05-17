package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.DataSource;
import com.google.api.services.fitness.model.Dataset;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class UserDataset {
	String name;
	String email;
	List<Dataset> _lDataSet;
	List<DataSource> _lDataSource;
}
