package com.finsimx.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotBlank(message = "Asset is required")
    @Size(min = 1, max = 50, message = "Asset must be between 1 and 50 characters")
    private String asset;

    @NotBlank(message = "Order type is required (BUY or SELL)")
    @Pattern(regexp = "^(BUY|SELL)$", message = "Order type must be BUY or SELL")
    private String type;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99999999", message = "Price is too large")
    private BigDecimal price;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @DecimalMax(value = "999999999.99999999", message = "Quantity is too large")
    private BigDecimal quantity;

    @Size(max = 255, message = "Notes cannot exceed 255 characters")
    private String notes;
}
