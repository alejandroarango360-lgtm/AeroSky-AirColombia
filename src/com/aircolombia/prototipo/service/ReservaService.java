package com.aircolombia.prototipo.service;

import com.aircolombia.prototipo.dao.*;
import com.aircolombia.prototipo.model.Pasajero;
import com.aircolombia.prototipo.model.Vuelo;
import java.util.List;
import java.util.Random;

public class ReservaService {
    private VueloDAO vueloDAO = new VueloDAO();
    private ReservaDAO reservaDAO = new ReservaDAO();
    private PasajeroDAO pasajeroDAO = new PasajeroDAO();
    private ReservaPasajeroDAO reservaPasajeroDAO = new ReservaPasajeroDAO();
    private IntentoPagoDAO intentoPagoDAO = new IntentoPagoDAO();

    // Clases
    public static final String CLASE_ECONOMICA = "Economica";
    public static final String CLASE_EJECUTIVA = "Ejecutiva";
    public static final String CLASE_PRIMERA = "Primera Clase";

    public static double obtenerMultiplicador(String clase) {
        if (CLASE_EJECUTIVA.equals(clase)) return 1.6;
        if (CLASE_PRIMERA.equals(clase)) return 2.5;
        return 1.0;
    }

    // RN-07: Penalidades de cancelación por clase
    public static final double PENALIDAD_ECONOMICA = 0.30;
    public static final double PENALIDAD_EJECUTIVA = 0.10;
    public static final double PENALIDAD_PRIMERA   = 0.00;

    public static double obtenerPenalidadCancelacion(String clase) {
        if (CLASE_PRIMERA.equals(clase))   return PENALIDAD_PRIMERA;
        if (CLASE_EJECUTIVA.equals(clase)) return PENALIDAD_EJECUTIVA;
        return PENALIDAD_ECONOMICA;
    }

    // RF-02: multiplicadores por tipo de pasajero
    public static final double FACTOR_ADULTO = 1.00;
    public static final double FACTOR_NINO   = 0.75;
    public static final double FACTOR_INFANTE = 0.10;

    // ---------- GENERAR PNR AEROLÍNEA (RF-08 / CA-10) ----------
    private String generarPNR() {
        String pnr;
        // Letras sin O, I y números sin 0, 1 para evitar confusiones
        String caracteresPermitidos = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();

        do {
            StringBuilder sb = new StringBuilder("AV"); // Prefijo de AeroSky
            for (int i = 0; i < 4; i++) {
                int index = random.nextInt(caracteresPermitidos.length());
                sb.append(caracteresPermitidos.charAt(index));
            }
            pnr = sb.toString();
        } while (reservaDAO.buscarPorPNR(pnr) != null); // Validación de unicidad

        return pnr;
    }

    // ---------- CALCULAR TOTAL (versión con pasajeros - RF-02) ----------
    public double calcularTotal(int idVueloIda, Integer idVueloRegreso,
                                String claseViaje, int adultos, int ninos, int infantes) {
        Vuelo ida = vueloDAO.buscarPorId(idVueloIda);
        if (ida == null) return 0;

        double factorClase = obtenerMultiplicador(claseViaje);
        double factorPasajeros = adultos * FACTOR_ADULTO
                               + ninos   * FACTOR_NINO
                               + infantes * FACTOR_INFANTE;

        double total = ida.getTarifaBase() * factorClase * factorPasajeros;

        if (idVueloRegreso != null) {
            Vuelo regreso = vueloDAO.buscarPorId(idVueloRegreso);
            if (regreso != null) {
                total += regreso.getTarifaBase() * factorClase * factorPasajeros;
            }
        }
        return total;
    }

    public double calcularTotal(int idVueloIda, Integer idVueloRegreso, String claseViaje) {
        return calcularTotal(idVueloIda, idVueloRegreso, claseViaje, 1, 0, 0);
    }

    // ---------- INICIAR RESERVA (RF-02) ----------
    public String iniciarReserva(int idVueloIda, Integer idVueloRegreso,
                                 String claseViaje, int adultos, int ninos, int infantes) {
        int asientos = adultos + ninos;

        if (!vueloDAO.actualizarAsientos(idVueloIda, asientos)) {
            return null;
        }
        if (idVueloRegreso != null) {
            if (!vueloDAO.actualizarAsientos(idVueloRegreso, asientos)) {
                vueloDAO.devolverAsiento(idVueloIda, asientos);
                return null;
            }
        }

        double total = calcularTotal(idVueloIda, idVueloRegreso, claseViaje, adultos, ninos, infantes);
        if (total <= 0) {
            vueloDAO.devolverAsiento(idVueloIda, asientos);
            if (idVueloRegreso != null) vueloDAO.devolverAsiento(idVueloRegreso, asientos);
            return null;
        }

        String pnr = generarPNR();
        String tipoViaje = (idVueloRegreso == null) ? "Solo_Ida" : "Ida_Y_Vuelta";

        if (!reservaDAO.crearReservaPendiente(pnr, idVueloIda, idVueloRegreso,
                total, claseViaje, tipoViaje, asientos)) {
            vueloDAO.devolverAsiento(idVueloIda, asientos);
            if (idVueloRegreso != null) vueloDAO.devolverAsiento(idVueloRegreso, asientos);
            return null;
        }
        return pnr;
    }

