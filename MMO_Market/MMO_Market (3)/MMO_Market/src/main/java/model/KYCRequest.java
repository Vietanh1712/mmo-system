package model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "KYCRequests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "citizen_id")
    private String citizenId;

    @Column(name = "front_id_image")
    private String frontIdImage;

    @Column(name = "back_id_image")
    private String backIdImage;

    @Column(name = "selfie_image")
    private String selfieImage;

    @Column(name = "status")
    private String status;

    @Column(name = "isDelete")
    private Boolean isDelete;
}