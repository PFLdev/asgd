package com.example.asgd.service.impl;

import com.example.asgd.dao.CheckDao;
import com.example.asgd.dto.CheckItemResponse;
import com.example.asgd.entity.CheckItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheckServiceImplTest {

    @Test
    void listItemsMapsDaoEntitiesToDtoResponses() {
        CheckDao checkDao = () -> List.of(new CheckItem("heap", "Heap Memory", true));
        CheckServiceImpl service = new CheckServiceImpl(checkDao);

        List<CheckItemResponse> responses = service.listItems();

        assertThat(responses)
                .containsExactly(new CheckItemResponse("heap", "Heap Memory", true));
    }
}
