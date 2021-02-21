package io.frebel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BannerPrinter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BannerPrinter.class);

    public static void print() {
        try {
            InputStream resource = BannerPrinter.class.getClassLoader().getResourceAsStream("frebel-banner");
            if (resource == null) {
                LOGGER.error("Can't print frebel banner because banner file not exist!");
                return;
            }
            byte[] bytes = toByteArray(resource);
            String bannerStr = new String(bytes);
            LOGGER.info("\n" + bannerStr);
        } catch (IOException e) {
            LOGGER.error("Can't print frebel banner.", e);
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
