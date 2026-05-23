-- =========================================================================
-- V2__fix_admin_password.sql
--
-- Corrige el hash BCrypt del usuario admin sembrado en V1__init.sql.
--
-- El hash original no correspondía a la password documentada "admin123",
-- impidiendo el login del administrador tras un init limpio.
--
-- El nuevo hash ha sido generado por el test AdminPasswordHashGenerator,
-- que usa el MISMO BCryptPasswordEncoder que la aplicación emplea para
-- validar passwords (configurado en SecurityConfig).
--
-- Credenciales resultantes:
--   email:    admin@leveluparcade.local
--   password: admin123
--
-- Nota: V1 no se modifica porque ya está aplicada en las BDs del equipo;
-- editarla rompería el checksum de Flyway. Esta migración aditiva es
-- segura y reproducible en cualquier entorno.
-- =========================================================================

UPDATE usuarios
SET password_hash = '$2a$10$p1ahm3od4u4QzoqOCun7..R9Op01QyzWtFkBnk4GcmIJUvfFDKYLC'
WHERE email = 'admin@leveluparcade.local';