package com.example.googlefit.googlefit.model;

import com.google.api.services.fitness.model.Dataset;

import java.util.List;


public class UserDatasetList extends UserDetails {
    List<Dataset> _lDataSet;

    public List<Dataset> get_lDataSet() {
        return _lDataSet;
    }

    public void set_lDataSet(List<Dataset> _lDataSet) {
        this._lDataSet = _lDataSet;
    }
}
