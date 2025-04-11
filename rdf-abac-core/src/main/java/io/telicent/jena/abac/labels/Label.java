package io.telicent.jena.abac.labels;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public record Label(byte[] data, Charset charset) {

    public String getText() {
        return new String(data, charset);
    }

    public static Label fromText(String text, Charset charset) {
        return new Label(text.getBytes(), charset);
    }

    public static Label fromText(String text){
        return Label.fromText(text, StandardCharsets.UTF_8);
    }

    ;
}
