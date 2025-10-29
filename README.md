# ms-andesstay-bff - Backend-For-Frontend y Security Gateway

**Asignatura:** DSY1107 - Desarrollo Cloud Native I  
**Evaluación:** Evaluación Parcial N°1 (EP1)  
**Caso de Estudio:** Caso 5 - Red de Hoteles, Cabañas y Lodges AndesStay  
**Alumno:** Samuel Urzua Moraga (`sa.urzua@duocuc.cl`)  

---

## 📋 Descripción

Microservicio **BFF (Backend-For-Frontend)** desarrollado en **Spring Boot 3 (Java 21)**. Actúa como capa de seguridad perimetral y punto único de entrada para el frontend de AndesStay, integrándose con **Azure Active Directory** como IDaaS y enrutando peticiones a los microservicios de dominio (`ms-andesstay-reservations` y `ms-andesstay-catalog`).

---

## 🔐 Características de Seguridad

- **OAuth2 Resource Server:** Validación de tokens JWT emitidos por Microsoft Entra ID (`sts.windows.net/64ab2952-0d1a-49ef-a64b-0494bc1432e8`).
- **Verificación Criptográfica:** Valida firmas digitales utilizando el JWK Set URI oficial de Microsoft:
  `https://login.microsoftonline.com/64ab2952-0d1a-49ef-a64b-0494bc1432e8/discovery/v2.0/keys`
- **Control de Acceso Basado en Roles (RBAC):** Extracción de roles desde el claim `roles` del JWT (`Admin`, `Recepcionista`, `Huesped`, `Auditor`).
- **Manejo Estándar de Errores (401 y 403):** Respuestas en formato JSON estructurado con `AuthenticationEntryPoint` y `AccessDeniedHandler` personalizados.

---

## 🏗️ Arquitectura y Enrutamiento

El BFF desacopla la seguridad perimetral de la lógica interna de los microservicios de dominio:
- `/api/reservations/**` -> Enruta internamente a `ms-andesstay-reservations` (puerto 8081).
- `/api/catalog/**` -> Enruta internamente a `ms-andesstay-catalog` (puerto 8082).
- `/api/report/**` & `/api/audit/**` -> Proveen endpoints base para la analítica de eventos.

---

## 🧪 Pruebas Unitarias y de Integración

Incluye suite de pruebas automatizadas con **JUnit 5**, **Spring Security Test** y **MockMvc**:

```bash
.\mvnw.cmd test
```

### Casos de prueba cubiertos:
1. `testUnauthenticatedAccessReturns401()`: Peticiones anónimas a rutas protegidas son rechazadas con 401.
2. `testHuespedCanReadReservations()`: Usuario con rol `Huesped` puede consultar reservas.
3. `testHuespedCannotUpdateReservationStatus()`: Usuario con rol `Huesped` recibe 403 al intentar cambiar estado de reservas.
4. `testRecepcionistaCanUpdateReservationStatus()`: Usuario con rol `Recepcionista` puede actualizar estados.
5. `testAuditorCannotUpdateReservationStatus()`: Usuario con rol `Auditor` no puede modificar reservas.
6. `testCatalogIsPubliclyAccessible()`: Catálogo accesible públicamente sin autenticación previa.
