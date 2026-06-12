package controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SePayWebhookRequest {
    private Long id; // SePay transaction ID
    private String gateway;
    
    @JsonProperty("transactionDate")
    private String transactionDate;
    
    @JsonProperty("accountNumber")
    private String accountNumber;
    
    private String code;
    private String content; // E.g. "MMO TOPUP 17"
    
    @JsonProperty("transferType")
    private String transferType; // "in" or "out"
    
    @JsonProperty("transferAmount")
    private Long transferAmount;
    
    @JsonProperty("accumulatedBalance")
    private Long accumulatedBalance;
    
    @JsonProperty("referenceCode")
    private String referenceCode;
    
    private String reference;
}
