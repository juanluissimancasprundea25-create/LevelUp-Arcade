package com.leveluparcade.repository;

import com.leveluparcade.entity.PasswordResetToken;
import com.leveluparcade.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Invalida (marca como usados) todos los tokens activos del usuario. */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usado = true " +
           "WHERE t.usuario = :usuario AND t.usado = false")
    void invalidarTokensActivos(@Param("usuario") Usuario usuario);

    /** Limpieza opcional de tokens caducados antiguos. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.fechaExpiracion < :fecha")
    int borrarCaducadosAntesDe(@Param("fecha") LocalDateTime fecha);
}