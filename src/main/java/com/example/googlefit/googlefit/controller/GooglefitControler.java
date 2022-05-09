package com.example.googlefit.googlefit.controller;


import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.googlefit.googlefit.GooglefitConstant;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.fitness.Fitness;
import com.google.api.services.fitness.Fitness.Users;
import com.google.api.services.fitness.Fitness.Users.DataSources.DataPointChanges;
import com.google.api.services.fitness.model.AggregateBucket;
import com.google.api.services.fitness.model.AggregateBy;
import com.google.api.services.fitness.model.AggregateRequest;
import com.google.api.services.fitness.model.AggregateResponse;
import com.google.api.services.fitness.model.DataPoint;
import com.google.api.services.fitness.model.DataSource;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataPointChangesResponse;
import com.google.api.services.fitness.model.ListDataSourcesResponse;

@RestController
public class GooglefitControler {

	@Autowired
	ElasticsearchRestTemplate eRestTemplate;

	private GoogleAuthorizationCodeFlow flow;

	private static final String APPLICATION_NAME = "fitNess";

	String clientId="286167874294-timc0s39bo6ernj98ujqsabqjir6rnc1.apps.googleusercontent.com";

	String clientSecret="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";

	@Value("${test.url}")
	private String callbackUrl;

	String USER_IDENTITY_KEY="userId";

	private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/fitness.activity.read");

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	HttpTransport httpTransport = new NetHttpTransport();

	@PostConstruct
	public void init() throws Exception {

		flow=new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
				.setCredentialDataStore(new MemoryDataStoreFactory().getDataStore("tokens"))
				.build();

	}



	@RequestMapping("/index")
	public ModelAndView home() throws Exception
	{
		Credential credential=flow.loadCredential(USER_IDENTITY_KEY);
		if(credential==null) {
			ModelAndView modelAndViewIndex = new ModelAndView();
		    modelAndViewIndex.setViewName("index");
		    return modelAndViewIndex;
		}
		else {
			ModelAndView modelAndViewDashboard = new ModelAndView();
			modelAndViewDashboard.setViewName("dashboard");
		    return modelAndViewDashboard;
		}
	}

	@GetMapping(value={"/googleSignin"})
	public void getFitnesData(HttpServletResponse response) throws Exception {

		GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
		String redirectUrl=url.setRedirectUri(callbackUrl).setAccessType("offline").build();
		response.sendRedirect(redirectUrl);

	}
	@GetMapping(value= {"/steps"})
	public ModelAndView saveAuthorizationCode(HttpServletRequest request) throws Exception {
		String code=request.getParameter("code");
		if(code!=null) {

			saveToken(code);

		}
		ModelAndView modelAndView = new ModelAndView();
	    modelAndView.setViewName("dashboard");
	    return modelAndView;
	}
	@GetMapping(value= {"/getDataSources"})
	public ListDataSourcesResponse getDataSources() throws Exception {
		Fitness service=fitNess();
		Fitness.Users.DataSources.List dataSources = service.users().dataSources().list("me");
//		ListDataSourcesResponse Ds=dataSources.execute();
//		System.out.println(Ds);
//		return Ds;
		OutputStream outputStream=new FileOutputStream("GoogleFitDataSources");
		//dataSources.executeAndDownloadTo(outputStream);


		//Elasticsearch->
		
//		List<DataSource> _lDs=dataSources.execute().getDataSource();
//		IndexCoordinates indices=IndexCoordinates.of("googlefit");
//		eRestTemplate.save(_lDs,indices);
        //System.out.println(_lDs);
		ListDataSourcesResponse Ds=dataSources.execute();
		return Ds;

	}

	@GetMapping(value= {"/dataSources"})
	public String dataSources() throws Exception {
		Fitness service=fitNess();
		Fitness.Users.DataSources.List dataSources = service.users().dataSources().list("me");
		ListDataSourcesResponse Ds=dataSources.execute();
		List<DataSource> dataSourcesList = Ds.getDataSource();
		String response= GooglefitConstant.HTML_BEGIN;
		for(DataSource ds:dataSourcesList) {

			response=response+"<a  href=\"/getDataStream/"+ds.getDataStreamId()+"\">"+ds.getDataStreamName()+"</a>\r\n"
					+ "		<br>";
		}
		response=response+GooglefitConstant.HTML_END;

		System.out.println(Ds);
		return response;
	}

