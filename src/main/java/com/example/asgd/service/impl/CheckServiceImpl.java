package com.example.asgd.service.impl;

import com.example.asgd.dao.CheckDao;
import com.example.asgd.dto.CheckItemResponse;
import com.example.asgd.dto.HealthResponse;
import com.example.asgd.entity.CheckItem;
import com.example.asgd.service.CheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckServiceImpl implements CheckService {

    @Autowired
    private CheckDao checkDao;

    public CheckServiceImpl() {
    }

    public CheckServiceImpl(CheckDao checkDao) {
        this.checkDao = checkDao;
    }

    @Override
    public HealthResponse getHealth() {
        return new HealthResponse("UP", "asgd");
    }

    @Override
    public List<CheckItemResponse> listItems() {
        return checkDao.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CheckItemResponse toResponse(CheckItem item) {
        return new CheckItemResponse(item.code(), item.name(), item.enabled());
    }
}
