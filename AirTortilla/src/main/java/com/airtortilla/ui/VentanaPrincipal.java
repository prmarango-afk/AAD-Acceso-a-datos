package com.airtortilla.ui;

import com.airtortilla.model.Reserva;
import com.airtortilla.repository.CsvReservaRepository;
import com.airtortilla.service.ConsultaFicheros;
import com.airtortilla.service.ExportadorPorPais;
import com.airtortilla.service.ReservaService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz gráfica (Swing) de AirTortilla Booking System.
 * Usa los mismos servicios que la versión de consola.
 */
public class VentanaPrincipal extends JFrame {

    private static final String[] COLUMNAS = {
            "Id", "Pasajero", "Origen", "País origen", "Destino", "País destino", "Fecha", "Vuelo", "Localizador"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ReservaService servicio;
    private final ExportadorPorPais exportador;
    private final ConsultaFicheros consulta;

    // Barra superior
    private final JTextField campoRuta = new JTextField(40);

    // Pestaña Reservas
    private final DefaultTableModel modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JComboBox<ReservaService.Criterio> comboCriterio = new JComboBox<>(ReservaService.Criterio.values());
    private final JTextField campoBuscarId = new JTextField(8);
    private final JTextField campoBuscarLocalizador = new JTextField(8);

    // Pestaña Ficheros
    private final DefaultListModel<Path> modeloFicheros = new DefaultListModel<>();
    private final JList<Path> listaFicheros = new JList<>(modeloFicheros);
    private final JTextArea areaContenido = new JTextArea();
    private final JButton botonSiguienteLinea = new JButton("Siguiente línea");
    private BufferedReader lectorLineaALinea; // fichero abierto en modo línea a línea

    // Feedback de las operaciones
    private final JTextArea areaRegistro = new JTextArea(5, 80);

    /**
     * @param servicio       gestión de las reservas
     * @param exportador     generador de ficheros por país
     * @param consulta       lectura de los ficheros generados
     * @param ficheroEntrada CSV que aparece seleccionado al abrir
     */
    public VentanaPrincipal(ReservaService servicio, ExportadorPorPais exportador,
                            ConsultaFicheros consulta, Path ficheroEntrada) {
        super("AirTortilla Booking System");
        this.servicio = servicio;
        this.exportador = exportador;
        this.consulta = consulta;
        campoRuta.setText(ficheroEntrada.toString());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        add(crearBarraCarga(), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);

        setSize(1100, 700);
        setLocationRelativeTo(null); // centrada en pantalla
        registrar("Aplicación iniciada. Carga un CSV para empezar.");
    }

    // ---------------------------------------------------------------
    // Construcción de la ventana
    // ---------------------------------------------------------------

    /** Crea la barra superior para elegir y cargar el CSV. */
    private JPanel crearBarraCarga() {
        JButton botonExaminar = new JButton("Examinar...");
        JButton botonCargar = new JButton("Cargar CSV");
        botonExaminar.addActionListener(e -> elegirFichero());
        botonCargar.addActionListener(e -> cargarCsv());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Fichero de reservas"));
        panel.add(new JLabel("CSV:"));
        panel.add(campoRuta);
        panel.add(botonExaminar);
        panel.add(botonCargar);
        return panel;
    }

    /** Crea las pestañas y, debajo, el registro de operaciones. */
    private JSplitPane crearCentro() {
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Reservas", crearPestanaReservas());
        pestanas.addTab("Ficheros por país", crearPestanaFicheros());

        areaRegistro.setEditable(false);
        areaRegistro.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane registro = new JScrollPane(areaRegistro);
        registro.setBorder(BorderFactory.createTitledBorder("Feedback de las operaciones"));

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pestanas, registro);
        division.setResizeWeight(0.8); // la mayor parte del espacio para las pestañas
        return division;
    }

