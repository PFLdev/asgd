package com.example.asgd.service;

import com.example.asgd.dto.CheckItemResponse;
import com.example.asgd.dto.HealthResponse;

import java.util.List;

public interface CheckService {

    HealthResponse getHealth();

    List<CheckItemResponse> listItems();
}
