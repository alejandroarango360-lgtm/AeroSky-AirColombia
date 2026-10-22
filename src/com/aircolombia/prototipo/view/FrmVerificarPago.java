package com.aircolombia.prototipo.view;

import com.aircolombia.prototipo.model.Pasajero;
import com.aircolombia.prototipo.service.ReservaService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class FrmVerificarPago extends JDialog {

    private JTextField txtCodigo;
    private String pnr, metodoPago, tarjeta, correoDestino;
    private List<Pasajero> pasajeros;
    private Pasajero comprador;
    private ReservaService reservaService;
    private JDialog ventanaResumen;
    private boolean pagoExitoso = false;

    // ESTE ES EL CONSTRUCTOR QUE FALTABA
    public FrmVerificarPago(JDialog padre, String pnr, List<Pasajero> pasajeros, Pasajero comprador,
                            String metodoPago, String tarjeta, String correoDestino) {
        super(padre, "Confirmación de Pago", true);
        this.ventanaResumen = padre;
        this.pnr = pnr;
        this.pasajeros = pasajeros;
        this.comprador = comprador;
        this.metodoPago = metodoPago;
        this.tarjeta = tarjeta;
        this.correoDestino = correoDestino;
        this.reservaService = new ReservaService();
        
        initComponents();
        setLocationRelativeTo(padre);
    }

    public boolean isPagoExitoso() {
        return pagoExitoso;
    }

    private void initComponents() {
        setSize(420, 300);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0xC8102E));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel lblTitulo = new JLabel("Verificación de Seguridad");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitulo.setForeground(Color.WHITE);
        header.add(lblTitulo, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // CENTRO
        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setBackground(Color.WHITE);
        centro.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblInfo = new JLabel("<html><div style='text-align:center;'>"
                + "Hemos enviado un código de 6 dígitos a:<br>"
                + "<b style='color:#C8102E;'>" + correoDestino + "</b><br><br>"
                + "Ingrésalo a continuación para completar el pago:</div></html>");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        centro.add(lblInfo);
        centro.add(Box.createRigidArea(new Dimension(0, 15)));

        txtCodigo = new JTextField();
        txtCodigo.setFont(new Font("Consolas", Font.BOLD, 24));
        txtCodigo.setHorizontalAlignment(JTextField.CENTER);
        txtCodigo.setMaximumSize(new Dimension(200, 40));
        txtCodigo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD5CFC0)),
                new EmptyBorder(5, 10, 5, 10)));
        centro.add(txtCodigo);

        add(centro, BorderLayout.CENTER);

        // BOTONES
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        botones.setBackground(Color.WHITE);
        botones.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD5CFC0)));

        JButton btnCancelar = new JButton("Cancelar Pago");
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelar.setBackground(new Color(0xE5E5E5));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> cancelarProceso());

        JButton btnVerificar = new JButton("Verificar y Pagar");
        btnVerificar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnVerificar.setBackground(new Color(0x15803D));
        btnVerificar.setForeground(Color.WHITE);
        btnVerificar.setFocusPainted(false);
        btnVerificar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerificar.addActionListener(e -> verificarPago());

        botones.add(btnCancelar);
        botones.add(btnVerificar);
        add(botones, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cancelarProceso();
            }
        });
    }

    private void verificarPago() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese el código enviado a su correo.");
            return;
        }

        if (reservaService.verificarCodigo(pnr, codigo)) {
            Object[] resultado = reservaService.procesarPagoConPasajeros(pnr, pasajeros, comprador, metodoPago, tarjeta);
            boolean exito = (boolean) resultado[0];
            String mensaje = (String) resultado[1];

            if (exito) {
                pagoExitoso = true;
                JOptionPane.showMessageDialog(this, "Pago aprobado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Error al procesar el pago: " + mensaje, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "El código ingresado es incorrecto.", "Código Inválido", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarProceso() {
        int confirm = JOptionPane.showConfirmDialog(this, 
                "¿Seguro que desea cancelar el pago?\nSe liberarán los cupos reservados.",
                "Cancelar Pago", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            reservaService.expirarReserva(pnr);
            pagoExitoso = false;
            dispose();
        }
    }
}