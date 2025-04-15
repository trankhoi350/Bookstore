package com.project.bookstore.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/demo-controller")
public class DemoController {
    private final CovidDataService covidDataService;

    public DemoController(CovidDataService covidDataService) {
        this.covidDataService = covidDataService;
    }

    @GetMapping
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hello Khoi");
    }

    @GetMapping("/covid")
    public ResponseEntity<List<CovidDataDto>> getCovidData() {
        List<CovidDataDto> data = covidDataService.retrieveCovidData();
        return ResponseEntity.ok(data);
    }
}
