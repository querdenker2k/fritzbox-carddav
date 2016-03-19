package org.czdev.fritzbox.carddav.model;

import java.util.ArrayList;
import java.util.List;

public class Contact {
    private final List<Telephone> telephone = new ArrayList<>();
    private final List<Address> addressList = new ArrayList<>();
    private String surname;
    private String givenNames;

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getGivenNames() {
        return givenNames;
    }

    public void setGivenNames(String givenNames) {
        this.givenNames = givenNames;
    }

    public List<Telephone> getTelephone() {
        return telephone;
    }


    public List<Address> getAddressList() {
        return addressList;
    }

    public String getFullName() {
        return this.givenNames + " " + this.surname;
    }
}
