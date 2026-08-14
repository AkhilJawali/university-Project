package com.utms.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@Builder
public class PagedResponse<T> {

    private List<T> data;
    private PageMeta meta;

    @Getter
    @Setter
    @Builder
    public static class PageMeta {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    public static <T> PagedResponse<T> from(Page<T> page) {
        return PagedResponse.<T>builder()
                .data(page.getContent())
                .meta(PageMeta.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }
}
