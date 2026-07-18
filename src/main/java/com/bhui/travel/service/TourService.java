package com.bhui.travel.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bhui.travel.entity.Location;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author JXS
 */
@Service
public class TourService {
    private static final String API_KEY = "538ad44097e0b56a7e4c4ef7dce5a3c8";
    private static final String KEY_SERVICE = "538ad44097e0b56a7e4c4ef7dce5a3c8";

    public List<String> getAttractions(String city) throws Exception {
        List<String> attractions = new ArrayList<>();
        String url = "https://restapi.amap.com/v3/place/text?key=" + API_KEY + "&keywords=景点&city=" + city + "&output=json";
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            CloseableHttpResponse response = client.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSONObject.parseObject(jsonResponse);
            JSONArray pois = jsonObject.getJSONArray("pois");
            for (int i = 0; i < pois.size(); i++) {
                JSONObject poi = pois.getJSONObject(i);
                attractions.add(poi.getString("name") + " - " + poi.getString("address"));
            }
            return attractions;
        }
    }

    public Map<String, String> getRouteCities(String origin, String destination) throws Exception {
        Map<String, String> cities = new HashMap<>();
        String originCoords = geocode(origin);
        String destCoords = geocode(destination);

        if (originCoords != null && destCoords != null) {
            cities.put(origin, originCoords);
            cities.put(destination, destCoords);

            String url = "https://restapi.amap.com/v3/direction/driving?origin=" + originCoords +
                    "&destination=" + destCoords + "&extensions=all&key=" + API_KEY;

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                CloseableHttpResponse response = client.execute(request);
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSONObject.parseObject(jsonResponse);

                JSONObject route = jsonObject.getJSONObject("route");
                JSONArray paths = route.getJSONArray("paths");
                if (paths != null && !paths.isEmpty()) {
                    JSONObject path = paths.getJSONObject(0);
                    JSONArray steps = path.getJSONArray("steps");
                    for (int i = 0; i < steps.size(); i++) {
                        JSONObject step = steps.getJSONObject(i);
                        JSONArray citiesInStep = step.getJSONArray("cities");
                        if (citiesInStep != null) {
                            for (int j = 0; j < citiesInStep.size(); j++) {
                                JSONObject city = citiesInStep.getJSONObject(j);
                                String cityName = city.getString("name");
                                String cityCoords = city.getString("longitude") + "," + city.getString("latitude");
                                if (!cities.containsKey(cityName)) {
                                    if (cityCoords.split(",").length == 2) {
                                        cities.put(cityName, cityCoords);
                                    } else {
                                        String geocodedCoords = geocode(cityName);
                                        if (geocodedCoords != null) {
                                            cities.put(cityName, geocodedCoords);
                                        } else {
                                            cities.put(cityName, "null,null");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            throw new Exception("Failed to geocode origin or destination");
        }

        for (Map.Entry<String, String> entry : cities.entrySet()) {
            if ("null,null".equals(entry.getValue())) {
                String geocodedCoords = geocode(entry.getKey());
                if (geocodedCoords != null) {
                    entry.setValue(geocodedCoords);
                }
            }
        }

        return cities;
    }

    public String geocode(String address) throws Exception {
        String url = "https://restapi.amap.com/v3/geocode/geo?address=" + URLEncoder.encode(address, "UTF-8") + "&key=" + API_KEY;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            CloseableHttpResponse response = client.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSONObject.parseObject(jsonResponse);
            JSONArray geocodes = jsonObject.getJSONArray("geocodes");
            if (geocodes != null && !geocodes.isEmpty()) {
                return geocodes.getJSONObject(0).getString("location");
            }
        }
        return null;
    }

    public List<Map<String, Object>> getRouteOptions(String origin, String destination) throws Exception {
        List<Map<String, Object>> routeOptions = new ArrayList<>();
        String originCoords = geocode(origin);
        String destCoords = geocode(destination);

        if (originCoords == null || destCoords == null) {
            throw new Exception("Failed to geocode origin or destination");
        }

        String url = "https://restapi.amap.com/v3/direction/driving?origin=" + originCoords +
                "&destination=" + destCoords + "&extensions=all&strategy=10&alternatives=1&key=" + API_KEY;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            CloseableHttpResponse response = client.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSONObject.parseObject(jsonResponse);

            JSONObject route = jsonObject.getJSONObject("route");
            JSONArray paths = route.getJSONArray("paths");

            for (int i = 0; i < paths.size(); i++) {
                JSONObject path = paths.getJSONObject(i);
                Map<String, Object> routeOption = new HashMap<>();
                List<String> citiesOrder = new ArrayList<>();
                citiesOrder.add(origin);

                JSONArray steps = path.getJSONArray("steps");
                List<String> coordinates = extractCoordinates(steps);
                citiesOrder.addAll(getCitiesFromCoordinates(coordinates));

                if (!citiesOrder.get(citiesOrder.size() - 1).equals(destination)) {
                    citiesOrder.add(destination);
                }

                routeOption.put("distance", path.getString("distance"));
                routeOption.put("duration", path.getString("duration"));
                routeOption.put("tolls", path.getString("tolls"));
                routeOption.put("citiesOrder", citiesOrder);

                routeOptions.add(routeOption);
            }
        }

        return routeOptions;
    }

    private List<String> extractCoordinates(JSONArray steps) {
        List<String> coordinates = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getJSONObject(i);
            String polyline = step.getString("polyline");
            if (polyline != null && !polyline.isEmpty()) {
                String[] points = polyline.split(";");
                coordinates.addAll(Arrays.asList(points));
            }
        }
        return coordinates;
    }

    private List<String> getCitiesFromCoordinates(List<String> coordinates) throws Exception {
        Set<String> uniqueCities = new LinkedHashSet<>();
        int step = Math.max(1, coordinates.size() / 10);
        for (int i = 0; i < coordinates.size(); i += step) {
            String coord = coordinates.get(i);
            String city = getCity(coord);
            if (city != null && !city.isEmpty()) {
                uniqueCities.add(city);
            }
        }
        return new ArrayList<>(uniqueCities);
    }

    private String getCity(String location) throws Exception {
        String url = "https://restapi.amap.com/v3/geocode/regeo?location=" + location + "&key=" + API_KEY;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            CloseableHttpResponse response = client.execute(request);
            String jsonResponse = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSONObject.parseObject(jsonResponse);
            JSONObject regeocode = jsonObject.getJSONObject("regeocode");
            if (regeocode != null) {
                JSONObject addressComponent = regeocode.getJSONObject("addressComponent");
                if (addressComponent != null) {
                    return addressComponent.getString("city");
                }
            }
        }
        return null;
    }

    public Location getLocationByAddress(String address) throws Exception {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = "https://restapi.amap.com/v3/geocode/geo?address=" + encodedAddress + "&key=" + KEY_SERVICE;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSONObject.parseObject(jsonResponse);
                JSONArray geocodes = jsonObject.getJSONArray("geocodes");

                if (geocodes != null && geocodes.size() > 0) {
                    JSONObject locationData = geocodes.getJSONObject(0);
                    String location = locationData.getString("location");
                    if (location != null && !location.isEmpty()) {
                        String[] lngLat = location.split(",");
                        return new Location(Double.parseDouble(lngLat[0]), Double.parseDouble(lngLat[1]));
                    }
                }
            }
        }
        return null;
    }
}
