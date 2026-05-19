package com.example.asgd.dto;

public record CheckItemResponse(
        String code,
        String name,
        boolean enabled
) {
}
