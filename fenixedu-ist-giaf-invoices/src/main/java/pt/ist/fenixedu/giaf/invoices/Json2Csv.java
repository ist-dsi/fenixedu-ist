package pt.ist.fenixedu.giaf.invoices;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Json2Csv implements Closeable {

    private final PrintStream stream;

    private final String fieldSeperator;

    private boolean isFirstLine = true;

    public Json2Csv(final String outputPath, final String fieldSeperator) throws FileNotFoundException {
        this.stream = new PrintStream(outputPath);
        this.fieldSeperator = fieldSeperator;
    }

    public void write(final JsonObject jo, boolean nl) {
        if (isFirstLine) {
            writeHeaders(jo);
        }

        for (final Entry<String, JsonElement> e : jo.entrySet()) {
            final JsonElement value = e.getValue();
            if (value.isJsonArray()) {
                final JsonArray a = value.getAsJsonArray();
                for (final JsonElement ae : a) {
                    write(ae.getAsJsonObject(), false);
                }
            } else if (value.isJsonNull()) {
                stream.print(" ");
                stream.print(fieldSeperator);
            } else if (value.isJsonObject()) {
                write(value.getAsJsonObject(), false);
            } else if (value.isJsonPrimitive()) {
                stream.print(value.getAsString());
                stream.print(fieldSeperator);
            } else {
                throw new Error("Unreachable code.");
            }
        }
        if (nl) {
            stream.println();
        }
    }

    private void writeHeaders(final JsonObject jo) {
        for (final Entry<String, JsonElement> e : jo.entrySet()) {
            final String key = e.getKey();
            final JsonElement value = e.getValue();
            if (value.isJsonArray()) {
                final JsonArray a = value.getAsJsonArray();
                for (final JsonElement ae : a) {
                    writeHeaders(ae.getAsJsonObject());
                }
            } else if (value.isJsonNull()) {
                stream.print(key);
                stream.print(fieldSeperator);
            } else if (value.isJsonObject()) {
                writeHeaders(value.getAsJsonObject());
            } else if (value.isJsonPrimitive()) {
                stream.print(key);
                stream.print(fieldSeperator);
            } else {
                throw new Error("Unreachable code.");
            }
        }
        stream.println();        
        isFirstLine = false;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

}
