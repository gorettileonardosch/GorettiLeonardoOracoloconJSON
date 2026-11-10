package gorettileonardooracolo;

import com.google.gson.Gson;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * Implementa il contratto JSON delegando a Google Gson la deserializzazione
 * e la serializzazione degli oggetti.
 */
final class GestoreJsonGson implements InterfacciaJson {
    /** Istanza Gson condivisa dalle operazioni di lettura e scrittura. */
    private final Gson gson = new Gson();

    /** {@inheritDoc} */
    @Override
    public <T> T leggi(Reader reader, Type tipo) {
        return gson.fromJson(reader, tipo);
    }

    /** {@inheritDoc} */
    @Override
    public void scrivi(Writer writer, Object valore) {
        gson.toJson(valore, writer);
    }
}