package gorettileonardooracolo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carica dal file JSON le risposte associate a ogni personalità.
 */
final class CatalogoRisposte {
    // Il server può rispondere solo con queste personalità.
    private static final List<String> PERSONALITA_RICHIESTE =
            List.of("Oracolo", "Chiromante", "Mago");

    private CatalogoRisposte() {
    }

    /**
     * Leggo il file delle risposte e controllo che abbia una struttura valida.
     */
    static Map<String, List<String>> carica() throws IOException {
        try (InputStream input = CatalogoRisposte.class.getResourceAsStream("risposte.json")) {
            if (input == null) {
                throw new IOException("risposte.json non trovato nel progetto.");
            }

            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try {
                return new Parser(json).parse();
            } catch (IllegalArgumentException e) {
                // Rendo esplicito che l'errore è dovuto al contenuto del file.
                throw new IOException("risposte.json non valido: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Legge la struttura JSON prevista, composta da personalità e liste di risposte.
     */
    private static final class Parser {
        private final String json;
        private int posizione;

        Parser(String json) {
            this.json = json;
        }

        /**
         * Legge tutte le risposte e verifica che ogni personalità abbia almeno una frase.
         */
        Map<String, List<String>> parse() {
            Map<String, List<String>> risposte = new LinkedHashMap<>();
            saltaSpazi();
            richiedi('{');
            saltaSpazi();
            if (!consuma('}')) {
                do {
                    saltaSpazi();
                    String personalita = leggiStringa();
                    saltaSpazi();
                    richiedi(':');
                    saltaSpazi();

                    List<String> frasi = leggiArray();
                    // Evito che una chiave ripetuta nasconda le risposte lette prima.
                    if (risposte.putIfAbsent(personalita, List.copyOf(frasi)) != null) {
                        throw errore("personalità duplicata: " + personalita);
                    }

                    saltaSpazi();
                } while (consuma(','));
                richiedi('}');
            }

            saltaSpazi();
            if (posizione != json.length()) {
                throw errore("contenuto inatteso dopo l'oggetto JSON");
            }

            for (String personalita : PERSONALITA_RICHIESTE) {
                List<String> frasi = risposte.get(personalita);
                if (frasi == null || frasi.isEmpty()) {
                    throw errore("mancano risposte per " + personalita);
                }
            }
            return Collections.unmodifiableMap(risposte);
        }

        /**
         * Legge l'elenco di frasi di una personalità e rifiuta liste o risposte vuote.
         */
        private List<String> leggiArray() {
            List<String> frasi = new ArrayList<>();
            richiedi('[');
            saltaSpazi();
            if (consuma(']')) {
                throw errore("l'elenco delle risposte non può essere vuoto");
            }

            do {
                saltaSpazi();
                String frase = leggiStringa();
                if (frase.isBlank()) {
                    throw errore("le risposte non possono essere vuote");
                }
                frasi.add(frase);
                saltaSpazi();
            } while (consuma(','));
            richiedi(']');
            return frasi;
        }

        /**
         * Legge una stringa JSON, convertendo anche i caratteri di escape.
         */
        private String leggiStringa() {
            richiedi('"');
            StringBuilder valore = new StringBuilder();
            while (posizione < json.length()) {
                char carattere = json.charAt(posizione++);
                if (carattere == '"') {
                    return valore.toString();
                }
                if (carattere == '\\') {
                    if (posizione >= json.length()) {
                        throw errore("sequenza di escape incompleta");
                    }
                    char escape = json.charAt(posizione++);
                    switch (escape) {
                        // Questi escape rappresentano direttamente il carattere indicato.
                        case '"':
                        case '\\':
                        case '/':
                            valore.append(escape);
                            break;
                        case 'b':
                            valore.append('\b');
                            break;
                        case 'f':
                            valore.append('\f');
                            break;
                        case 'n':
                            valore.append('\n');
                            break;
                        case 'r':
                            valore.append('\r');
                            break;
                        case 't':
                            valore.append('\t');
                            break;
                        case 'u':
                            // Le sequenze Unicode sono scritte nel JSON come quattro cifre esadecimali.
                            valore.append(leggiUnicode());
                            break;
                        default:
                            throw errore("escape non valido");
                    }
                } else if (carattere < 0x20) {
                    throw errore("carattere di controllo non valido in una stringa");
                } else {
                    valore.append(carattere);
                }
            }
            throw errore("stringa non terminata");
        }

        /**
         * Converte le quattro cifre esadecimali di un escape Unicode in un carattere.
         */
        private char leggiUnicode() {
            if (posizione + 4 > json.length()) {
                throw errore("escape Unicode incompleto");
            }

            int valore = 0;
            for (int i = 0; i < 4; i++) {
                int cifra = Character.digit(json.charAt(posizione++), 16);
                if (cifra < 0) {
                    throw errore("escape Unicode non valido");
                }
                valore = valore * 16 + cifra;
            }
            return (char) valore;
        }

        /**
         * Avanza oltre gli spazi presenti tra gli elementi del JSON.
         */
        private void saltaSpazi() {
            while (posizione < json.length() && Character.isWhitespace(json.charAt(posizione))) {
                posizione++;
            }
        }

        /**
         * Controlla che il prossimo carattere sia quello atteso.
         */
        private void richiedi(char carattere) {
            if (!consuma(carattere)) {
                throw errore("atteso '" + carattere + "'");
            }
        }

        /**
         * Consuma il carattere indicato solo se si trova nella posizione corrente.
         */
        private boolean consuma(char carattere) {
            if (posizione < json.length() && json.charAt(posizione) == carattere) {
                posizione++;
                return true;
            }
            return false;
        }

        /**
         * Crea un errore indicando il punto del file in cui la lettura non è valida.
         */
        private IllegalArgumentException errore(String messaggio) {
            return new IllegalArgumentException(messaggio + " alla posizione " + posizione);
        }
    }
}