    public String iniciarReserva(int idVueloIda, Integer idVueloRegreso, String claseViaje) {
        return iniciarReserva(idVueloIda, idVueloRegreso, claseViaje, 1, 0, 0);
    }

    // ==================== RF-02 & RF-07: PROCESAR PAGO CON COMPRADOR ====================
    public Object[] procesarPagoConPasajeros(String pnr, List<Pasajero> pasajeros, 
                                             Pasajero comprador, String metodo, String tarjeta) {
        if (pasajeros == null || pasajeros.isEmpty()) {
            return new Object[]{false, "Debe registrar al menos un pasajero.", 0.0};
        }

        double total = reservaDAO.obtenerTotalReserva(pnr);
        boolean aprobado = simularPasarela(metodo, tarjeta);
        String motivo = aprobado ? "Transacción aprobada por la pasarela" : obtenerMotivoRechazo(metodo, tarjeta);

        intentoPagoDAO.registrar(pnr, total, metodo,
                (tarjeta == null || tarjeta.isEmpty()) ? "N/A" : tarjeta,
                aprobado ? "Aprobado" : "Rechazado", motivo);

        if (!aprobado) {
            return new Object[]{false, motivo, total};
        }
        if (!reservaDAO.cambiarEstado(pnr, "Confirmada")) {
            return new Object[]{false, "Error interno al confirmar la reserva.", total};
        }

        // RF-07: Registrar y asociar al comprador
        int idComprador = -1;
        if (comprador != null) {
            idComprador = pasajeroDAO.obtenerOCrear(comprador);
        } else {
            // Si no hay comprador separado, el pasajero titular (el primero) es el comprador
            idComprador = pasajeroDAO.obtenerOCrear(pasajeros.get(0));
        }

        if (idComprador == -1) {
            return new Object[]{false, "Error al registrar los datos del comprador.", total};
        }
        reservaDAO.asociarComprador(pnr, idComprador);

        // Registrar TODOS los pasajeros (RF-02)
        int index = 0;
        for (Pasajero p : pasajeros) {
            int idPasajero = pasajeroDAO.obtenerOCrear(p);
            if (idPasajero == -1) {
                return new Object[]{false, "Error al registrar el pasajero " + p.getNombreCompleto(), total};
            }
            reservaPasajeroDAO.agregar(pnr, idPasajero, index == 0);
            index++;
        }

        return new Object[]{true,
                "Pago aprobado. Reserva confirmada con " + pasajeros.size() + " pasajero(s).",
                total};
    }

    // Versión antigua (compatibilidad)
    public Object[] procesarPago(String pnr, String nombre, String numeroDoc,
                                 String correo, String metodo, String tarjeta) {
        java.util.List<Pasajero> lista = new java.util.ArrayList<>();
        lista.add(new Pasajero("CC", numeroDoc, nombre, correo, null));
        Object[] r = procesarPagoConPasajeros(pnr, lista, null, metodo, tarjeta);
        return new Object[]{r[0], r[1]};
    }

    // ---------- EXPIRAR RESERVA ----------
    public void expirarReserva(String pnr) {
        Object[] info = reservaDAO.buscarPorPNR(pnr);
        if (info != null) {
            int idVuelo = ((Number) info[0]).intValue();
            Integer idRegreso = (info[1] == null) ? null : ((Number) info[1]).intValue();
            int asientos = obtenerAsientos(info);

            reservaDAO.cambiarEstado(pnr, "Expirada");
            vueloDAO.devolverAsiento(idVuelo, asientos);
            if (idRegreso != null) vueloDAO.devolverAsiento(idRegreso, asientos);
        }
    }

    // ---------- LIMPIAR RESERVAS HUÉRFANAS (Mantenimiento) ----------
    public int limpiarReservasHuerfanas(int minutos) {
        List<Object[]> pendientes = reservaDAO.obtenerReservasPendientesAntiguas(minutos);
        int count = 0;
        for (Object[] r : pendientes) {
            String pnr = (String) r[0];
            int idVuelo = ((Number) r[1]).intValue();
            Integer idRegreso = (r[2] == null) ? null : ((Number) r[2]).intValue();
            int asientos = ((Number) r[3]).intValue();

            if (reservaDAO.cambiarEstado(pnr, "Expirada")) {
                vueloDAO.devolverAsiento(idVuelo, asientos);
                if (idRegreso != null) vueloDAO.devolverAsiento(idRegreso, asientos);
                count++;
            }
        }
        return count;
    }