    /** Crea la pestaña con la tabla de reservas y los controles de ordenar y buscar. */
    private JPanel crearPestanaReservas() {
        JButton botonOrdenar = new JButton("Ordenar");
        JButton botonBuscarId = new JButton("Buscar id");
        JButton botonBuscarLoc = new JButton("Buscar localizador");
        JButton botonTodas = new JButton("Mostrar todas");
        botonOrdenar.addActionListener(e -> ordenar());
        botonBuscarId.addActionListener(e -> buscarPorId());
        botonBuscarLoc.addActionListener(e -> buscarPorLocalizador());
        botonTodas.addActionListener(e -> mostrarEnTabla(servicio.getReservas()));
        campoBuscarId.addActionListener(e -> buscarPorId());          // Enter en el campo
        campoBuscarLocalizador.addActionListener(e -> buscarPorLocalizador());

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controles.add(new JLabel("Ordenar por:"));
        controles.add(comboCriterio);
        controles.add(botonOrdenar);
        controles.add(new JLabel("   Id:"));
        controles.add(campoBuscarId);
        controles.add(botonBuscarId);
        controles.add(new JLabel("   Localizador:"));
        controles.add(campoBuscarLocalizador);
        controles.add(botonBuscarLoc);
        controles.add(botonTodas);

        JTable tabla = new JTable(modeloTabla);
        tabla.setAutoCreateRowSorter(false); // la ordenación la hace nuestro MergeSort
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(controles, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    /** Crea la pestaña para generar los ficheros por país y consultarlos. */
    private JPanel crearPestanaFicheros() {
        JButton botonGenerar = new JButton("Crear ficheros por país de destino");
        JButton botonRefrescar = new JButton("Refrescar lista");
        JButton botonTotal = new JButton("Ver completo");
        JButton botonLineaALinea = new JButton("Ver línea a línea");
        botonGenerar.addActionListener(e -> generarFicheros());
        botonRefrescar.addActionListener(e -> refrescarFicheros());
        botonTotal.addActionListener(e -> verCompleto());
        botonLineaALinea.addActionListener(e -> empezarLineaALinea());
        botonSiguienteLinea.addActionListener(e -> siguienteLinea());
        botonSiguienteLinea.setEnabled(false);

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controles.add(botonGenerar);
        controles.add(botonRefrescar);
        controles.add(new JLabel("   "));
        controles.add(botonTotal);
        controles.add(botonLineaALinea);
        controles.add(botonSiguienteLinea);

        // La lista muestra solo el nombre del fichero, no la ruta completa
        listaFicheros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaFicheros.setCellRenderer((lista, valor, indice, seleccionado, foco) -> {
            JLabel etiqueta = new JLabel(valor.getFileName().toString());
            etiqueta.setOpaque(true);
            etiqueta.setBackground(seleccionado ? lista.getSelectionBackground() : lista.getBackground());
            etiqueta.setForeground(seleccionado ? lista.getSelectionForeground() : lista.getForeground());
            return etiqueta;
        });

        areaContenido.setEditable(false);
        areaContenido.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(listaFicheros), new JScrollPane(areaContenido));
        division.setDividerLocation(280);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(controles, BorderLayout.NORTH);
        panel.add(division, BorderLayout.CENTER);
        refrescarFicheros();
        return panel;
    }

    // ---------------------------------------------------------------
    // Acciones de los botones
    // ---------------------------------------------------------------

    /** Abre un selector de archivos para elegir el CSV. */
    private void elegirFichero() {
        JFileChooser selector = new JFileChooser(Path.of(campoRuta.getText()).toAbsolutePath().getParent().toFile());
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoRuta.setText(selector.getSelectedFile().getPath());
        }
    }

    /** Carga el CSV indicado, muestra las reservas en la tabla y el resumen en el registro. */
    private void cargarCsv() {
        Path fichero = Path.of(campoRuta.getText().trim());
        if (!Files.exists(fichero)) {
            error("No existe el fichero " + fichero.toAbsolutePath());
            return;
        }
        try {
            servicio.cargar(new CsvReservaRepository(fichero));
        } catch (IOException e) {
            error("No se pudo leer el CSV: " + e.getMessage());
            return;
        }
        mostrarEnTabla(servicio.getReservas());

        List<String> errores = servicio.getErroresCarga();
        registrar("CSV cargado: " + servicio.getReservas().size() + " reservas, "
                + servicio.getGruposConLocalizador() + " grupos con localizador, "
                + errores.size() + " líneas descartadas.");
        errores.forEach(e -> registrar("   - " + e));
    }

    /** Muestra en la tabla las reservas ordenadas por el criterio elegido. */
    private void ordenar() {
        if (!comprobarDatos()) return;
        ReservaService.Criterio criterio = (ReservaService.Criterio) comboCriterio.getSelectedItem();
        mostrarEnTabla(servicio.ordenar(criterio));
        registrar("Reservas ordenadas por " + criterio.name().toLowerCase().replace('_', ' ') + " (MergeSort).");
    }

    /** Muestra en la tabla la reserva con el id indicado. */
    private void buscarPorId() {
        if (!comprobarDatos()) return;
        String id = campoBuscarId.getText().trim();
        Optional<Reserva> reserva = servicio.buscarPorId(id);
        if (reserva.isPresent()) {
            mostrarEnTabla(List.of(reserva.get()));
            registrar("Reserva " + id + " encontrada (búsqueda binaria).");
        } else {
            registrar("No existe ninguna reserva con id " + id + ".");
        }
    }

