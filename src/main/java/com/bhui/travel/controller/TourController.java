package com.bhui.travel.controller;

import com.bhui.response.ResponseData;
import com.bhui.travel.entity.Location;
import com.bhui.travel.service.TourService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TourController {

    @Autowired
    private TourService tourService;

    @GetMapping("/attractions")
    public ResponseData<List<String>> getAttractions(@RequestParam String city) {
        try {
            List<String> attractions = tourService.getAttractions(city);
            return ResponseData.succeed(attractions);
        } catch (Exception e) {
            return ResponseData.failed();
        }
    }

    @GetMapping("/route-cities")
    public ResponseData<Map<String, String>> getRouteCities(
            @RequestParam String origin,
            @RequestParam String destination) {
        try {
            Map<String, String> routeCities = tourService.getRouteCities(origin, destination);
            return ResponseData.succeed(routeCities);
        } catch (Exception e) {
            return ResponseData.failed();
        }
    }

    @GetMapping("/route-cities2")
    public ResponseData<Map<String, String>> getRouteCities2(@RequestParam String origin, @RequestParam String destination) throws Exception {
        List<Map<String, Object>> routeOptions = tourService.getRouteOptions(origin, destination);

        if (!routeOptions.isEmpty()) {
            Map<String, Object> firstRoute = routeOptions.get(0);
            List<String> cities = (List<String>) firstRoute.get("citiesOrder");

            Map<String, String> cityCoordinates = new LinkedHashMap<>();
            for (String city : cities) {
                String coordinates = tourService.geocode(city);
                if (coordinates != null) {
                    cityCoordinates.put(city, coordinates);
                }
            }
            return ResponseData.succeed(cityCoordinates);
        }

        return ResponseData.failed();
    }

    @GetMapping("/getLocationByAddress")
    public ResponseData<Location> getLocationByAddress(@RequestParam String address) throws Exception {
        Location locationByAddress = tourService.getLocationByAddress(address);
        if (locationByAddress != null) {
            return ResponseData.succeed(locationByAddress);
        }
        return ResponseData.failed();
    }
}