    // ==================== RN-07: CANCELAR CON PENALIDAD ====================
    public Object[] cancelarTiqueteConDetalle(String pnr) {
        Object[] info = reservaDAO.buscarPorPNR(pnr);
        if (info == null) {
            return new Object[]{false, "No existe una reserva con el PNR " + pnr, 0.0, 0.0, 0.0};
        }
        int idVuelo = ((Number) info[0]).intValue();
        Integer idRegreso = (info[1] == null) ? null : ((Number) info[1]).intValue();
        String estado = (String) info[2];
        int asientos = obtenerAsientos(info);

        if ("Cancelada".equals(estado)) {
            return new Object[]{false, "La reserva con PNR " + pnr + " ya fue cancelada.", 0.0, 0.0, 0.0};
        }
        if ("Expirada".equals(estado)) {
            return new Object[]{false, "La reserva con PNR " + pnr + " ya expiró.", 0.0, 0.0, 0.0};
        }
        if (!"Confirmada".equals(estado)) {
            return new Object[]{false, "Solo se pueden cancelar reservas confirmadas. Estado actual: " + estado, 0.0, 0.0, 0.0};
        }

        double total = 0.0;
        String clase = CLASE_ECONOMICA;
        Object[] detalle = reservaDAO.obtenerInfoReserva(pnr);
        if (detalle != null) {
            total = (Double) detalle[4];
            String c = (String) detalle[7];
            if (c != null) clase = c;
        }

        double factorPenalidad = obtenerPenalidadCancelacion(clase);
        double penalidad = total * factorPenalidad;
        double reembolso = total - penalidad;

        if (!reservaDAO.cambiarEstado(pnr, "Cancelada")) {
            return new Object[]{false, "No se pudo actualizar el estado de la reserva.", 0.0, 0.0, 0.0};
        }

        vueloDAO.devolverAsiento(idVuelo, asientos);
        if (idRegreso != null) vueloDAO.devolverAsiento(idRegreso, asientos);

        int pct = (int) Math.round(factorPenalidad * 100);
        String mensaje = "Reserva " + pnr + " cancelada correctamente.\n"
                + "Cupos devueltos: " + asientos + "\n\n"
                + "Total pagado: $ " + String.format("%,.0f", total) + "\n"
                + "Penalidad (" + pct + "%): -$ " + String.format("%,.0f", penalidad) + "\n"
                + "Reembolso: $ " + String.format("%,.0f", reembolso);

        return new Object[]{true, mensaje, total, penalidad, reembolso};
    }

    public String cancelarTiquete(String pnr) {
        Object[] r = cancelarTiqueteConDetalle(pnr);
        boolean exito = (Boolean) r[0];
        String msg = (String) r[1];
        return (exito ? "OK: " : "ERROR: ") + msg;
    }

    // Helper: obtiene los asientos de la reserva desde info[3]
    private int obtenerAsientos(Object[] info) {
        if (info.length >= 4 && info[3] != null) {
            return ((Number) info[3]).intValue();
        }
        return 1;
    }

    // ---------- SIMULACIÓN DE LA PASARELA ----------
    private boolean simularPasarela(String metodo, String numeroTarjeta) {
        if ("PSE".equals(metodo)) return true;
        if (numeroTarjeta == null || numeroTarjeta.isEmpty()) return false;
        return numeroTarjeta.charAt(numeroTarjeta.length() - 1) != '0';
    }

    private String obtenerMotivoRechazo(String metodo, String numeroTarjeta) {
        if (numeroTarjeta == null || numeroTarjeta.isEmpty()) {
            return "Número de tarjeta no proporcionado.";
        }
        if (numeroTarjeta.length() < 16) {
            return "El número de tarjeta debe tener 16 dígitos.";
        }
        if (numeroTarjeta.charAt(numeroTarjeta.length() - 1) == '0') {
            return "Fondos insuficientes o tarjeta bloqueada (código 51).";
        }
        return "Transacción rechazada por la pasarela.";
    }
    
        // ---------- ENVIAR CORREO CON TOKEN ----------
    public boolean enviarCorreoConfirmacion(String pnr, String emailDestinatario) {
        // Generar código de 6 dígitos
        String token = String.format("%06d", new java.util.Random().nextInt(999999));
        
        if (reservaDAO.guardarToken(pnr, token)) {
            return com.aircolombia.prototipo.utils.EmailService.enviarCodigoConfirmacion(emailDestinatario, pnr, token);
        }
        return false;
    }

    // ---------- VERIFICAR CÓDIGO (Para la vista) ----------
    public boolean verificarCodigo(String pnr, String token) {
        return reservaDAO.verificarToken(pnr, token);
    }
}