package controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionResponse {
    private boolean success;
    private String message;
    private String newRole;
    private Boolean isLocked;
}
