package com.example.asgd.controller;

import com.example.asgd.dto.CheckItemResponse;
import com.example.asgd.dto.HealthResponse;
import com.example.asgd.dto.OomAllocationResponse;
import com.example.asgd.service.CheckService;
import com.example.asgd.service.OomSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/check")
public class CheckController {

    @Autowired
    private CheckService checkService;

    @Autowired
    private OomSimulationService oomSimulationService;

    @GetMapping("/health")
    public HealthResponse health() {
        return checkService.getHealth();
    }

    @GetMapping("/items")
    public List<CheckItemResponse> listItems() {
        return checkService.listItems();
    }

    @GetMapping("/oom")
    public OomAllocationResponse simulateOom(@RequestParam(required = false) Integer mb) {
        if (mb == null) {
            return oomSimulationService.allocateUntilOom();
        }
        return oomSimulationService.allocateMegabytes(mb);
    }

    @GetMapping("/oom/clear")
    public OomAllocationResponse clearOomSimulationMemory() {
        return oomSimulationService.clear();
    }
}
