package net.maritimeconnectivity.serviceregistry.utils;

import java.io.*;


/**
 * Utility class for performing RW operations on files.
 *
 * @author Jakob Svenningsen (email: jakob@dmc.international)
 */
public final class ReconnectTokenUtil {

    private static final String filename = "rc_token.txt";

    private ReconnectTokenUtil() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    public static void writeRcToken(String text) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(text);
        }
    }

    public static String readRcToken() throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        }
        return content.toString();
    }

}
