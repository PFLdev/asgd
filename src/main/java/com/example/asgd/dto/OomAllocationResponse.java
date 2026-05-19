package com.example.asgd.dto;

public record OomAllocationResponse(
        int allocatedMegabytes,
        int retainedChunks,
        long retainedMegabytes
) {
}
