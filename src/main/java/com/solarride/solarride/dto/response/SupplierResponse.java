package com.solarride.solarride.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierResponse(
        UUID supplierId,
        UUID userId,
        String email,
        String companyName,
        String repName,
        String shopAddress,
        String status,
        BigDecimal averageRating
) {}