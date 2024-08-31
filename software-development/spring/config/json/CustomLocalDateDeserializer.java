import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    public CustomLocalDateDeserializer() {
    }

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return Instant.parse(p.getText()).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
