package ru.bublinoid.thenails.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bublinoid.thenails.model.Email;

import java.util.Optional;
import java.util.UUID;

public interface EmailRepository extends JpaRepository<Email, UUID> {
    Optional<Email> findByChatId(Long chatId);
}
