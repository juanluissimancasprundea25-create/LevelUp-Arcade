package com.leveluparcade.repository;

import com.leveluparcade.entity.LineaDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineaDevolucionRepository extends JpaRepository<LineaDevolucion, Long> {

    List<LineaDevolucion> findByDevolucionId(Long devolucionId);
}