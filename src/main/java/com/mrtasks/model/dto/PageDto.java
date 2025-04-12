package com.mrtasks.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageDto<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private int totalPages;
    private long totalElements;
}