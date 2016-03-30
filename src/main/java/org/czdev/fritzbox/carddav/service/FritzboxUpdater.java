package org.czdev.fritzbox.carddav.service;

import de.mapoll.javaAVMTR064.Action;
import de.mapoll.javaAVMTR064.FritzConnection;
import de.mapoll.javaAVMTR064.Response;
import de.mapoll.javaAVMTR064.Service;
import org.apache.commons.io.output.StringBuilderWriter;
import org.czdev.fritzbox.carddav.config.Configuration;
import org.czdev.fritzbox.carddav.model.Contact;
import org.czdev.fritzbox.carddav.model.Telephone;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Robert Delbrueck
 */
public class FritzboxUpdater {
    private static final Logger LOG = Logger.getLogger(FritzboxUpdater.class.getName());

    private final Configuration configuration = Configuration.getInstance();

    public void updatePhonebook(List<Contact> contactList) {
        try {
            addNewEntries(contactList);
        } catch (ParserConfigurationException | TransformerException | JAXBException | IOException | NoSuchFieldException e) {
            throw new IllegalStateException("cannot create xml", e);
        }
    }

    private void addNewEntries(List<Contact> contactList) throws IOException, JAXBException, TransformerException, ParserConfigurationException, NoSuchFieldException {
        FritzConnection fc = new FritzConnection(configuration.getPropertyAsString(Configuration.KEY_ADDRESS_FRITZBOX),
                configuration.getPropertyAsString(Configuration.KEY_USERNAME_FRITZBOX),
                configuration.getPropertyAsString(Configuration.KEY_PASSWORD_FRITZBOX)
        );
        fc.init();
//        fc.printInfo();
        String phonebookName = configuration.getPropertyAsString(Configuration.KEY_PHONEBOOK);
        Integer phonebookId = findPhonebookId(fc, phonebookName);
        this.deletePhonebook(fc, phonebookId);
        phonebookId = findPhonebookId(fc, phonebookName);
        if (phonebookId == null) {
            throw new IllegalStateException("cannot find phonebook id for: " + phonebookName);
        }
        for (Contact contact : contactList) {
            String xmlForContact = this.createXmlForContact(contact);
            addNewEntry(fc, phonebookId, xmlForContact);
        }
    }

    private void deletePhonebook(FritzConnection fc, Integer phonebookId) throws IOException {
        if (phonebookId != null) {
            boolean deletePhonebook = Boolean.parseBoolean(this.configuration.getPropertyAsString(Configuration.KEY_DELETE_PHONEBOOK));
            if (deletePhonebook) {
                Map<String, Object> argumentsDelete = new HashMap<>();
                argumentsDelete.put("NewPhonebookID", phonebookId);
                argumentsDelete.put("NewPhonebookExtraID", "");
                fc.getService("X_AVM-DE_OnTel:1").getAction("DeletePhonebook").execute(argumentsDelete);
            }
        }

        String phonebookName = configuration.getPropertyAsString(Configuration.KEY_PHONEBOOK);
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("NewPhonebookName", phonebookName);
        arguments.put("NewPhonebookExtraID", "100");
        final Response addPhonebook = fc.getService("X_AVM-DE_OnTel:1").getAction("AddPhonebook").execute(arguments);
        System.out.println(addPhonebook);
    }

    private Integer findPhonebookId(FritzConnection fc, String phonebookName) throws IOException, NoSuchFieldException {
        String newPhonebookList = fc.getService("X_AVM-DE_OnTel:1").getAction("GetPhonebookList").execute().getValueAsString("NewPhonebookList");
        for (String phonebookIdString : newPhonebookList.split(",")) {
            if (phonebookIdString.isEmpty()) {
                continue;
            }
            Integer phonebookId = Integer.parseInt(phonebookIdString);
            String foundPhonebookName = fc.getService("X_AVM-DE_OnTel:1").getAction("GetPhonebook").execute(Collections.<String, Object>singletonMap("NewPhonebookID", phonebookId)).getValueAsString("NewPhonebookName");
            if (foundPhonebookName.equals(phonebookName)) {
                return phonebookId;
            }
        }
        return null;
    }

    private void addNewEntry(FritzConnection fc, int phonebookId, String xmlForContact) throws IOException {
        Service service = fc.getService("X_AVM-DE_OnTel:1");
        Action action = service.getAction("SetPhonebookEntry");
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("NewPhonebookID", phonebookId);
        arguments.put("NewPhonebookEntryID", 0);
        LOG.fine("adding xml: " + xmlForContact);
        arguments.put("NewPhonebookEntryData", (xmlForContact));
        action.execute(arguments);
    }

    private String createXmlForContact(Contact contact) throws ParserConfigurationException, TransformerException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        Element elContact = document.createElement("contact");
        document.appendChild(elContact);

        elContact.appendChild(document.createElement("category"));

        Element elPerson = document.createElement("person");
        elContact.appendChild(elPerson);

        Element elRealName = document.createElement("realName");
        elRealName.setTextContent(contact.getFullName());
        elPerson.appendChild(elRealName);

        Element elTelephony = document.createElement("telephony");
        elPerson.appendChild(elTelephony);

        Element elServices = document.createElement("services");
        elTelephony.appendChild(elServices);

        for (Telephone telephone : contact.getTelephone()) {
            Element elNumber = document.createElement("number");
            elNumber.setAttribute("type", mapPhoneType(telephone.getType()));
            elNumber.setAttribute("quickdial", "");
            elNumber.setAttribute("vanity", "");
            elNumber.setAttribute("prio", "0");
            elNumber.setTextContent(telephone.getNumber());
            elTelephony.appendChild(elNumber);
        }

        Element elUniqueid = document.createElement("uniqueid");
        elUniqueid.setTextContent(String.valueOf(contact.hashCode()));
        elContact.appendChild(elUniqueid);

        DOMSource domSource = new DOMSource(document);
        StringBuilder stringBuilder = new StringBuilder();
        StreamResult streamResult = new StreamResult(new StringBuilderWriter(stringBuilder));
        TransformerFactory tf = TransformerFactory.newInstance();

        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.transform(domSource, streamResult);
        return stringBuilder.toString();
    }

    private String mapPhoneType(String type) {
        if (type.contains("Work")) {
            return "work";
        } else if (type.contains("Cell")) {
            return "mobile";
        } else if (type.contains("Home")) {
            return "home";
        }
        return "";
    }
}
