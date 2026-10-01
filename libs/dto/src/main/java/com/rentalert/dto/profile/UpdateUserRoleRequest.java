package com.rentalert.dto.profile;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull UserRole role
) {

}
