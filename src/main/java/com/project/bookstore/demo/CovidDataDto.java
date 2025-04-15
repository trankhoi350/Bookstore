package com.project.bookstore.demo;

public class CovidDataDto {
    private String country;
    private Integer cases;
    private Integer deaths;
    private Integer recoveries;

    public CovidDataDto() {}

    public CovidDataDto(String country, Integer cases, Integer deaths, Integer recoveries) {
        this.country = country;
        this.cases = cases;
        this.deaths = deaths;
        this.recoveries = recoveries;
    }

    // Getters and setters

    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public Integer getCases() {
        return cases;
    }
    public void setCases(Integer cases) {
        this.cases = cases;
    }
    public Integer getDeaths() {
        return deaths;
    }
    public void setDeaths(Integer deaths) {
        this.deaths = deaths;
    }
    public Integer getRecoveries() {
        return recoveries;
    }
    public void setRecoveries(Integer recoveries) {
        this.recoveries = recoveries;
    }

    @Override
    public String toString() {
        return "CovidDataDto{" +
                "country='" + country + '\'' +
                ", cases=" + cases +
                ", deaths=" + deaths +
                ", recoveries=" + recoveries +
                '}';
    }
}
