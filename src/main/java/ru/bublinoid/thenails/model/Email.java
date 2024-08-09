package ru.bublinoid.thenails.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.bublinoid.thenails.utils.HashField;
import ru.bublinoid.thenails.utils.Historical;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "email", schema = "the_nails")
public class Email implements Historical {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID hash;

    private Long chatId;

    @HashField
    private String email;

    private String confirmationCode;

    private Boolean confirm;

    @Column(name = "insert_dt", nullable = false)
    private LocalDateTime insertDt = LocalDateTime.now();

    @Override
    public void setHash(UUID uuid) {
        this.hash = uuid;
    }

    @Override
    public UUID getHash() {
        return this.hash;
    }

    @PrePersist
    public void prePersist() {
        if (this.hash == null) {
            this.hash = generateHash();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.hash == null) {
            this.hash = generateHash();
        }
    }
}
