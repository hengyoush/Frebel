package test;

import io.frebel.util.BannerPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Log {
    private static final Logger log = LoggerFactory.getLogger(Log.class);
    public static void main(String[] args) throws URISyntaxException, IOException {
        URL resource = BannerPrinter.class.getClassLoader().getResource("frebel-banner");
        if (resource == null) {
            log.error("Can't print frebel banner because banner file doesn't exist!");
            return;
        }
        byte[] bytes = Files.readAllBytes(Paths.get(resource.toURI()));
        String bannerStr = new String(bytes);
        log.info("\n" + bannerStr);
    }
}
