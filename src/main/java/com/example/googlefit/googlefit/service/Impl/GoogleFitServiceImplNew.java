package com.example.googlefit.googlefit.service.Impl;

import com.example.googlefit.googlefit.GooglefitConstant;
import com.example.googlefit.googlefit.model.UserDataSourceList;
import com.example.googlefit.googlefit.model.UserDetails;
import com.example.googlefit.googlefit.service.GoogleFitServiceINew;
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
import com.google.api.services.fitness.model.DataSource;
import com.google.api.services.fitness.model.Dataset;
import com.google.api.services.fitness.model.ListDataSourcesResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class GoogleFitServiceImplNew implements GoogleFitServiceINew {

    @Autowired
    ElasticsearchRestTemplate eRestTemplate;

    @Value("${test.url}")
    private String callbackUrl;

    @Value("${clientId}")
    private String clientId;

    @Value("${clientSecret}")
    private String clientSecret;

    @Value("${session.timeout}")
    private int sessionLife;

    @Value("${test.DatabaseName}")
    private String DatabaseName;

    private GoogleAuthorizationCodeFlow flow;

    private static final String APPLICATION_NAME = "fitNess";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String GFITLOGINUSEREMAIL = "gfitLoginUserEmail";

    HttpTransport httpTransport = new NetHttpTransport();

    private static long convertToMillis = 1000000;
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

    UserDetails userDetails = new UserDetails();

    @PostConstruct
    public void init() {
        try {
            flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
                    .setCredentialDataStore(new MemoryDataStoreFactory().getDataStore("tokens"))
                    .build();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectUrl = url.setRedirectUri(callbackUrl).setAccessType("offline").build();
        response.sendRedirect(redirectUrl);
    }

    @Override
    public void saveToken(String code, HttpServletRequest httpReq, HttpServletResponse httpRes) throws Exception {
        log.info("Inside saveToken");
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(callbackUrl).execute();

        HttpClient client = HttpClient.newHttpClient();
        String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + response.getAccessToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("accept", "application/json")
                .build();

        HttpResponse res = client.send(request, BodyHandlers.ofString());
        System.out.println(res.body());
        JsonObject userData = JsonParser.parseString(res.body().toString()).getAsJsonObject();
        userDetails.setName(userData.get("name").getAsString());
        userDetails.setEmail(userData.get("email").getAsString());
        System.out.println(userData.get("name").getAsString());
        System.out.println(userData.get("email").getAsString());

        flow.createAndStoreCredential(response, userDetails.getEmail());
        System.out.println(response.getAccessToken());

        String user = httpReq.getParameter(GFITLOGINUSEREMAIL);
        if (user == null) {
            Cookie loginCookie = new Cookie(GFITLOGINUSEREMAIL, userDetails.getEmail());
//			setting cookie to expiry in 20 mins
            loginCookie.setMaxAge(sessionLife);
            httpRes.addCookie(loginCookie);
            httpRes.sendRedirect("/dashboard");
        }
    }


    @Override
    public Credential getCredential() throws IOException {
        log.info("Inside getCredential");
        return flow.loadCredential(userDetails.getEmail());
    }

    @Override
    public ListDataSourcesResponse getDetailsDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //log.info("Inside getDetailsDataSources");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            Fitness.Users.DataSources.List dataSources = service.users().dataSources().list("me");
            List<DataSource> _lDs = dataSources.execute().getDataSource();

            /* ElasticDB upload */
            UserDataSourceList dataSourceList = new UserDataSourceList();
            dataSourceList.set_lDataSource(_lDs);
            // IndexCoordinates indices = IndexCoordinates.of("datasources");
            // System.out.println(dataSourceList.toString());
            //eRestTemplate.save(dataSourceList, indices);
            //log.info("DataSource saved into Elasticsearch.");
            ListDataSourcesResponse Ds = dataSources.execute();
            return Ds;
        } else {
            response.sendRedirect("/signin");
        }
        return null;
    }


    @Override
    public Dataset getDataSetsByFiltering(HttpServletRequest request, HttpServletResponse response, String id, String type, String startDateTime, String endDateTime) throws Exception {
        //log.info("Inside getDataSetsByFiltering");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            String startTimeString = startDateTime;
            String endTimeString = endDateTime;

            String datasetId = startTimeString + "-" + endTimeString;
            // System.out.println("datasetId: " + datasetId);
            Fitness.Users.DataSources.Datasets.Get dataSet = service.users().dataSources().datasets().get("me", id, datasetId);
            Dataset ds = dataSet.execute();


            /* ElasticDB upload */
//            Long maxEndTimeNs = ds.getMaxEndTimeNs();
//            Long maxEndTimeMilli = maxEndTimeNs / convertToMillis;
//            //System.out.println("maxEndTime:" + maxEndTimeMilli);
//            ds.setMaxEndTimeNs(maxEndTimeMilli);
//            Long minStartTimeMili = ds.getMinStartTimeNs() / convertToMillis;
//            ds.setMinStartTimeNs(minStartTimeMili);
//            List<DataPoint> dataPoint = ds.getPoint();
//            for (DataPoint dp : dataPoint) {
//                Long endTimenanos = dp.getEndTimeNanos();
//                Long startTimenanos = dp.getStartTimeNanos();
//                long startTimeMillis = startTimenanos / convertToMillis;
//                long endTimeMillis = endTimenanos / convertToMillis;
//                dp.setStartTimeNanos(startTimeMillis);
//                dp.setEndTimeNanos(endTimeMillis);
//
//            }
//
//            // List<DataPoint> dataPoint = ds.getPoint();
//            Point point = new Point();
//            for (DataPoint dp : dataPoint) {
//
//                point.setName(userDetails.getName());
//                point.setEmail(userDetails.getEmail());
//                point.setDataTypeName(dp.getDataTypeName());
//                point.setOriginDataSourceId(dp.getOriginDataSourceId());
//                point.setStartTimeDate(dp.getStartTimeNanos().toString());
//                point.setEndTimeDate(dp.getEndTimeNanos().toString());
//                point.setModifiedTimeDate(dp.getModifiedTimeMillis().toString());
//                for (com.google.api.services.fitness.model.Value va : dp.getValue()) {
//                    //   System.out.println(va.getFpVal());
//                    if (va.getFpVal() != null) {
//                        point.setValue(va.getFpVal());
//                    } else if (va.getIntVal() != null) {
//                        point.setValue(Double.valueOf(va.getIntVal()));
//
//                    }
//                }
//
//                IndexCoordinates indices = IndexCoordinates.of(DatabaseName);
//
//                eRestTemplate.save(point, indices);
//
//            }
//            log.info("Points saved into Elasticsearch.");
            return ds;
        } else {
            response.sendRedirect("/signin");
        }
        return null;
    }


    @Override
    public List<Dataset> getDataSetsForActivityType(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception {
        String endTimeString = null;
        String startTimeString = null;
        if (StringUtils.isEmpty(startDateTime) || StringUtils.isEmpty(endDateTime)) {
            startTimeString = String.valueOf(new DateTime().withTimeAtStartOfDay().getMillis() * convertToMillis);
            endTimeString = String.valueOf(DateTime.now().getMillis() * convertToMillis);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date startdate = sdf.parse(startDateTime.replace("T", " "));
            Date enddate = sdf.parse(endDateTime.replace("T", " "));
            long startTimeNanos = startdate.getTime() * convertToMillis;
            long endTimeNanos = enddate.getTime() * convertToMillis;
            startTimeString = String.valueOf(startTimeNanos);
            endTimeString = String.valueOf(endTimeNanos);

        }
        if (activityTypes.length > 0) {
            List<DataSource> dataSourceList = getDetailsDataSources(request, response).getDataSource();
            List<String> activityDataTypesList = new ArrayList<>();
            for (String activityType : activityTypes) {
                switch (activityType) {
                    case "Health":
                        activityDataTypesList.addAll(GooglefitConstant.HEALTH_DATATYPES);
                        break;
                    case "Activity":
                        activityDataTypesList.addAll(GooglefitConstant.ACTIVITY_DATATYPES);
                        break;
                    case "Location":
                        activityDataTypesList.addAll(GooglefitConstant.LOCATION_DATATYPES);
                        break;
                    case "Nutrition":
                        activityDataTypesList.addAll(GooglefitConstant.NUTRITION_DATATYPES);
                        break;
                    default:
                        break;
                }
            }
            List<Dataset> datasetList = new ArrayList<>();
            for (DataSource dataSource : dataSourceList) {
                if (activityDataTypesList.contains(dataSource.getDataType().getName())) {
                    datasetList.add(getDataSetsByFiltering(request, response, dataSource.getDataStreamId(), dataSource.getDataType().getName(), startTimeString, endTimeString));
                }
            }
            return datasetList;
        }
        return null;
    }

    private Fitness fitNess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //log.info("Inside fitNess");
        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(GFITLOGINUSEREMAIL)) {
                loginCookie = cookie;
                break;
            }
        }
        Credential credential = flow.loadCredential(loginCookie.getValue());
        // System.out.println("AccessToken: " + credential.getAccessToken());
        Fitness service = new Fitness.Builder(
                httpTransport,
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        return service;
    }

    private boolean checkCookieLife(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //log.info("Inside checkCookieLife");
        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(GFITLOGINUSEREMAIL)) {
                    loginCookie = cookie;
                    break;
                }
            }
            if (loginCookie != null) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
