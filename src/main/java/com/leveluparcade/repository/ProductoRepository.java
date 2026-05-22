package com.leveluparcade.repository;

import com.leveluparcade.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsBySku(String sku);
}