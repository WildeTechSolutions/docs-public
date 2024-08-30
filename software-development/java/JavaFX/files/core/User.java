package com.thomaswilde.wildebeans.application;

import java.util.Set;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

public interface User {

    IntegerProperty idProperty();


    int getId();


    void setId(final int id);


    StringProperty firstNameProperty();


    String getFirstName();


    void setFirstName(final String firstName);


    StringProperty lastNameProperty();


    String getLastName();


    void setLastName(final String lastName);

    StringProperty dsIdProperty();


    String getDsId();


    void setDsId(final String dsId);

    Set<String> getPermissions();
    
}
