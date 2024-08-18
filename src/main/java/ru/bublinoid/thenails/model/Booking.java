package ru.bublinoid.thenails.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking", schema = "the_nails")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Booking {

    @Id
    @Column(name = "hash", updatable = false, nullable = false)
    private UUID hash;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "service", nullable = false)
    private String service;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "confirm", nullable = false)
    private Boolean confirm = false;

    @Column(name = "insert_dt", nullable = false, updatable = false)
    private LocalDateTime insertDt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.hash == null) {
            this.hash = UUID.randomUUID();
        }
        if (this.insertDt == null) {
            this.insertDt = LocalDateTime.now();
        }
    }
}
