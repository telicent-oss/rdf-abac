package io.telicent.jena.abac.labels;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable class to represent security labels
 *
 * @param data    a byte array of the encoded label
 * @param charset the character set used to encode the label
 */
public record Label(byte[] data, Charset charset) {

    /**
     * Creates a Label from a string value and character set
     *
     * @param text    the label text
     * @param charset the character set to use
     * @return the new Label
     */
    public static Label fromText(final String text, final Charset charset) {
        return new Label(text.getBytes(), charset);
    }

    /**
     * Convenience method to create a label from the default character encoding of UTF-8
     *
     * @param text the label text
     * @return the new Label
     */
    public static Label fromText(final String text) {
        return Label.fromText(text, StandardCharsets.UTF_8);
    }

    /**
     * Returns the string value of the label
     *
     * @return the label text
     */
    public String getText() {
        return new String(data, charset);
    }

    /**
     * Arrays need to be compared using the Arrays.equals() method
     *
     * @param other the reference object with which to compare.
     * @return true if the contents of the label are the same otherwise false
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Label)) {
            return false;
        }
        return (Arrays.equals(this.data, ((Label) other).data) && this.charset == ((Label) other).charset);
    }

    /**
     * The hash code of the byte array needs to be generated using the Arrays.hashCode() method and the hashcode of the
     * charset needs to be generated using the Objects.hashCode() method
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data) + Objects.hashCode(charset);
    }

    @Override
    public String toString() {
        return getText();
    }
}
