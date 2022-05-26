package com.example.googlefit.googlefit.model;

import java.util.List;

import com.google.api.services.fitness.model.DataSource;

public class UserDataSourceList extends UserDetails{
	List<DataSource> _lDataSource;

	public List<DataSource> get_lDataSource() {
		return _lDataSource;
	}

	public void set_lDataSource(List<DataSource> _lDataSource) {
		this._lDataSource = _lDataSource;
	}
}
