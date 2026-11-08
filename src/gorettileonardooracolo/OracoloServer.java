package gorettileonardooracolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Questo è il server: accetta i client e risponde alle loro richieste.
 */
public class OracoloServer {
    // Queste parole mi servono per capire che comando è arrivato.
    private static final String COMANDO_STORIA = "STORIA";
    private static final String COMANDO_ESCI = "ESCI";
    private static final String PREFISSO_PERSONA = "PERSONA_";
    private static final String PREFISSO_DOMANDA = "DOMANDA_";

    // Qui tengo tutti i client collegati, anche se ognuno è gestito da un thread diverso.
    private static final List<Socket> CLIENT_CONNESSI =
            Collections.synchronizedList(new ArrayList<>());

    // volatile fa vedere a tutti i thread quando è stato chiesto di chiudere il server.
    private static volatile boolean chiusuraRichiesta;
    private static Map<String, List<String>> risposteBot;

    /**
     * Avvio il server e affido ogni nuova connessione a un thread separato.
     */
    public static void main(String[] args) {
        chiusuraRichiesta = false;

        try {
            risposteBot = CatalogoRisposte.carica();
        } catch (IOException e) {
            System.err.println("Impossibile caricare le risposte dei bot: " + e.getMessage());
            return;
        }

        try (ServerSocket server = new ServerSocket(GorettiLeonardoOracolo.PORTA)) {
            System.out.println("OracoloServer in ascolto sulla porta " + GorettiLeonardoOracolo.PORTA);

            while (true) {
                Socket client = server.accept();
                CLIENT_CONNESSI.add(client);
                System.out.println("Nuovo client: " + client.getRemoteSocketAddress());

                GestoreClient gestore = new GestoreClient(client, server);
                Thread thread = new Thread(gestore);
                thread.start();
            }
        } catch (IOException e) {
            if (!chiusuraRichiesta) {
                System.err.println("Impossibile avviare o mantenere attivo il server: " + e.getMessage());
            }
        }

        System.out.println("OracoloServer terminato.");
    }

    /**
     * Qui gestisco le richieste di un client alla volta.
     */
    private static class GestoreClient implements Runnable {
        private final Socket client;
        private final ServerSocket server;
        private final Random casuale = new Random();
        private final List<String> domande = new ArrayList<>();
        private String persona = "Oracolo";

        GestoreClient(Socket client, ServerSocket server) {
            this.client = client;
            this.server = server;
        }

        /**
         * Leggo i comandi finché il client resta collegato, poi chiudo la connessione.
         */
        @Override
        public void run() {
            // Alla fine Java chiude da solo socket e flussi aperti qui.
            try (
                Socket socket = client;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String linea;
                while ((linea = in.readLine()) != null) {
                    linea = linea.trim();
                    // Lo rendo tutto maiuscolo così accetto il comando anche se scritto in modo diverso.
                    String comandoMaiuscolo = linea.toUpperCase(Locale.ROOT);

                    if (COMANDO_ESCI.equals(comandoMaiuscolo)) {
                        out.println("Server in chiusura.");
                        chiudiServer();
                        break;
                    }

                    if (COMANDO_STORIA.equals(comandoMaiuscolo)) {
                        mostraCronologia(out);
                    } else if (comandoMaiuscolo.startsWith(PREFISSO_PERSONA)) {
                        cambiaPersona(linea.substring(PREFISSO_PERSONA.length()), out);
                    } else if (comandoMaiuscolo.startsWith(PREFISSO_DOMANDA)) {
                        rispondi(linea.substring(PREFISSO_DOMANDA.length()), out);
                    } else {
                        out.println("Comando non riconosciuto.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Connessione client terminata.");
            } finally {
                CLIENT_CONNESSI.remove(client);
            }
        }

        /**
         * Mando al client le domande che ha fatto in questa sessione.
         */
        private void mostraCronologia(PrintWriter out) {
            if (domande.isEmpty()) {
                out.println("Non hai ancora fatto domande in questa sessione.");
            } else {
                out.println("Ultime domande: " + String.join(" | ", domande));
            }
        }

        /**
         * Controllo la personalità scelta e la salvo per questo client.
         */
        private void cambiaPersona(String scelta, PrintWriter out) {
            scelta = scelta.trim();

            if ("Oracolo".equalsIgnoreCase(scelta)) {
                persona = "Oracolo";
            } else if ("Chiromante".equalsIgnoreCase(scelta)) {
                persona = "Chiromante";
            } else if ("Mago".equalsIgnoreCase(scelta)) {
                persona = "Mago";
            } else {
                out.println("Personalità non valida. Scegli Oracolo, Chiromante o Mago.");
                return;
            }

            out.println("Hai scelto la personalità " + persona + ".");
        }

        /**
         * Salvo la domanda e rispondo con una frase a caso della personalità attiva.
         */
        private void rispondi(String domanda, PrintWriter out) {
            domanda = domanda.trim();
            if (domanda.isEmpty()) {
                out.println("Scrivi la domanda dopo DOMANDA_.");
                return;
            }

            domande.add(domanda);
            if (domande.size() > 5) {
                domande.remove(0);
            }

            out.println(persona + ": " + scegliRisposta());
        }

        /**
         * Quando esco, smetto di accettare client e chiudo quelli ancora collegati.
         */
        private void chiudiServer() {
            chiusuraRichiesta = true;
            try {
                server.close();
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura del server: " + e.getMessage());
            }

            // Blocco la lista mentre la scorro, così gli altri thread non la modificano insieme a me.
            synchronized (CLIENT_CONNESSI) {
                for (Socket socket : CLIENT_CONNESSI) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Errore durante la chiusura di un client: " + e.getMessage());
                    }
                }
            }
        }

        /**
         * Pesco una frase a caso tra quelle della personalità attiva.
         */
        private String scegliRisposta() {
            List<String> frasi = risposteBot.get(persona);
            if (frasi == null || frasi.isEmpty()) {
                throw new IllegalStateException("Nessuna risposta configurata per " + persona + ".");
            }

            int indice = casuale.nextInt(frasi.size());
            return frasi.get(indice);
        }
    }
}
