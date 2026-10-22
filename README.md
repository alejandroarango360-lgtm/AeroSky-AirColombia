# ✈️ AeroSky - Sistema de Gestión Aerocomercial

Sistema de gestión aerocomercial desarrollado en **Java (Swing)** con base de datos **MySQL**, que automatiza los procesos de comercialización, reserva, emisión de tiquetes y gestiones operativas de vuelos.

Proyecto académico de la asignatura **Análisis y Diseño de Software** — Fundación Universitaria Konrad Lorenz.

---

## 🚀 Características Principales

- 🔍 **Consulta de vuelos** en tiempo real por origen, destino, fecha de ida y regreso.
- ⚙️ **Filtros y ordenamiento** dinámico (por precio, duración, escalas y horario).
- 🔄 **Validación de itinerarios** de ida y vuelta (evita seleccionar regresos anteriores a la ida).
- 👥 **Registro múltiple de pasajeros** categorizados por edad (Adultos 18+, Jóvenes 2-17, Infantes 0-2).
- 🛒 **Separación de comprador y pasajeros:** Permite registrar los datos del comprador cuando no viaja.
- 🎫 **Compra de tiquetes** con bloqueo temporal de cupos (15 minutos) y liberación automática.
- 💳 **Resumen de compra** con desglose de tarifas, impuestos y total antes de pagar.
- 🔐 **Verificación 2FA:** Envío de código de seguridad real al correo del comprador para confirmar la transacción.
- 📧 **Emisión de tiquetes:** Generación de PNR único (6 caracteres) y número de Tiquete Electrónico.
- ❌ **Cancelación de tiquetes** con aplicación de penalidades según la clase tarifaria.
- 🛠️ **Módulo de administración** para gestionar vuelos (CRUD).
- 📊 **Auditoría de pagos** (cada intento aprobado/rechazado queda registrado en la BD).

---

## 🏗️ Arquitectura

El sistema sigue una **arquitectura de 3 capas** + utilidades:

```text
com.aircolombia.prototipo
├── dao/          → Capa de Persistencia (acceso a datos)
├── model/        → Modelos de dominio (POJOs)
├── service/      → Capa de Negocio (lógica y orquestación)
├── view/         → Capa de Presentación (interfaces Swing)
└── utils/        → Utilidades (ej. EmailService para envío de correos por SMTP)
🗄️ Base de Datos
Nombre: aircolombia_mvp (MySQL/MariaDB)

Tablas principales:
Tabla	Descripción
aeropuerto	Catálogo de aeropuertos (IATA, ciudad, país)
aeronave	Flota de aeronaves
vuelo	Vuelos programados (origen, destino, fecha, tarifa, escalas)
pasajero	Datos de los pasajeros (incluye residencia y contacto)
reserva	Reservas con código PNR, id_comprador y token de verificación
reserva_pasajero	Relación reserva ↔ pasajero (marcando al titular)
intentos_pago	Auditoría de transacciones de pago
🛠️ Tecnologías Utilizadas
Java 8+

Swing (interfaz gráfica)

JDBC con MySQL Connector/J 9.4.0

JavaMail API (envío de correos SMTP para verificación de pagos)

MySQL / MariaDB (vía XAMPP)

NetBeans IDE

⚙️ Instalación y Ejecución
Requisitos previos:
Java JDK 8 o superior

MySQL / XAMPP

NetBeans IDE

Drivers: mysql-connector-j-9.4.0.jar, javax.mail.jar, jakarta.activation.jar

Pasos:
Clonar el repositorio:

bash
git clone https://github.com/alejandroarango360-lgtm/AeroSky-AirColombia.git
Crear la base de datos:

Abre phpMyAdmin

Ejecuta el script SQL del proyecto para crear la BD aircolombia_mvp.

Abrir el proyecto en NetBeans:

File → Open Project → selecciona la carpeta del proyecto.

Agregar las librerías:

Clic derecho en Libraries → Add JAR/Folder → agrega los 3 archivos .jar mencionados arriba.

Configurar el correo (Opcional):

En utils/EmailService.java, configura tu correo de Gmail y tu contraseña de aplicación para probar el envío del código de verificación.

Ejecutar:

Run File sobre FrmPrincipal.java

📖 Flujo del Sistema
Caso de uso principal: Compra de tiquetes
El usuario busca vuelos por origen, destino, fechas y cantidad de pasajeros.

El sistema muestra vuelos disponibles (con filtros de precio, duración, escalas).

El usuario selecciona un vuelo y completa los datos de todos los pasajeros.

El sistema crea una reserva pendiente y bloquea los cupos por 15 minutos.

El usuario llega al Resumen de Compra (ve el desglose de impuestos y el total).

Al darle "Pagar Ahora", el sistema envía un código de 6 dígitos al correo real del usuario.

El usuario ingresa el código en la app (Verificación 2FA).

Si el código es correcto → se emite el Tiquete y se confirma la reserva.

Si el usuario cancela o el tiempo expira → se liberan los cupos automáticamente.

👥 Autores


Alejandro Arango

konrad Lorenz. Uso educativo.
