package com.example.googlefit.googlefit.service.Impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import com.example.googlefit.googlefit.GooglefitConstant;
import com.example.googlefit.googlefit.model.UserDataset;
import com.example.googlefit.googlefit.service.GoogleFitServiceI;
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
import com.google.api.services.fitness.model.AggregateBucket;
import com.google.api.services.fitness.model.AggregateBy;
import com.google.api.services.fitness.model.AggregateRequest;
import com.google.api.services.fitness.model.AggregateResponse;
import com.google.api.services.fitness.model.DataSource;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataPointChangesResponse;
import com.google.api.services.fitness.model.ListDataSourcesResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleFitServiceImpl implements GoogleFitServiceI{
	
	@Autowired
	ElasticsearchRestTemplate eRestTemplate;
	
	@Value("${test.url}")
	private String callbackUrl;
	
	@Value("${clientId}")
	private String clientId;
	
	@Value("${clientSecret}")
	private String clientSecret;
	
	@Value("${base.url}")
	private String baseUrl;
	
	private GoogleAuthorizationCodeFlow flow;
	
	private static final String APPLICATION_NAME = "fitNess";
	private static final String USER_IDENTITY_KEY="userId";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	HttpTransport httpTransport = new NetHttpTransport();
	
	private static final List<String> SCOPES = Arrays.asList(
			"https://www.googleapis.com/auth/fitness.activity.read",
			"https://www.googleapis.com/auth/fitness.blood_glucose.read",
			"https://www.googleapis.com/auth/fitness.blood_pressure.read",
			"https://www.googleapis.com/auth/fitness.body.read",
			"https://www.googleapis.com/auth/fitness.body_temperature.read",
			"https://www.googleapis.com/auth/fitness.heart_rate.read",
			"https://www.googleapis.com/auth/fitness.location.read",
			"https://www.googleapis.com/auth/fitness.nutrition.read",
			"https://www.googleapis.com/auth/fitness.oxygen_saturation.read",
			"https://www.googleapis.com/auth/fitness.reproductive_health.read",
			"https://www.googleapis.com/auth/fitness.sleep.read",
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile");

	UserDataset userDataset=new UserDataset();
	
	@PostConstruct
	public void init() throws Exception {
		flow=new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
				.setCredentialDataStore(new MemoryDataStoreFactory().getDataStore("tokens"))
				.build();
	}

	@Override
	public void googleSignIn(HttpServletResponse response) throws Exception{
		GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
		String redirectUrl=url.setRedirectUri(callbackUrl).setAccessType("offline").build();
		response.sendRedirect(redirectUrl);
	}
	
	@Override
	public void saveToken(String code) throws Exception {
		GoogleTokenResponse response=flow.newTokenRequest(code).setRedirectUri(callbackUrl).execute();
		flow.createAndStoreCredential(response, USER_IDENTITY_KEY);
		System.out.println(response.getAccessToken());
		HttpClient client = HttpClient.newHttpClient();
		
		String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + response.getAccessToken();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("accept", "application/json")
				.build();
		
		HttpResponse res = client.send(request, BodyHandlers.ofString());
		System.out.println(res.body());
		JsonObject userData = JsonParser.parseString(res.body().toString()).getAsJsonObject();
		userDataset.setName(userData.get("name").getAsString());
		userDataset.setEmail(userData.get("email").getAsString());
		System.out.println(userData.get("name").getAsString());
		System.out.println(userData.get("email").getAsString());
	}
	
	@Override
	public Credential getCredential() throws IOException {
		return flow.loadCredential(USER_IDENTITY_KEY);
	}
	
	@Override
	public ListDataSourcesResponse getDetailsDataSources() throws Exception {
		Fitness service=fitNess();
		Fitness.Users.DataSources.List dataSources = service.users().dataSources().list("me");
		List<DataSource> _lDs=dataSources.execute().getDataSource();
//		userDataset.set_lDataSource(_lDs);
//		IndexCoordinates indices=IndexCoordinates.of("datasources");
//		System.out.println(userDataset.toString());
//		eRestTemplate.save(userDataset,indices);

		//log.info("DataSource saved into Elasticsearch.");
		ListDataSourcesResponse Ds=dataSources.execute();
		return Ds;
	}
	
	@Override
	public String getDataSources() throws Exception {
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
	
	@Override
	public String getActivityTypeList() throws Exception {
		Fitness service=fitNess();
		List<DataSource> dataSourcesList = service.users().dataSources().list("me").execute().getDataSource();
		String response= GooglefitConstant.HTML_BEGIN;
		String activityName = null;
		for(DataSource ds:dataSourcesList) {
			if(ds.getDataStreamName().equals("top_level")) {
				activityName = ds.getDataType().getName().substring(11,  ds.getDataType().getName().length());
				response=response+"<a  href=\"/saveandshow/datastreamid/"+ds.getDataStreamId()+"/activitytpye/"+ activityName +"\">"+activityName+"</a>\r\n"
						+ "		<br>";
			}
		}
		response=response+GooglefitConstant.HTML_END;
		return response;
	}
	
	@Override
	public String saveAndShowActivityTypeData(String dataStreamId, String activityType) throws Exception {
		System.out.println("\n\nDataStreamid : " + dataStreamId);
		HttpClient client = HttpClient.newHttpClient();
		
		String url = baseUrl + "/getDataStream/" + dataStreamId;
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("accept", "application/json")
				.build();
		
		HttpResponse res = client.send(request, BodyHandlers.ofString());
		JsonObject dataStream = JsonParser.parseString(res.body().toString()).getAsJsonObject();
		userDataset.setDataStream(dataStream);
		IndexCoordinates indices=IndexCoordinates.of(activityType);
		eRestTemplate.save(userDataset,indices);
		return res.body().toString();
	}
	
	@Override
	public ListDataPointChangesResponse getDataPointChanges(String id) throws Exception {
		Fitness service=fitNess();
		String dataStreamId=id;
		Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes=service.users().dataSources().dataPointChanges().list("me", dataStreamId);
		ListDataPointChangesResponse ds=dataPointChangesRes.execute();
		return ds;
	}
	
	@Override
	public String getDataSets() throws Exception {
		//log.info("Inside getDataSets()");
		Fitness service=fitNess();
		List<DataSource> dataSources =getDetailsDataSources().getDataSource();
				String response= GooglefitConstant.HTML_BEGIN;
		String activityDataType = null;
		for(DataSource Ds:dataSources) {
			if(Ds.getDataStreamName().equals("top_level")) {
				System.out.println("DataStreamName:"+Ds.getDataStreamName());
				activityDataType=Ds.getDataType().getName();
				System.out.println("DataType:"+activityDataType);
				response=response+"<a  href=\"/getDataSets/datastreamid/"+Ds.getDataStreamId()+"\">"+activityDataType+"</a>\r\n"
						+ "		<br>";
			}
			
		}
		response=response+GooglefitConstant.HTML_END;
		return response;
		
		//log.info("Datasets extracted from GoogleFit");
//		userDataset.set_lDataSet(dataSets);
//		IndexCoordinates indices=IndexCoordinates.of("datasets");
//		System.out.println(userDataset.toString());
//		eRestTemplate.save(userDataset,indices);

		//log.info("DataSets saved into Elasticsearch.");
//		return userDataset;
		//return dataSets;
	}
	
	@Override
	public Dataset getDataSetsByFiltering(String id) throws Exception {
		// TODO Auto-generated method stub
		Fitness service=fitNess();
		String startTimeString= String.valueOf(new DateTime().withTimeAtStartOfDay().getMillis()*1000000);
		String endTimeString=String.valueOf(DateTime.now().getMillis()*1000000);
		String datasetId=startTimeString+"-"+endTimeString;
		System.out.println("datasetId: "+datasetId);
		Fitness.Users.DataSources.Datasets.Get dataSet=service.users().dataSources().datasets().get("me", id, datasetId);
		Dataset ds=dataSet.execute();
		return ds;
	}
	
	
	
	@Override
	public String getListOfDataPointChanges() throws Exception {
		Fitness service=fitNess();
		List<DataSource> dataSources =getDetailsDataSources().getDataSource();
		String response= GooglefitConstant.HTML_BEGIN;
		String activityDataType = null;

		for(DataSource Ds:dataSources) {
			if(Ds.getDataStreamName().equals("top_level")) {
				System.out.println("DataStreamName:"+Ds.getDataStreamName());
				activityDataType=Ds.getDataType().getName();
				System.out.println("DataType:"+activityDataType);
				response=response+"<a  href=\"/getDataPointChanges/datastreamid/"+Ds.getDataStreamId()+"\">"+activityDataType+"</a>\r\n"
						+ "		<br>";
			}
		}
		response=response+GooglefitConstant.HTML_END;
		return response;

	}
	
	@Override
	public ListDataPointChangesResponse getDataPointChangesByFiltering(String id) throws Exception {
		// TODO Auto-generated method stub
		Fitness service=fitNess();
		String dataStreamId=id;
		Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes=service.users().dataSources().dataPointChanges().list("me", dataStreamId);
		ListDataPointChangesResponse ds=dataPointChangesRes.execute();
		return ds;

	}

	
	@Override
	public List<Dataset> getDataSetsByAggregate() throws Exception {
		Fitness service=fitNess();
		Fitness.Users.DataSources.List ds = service.users().dataSources().list("me");
		List<DataSource> dataSources = ds.execute().getDataSource();
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

	
	

	
}