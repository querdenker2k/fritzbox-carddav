package org.czdev.fritzbox.carddav.service;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.exceptions.VCardParseException;
import net.sourceforge.cardme.vcard.types.AdrType;
import net.sourceforge.cardme.vcard.types.TelType;
import net.sourceforge.cardme.vcard.types.params.TelParamType;
import org.apache.commons.io.IOUtils;
import org.czdev.fritzbox.carddav.config.Configuration;
import org.czdev.fritzbox.carddav.model.Contact;
import org.czdev.fritzbox.carddav.model.Telephone;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Robert Delbrueck
 */
public class VCardLoader {
    private static final Logger logger = Logger.getLogger(VCardLoader.class.getName());
    private Configuration configuration = Configuration.getInstance();
    private VCardEngine vCardEngine = new VCardEngine();

    private static String normalize(String in) {
        in = in.replace(" ", "");
        in = in.replace("-", "");
        in = in.replace("/", "");
        if (in.startsWith("0")) {
            in = "+49" + in.substring(1);
        } else if (in.startsWith("+")) {
            // do nothing
        } else {
            in = "+4930" + in;
        }
        return in;
    }

    public List<Contact> loadContacts() {
        try {
            List<Contact> contactList = new ArrayList<>();
            for (String name : configuration.getPropertyAsString(Configuration.KEY_NAMES_CARDDAV).split(",")) {
                contactList.addAll(this.loadContacts(configuration.getPropertyAsString(Configuration.KEY_ADDRESS_CARDDAV, "<name>", name),
                        configuration.getPropertyAsString(Configuration.KEY_USERNAME_CARDDAV, "<name>", name),
                        configuration.getPropertyAsString(Configuration.KEY_PASSWORD_CARDDAV, "<name>", name)));
            }

            return contactList;
        } catch (IOException | VCardParseException e) {
            throw new IllegalStateException("cannot load contacts", e);
        }
    }

    private List<Contact> loadContacts(String address, String username, String password) throws IOException, VCardParseException {
        logger.fine("starting...");

        final List<Contact> contacts = new ArrayList<Contact>();

        Sardine sardine = SardineFactory.begin(username, password);

        List<DavResource> list = sardine.list(address);
        logger.fine("listing contacts...");
        for (DavResource resource : list) {
            if (resource.isDirectory()) {
                continue;
            }

            URI href = resource.getHref();
            URL url = new URL(address);
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), href.toString());
            InputStream inputStream = sardine.get(url.toString());
            byte[] bytes = IOUtils.toByteArray(inputStream);
            VCard parse = vCardEngine.parse(new String(bytes));

            Contact contact = new Contact();
            contact.setSurname(parse.getN().getFamilyName());
            contact.setGivenNames(parse.getN().getGivenName());
            if (parse.getTels() != null) {
                for (TelType telType : parse.getTels()) {
                    Telephone telephone = new Telephone();
                    StringBuilder sb = new StringBuilder();
                    if (telType.getParams() != null) {
                        for (TelParamType telParamType : telType.getParams()) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(telParamType.getDescription());
                        }
                    }
                    telephone.setType(sb.toString());
                    telephone.setNumber(normalize(telType.getTelephone()));
                    contact.getTelephone().add(telephone);
                }
            }
//            if (parse.getAdrs() != null) {
            for (AdrType adrType : parse.getAdrs()) {
//                    Address address = new Address();
//                    address.setContact(contact);
//                    address.setStreet(adrType.getStreetAddress());
//                    address.setCity(adrType.getRegion());
//                    address.setPostalCode(adrType.getPostalCode());
//                    address.setCountry(adrType.getCountryName());
//                    StringBuilder sb = new StringBuilder();
//                    if (adrType.getParams() != null) {
//                        for (AdrParamType adrParamType : adrType.getParams()) {
//                            if (sb.length() > 0) {
//                                sb.append(", ");
//                            }
//                            sb.append(adrParamType.getDescription());
//                        }
//                        address.setLabel(sb.toString());
//                    }
//                    Location location = geocode(address);
//                    address.setLocation(location);
//                    contact.getAddressList().add(address);
//                }
            }
            logger.fine(String.format("found contact: %s, %s: telephones: %s, addresses: %s", contact.getSurname(), contact.getGivenNames(), contact.getTelephone(), contact.getAddressList()));
            contacts.add(contact);
        }
        return contacts;
    }
}
