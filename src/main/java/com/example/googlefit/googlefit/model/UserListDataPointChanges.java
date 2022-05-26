package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.ListDataPointChangesResponse;

public class UserListDataPointChanges extends UserDetails{
	ListDataPointChangesResponse listDataPointChangesRes;

	public ListDataPointChangesResponse getListDataPointChangesRes() {
		return listDataPointChangesRes;
	}

	public void setListDataPointChangesRes(ListDataPointChangesResponse listDataPointChangesRes) {
		this.listDataPointChangesRes = listDataPointChangesRes;
	}
}
