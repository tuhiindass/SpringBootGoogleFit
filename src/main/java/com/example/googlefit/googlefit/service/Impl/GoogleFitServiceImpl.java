package com.example.googlefit.googlefit.service.Impl;

import com.example.googlefit.googlefit.GooglefitConstant;
import com.example.googlefit.googlefit.model.*;
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
import com.google.api.services.fitness.model.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class GoogleFitServiceImpl implements GoogleFitServiceI {

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
        log.info("Inside getDetailsDataSources");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            Fitness.Users.DataSources.List dataSources = service.users().dataSources().list("me");
            List<DataSource> _lDs = dataSources.execute().getDataSource();

            /* ElasticDB upload */
            UserDataSourceList dataSourceList = new UserDataSourceList();
            dataSourceList.set_lDataSource(_lDs);
            IndexCoordinates indices = IndexCoordinates.of("datasources");
            System.out.println(dataSourceList.toString());
            eRestTemplate.save(dataSourceList, indices);
            log.info("DataSource saved into Elasticsearch.");
            ListDataSourcesResponse Ds = dataSources.execute();
            return Ds;
        } else {
            response.sendRedirect("/signin");
        }
        return null;
    }

    @Override
    public String getDataSources(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Inside getDataSources");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            Fitness.Users.DataSources.List dataSources = service.users().dataSources().list("me");
            ListDataSourcesResponse Ds = dataSources.execute();
            List<DataSource> dataSourcesList = Ds.getDataSource();
            String res = GooglefitConstant.HTML_BEGIN;
            for (DataSource ds : dataSourcesList) {
                res = res + "<a  href=\"/getDataStream/" + ds.getDataStreamId() + "\">" + ds.getDataStreamName() + "</a>\r\n"
                        + "		<br>";
            }
            res = res + GooglefitConstant.HTML_END;

            System.out.println(Ds);
            return res;
        } else {
            response.sendRedirect("/signin");
        }
        return null;

    }

    @Override
    public String getActivityTypeList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Inside getActivityTypeList");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            List<DataSource> dataSourcesList = service.users().dataSources().list("me").execute().getDataSource();
            String res = GooglefitConstant.HTML_BEGIN;
            String activityName = null;
            for (DataSource ds : dataSourcesList) {
                if (!ds.getDataStreamName().equals("top_level") && !ds.getDataStreamName().equals("user_input")) {

                    String name = null;

                    if (ds.getDataStreamName().equals("merged")) {
                        name = ds.getDataType().getName().substring(11, ds.getDataType().getName().length());

                    } else
                        name = ds.getDataStreamName().replace("merge_", " ");

                    res = res + "<a  href=\"/saveandshow/datastreamid/" + ds.getDataStreamId() + "/activitytpye/" + activityName + "\">" + name + "</a>\r\n"
                            + "		<br>";
                }
            }
            res = res + GooglefitConstant.HTML_END;
            return res;
        } else {
            response.sendRedirect("/signin");
        }
        return null;

    }

    @Override
    public ListDataPointChangesResponse saveAndShowActivityTypeData(HttpServletRequest request, HttpServletResponse response, String dataStreamId, String activityType) throws Exception {
        log.info("Inside saveAndShowActivityTypeData");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes = service.users().dataSources().dataPointChanges().list("me", dataStreamId);
            ListDataPointChangesResponse ds = dataPointChangesRes.execute();

            /* ElasticDB upload */
            UserDataStream userDataStream = new UserDataStream();
            userDataStream.setDataStream(ds);
            IndexCoordinates indices = IndexCoordinates.of(activityType + "_datastream");
            eRestTemplate.save(userDataStream, indices);
            log.info("Saved DataStream in db");
            return ds;
        } else {
            response.sendRedirect("/signin");
        }
        return null;

    }

    @Override
    public ListDataPointChangesResponse getDataPointChanges(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        log.info("Inside getDataPointChanges");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            String dataStreamId = id;
            Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes = service.users().dataSources().dataPointChanges().list("me", dataStreamId);
            ListDataPointChangesResponse ds = dataPointChangesRes.execute();
            List<DataPoint> deletedDataPoint = ds.getDeletedDataPoint();
            for (DataPoint dp : deletedDataPoint) {

                Long starttime = dp.getEndTimeNanos();
                Long endTime = dp.getStartTimeNanos();
                long startMilliTime = starttime / convertToMillis;
                long endMilliTime = endTime / convertToMillis;
                dp.setEndTimeNanos(endMilliTime);
                dp.setStartTimeNanos(startMilliTime);
                System.out.println("time:" + endMilliTime);
            }
            List<DataPoint> insertedDatapoint = ds.getInsertedDataPoint();
            for (DataPoint dp : insertedDatapoint) {


                Long starttime = dp.getEndTimeNanos();
                Long endTime = dp.getStartTimeNanos();
                long startMilliTime = starttime / convertToMillis;
                long endMilliTime = endTime / convertToMillis;
                dp.setEndTimeNanos(endMilliTime);
                dp.setStartTimeNanos(startMilliTime);
                System.out.println("time:" + endMilliTime);
            }

            return ds;
        } else {
            response.sendRedirect("/signin");
        }
        return null;
    }

    @Override
    public String getDataSets(HttpServletRequest request, HttpServletResponse response, String startDateTime, String endDateTime) throws Exception {
        log.info("Inside getDataSets");
        boolean isCookieActive = checkCookieLife(request, response);
        System.out.println("startDateTime " + startDateTime);
        System.out.println("endDateTime  " + endDateTime);


        //System.out.println("Time in millis  "+timeInMillis);
        String endTimeString = null;
        String startTimeString = null;
        if (isCookieActive) {
            log.info("Inside getDataSets()");
            if (startDateTime == null && endDateTime == null) {
                startTimeString = String.valueOf(new DateTime().withTimeAtStartOfDay().getMillis() * convertToMillis);
                endTimeString = String.valueOf(DateTime.now().getMillis() * convertToMillis);
            } else {
                //SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date startdate = sdf.parse(startDateTime.replace("T", " "));
                Date enddate = sdf.parse(endDateTime.replace("T", " "));
                long startTimeNanos = startdate.getTime() * convertToMillis;
                long endTimeNanos = enddate.getTime() * convertToMillis;
                System.out.println("StarTime in millis " + startTimeNanos);

                System.out.println("EndTime in millis  " + endTimeNanos);
                startTimeString = String.valueOf(startTimeNanos);
                endTimeString = String.valueOf(endTimeNanos);

            }

            List<DataSource> dataSources = getDetailsDataSources(request, response).getDataSource();
            String res = GooglefitConstant.HTML_BEGIN;
            String activityDataType = null;
            for (DataSource Ds : dataSources) {
                if (!Ds.getDataStreamId().contains("com.google.android.fit") && !Ds.getDataStreamId().contains("com.google.android.apps.fitness")) {
                    // if (!Ds.getDataStreamName().equals("top_level") && !Ds.getDataStreamName().equals("user_input")) {
                    System.out.println("DataStreamName:" + Ds.getDataStreamName());

                    String name = null;

                    if (Ds.getDataStreamName().equals("merged")) {
                        name = Ds.getDataType().getName().substring(11, Ds.getDataType().getName().length());

                    } else if (Ds.getDataType().getName().equals("com.google.step_count.cumulative")) {
                        name = Ds.getDataStreamName() + " -" + Ds.getDevice().getModel();
                    } else
                        name = Ds.getDataStreamName().replace("merge_", " ");
                    System.out.println("name......" + name);
                    System.out.println("DataType:" + activityDataType);
                    res = res + "<a  href=\"/getDataSets/datastreamid/" + Ds.getDataStreamId() + "/activityType/" + activityDataType + "/startDateTime/" + startTimeString + "/endDateTime/" + endTimeString + "\">" + name + "</a>\r\n"
                            + "		<br>";
                }
            }

            res = res + GooglefitConstant.HTML_END;
            return res;

        } else {
            response.sendRedirect("/signin");
        }
        return null;

    }

    @Override
    public Dataset getDataSetsByFiltering(HttpServletRequest request, HttpServletResponse response, String id, String type, String startDateTime, String endDateTime) throws Exception {
        log.info("Inside getDataSetsByFiltering");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            String startTimeString = startDateTime;
            String endTimeString = endDateTime;

            String datasetId = startTimeString + "-" + endTimeString;
            System.out.println("datasetId: " + datasetId);
            Fitness.Users.DataSources.Datasets.Get dataSet = service.users().dataSources().datasets().get("me", id, datasetId);
            Dataset ds = dataSet.execute();


            Long maxEndTimeNs = ds.getMaxEndTimeNs();
            Long maxEndTimeMilli = maxEndTimeNs / convertToMillis;
            System.out.println("maxEndTime:" + maxEndTimeMilli);
            ds.setMaxEndTimeNs(maxEndTimeMilli);
            Long minStartTimeMili = ds.getMinStartTimeNs() / convertToMillis;
            ds.setMinStartTimeNs(minStartTimeMili);
            List<DataPoint> dataPoint = ds.getPoint();
            for (DataPoint dp : dataPoint) {
                Long endTimenanos = dp.getEndTimeNanos();
                Long startTimenanos = dp.getStartTimeNanos();
                long startTimeMillis = startTimenanos / convertToMillis;
                long endTimeMillis = endTimenanos / convertToMillis;
                dp.setStartTimeNanos(startTimeMillis);
                dp.setEndTimeNanos(endTimeMillis);

            }
            /* ElasticDB upload */
            Point point = new Point();
            for (DataPoint dp : dataPoint) {

                point.setName(userDetails.getName());
                point.setEmail(userDetails.getEmail());
                point.setDataTypeName(dp.getDataTypeName());
                point.setOriginDataSourceId(dp.getOriginDataSourceId());
                point.setStartTimeDate(dp.getStartTimeNanos().toString());
                point.setEndTimeDate(dp.getEndTimeNanos().toString());
                point.setModifiedTimeDate(dp.getModifiedTimeMillis().toString());
                for (com.google.api.services.fitness.model.Value va : dp.getValue()) {
                    System.out.println(va.getFpVal());
                    if (va.getFpVal() != null) {
                        point.setValue(va.getFpVal());
                    } else if (va.getIntVal() != null) {
                        point.setValue(Double.valueOf(va.getIntVal()));

                    }
                }

                //ncompatible types. Found: 'java.util.Map.Entry', required: 'com.google.api.services.fitness.model.Value'
                System.out.println(point);

                //IndexCoordinates indices = IndexCoordinates.of(type + "_datasetstesting");
                IndexCoordinates indices = IndexCoordinates.of("alyfdatetest");

                eRestTemplate.save(point, indices);

            }
            log.info("Points saved into Elasticsearch.");
            return ds;
        } else {
            response.sendRedirect("/signin");
        }
        return null;
    }


    @Override
    public String getListOfDataPointChanges(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Inside getListOfDataPointChanges");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            List<DataSource> dataSources = getDetailsDataSources(request, response).getDataSource();
            String res = GooglefitConstant.HTML_BEGIN;
            String activityDataType = null;

            for (DataSource Ds : dataSources) {
                if (!Ds.getDataStreamId().contains("com.google.android.fit") && !Ds.getDataStreamId().contains("com.google.android.apps.fitness")) {

                    System.out.println("DataStreamName:" + Ds.getDataStreamName());

                    String name = null;

                    if (Ds.getDataStreamName().equals("merged")) {
                        name = Ds.getDataType().getName().substring(11, Ds.getDataType().getName().length());

                    } else if (Ds.getDataType().getName().equals("com.google.step_count.cumulative")) {
                        name = Ds.getDataStreamName() + " -" + Ds.getDevice().getModel();
                    } else
                        name = Ds.getDataStreamName().replace("merge_", " ");
                    System.out.println("name......" + name);
                    System.out.println("DataType:" + activityDataType);
                    res = res + "<a  href=\"/getDataPointChanges/datastreamid/" + Ds.getDataStreamId() + "/activityType/" + activityDataType + "\">" + name + "</a>\r\n"
                            + "		<br>";
                }
            }
            res = res + GooglefitConstant.HTML_END;
            return res;
        } else {
            response.sendRedirect("/signin");
        }
        return null;


    }

    @Override
    public ListDataPointChangesResponse getDataPointChangesByFiltering(HttpServletRequest request, HttpServletResponse response, String id, String type) throws Exception {
        log.info("Inside getDataPointChangesByFiltering");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            String dataStreamId = id;
            Fitness.Users.DataSources.DataPointChanges.List dataPointChangesRes = service.users().dataSources().dataPointChanges().list("me", dataStreamId);
            ListDataPointChangesResponse ds = dataPointChangesRes.execute();
            List<DataPoint> deletedDataPoint = ds.getDeletedDataPoint();
            for (DataPoint dp : deletedDataPoint) {
                //Long nonosTime=dp.getEndTimeNanos();
                Long starttime = dp.getEndTimeNanos();
                Long endTime = dp.getStartTimeNanos();
                long startMilliTime = starttime / convertToMillis;
                long endMilliTime = endTime / convertToMillis;
                dp.setEndTimeNanos(endMilliTime);
                dp.setStartTimeNanos(startMilliTime);
                System.out.println("time:" + endMilliTime);
            }
            List<DataPoint> insertedDatapoint = ds.getInsertedDataPoint();
            for (DataPoint dp : insertedDatapoint) {
                //	DataPoint ins = dp.getEndTimeNanos();

                Long starttime = dp.getEndTimeNanos();
                Long endTime = dp.getStartTimeNanos();

                long startMilliTime = starttime / convertToMillis;
                long endMilliTime = endTime / convertToMillis;
                dp.setEndTimeNanos(endMilliTime);
                dp.setStartTimeNanos(startMilliTime);
                System.out.println("time:" + endMilliTime);
            }
            /* ElasticDB upload */
            log.info("ListDataPointChangesResponse extracted from GoogleFit");
            UserListDataPointChanges userListDataPointChange = new UserListDataPointChanges();
            userListDataPointChange.setListDataPointChangesRes(ds);
            IndexCoordinates indices = IndexCoordinates.of(type + "_lastdatapointchanges");
            //System.out.println(userListDataPointChange.toString());
            eRestTemplate.save(userListDataPointChange, indices);

            log.info("DataSets saved into Elasticsearch.");
            return ds;
        } else {
            response.sendRedirect("/signin");
        }
        return null;

    }


    @Override
    public List<Dataset> getDataSetsByAggregate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Inside getDataSetsByAggregate");
        boolean isCookieActive = checkCookieLife(request, response);
        if (isCookieActive) {
            Fitness service = fitNess(request, response);
            Fitness.Users.DataSources.List ds = service.users().dataSources().list("me");
            List<DataSource> dataSources = ds.execute().getDataSource();
            System.out.println("Data Sources: " + dataSources);
            List<Dataset> dataSets = new ArrayList<Dataset>();
            for (DataSource Ds : dataSources) {
                String dataStreamId = Ds.getDataStreamId();
                System.out.println("Data Stream Id:" + dataStreamId);
                AggregateRequest aggregateRequest = new AggregateRequest();
                aggregateRequest.setAggregateBy(Collections.singletonList(
                        new AggregateBy()
                                .setDataSourceId(dataStreamId)));
                aggregateRequest.setStartTimeMillis(DateMidnight.now().getMillis());
                aggregateRequest.setEndTimeMillis(DateTime.now().getMillis());
                Fitness.Users.Dataset.Aggregate aggregaterequest = service.users().dataset().aggregate("me", aggregateRequest);
                AggregateResponse res = aggregaterequest.execute();
                List<AggregateBucket> aggregateData = res.getBucket();
                for (AggregateBucket bucket : aggregateData) {
                    List<Dataset> dataset = bucket.getDataset();
                    dataSets.addAll(dataset);
                }
            }
            System.out.println("Datasets: " + dataSets);
            return dataSets;
        } else {
            response.sendRedirect("/signin");
        }
        return null;

    }

    private Fitness fitNess(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Inside fitNess");
        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(GFITLOGINUSEREMAIL)) {
                loginCookie = cookie;
                break;
            }
        }
        Credential credential = flow.loadCredential(loginCookie.getValue());
        System.out.println("AccessToken: " + credential.getAccessToken());
        Fitness service = new Fitness.Builder(
                httpTransport,
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        return service;
    }

    private boolean checkCookieLife(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("Inside checkCookieLife");
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
