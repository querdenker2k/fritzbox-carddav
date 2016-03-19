package org.czdev.fritzbox.carddav.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Robert Delbrueck
 */
public class Configuration {
    public static final String FILENAME = "fritzbox-carddav.properties";
    public static final String KEY_NAMES_CARDDAV = "carddav.names";
    public static final String KEY_ADDRESS_CARDDAV = "carddav.<name>.address";
    public static final String KEY_USERNAME_CARDDAV = "carddav.<name>.username";
    public static final String KEY_PASSWORD_CARDDAV = "carddav.<name>.password";
    public static final String KEY_ADDRESS_FRITZBOX = "fritzbox.address";
    public static final String KEY_USERNAME_FRITZBOX = "fritzbox.username";
    public static final String KEY_PASSWORD_FRITZBOX = "fritzbox.password";
    public static final String KEY_PHONEBOOK = "fritzbox.phonebook";
    public static final String KEY_DELETE_PHONEBOOK = "fritzbox.clear_phonebook";
    private static Configuration instance;
    private final Properties p = new Properties();

    private Configuration() {
        try {
            InputStream inputStream = ClassLoader.getSystemResourceAsStream(FILENAME);
            if (inputStream == null) {
                throw new IllegalStateException("cannot find config: " + FILENAME);
            }
            this.p.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("cannot load configuration: " + FILENAME);
        }
    }

    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    public String getPropertyAsString(String key, String placeholderKey, String placeholderValue) {
        return this.p.getProperty(key.replace(placeholderKey, placeholderValue));
    }

    public String getPropertyAsString(String key) {
        return this.p.getProperty(key);
    }
}
