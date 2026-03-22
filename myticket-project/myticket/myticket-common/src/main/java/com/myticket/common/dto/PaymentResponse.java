package com.myticket.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    /** URL to redirect user for payment (if applicable) */
    private String redirectUrl;
    /** Transaction ID for tracking */
    private String txnId;
}