    /** Muestra en la tabla todas las reservas del localizador indicado. */
    private void buscarPorLocalizador() {
        if (!comprobarDatos()) return;
        String localizador = campoBuscarLocalizador.getText().trim();
        List<Reserva> grupo = servicio.buscarPorLocalizador(localizador);
        if (grupo.isEmpty()) {
            registrar("No hay reservas con el localizador " + localizador + ".");
        } else {
            mostrarEnTabla(grupo);
            registrar("Localizador " + localizador + ": " + grupo.size() + " reservas.");
        }
    }

    /** Genera los ficheros por país de destino y actualiza la lista. */
    private void generarFicheros() {
        if (!comprobarDatos()) return;
        try {
            List<Path> creados = exportador.exportar(servicio.getReservas());
            registrar(creados.size() + " ficheros creados en " + exportador.getCarpetaSalida().toAbsolutePath());
        } catch (IOException e) {
            error("No se pudieron crear los ficheros: " + e.getMessage());
        }
        refrescarFicheros();
    }

    /** Vuelve a leer la carpeta de salida y actualiza la lista de ficheros. */
    private void refrescarFicheros() {
        modeloFicheros.clear();
        try {
            consulta.listar().forEach(modeloFicheros::addElement);
        } catch (IOException e) {
            error("No se pudo leer la carpeta de salida: " + e.getMessage());
        }
    }

    /** Modalidad total: muestra el fichero seleccionado entero. */
    private void verCompleto() {
        Path fichero = ficheroSeleccionado();
        if (fichero == null) return;
        cerrarLineaALinea();
        try {
            areaContenido.setText(String.join("\n", consulta.leerTodo(fichero)));
            areaContenido.setCaretPosition(0);
            registrar("Mostrado completo: " + fichero.getFileName());
        } catch (IOException e) {
            error("No se pudo leer el fichero: " + e.getMessage());
        }
    }

    /** Modalidad línea a línea: abre el fichero seleccionado y muestra la primera línea. */
    private void empezarLineaALinea() {
        Path fichero = ficheroSeleccionado();
        if (fichero == null) return;
        cerrarLineaALinea();
        try {
            lectorLineaALinea = consulta.abrir(fichero);
        } catch (IOException e) {
            error("No se pudo abrir el fichero: " + e.getMessage());
            return;
        }
        areaContenido.setText("");
        botonSiguienteLinea.setEnabled(true);
        registrar("Leyendo línea a línea: " + fichero.getFileName());
        siguienteLinea();
    }

    /** Añade la siguiente línea del fichero abierto en modo línea a línea. */
    private void siguienteLinea() {
        if (lectorLineaALinea == null) return;
        try {
            String linea = lectorLineaALinea.readLine();
            if (linea == null) {
                areaContenido.append("--- fin del fichero ---\n");
                cerrarLineaALinea();
            } else {
                areaContenido.append(linea + "\n");
            }
        } catch (IOException e) {
            error("Error leyendo el fichero: " + e.getMessage());
            cerrarLineaALinea();
        }
    }

    // ---------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------

    /** Cierra el fichero abierto en modo línea a línea, si lo hay. */
    private void cerrarLineaALinea() {
        botonSiguienteLinea.setEnabled(false);
        if (lectorLineaALinea != null) {
            try {
                lectorLineaALinea.close();
            } catch (IOException ignorada) {
                // no afecta al usuario
            }
            lectorLineaALinea = null;
        }
    }

    /** @return el fichero seleccionado en la lista, o null (avisando al usuario) si no hay ninguno */
    private Path ficheroSeleccionado() {
        Path fichero = listaFicheros.getSelectedValue();
        if (fichero == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un fichero de la lista.");
        }
        return fichero;
    }

    /** @return true si hay reservas cargadas; si no, avisa al usuario */
    private boolean comprobarDatos() {
        if (!servicio.hayDatos()) {
            JOptionPane.showMessageDialog(this, "Primero carga el CSV de reservas.");
            return false;
        }
        return true;
    }

    /** Sustituye el contenido de la tabla por las reservas indicadas. */
    private void mostrarEnTabla(List<Reserva> reservas) {
        modeloTabla.setRowCount(0);
        for (Reserva r : reservas) {
            modeloTabla.addRow(new Object[]{
                    r.getId(), r.getNombrePasajero(), r.getOrigen(), r.getPaisOrigen(),
                    r.getDestino(), r.getPaisDestino(), r.getFechaReserva(), r.getNumeroVuelo(),
                    r.getLocalizador().isEmpty() ? "-" : r.getLocalizador()});
        }
    }

    /** Añade un mensaje con la hora al registro de operaciones. */
    private void registrar(String mensaje) {
        areaRegistro.append("[" + LocalTime.now().format(HORA) + "] " + mensaje + "\n");
        areaRegistro.setCaretPosition(areaRegistro.getDocument().getLength());
    }

    /** Muestra un error en una ventana emergente y en el registro. */
    private void error(String mensaje) {
        registrar("ERROR: " + mensaje);
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
