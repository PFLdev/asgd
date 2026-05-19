package com.example.asgd.service;

import com.example.asgd.dto.OomAllocationResponse;

public interface OomSimulationService {

    OomAllocationResponse allocateMegabytes(int megabytes);

    OomAllocationResponse allocateUntilOom();

    OomAllocationResponse clear();
}
