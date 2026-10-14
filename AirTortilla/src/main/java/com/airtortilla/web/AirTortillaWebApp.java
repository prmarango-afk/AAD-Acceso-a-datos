package com.airtortilla.web;

import com.airtortilla.service.ConsultaFicheros;
import com.airtortilla.service.ExportadorPorPais;
import com.airtortilla.service.ReservaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;

/**
 * Aplicación web (Spring Boot). Crea los servicios una sola vez y los comparte
 * con el controlador REST. La página está en src/main/resources/static.
 */
@SpringBootApplication
public class AirTortillaWebApp {

    @Value("${server.port:8080}")
    private int puerto;

    @Value("${airtortilla.abrir-navegador:true}")
    private boolean abrirNavegador;

    /** Arranca el servidor web. */
    public static void arrancar(String[] args) {
        SpringApplication app = new SpringApplication(AirTortillaWebApp.class);
        app.setHeadless(false); // necesario para poder abrir el navegador
        app.run(args);
    }

    @Bean
    public ReservaService reservaService() {
        return new ReservaService();
    }

    @Bean
    public ExportadorPorPais exportadorPorPais(@Value("${airtortilla.salida}") String salida) {
        return new ExportadorPorPais(Path.of(salida));
    }

    @Bean
    public ConsultaFicheros consultaFicheros(@Value("${airtortilla.salida}") String salida) {
        return new ConsultaFicheros(Path.of(salida));
    }

    /** Cuando el servidor está listo, abre la página en el navegador. */
    @EventListener(ApplicationReadyEvent.class)
    public void abrirEnNavegador() {
        String url = "http://localhost:" + puerto;
        System.out.println("AirTortilla disponible en " + url);
        if (!abrirNavegador) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.out.println("No se pudo abrir el navegador; entra manualmente en " + url);
        }
    }
}
