package com.utms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateResponse<T> {
    private T data;
    private List<String> warnings;

    public CreateResponse(T data) {
        this.data = data;
        this.warnings = List.of();
    }
}
