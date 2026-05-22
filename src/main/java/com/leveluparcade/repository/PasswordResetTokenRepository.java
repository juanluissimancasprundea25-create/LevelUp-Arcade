package com.leveluparcade.repository;

import com.leveluparcade.entity.PasswordResetToken;
import com.leveluparcade.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUsuario(Usuario usuario);
}