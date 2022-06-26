package com.example.googlefit.service.impl;

import com.example.googlefit.GooglefitConstant;
import com.example.googlefit.model.Point;
import com.example.googlefit.model.User;
import com.example.googlefit.repository.UserRepository;
import com.example.googlefit.service.IGoogleFitService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.fitness.Fitness;
import com.google.api.services.fitness.model.DataPoint;
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
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GoogleFitService implements IGoogleFitService {

    @Autowired
    ElasticsearchRestTemplate eRestTemplate;

    @Value("${server.url}")
    private String callbackUrl;

    @Value("${clientId}")
    private String clientId;

    @Value("${clientSecret}")
    private String clientSecret;

    @Value("${session.timeout}")
    private int sessionLife;

    @Value("${elasticsearch.databaseName}")
    private String databaseName;

    @Value("${session.maxRequestsPerMinutePerUser}")
    private int maxRequestsPerMinutePerUser;

    @Value("${elasticsearch.batchSize}")
    private int batchSize;

    @Autowired
    UserRepository userRepository;

    private GoogleAuthorizationCodeFlow flow;
    private static final String APPLICATION_NAME = "fitNess";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String GFITLOGINUSEREMAIL = "gfitLoginUserEmail";
    HttpTransport httpTransport = new NetHttpTransport();

    private static final long convertToMillis = 1000000;
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

    @PostConstruct
    public void init() {
        try {
            flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
                    .setCredentialDataStore(new MemoryDataStoreFactory().getDataStore("tokens"))
                    .build();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void googleSignIn(HttpServletRequest request, HttpServletResponse response) throws Exception {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectUrl = url.setRedirectUri(callbackUrl).setAccessType("offline").build();
        response.sendRedirect(redirectUrl);
    }




    @Override
    public Credential getCredential(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("Inside getCredential");
        Cookie loginCookie = getLoginCookie(request);
        if (loginCookie != null) {
            String email = loginCookie.getValue();

            return flow.loadCredential(email);
        } else {
            return null;
        }
    }

    @Override
    public void saveToken(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        log.info("Inside saveToken");
        GoogleTokenResponse googleTokenResponse = flow.newTokenRequest(code).setRedirectUri(callbackUrl).execute();
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + googleTokenResponse.getAccessToken();
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                .header("accept", "application/json")
                .build();
        HttpResponse<String> res = client.send(httpRequest, BodyHandlers.ofString());
        log.info(res.body());
        JsonObject userData = JsonParser.parseString(res.body()).getAsJsonObject();
        String userName = userData.get("name").getAsString();
        String userEmail = userData.get("email").getAsString();
        String userToken = googleTokenResponse.getAccessToken();
        log.info(userName);
        log.info(userEmail);
        log.info(userToken);
        String user = request.getParameter(GFITLOGINUSEREMAIL);
        String nameEmailEncoded = URLEncoder.encode((userName + "#" + userEmail +"#"+userToken), "UTF-8");
        if (user == null) {
            Cookie loginCookie = new Cookie(GFITLOGINUSEREMAIL, nameEmailEncoded);
            flow.createAndStoreCredential(googleTokenResponse, nameEmailEncoded);
            log.info(googleTokenResponse.getAccessToken());
            loginCookie.setMaxAge(sessionLife);
            response.addCookie(loginCookie);
            response.sendRedirect("/dashboard");
        }
    }

    @Override
    public List<Point> getDataSetsForActivityType(String accessToken,
                                                  String[] activityTypes, String startDateTime, String endDateTime,String email,String name) throws Exception {
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        Fitness service = new Fitness.Builder(
                httpTransport,
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
            if (service != null) {
                if (StringUtils.isEmpty(startDateTime)) {
                    startDateTime = String.valueOf(new DateTime().withTimeAtStartOfDay());
                }
                if (StringUtils.isEmpty(endDateTime)) {
                    endDateTime = String.valueOf(DateTime.now());
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date startDate = sdf.parse(startDateTime.replace("T", " "));
                Date endDate = sdf.parse(endDateTime.replace("T", " "));
                long startTimeNanos = startDate.getTime() * convertToMillis;
                long endTimeNanos = endDate.getTime() * convertToMillis;
                String startTimeString = String.valueOf(startTimeNanos);
                String endTimeString = String.valueOf(endTimeNanos);

                if (activityTypes.length > 0) {
                    List<DataSource> dataSourceList = getDetailsDataSources(service).getDataSource();
                    List<String> activityDataTypesList = getActivityDataTypesList(activityTypes);
                    List<Point> savePointsInDBMap = savePointsInDB( service, endTimeString, startTimeString, dataSourceList, activityDataTypesList,email,name);
//                    if ((boolean) savePointsInDBMap.get("isPointsAvailable")) {
//                        return (List<Dataset>) savePointsInDBMap.get("dataSetList");
//                    }
                    return savePointsInDBMap;
                }
            }

        return new ArrayList<>();
    }



    @Override
    public String storeUserAllDetails(HttpServletRequest request, HttpServletResponse response, String[] activityTypes, String startDateTime, String endDateTime) throws Exception {
        Cookie loginCookie = getLoginCookie(request);
        boolean isCookieActive = checkCookieLife(loginCookie);
        if (isCookieActive) {
            Fitness service = fitNess(request);
            if (service != null) {
                String userDetails[] = URLDecoder.decode(loginCookie.getValue(), "UTF-8").split("#");

                User user = new User(userDetails[1],userDetails[0],userDetails[2],java.util.Arrays.toString(activityTypes),startDateTime,endDateTime);

                userRepository.save(user);


            } else {
                response.sendRedirect("/signin");
            }
        } else {
            response.sendRedirect("/signin");
        }

        return "Done";
    }

//    @Override
    public List<Point> getUserByEmail(String email,String startTime,String endTime) throws Exception {

        Optional<User> userData= userRepository.findByEmail(email);


        String name = null;
        String token = null;
        String[] activitys = null;
        //String startTime = null;
        //String endTime = null;

        if(userData.isPresent()){
            User user = userData.get();
            name = user.getName();
            token = user.getToken();
            String activityStr=user.getActivity();
            String activityTrim;
            activityTrim = activityStr.replace("]", "");
            activityTrim =activityTrim.replace("[","");
            activitys = activityTrim.split(",");
            //startTime = user.getStartTime();
            //endTime = user.getEndTime();
            return callGoogleDataPoint(token,activitys,startTime,endTime,email,name);
            
      }



        return new ArrayList<>();

    }

    public List<Point> callGoogleDataPoint(String token,String[] activitys,String startTime,String endTime,String email,String name) throws Exception {
        return getDataSetsForActivityType(token,activitys,startTime,endTime,email,name);

    }

    private ListDataSourcesResponse getDetailsDataSources(Fitness service) throws Exception {
        Fitness.Users.DataSources.List dataSources = service.users().dataSources().list("me");
        return dataSources.execute();
    }

    private List<Point> getDataSetsAndPointMapByFiltering(Fitness service, String id, String startDateTime,
                                                          String endDateTime, String userName, String userEmail) throws Exception {
        HashMap<String, Object> dataSetAndPointMap = new HashMap<>();
        String datasetId = startDateTime + "-" + endDateTime;
        Fitness.Users.DataSources.Datasets.Get dataSet = service.users().dataSources().datasets().get("me", id, datasetId);
        Dataset ds = dataSet.execute();
        ds.setMaxEndTimeNs(ds.getMaxEndTimeNs() / convertToMillis);
        ds.setMinStartTimeNs(ds.getMinStartTimeNs() / convertToMillis);

        List<DataPoint> dataPoint = ds.getPoint();
        List<Point> points = new ArrayList<>();
        for (DataPoint dp : dataPoint) {
            Point point = new Point();
            point.setName(userName);
            point.setEmail(userEmail);
            point.setDataTypeName(dp.getDataTypeName());
            point.setOriginDataSourceId(dp.getOriginDataSourceId());
            point.setStartTimeDate(Long.toString(dp.getStartTimeNanos() / convertToMillis));
            point.setEndTimeDate(Long.toString(dp.getEndTimeNanos() / convertToMillis));
            point.setModifiedTimeDate(dp.getModifiedTimeMillis().toString());
            for (com.google.api.services.fitness.model.Value va : dp.getValue()) {
                if (va.getFpVal() != null) {
                    point.setValue(va.getFpVal());
                } else if (va.getIntVal() != null) {
                    point.setValue(Double.valueOf(va.getIntVal()));
                }
            }
            points.add(point);
        }
        dataSetAndPointMap.put("pointList", points);
        dataSetAndPointMap.put("dataSetList", ds);
        return points;
    }

    private List<Point> savePointsInDB( Fitness service,String endTimeString,
                                       String startTimeString, List<DataSource> dataSourceList, List<String> activityDataTypesList,String email,String name) throws Exception {
        HashMap<String, Object> savePointsInDBMap = new HashMap<>();
        List<Dataset> datasetList = new ArrayList<>();
        List<Point> pointsListMaster = new ArrayList<>();
        int count = 0;
        for (DataSource dataSource : dataSourceList) {
            if (activityDataTypesList.contains(dataSource.getDataType().getName())) {
                count++;
                if (count > maxRequestsPerMinutePerUser) {
                    System.gc();
                    log.info("Configured {} Max Requests per minute per user reached. Application will sleep for 1 minute.", maxRequestsPerMinutePerUser);
                    TimeUnit.SECONDS.sleep(61);
                    count = 0;
                }
               // String userDetails[] = URLDecoder.decode(loginCookie.getValue(), "UTF-8").split("#");
                List<Point> dataSetAndPointMap = getDataSetsAndPointMapByFiltering(service,
                        dataSource.getDataStreamId(), startTimeString, endTimeString,name,
                        email);
                pointsListMaster.addAll(dataSetAndPointMap);
                // datasetList.add((Dataset) dataSetAndPointMap.get("dataSetList"));
            }
        }
        boolean isPointsAvailable = false;
        int totalPointsSize = pointsListMaster.size();
        if (totalPointsSize > 0) {
            isPointsAvailable = true;
//            DateTime startTime = DateTime.now();
//            log.info("Batch Insertion Started");
//            List<List<Point>> pointsListSubsets = Lists.partition(pointsListMaster, batchSize);
//            pointsListSubsets.forEach(pointListBatch -> {
//                IndexCoordinates indices = IndexCoordinates.of(databaseName);
//                eRestTemplate.save(pointListBatch, indices);
//                System.gc();
//            });
//            DateTime endTime = DateTime.now();
//            log.info("Batch Insertion Ended");
//            log.info("Total {} points inserted into Elasticsearch.", totalPointsSize);
//            log.info("Total Time Taken in secs - " + Seconds.secondsBetween(startTime, endTime).getSeconds());
        }
        savePointsInDBMap.put("dataSetList", datasetList);
        savePointsInDBMap.put("isPointsAvailable", isPointsAvailable);
        return pointsListMaster;
    }

    private List<String> getActivityDataTypesList(String[] activityTypes) {
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
        return activityDataTypesList;
    }

    private Cookie getLoginCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        Cookie loginCookie = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(GFITLOGINUSEREMAIL)) {
                    loginCookie = cookie;
                    break;
                }
            }
        }
        return loginCookie;
    }

    private Fitness fitNess(HttpServletRequest request) throws Exception {
        Credential credential = getCredential(request, null);
        if (credential != null) {
            return new Fitness.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return null;
    }

    private boolean checkCookieLife(Cookie cookie) {
        return cookie != null;
    }
}
