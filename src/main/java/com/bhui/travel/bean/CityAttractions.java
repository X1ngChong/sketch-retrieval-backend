package com.bhui.travel.bean;

import java.util.List;

public class CityAttractions {
    private List<String> attractions;
    private double lng;
    private double lat;

    public CityAttractions(List<String> attractions, double lng, double lat) {
        this.attractions = attractions;
        this.lng = lng;
        this.lat = lat;
    }

    public List<String> getAttractions() {
        return attractions;
    }

    public void setAttractions(List<String> attractions) {
        this.attractions = attractions;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }
}
