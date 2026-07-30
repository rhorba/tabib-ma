package com.tabibma.clinic.dto;

import java.io.Serializable;
import java.util.List;

public record DoctorSearchResponse(
        List<DoctorSearchResultResponse> results,
        int page,
        int size,
        long totalElements,
        int totalPages
) implements Serializable {
}
