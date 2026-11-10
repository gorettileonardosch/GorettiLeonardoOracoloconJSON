package gorettileonardooracolo;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * Definisce il contratto comune per leggere e scrivere dati in formato JSON,
 * indipendentemente dalla libreria usata per la conversione.
 */
public interface InterfacciaJson {
    /**
     * Deserializza il contenuto del lettore nel tipo specificato.
     *
     * @param reader sorgente da cui leggere il JSON
     * @param tipo tipo Java dell'oggetto da ricostruire
     * @param <T> tipo del valore restituito
     * @return oggetto ottenuto dal JSON
     */
    <T> T leggi(Reader reader, Type tipo);

    /**
     * Serializza il valore ricevuto e lo scrive nella destinazione indicata.
     *
     * @param writer destinazione in cui scrivere il JSON
     * @param valore oggetto da convertire in JSON
     */
    void scrivi(Writer writer, Object valore);
}