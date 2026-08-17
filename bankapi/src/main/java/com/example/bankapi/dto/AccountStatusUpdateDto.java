package com.example.bankapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AccountStatusUpdateDto(
        @NotNull @Pattern(regexp = "ACTIVE|INACTIVE") String status
) {
}
