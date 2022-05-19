package com.example.googlefit.googlefit.controller;


import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.example.googlefit.googlefit.model.UserDataset;
import com.example.googlefit.googlefit.service.GoogleFitServiceI;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataPointChangesResponse;
import com.google.api.services.fitness.model.ListDataSourcesResponse;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class GooglefitControler {

	@Autowired
	GoogleFitServiceI googleFitSvc;


	@RequestMapping("/index")
	public ModelAndView home() throws Exception
	{
		ModelAndView mav = new ModelAndView();
		Credential credential= googleFitSvc.getCredential();
		if(credential==null) {
			mav.setViewName("index");
		}
		else {
			mav.setViewName("dashboard");
		}
		return mav;
	}

	@GetMapping(value={"/signin"})
	public void googleSignIn(HttpServletResponse response) throws Exception {
		googleFitSvc.googleSignIn(response);
	}
	
	@GetMapping(value= {"/steps"})
	public ModelAndView saveAuthorizationCode(HttpServletRequest request) throws Exception {
		String code=request.getParameter("code");
		if(code!=null) {
			googleFitSvc.saveToken(code);
		}
		ModelAndView modelAndView = new ModelAndView();
	    modelAndView.setViewName("dashboard");
	    return modelAndView;
	}
	
	@GetMapping(value= {"/rawdatasources"})
	public ListDataSourcesResponse getDetailsDataSources() throws Exception {
		return googleFitSvc.getDetailsDataSources();

	}

	@GetMapping(value= {"/datasources"})
	public String getDataSources() throws Exception {
		return googleFitSvc.getDataSources();
	}
	
	@GetMapping(value= {"/activitytype"})
	public String getActivityTypeList() throws Exception {
		return googleFitSvc.getActivityTypeList();
	}
	
	@GetMapping(value= {"/saveandshow/datastreamid/{id}/activitytpye/{type}"})
	public String saveAndShowActivityType(@PathVariable String id, @PathVariable String type) throws Exception {
		return googleFitSvc.saveAndShowActivityTypeData(id, type);
	}

	@GetMapping(value={"/getDataStream/{id}"})
	public ListDataPointChangesResponse getDataPoints(@PathVariable String id) throws Exception {
		return googleFitSvc.getDataPointChanges(id);
	}

	@GetMapping(value={"/datasets"})
	public String getDataSets() throws Exception {
		return googleFitSvc.getDataSets();
	}
	
	@GetMapping(value= {"/getDataSets/datastreamid/{id}"})
	public  Dataset getDataSetsByFiltering(@PathVariable String id ) throws Exception {
		return googleFitSvc.getDataSetsByFiltering(id);
	}

	@GetMapping(value={"/datapointchanges"})
	public String getDataPoints() throws Exception {
		return googleFitSvc.getListOfDataPointChanges();
	}
	
	@GetMapping(value= {"/getDataPointChanges/datastreamid/{id}"})
	 public ListDataPointChangesResponse getDataPointChangesByFiltering(@PathVariable String id) throws Exception {
		return googleFitSvc.getDataPointChangesByFiltering(id);
	}
	
	@GetMapping(value= {"/datasetsbyaggregate"})
	public List<Dataset> getDataSetsByAggregate() throws Exception {
		return googleFitSvc.getDataSetsByAggregate();
	}
	
}
