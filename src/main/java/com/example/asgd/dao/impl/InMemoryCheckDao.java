package com.example.asgd.dao.impl;

import com.example.asgd.dao.CheckDao;
import com.example.asgd.entity.CheckItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryCheckDao implements CheckDao {

    private final List<CheckItem> items = List.of(
            new CheckItem("heap", "Heap Memory", true),
            new CheckItem("thread", "Thread Count", true)
    );

    @Override
    public List<CheckItem> findAll() {
        return items;
    }
}
