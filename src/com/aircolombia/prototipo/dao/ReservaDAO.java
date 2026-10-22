package com.aircolombia.prototipo.dao;

import java.sql.*;

public class ReservaDAO {

    // ---------- CREAR RESERVA PENDIENTE (con asientos) ----------
    public boolean crearReservaPendiente(String codigoPnr, int idVuelo, Integer idVueloRegreso,
                                         double total, String claseViaje, String tipoViaje,
                                         int asientos) {
        String sql = "INSERT INTO reserva (codigo_pnr, id_vuelo, id_vuelo_regreso, " +
                     "estado_reserva, clase_viaje, tipo_viaje, asientos, total) " +
                     "VALUES (?, ?, ?, 'Pendiente_Pago', ?, ?, ?, ?)";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigoPnr);
            pstmt.setInt(2, idVuelo);
            if (idVueloRegreso == null) {
                pstmt.setNull(3, Types.INTEGER);
            } else {
                pstmt.setInt(3, idVueloRegreso);
            }
            pstmt.setString(4, claseViaje);
            pstmt.setString(5, tipoViaje);
            pstmt.setInt(6, asientos);
            pstmt.setDouble(7, total);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error crearReservaPendiente: " + e.getMessage());
            return false;
        }
    }

    // ---------- CAMBIAR ESTADO ----------
    public boolean cambiarEstado(String codigoPnr, String nuevoEstado) {
        String sql = "UPDATE reserva SET estado_reserva = ? WHERE codigo_pnr = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nuevoEstado);
            pstmt.setString(2, codigoPnr);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error cambiarEstado: " + e.getMessage());
            return false;
        }
    }

    // ---------- BUSCAR POR PNR (con asientos) ----------
    // Retorna: [idVuelo, idVueloRegreso(Integer o null), estado, asientos, tipoViaje]
    public Object[] buscarPorPNR(String pnr) {
        String sql = "SELECT id_vuelo, id_vuelo_regreso, estado_reserva, asientos, tipo_viaje " +
                     "FROM reserva WHERE codigo_pnr = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pnr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int idRegreso = rs.getInt("id_vuelo_regreso");
                Integer regreso = rs.wasNull() ? null : idRegreso;
                return new Object[]{
                    rs.getInt("id_vuelo"),
                    regreso,
                    rs.getString("estado_reserva"),
                    rs.getInt("asientos"),
                    rs.getString("tipo_viaje")
                };
            }
        } catch (SQLException e) {
            System.out.println("Error buscarPorPNR: " + e.getMessage());
        }
        return null;
    }

    // ---------- OBTENER TOTAL DE UNA RESERVA ----------
    public double obtenerTotalReserva(String codigoPnr) {
        String sql = "SELECT total FROM reserva WHERE codigo_pnr = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigoPnr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            System.out.println("Error obtenerTotal: " + e.getMessage());
        }
        return 0;
    }

    // ---------- INFO COMPLETA PARA CANCELACIÓN ----------
    // Retorna: {pnr, origen, destino, fechaSalida, total, nombre, email, clase, tipoViaje, asientos}
    public Object[] obtenerInfoReserva(String codigoPnr) {
        String sql = "SELECT r.codigo_pnr, r.total, r.clase_viaje, r.tipo_viaje, " +
                     "       r.estado_reserva, r.asientos, " +
                     "       v.fecha_salida, ao.ciudad AS origen_ciudad, ad.ciudad AS destino_ciudad, " +
                     "       p.nombre_completo, p.email " +
                     "FROM reserva r " +
                     "INNER JOIN vuelo v ON r.id_vuelo = v.id_vuelo " +
                     "INNER JOIN aeropuerto ao ON v.origen_iata = ao.codigo_iata " +
                     "INNER JOIN aeropuerto ad ON v.destino_iata = ad.codigo_iata " +
                     "LEFT JOIN reserva_pasajero rp ON r.codigo_pnr = rp.codigo_pnr " +
                     "LEFT JOIN pasajero p ON rp.id_pasajero = p.id_pasajero " +
                     "WHERE r.codigo_pnr = ? LIMIT 1";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigoPnr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Object[]{
                        rs.getString("codigo_pnr"),
                        rs.getString("origen_ciudad"),
                        rs.getString("destino_ciudad"),
                        rs.getString("fecha_salida"),
                        rs.getDouble("total"),
                        rs.getString("nombre_completo"),
                        rs.getString("email"),
                        rs.getString("clase_viaje"),
                        rs.getString("tipo_viaje"),
                        rs.getInt("asientos")
                };
            }
        } catch (SQLException e) {
            System.out.println("Error info reserva: " + e.getMessage());
        }
        return null;
    }
        // ---------- OBTENER RESERVAS HUÉRFANAS ----------
    // Retorna una lista de Object[]: {codigo_pnr, id_vuelo, id_vuelo_regreso, asientos}
    public java.util.List<Object[]> obtenerReservasPendientesAntiguas(int minutos) {
        java.util.List<Object[]> lista = new java.util.ArrayList<>();
        String sql = "SELECT codigo_pnr, id_vuelo, id_vuelo_regreso, asientos " +
                     "FROM reserva " +
                     "WHERE estado_reserva = 'Pendiente_Pago' " +
                     "AND fecha_creacion < NOW() - INTERVAL ? MINUTE";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, minutos);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int idRegreso = rs.getInt("id_vuelo_regreso");
                Integer regreso = rs.wasNull() ? null : idRegreso;
                lista.add(new Object[]{
                    rs.getString("codigo_pnr"),
                    rs.getInt("id_vuelo"),
                    regreso,
                    rs.getInt("asientos")
                });
            }
        } catch (SQLException e) {
            System.out.println("Error obtenerReservasPendientesAntiguas: " + e.getMessage());
        }
        return lista;
    }
        // ---------- ASOCIAR COMPRADOR (RF-07) ----------
    public boolean asociarComprador(String codigoPnr, int idComprador) {
        String sql = "UPDATE reserva SET id_comprador = ? WHERE codigo_pnr = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idComprador);
            pstmt.setString(2, codigoPnr);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error asociarComprador: " + e.getMessage());
            return false;
        }
    }
   // ---------- GUARDAR TOKEN DE CONFIRMACIÓN ----------
    public boolean guardarToken(String pnr, String token) {
        String sql = "UPDATE reserva SET token_confirmacion = ? WHERE codigo_pnr = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            pstmt.setString(2, pnr);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error guardarToken: " + e.getMessage());
            return false;
        }
    }

    // ---------- VERIFICAR TOKEN ----------
    public boolean verificarToken(String pnr, String token) {
        String sql = "SELECT token_confirmacion FROM reserva WHERE codigo_pnr = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pnr);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String tokenGuardado = rs.getString("token_confirmacion");
                return tokenGuardado != null && tokenGuardado.equals(token);
            }
        } catch (SQLException e) {
            System.out.println("Error verificarToken: " + e.getMessage());
        }
        return false;
    }
}