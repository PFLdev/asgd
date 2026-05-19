package com.example.asgd.service.impl;

import com.example.asgd.dto.OomAllocationResponse;
import com.example.asgd.service.OomSimulationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class OomSimulationServiceImpl implements OomSimulationService {

    private static final int BYTES_PER_MEGABYTE = 1024 * 1024;
    private static final List<Integer[]> RETAINED_HEAP = new ArrayList<>();

    @Override
    public synchronized OomAllocationResponse allocateMegabytes(int megabytes) {
        if (megabytes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mb must be greater than 0");
        }
        allocateChunks(megabytes);
        return response(megabytes);
    }

    @Override
    public synchronized OomAllocationResponse allocateUntilOom() {
        int allocatedMegabytes = 0;
        while (true) {
            allocateChunks(1);
            allocatedMegabytes++;
        }
    }

    @Override
    public synchronized OomAllocationResponse clear() {
        RETAINED_HEAP.clear();
        return response(0);
    }

    private static void allocateChunks(int megabytes) {
        for (int i = 0; i < megabytes; i++) {
            RETAINED_HEAP.add(new Integer[BYTES_PER_MEGABYTE]);
        }
    }

    private static OomAllocationResponse response(int allocatedMegabytes) {
        return new OomAllocationResponse(
                allocatedMegabytes,
                RETAINED_HEAP.size(),
                RETAINED_HEAP.size()
        );
    }
}
