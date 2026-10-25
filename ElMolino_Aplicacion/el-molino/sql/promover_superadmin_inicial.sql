-- Reemplaza el correo de ejemplo por el correo de una cuenta ADMINISTRADOR existente.
-- Ejecuta esta instruccion una sola vez para habilitar el primer SuperAdmin.
UPDATE usuarios
SET rol = 'SUPERADMIN'
WHERE email = 'REEMPLAZAR_POR_EL_CORREO_DEL_USUARIO'
  AND rol = 'ADMINISTRADOR';
