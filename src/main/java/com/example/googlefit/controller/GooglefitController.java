package com.example.googlefit.controller;


import com.example.googlefit.service.IGoogleFitService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.model.Dataset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@Slf4j
public class GooglefitController {

    @Autowired
    IGoogleFitService googleFitSvc;


    @RequestMapping("/index")
    public ModelAndView home(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView();
        Credential credential = googleFitSvc.getCredential(request, response);
        if (credential == null) {
            mav.setViewName("index");
        } else {
            mav.setViewName("show-data");
        }
        return mav;
    }

    @GetMapping(value = {"/signin"})
    public void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception {
        googleFitSvc.googleSignIn(request, response);
    }

    @GetMapping(value = {"/steps"})
    public ModelAndView saveAuthorizationCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String code = request.getParameter("code");
        if (code != null) {
            googleFitSvc.saveToken(request, response, code);
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("show-data");
        return modelAndView;
    }

    @GetMapping(value = {"/dashboard"})
    public ModelAndView dashboard(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("show-data");
        return modelAndView;
    }

    @PostMapping(value = "/getDataSetsForActivityType")
    public List<Dataset> getDataSetsForActivityType(HttpServletRequest request, HttpServletResponse response, @RequestParam("activityType") String[] activityTypes, @RequestParam("startDateTime") String startDateTime, @RequestParam("endDateTime") String endDateTime) throws Exception {
        return googleFitSvc.getDataSetsForActivityType(request, response, activityTypes, startDateTime, endDateTime);
    }
}