	@GetMapping(value={"/getDataStream/{id}"})
	public ListDataPointChangesResponse dataPoints( @PathVariable String id) throws Exception {
		Fitness service=fitNess();




		String dataStreamId=id;
		Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes=service.users().dataSources().dataPointChanges().list("me", dataStreamId);
		ListDataPointChangesResponse ds=dataPointChangesRes.execute();

		return ds;

	}

	@GetMapping(value={"/getdatasets"})
	public List<Dataset> getDataSets() throws Exception {
	Fitness service=fitNess();
	ListDataSourcesResponse dataSourceRes=getDataSources();
	List<DataSource> dataSources =dataSourceRes.getDataSource();
	 List<Dataset> dataSets =new ArrayList<Dataset>();
	for(DataSource Ds:dataSources) {
		String dataStreamId=Ds.getDataStreamId();
		Fitness.Users.DataSources.Datasets.Get dataSet=service.users().dataSources().datasets().get("me", dataStreamId, "1650479400000000000-1650482111200656000");
		Dataset ds=dataSet.execute();
		dataSets.add(ds);
	}
		return dataSets;
	}

	@GetMapping(value={"/getdatapointChanges"})
	public List<ListDataPointChangesResponse> getDataPoints() throws Exception {
	Fitness service=fitNess();
	ListDataSourcesResponse dataSourceRes=getDataSources();
	List<DataSource> dataSources =dataSourceRes.getDataSource();
	List<ListDataPointChangesResponse> dataPointChanges =new ArrayList<ListDataPointChangesResponse>();
	for(DataSource Ds:dataSources) {
		String dataStreamId=Ds.getDataStreamId();
		System.out.println(dataStreamId);
		Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes=service.users().dataSources().dataPointChanges().list("me", dataStreamId);
		//OutputStream outputStream=new FileOutputStream("GoogleFitDataPoint");
		//dataPointChangesRes.executeAndDownloadTo(outputStream);
		//dataPointChanges.add(ds);
	}
		//return dataPointChanges;

		for(DataSource Ds:dataSources) {
			String dataStreamId=Ds.getDataStreamId();
			Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes=service.users().dataSources().dataPointChanges().list("me", dataStreamId);
			ListDataPointChangesResponse ds=dataPointChangesRes.execute();
			dataPointChanges.add(ds);
		}
		return dataPointChanges;

	}
	@GetMapping(value= {"/getdatasetsbyaggregate"})
	public List<Dataset> getDataSetsByAggregate() throws Exception {
		Fitness service=fitNess();
		Fitness.Users.DataSources.List ds = service.users().dataSources().list("me");
		ListDataSourcesResponse dataSourceList = ds.execute();
		List<DataSource> dataSources = dataSourceList.getDataSource();
		System.out.println("Data Sources: "+dataSources);
		List<Dataset> dataSets =new ArrayList<Dataset>();
			for(DataSource Ds:dataSources) {
					String dataStreamId=Ds.getDataStreamId();
					System.out.println("Data Stream Id:"+dataStreamId);
					AggregateRequest aggregateRequest = new AggregateRequest();
			        aggregateRequest.setAggregateBy(Collections.singletonList(
			                new AggregateBy()
			                        .setDataSourceId(dataStreamId)));
			        aggregateRequest.setStartTimeMillis(DateMidnight.now().getMillis());
			        aggregateRequest.setEndTimeMillis(DateTime.now().getMillis());
			        Fitness.Users.Dataset.Aggregate aggregaterequest = service.users().dataset().aggregate("me", aggregateRequest);
			        AggregateResponse response = aggregaterequest.execute();
			        List<AggregateBucket> aggregateData = response.getBucket();
			     	for(AggregateBucket bucket:aggregateData) {
			    	 List<Dataset> dataset = bucket.getDataset();
			    	 dataSets.addAll(dataset);

			     	}
				}
				System.out.println("Datasets: "+dataSets);
				 return dataSets;

	      }
	private Fitness fitNess() throws Exception {
		Credential credential=flow.loadCredential(USER_IDENTITY_KEY);
		System.out.println("Credential for accessToken: "+credential);
		Fitness service = new Fitness.Builder(
                httpTransport,
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
		return service;
	}

	private void saveToken(String code) throws Exception {
	GoogleTokenResponse response=flow.newTokenRequest(code).setRedirectUri(callbackUrl).execute();
	flow.createAndStoreCredential(response, USER_IDENTITY_KEY);

}
}
