-- 40 clientes ficticios para pruebas. Puede ejecutarse más de una vez:
-- omite los registros cuyo DNI o email ya existan.
START TRANSACTION;
INSERT INTO cliente (dni, nombre, apellido, telefono, email, direccion)
SELECT muestra.dni, muestra.nombre, muestra.apellido, muestra.telefono, muestra.email, muestra.direccion
FROM (
    SELECT 99000101 AS dni, 'Ana' AS nombre, 'Acosta' AS apellido, 1140000001 AS telefono, 'cliente.prueba001@example.com' AS email, 'Calle de Prueba 1001' AS direccion
    UNION ALL SELECT 99000102 AS dni, 'Bruno' AS nombre, 'Acosta' AS apellido, 1140000002 AS telefono, 'cliente.prueba002@example.com' AS email, 'Calle de Prueba 1002' AS direccion
    UNION ALL SELECT 99000103 AS dni, 'Camila' AS nombre, 'Acosta' AS apellido, 1140000003 AS telefono, 'cliente.prueba003@example.com' AS email, 'Calle de Prueba 1003' AS direccion
    UNION ALL SELECT 99000104 AS dni, 'Diego' AS nombre, 'Acosta' AS apellido, 1140000004 AS telefono, 'cliente.prueba004@example.com' AS email, 'Calle de Prueba 1004' AS direccion
    UNION ALL SELECT 99000105 AS dni, 'Elena' AS nombre, 'Acosta' AS apellido, 1140000005 AS telefono, 'cliente.prueba005@example.com' AS email, 'Calle de Prueba 1005' AS direccion
    UNION ALL SELECT 99000106 AS dni, 'Facundo' AS nombre, 'Acosta' AS apellido, 1140000006 AS telefono, 'cliente.prueba006@example.com' AS email, 'Calle de Prueba 1006' AS direccion
    UNION ALL SELECT 99000107 AS dni, 'Gabriela' AS nombre, 'Acosta' AS apellido, 1140000007 AS telefono, 'cliente.prueba007@example.com' AS email, 'Calle de Prueba 1007' AS direccion
    UNION ALL SELECT 99000108 AS dni, 'Hugo' AS nombre, 'Acosta' AS apellido, 1140000008 AS telefono, 'cliente.prueba008@example.com' AS email, 'Calle de Prueba 1008' AS direccion
    UNION ALL SELECT 99000109 AS dni, 'Inés' AS nombre, 'Acosta' AS apellido, 1140000009 AS telefono, 'cliente.prueba009@example.com' AS email, 'Calle de Prueba 1009' AS direccion
    UNION ALL SELECT 99000110 AS dni, 'Juan' AS nombre, 'Acosta' AS apellido, 1140000010 AS telefono, 'cliente.prueba010@example.com' AS email, 'Calle de Prueba 1010' AS direccion
    UNION ALL SELECT 99000111 AS dni, 'Lucía' AS nombre, 'Acosta' AS apellido, 1140000011 AS telefono, 'cliente.prueba011@example.com' AS email, 'Calle de Prueba 1011' AS direccion
    UNION ALL SELECT 99000112 AS dni, 'Mateo' AS nombre, 'Acosta' AS apellido, 1140000012 AS telefono, 'cliente.prueba012@example.com' AS email, 'Calle de Prueba 1012' AS direccion
    UNION ALL SELECT 99000113 AS dni, 'Noelia' AS nombre, 'Acosta' AS apellido, 1140000013 AS telefono, 'cliente.prueba013@example.com' AS email, 'Calle de Prueba 1013' AS direccion
    UNION ALL SELECT 99000114 AS dni, 'Pablo' AS nombre, 'Acosta' AS apellido, 1140000014 AS telefono, 'cliente.prueba014@example.com' AS email, 'Calle de Prueba 1014' AS direccion
    UNION ALL SELECT 99000115 AS dni, 'Rocío' AS nombre, 'Acosta' AS apellido, 1140000015 AS telefono, 'cliente.prueba015@example.com' AS email, 'Calle de Prueba 1015' AS direccion
    UNION ALL SELECT 99000116 AS dni, 'Santiago' AS nombre, 'Acosta' AS apellido, 1140000016 AS telefono, 'cliente.prueba016@example.com' AS email, 'Calle de Prueba 1016' AS direccion
    UNION ALL SELECT 99000117 AS dni, 'Teresa' AS nombre, 'Acosta' AS apellido, 1140000017 AS telefono, 'cliente.prueba017@example.com' AS email, 'Calle de Prueba 1017' AS direccion
    UNION ALL SELECT 99000118 AS dni, 'Ulises' AS nombre, 'Acosta' AS apellido, 1140000018 AS telefono, 'cliente.prueba018@example.com' AS email, 'Calle de Prueba 1018' AS direccion
    UNION ALL SELECT 99000119 AS dni, 'Valentina' AS nombre, 'Acosta' AS apellido, 1140000019 AS telefono, 'cliente.prueba019@example.com' AS email, 'Calle de Prueba 1019' AS direccion
    UNION ALL SELECT 99000120 AS dni, 'Zoe' AS nombre, 'Acosta' AS apellido, 1140000020 AS telefono, 'cliente.prueba020@example.com' AS email, 'Calle de Prueba 1020' AS direccion
    UNION ALL SELECT 99000121 AS dni, 'Ana' AS nombre, 'Benítez' AS apellido, 1140000021 AS telefono, 'cliente.prueba021@example.com' AS email, 'Calle de Prueba 1021' AS direccion
    UNION ALL SELECT 99000122 AS dni, 'Bruno' AS nombre, 'Benítez' AS apellido, 1140000022 AS telefono, 'cliente.prueba022@example.com' AS email, 'Calle de Prueba 1022' AS direccion
    UNION ALL SELECT 99000123 AS dni, 'Camila' AS nombre, 'Benítez' AS apellido, 1140000023 AS telefono, 'cliente.prueba023@example.com' AS email, 'Calle de Prueba 1023' AS direccion
    UNION ALL SELECT 99000124 AS dni, 'Diego' AS nombre, 'Benítez' AS apellido, 1140000024 AS telefono, 'cliente.prueba024@example.com' AS email, 'Calle de Prueba 1024' AS direccion
    UNION ALL SELECT 99000125 AS dni, 'Elena' AS nombre, 'Benítez' AS apellido, 1140000025 AS telefono, 'cliente.prueba025@example.com' AS email, 'Calle de Prueba 1025' AS direccion
    UNION ALL SELECT 99000126 AS dni, 'Facundo' AS nombre, 'Benítez' AS apellido, 1140000026 AS telefono, 'cliente.prueba026@example.com' AS email, 'Calle de Prueba 1026' AS direccion
    UNION ALL SELECT 99000127 AS dni, 'Gabriela' AS nombre, 'Benítez' AS apellido, 1140000027 AS telefono, 'cliente.prueba027@example.com' AS email, 'Calle de Prueba 1027' AS direccion
    UNION ALL SELECT 99000128 AS dni, 'Hugo' AS nombre, 'Benítez' AS apellido, 1140000028 AS telefono, 'cliente.prueba028@example.com' AS email, 'Calle de Prueba 1028' AS direccion
    UNION ALL SELECT 99000129 AS dni, 'Inés' AS nombre, 'Benítez' AS apellido, 1140000029 AS telefono, 'cliente.prueba029@example.com' AS email, 'Calle de Prueba 1029' AS direccion
    UNION ALL SELECT 99000130 AS dni, 'Juan' AS nombre, 'Benítez' AS apellido, 1140000030 AS telefono, 'cliente.prueba030@example.com' AS email, 'Calle de Prueba 1030' AS direccion
    UNION ALL SELECT 99000131 AS dni, 'Lucía' AS nombre, 'Benítez' AS apellido, 1140000031 AS telefono, 'cliente.prueba031@example.com' AS email, 'Calle de Prueba 1031' AS direccion
    UNION ALL SELECT 99000132 AS dni, 'Mateo' AS nombre, 'Benítez' AS apellido, 1140000032 AS telefono, 'cliente.prueba032@example.com' AS email, 'Calle de Prueba 1032' AS direccion
    UNION ALL SELECT 99000133 AS dni, 'Noelia' AS nombre, 'Benítez' AS apellido, 1140000033 AS telefono, 'cliente.prueba033@example.com' AS email, 'Calle de Prueba 1033' AS direccion
    UNION ALL SELECT 99000134 AS dni, 'Pablo' AS nombre, 'Benítez' AS apellido, 1140000034 AS telefono, 'cliente.prueba034@example.com' AS email, 'Calle de Prueba 1034' AS direccion
    UNION ALL SELECT 99000135 AS dni, 'Rocío' AS nombre, 'Benítez' AS apellido, 1140000035 AS telefono, 'cliente.prueba035@example.com' AS email, 'Calle de Prueba 1035' AS direccion
    UNION ALL SELECT 99000136 AS dni, 'Santiago' AS nombre, 'Benítez' AS apellido, 1140000036 AS telefono, 'cliente.prueba036@example.com' AS email, 'Calle de Prueba 1036' AS direccion
    UNION ALL SELECT 99000137 AS dni, 'Teresa' AS nombre, 'Benítez' AS apellido, 1140000037 AS telefono, 'cliente.prueba037@example.com' AS email, 'Calle de Prueba 1037' AS direccion
    UNION ALL SELECT 99000138 AS dni, 'Ulises' AS nombre, 'Benítez' AS apellido, 1140000038 AS telefono, 'cliente.prueba038@example.com' AS email, 'Calle de Prueba 1038' AS direccion
    UNION ALL SELECT 99000139 AS dni, 'Valentina' AS nombre, 'Benítez' AS apellido, 1140000039 AS telefono, 'cliente.prueba039@example.com' AS email, 'Calle de Prueba 1039' AS direccion
    UNION ALL SELECT 99000140 AS dni, 'Zoe' AS nombre, 'Benítez' AS apellido, 1140000040 AS telefono, 'cliente.prueba040@example.com' AS email, 'Calle de Prueba 1040' AS direccion
) AS muestra
WHERE NOT EXISTS (
    SELECT 1 FROM cliente existente
    WHERE existente.dni = muestra.dni OR existente.email = muestra.email
);
COMMIT;