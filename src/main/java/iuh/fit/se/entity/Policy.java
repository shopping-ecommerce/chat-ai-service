// src/main/java/iuh/fit/se/entity/policy/Policy.java
package iuh.fit.se.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "policies")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Policy {

    @Id
    String id;

    @Column(nullable = false, unique = true, length = 100)
    String code;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, length = 32)
    String version;

    @Lob
    @Column(name = "content_markdown", columnDefinition = "MEDIUMTEXT", nullable = false)
    String contentMarkdown;

    @Column(nullable = false)
    LocalDateTime effectiveDate;

    LocalDateTime createdTime;
    LocalDateTime modifiedTime;
}
