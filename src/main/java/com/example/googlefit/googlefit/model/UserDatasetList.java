package com.example.googlefit.googlefit.model;

import java.util.List;

import com.google.api.services.fitness.model.Dataset;


public class UserDatasetList extends UserDetails{
	List<Dataset> _lDataSet;

	public List<Dataset> get_lDataSet() {
		return _lDataSet;
	}

	public void set_lDataSet(List<Dataset> _lDataSet) {
		this._lDataSet = _lDataSet;
	}
}
