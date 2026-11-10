package gorettileonardooracolo;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carica dal file JSON le risposte associate a ogni personalità.
 */
public final class CatalogoRisposte {
    /** Personalità che devono sempre avere almeno una risposta nel catalogo. */
    private static final List<String> PERSONALITA_RICHIESTE =
            List.of("Oracolo", "Chiromante", "Mago");

    /** Gestore JSON usato per delegare conversione e serializzazione a Gson. */
    private static final InterfacciaJson GESTORE_JSON = new GestoreJsonGson();

    /** Tipo completo necessario a Gson per ricostruire mappa, liste e stringhe. */
    private static final Type TIPO_MAPPA_RISPOSTE = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    private CatalogoRisposte() {
    }

    /**
        * Carica dal classpath il catalogo delle risposte e ne verifica il contenuto.
        *
        * @return catalogo validato e non modificabile
        * @throws IOException se il file manca o contiene JSON non valido
     */
    public static Map<String, List<String>> carica() throws IOException {
        try (InputStream input = CatalogoRisposte.class.getResourceAsStream("risposte.json")) {
            if (input == null) {
                throw new IOException("risposte.json non trovato nel progetto.");
            }

            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                try {
                    return valida(GESTORE_JSON.leggi(reader, TIPO_MAPPA_RISPOSTE));
                } catch (IllegalArgumentException | JsonParseException e) {
                    throw new IOException("risposte.json non valido: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
        * Valida e salva il catalogo nel percorso JSON indicato usando UTF-8.
        *
        * @param percorso file JSON in cui scrivere il catalogo
        * @param risposte catalogo da salvare
        * @throws IOException se il file non può essere scritto o la serializzazione fallisce
        * @throws IllegalArgumentException se il catalogo non rispetta la struttura richiesta
     */
    public static void salva(Path percorso, Map<String, List<String>> risposte) throws IOException {
        Map<String, List<String>> risposteVerificate = valida(risposte);
        try (Writer writer = Files.newBufferedWriter(percorso, StandardCharsets.UTF_8)) {
            GESTORE_JSON.scrivi(writer, risposteVerificate);
        } catch (JsonParseException e) {
            throw new IOException("Impossibile scrivere il catalogo JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Controlla che il catalogo contenga le personalità e le risposte richieste,
     * quindi ne crea una copia non modificabile.
     *
     * @param risposte dati da controllare
     * @return copia validata e non modificabile del catalogo
     * @throws IllegalArgumentException se il catalogo o una delle sue risposte è vuota o non valida
     */
    private static Map<String, List<String>> valida(Map<String, List<String>> risposte) {
        if (risposte == null || risposte.isEmpty()) {
            throw new IllegalArgumentException("file JSON vuoto");
        }

        Map<String, List<String>> risposteVerificate = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> voce : risposte.entrySet()) {
            String personalita = voce.getKey();
            List<String> frasi = voce.getValue();
            if (personalita == null || personalita.isBlank()) {
                throw new IllegalArgumentException("personalità non valida");
            }
            if (frasi == null || frasi.isEmpty()) {
                throw new IllegalArgumentException("elenco risposte vuoto per " + personalita);
            }
            if (frasi.stream().anyMatch(frase -> frase == null || frase.isBlank())) {
                throw new IllegalArgumentException("le risposte non possono essere vuote");
            }
            risposteVerificate.put(personalita, List.copyOf(frasi));
        }

        for (String personalita : PERSONALITA_RICHIESTE) {
            if (!risposteVerificate.containsKey(personalita)) {
                throw new IllegalArgumentException("mancano risposte per " + personalita);
            }
        }

        return Collections.unmodifiableMap(risposteVerificate);
    }
}
