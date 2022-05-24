package com.example.googlefit.googlefit.controller;


import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	public void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception {
		googleFitSvc.googleSignIn(request, response);
	}
	
	@GetMapping(value= {"/steps"})
	public ModelAndView saveAuthorizationCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String code=request.getParameter("code");
		if(code!=null) {
			googleFitSvc.saveToken(code, request, response);
		}
		ModelAndView modelAndView = new ModelAndView();
	    modelAndView.setViewName("dashboard");
	    return modelAndView;
	}
	
	@GetMapping(value= {"/dashboard"})
	public ModelAndView dashboard(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView modelAndView = new ModelAndView();
	    modelAndView.setViewName("dashboard");
	    return modelAndView;
	}
	
	@GetMapping(value= {"/rawdatasources"})
	public ListDataSourcesResponse getDetailsDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return googleFitSvc.getDetailsDataSources(request, response);

	}

	@GetMapping(value= {"/datasources"})
	public String getDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return googleFitSvc.getDataSources(request, response);
	}
	
	@GetMapping(value= {"/activitytype"})
	public String getActivityTypeList(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return googleFitSvc.getActivityTypeList(request, response);
	}
	
	@GetMapping(value= {"/saveandshow/datastreamid/{id}/activitytpye/{type}"})
	public String saveAndShowActivityType(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable String id, @PathVariable String type) throws Exception {
		return googleFitSvc.saveAndShowActivityTypeData(request, response, id, type);
	}

	@GetMapping(value={"/getDataStream/{id}"})
	public ListDataPointChangesResponse getDataPoints(HttpServletRequest request, HttpServletResponse response,
			@PathVariable String id) throws Exception {
		return googleFitSvc.getDataPointChanges(request, response, id);
	}

	@GetMapping(value={"/datasets"})
	public String getDataSets(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return googleFitSvc.getDataSets(request, response, null, null);
	}
	
	
	
	@GetMapping(value= {"/timeforDataSets"})
	public ModelAndView getTimeForDataSets(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView modelAndView = new ModelAndView();
	    modelAndView.setViewName("form");
	    return modelAndView;
	}
	@PostMapping(value= {"/datasets"})
	public String form(HttpServletRequest request, HttpServletResponse response, @RequestParam("startTime") String userEmail, @RequestParam("endTime") String pass) throws Exception{
		return googleFitSvc.getDataSets(request, response, userEmail, pass);
	}
	@GetMapping(value= {"/getDataSets/datastreamid/{id}/activityType/{type}/startDateTime/{startDateTime}/endDateTime/{endDateTime}"})
	public  Dataset getDataSetsByFiltering(HttpServletRequest request, HttpServletResponse response, @PathVariable String id, @PathVariable String type, @PathVariable String startDateTime, @PathVariable String endDateTime) throws Exception {
		return googleFitSvc.getDataSetsByFiltering(request, response, id, type, startDateTime, endDateTime);
	}

	@GetMapping(value={"/datapointchanges"})
	public String getDataPoints(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return googleFitSvc.getListOfDataPointChanges(request, response);
	}
	
	@GetMapping(value= {"/getDataPointChanges/datastreamid/{id}/activityType/{type}"})
	 public ListDataPointChangesResponse getDataPointChangesByFiltering(HttpServletRequest request, HttpServletResponse response, @PathVariable String id, @PathVariable String type) throws Exception {
		return googleFitSvc.getDataPointChangesByFiltering(request, response, id, type);
	}
	
	@GetMapping(value= {"/datasetsbyaggregate"})
	public List<Dataset> getDataSetsByAggregate(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return googleFitSvc.getDataSetsByAggregate(request, response);
	}
	
}
