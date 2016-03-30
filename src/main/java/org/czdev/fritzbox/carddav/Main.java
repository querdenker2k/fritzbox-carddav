package org.czdev.fritzbox.carddav;

import org.czdev.fritzbox.carddav.config.Configuration;
import org.czdev.fritzbox.carddav.model.Contact;
import org.czdev.fritzbox.carddav.service.FritzboxUpdater;
import org.czdev.fritzbox.carddav.service.VCardLoader;

import java.util.List;

/**
 * @author Robert Delbrueck
 */
public class Main {
    public static void main(String[] args) {
        if (args.length == 1) {
            final Configuration instance = Configuration.getInstance();
            instance.init(args[0]);
        }

        VCardLoader vCardLoader = new VCardLoader();
        FritzboxUpdater fritzboxUpdater = new FritzboxUpdater();

        List<Contact> contacts = vCardLoader.loadContacts();
        fritzboxUpdater.updatePhonebook(contacts);
    }
}
