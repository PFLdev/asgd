package com.example.asgd.dao;

import com.example.asgd.entity.CheckItem;

import java.util.List;

public interface CheckDao {

    List<CheckItem> findAll();
}
