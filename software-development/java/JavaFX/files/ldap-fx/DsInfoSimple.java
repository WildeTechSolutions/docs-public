package com.thomaswilde.fx.ldap;

import com.thomaswilde.ldap.LDAP;
import com.thomaswilde.wildebeans.annotations.UiProperty;

import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DsInfoSimple {

    @UiProperty(displayName = "Common Name", displayCategory = "Personal")
    private final StringProperty cn = new SimpleStringProperty(this, "cn");

    @UiProperty(displayName = "Full Name", displayCategory = "Personal")
    private final StringProperty fullName = new SimpleStringProperty(this, "fullName");

    @UiProperty(displayName = "Employee ID", displayCategory = "Personal")
    private final StringProperty uid = new SimpleStringProperty(this, "uid");

    @UiProperty(displayName = "RTX ID", displayCategory = "Personal")
    private final StringProperty rtxID = new SimpleStringProperty(this, "rtxID");

    @UiProperty(displayName = "Site", displayCategory = "Personal")
    private final StringProperty houseIdentifier = new SimpleStringProperty(this, "houseIdentifier");

    @UiProperty(displayName = "Phone Number", displayCategory = "Contact")
    private final StringProperty telephoneNumber = new SimpleStringProperty(this, "telephoneNumber");

    @UiProperty(displayName = "Email", displayCategory = "Contact")
    private final StringProperty email = new SimpleStringProperty(this, "email");

    @UiProperty(displayName = "Department Name", displayCategory = "Organization")
    private final StringProperty departmentName = new SimpleStringProperty(this, "departmentName");


    private Map<String, String> ldapValues;

    public DsInfoSimple(){

    }

    public DsInfoSimple(Map<String, String> ldapValues){
        this.ldapValues = ldapValues;
        setLdapValues(ldapValues);
    }

    public void setLdapValues(Map<String, String> ldapValues){
        this.ldapValues = ldapValues;

        if (ldapValues.containsKey(LDAP.cn)) {
            setCn(ldapValues.get(LDAP.cn));
        }

        if (ldapValues.containsKey(LDAP.fullName)) {
            setFullName(ldapValues.get(LDAP.fullName));
        }

        if (ldapValues.containsKey(LDAP.uid)) {
            setUid(ldapValues.get(LDAP.uid));
        }

        if (ldapValues.containsKey(LDAP.rtxID)) {
            setRtxID(ldapValues.get(LDAP.rtxID));
        }

        if (ldapValues.containsKey(LDAP.houseIdentifier)) {
            setHouseIdentifier(ldapValues.get(LDAP.houseIdentifier));
        }

        if (ldapValues.containsKey(LDAP.telephoneNumber)) {
            setTelephoneNumber(ldapValues.get(LDAP.telephoneNumber));
        }

        if (ldapValues.containsKey(LDAP.email)) {
            setEmail(ldapValues.get(LDAP.email));
        }

        if (ldapValues.containsKey(LDAP.departmentName)) {
            setDepartmentName(ldapValues.get(LDAP.departmentName));
        }
    }

    public final StringProperty departmentNameProperty() {
       return departmentName;
    }
    public final String getDepartmentName() {
       return departmentName.get();
    }
    public final void setDepartmentName(String value) {
        departmentName.set(value);
    }


    public final StringProperty emailProperty() {
       return email;
    }
    public final String getEmail() {
       return email.get();
    }
    public final void setEmail(String value) {
        email.set(value);
    }

    public final StringProperty rtxIDProperty() {
        return rtxID;
    }
    public final String getRtxID() {
        return rtxIDProperty().get();
    }
    public final void setRtxID(String value) {
        rtxIDProperty().set(value);
    }

    public final StringProperty telephoneNumberProperty() {
       return telephoneNumber;
    }
    public final String getTelephoneNumber() {
       return telephoneNumber.get();
    }
    public final void setTelephoneNumber(String value) {
        telephoneNumber.set(value);
    }


    public final StringProperty houseIdentifierProperty() {
       return houseIdentifier;
    }
    public final String getHouseIdentifier() {
       return houseIdentifier.get();
    }
    public final void setHouseIdentifier(String value) {
        houseIdentifier.set(value);
    }


    public final StringProperty uidProperty() {
       return uid;
    }
    public final String getUid() {
       return uid.get();
    }
    public final void setUid(String value) {
        uid.set(value);
    }


    public final StringProperty fullNameProperty() {
       return fullName;
    }
    public final String getFullName() {
       return fullName.get();
    }
    public final void setFullName(String value) {
        fullName.set(value);
    }


    public final StringProperty cnProperty() {
       return cn;
    }
    public final String getCn() {
       return cn.get();
    }
    public final void setCn(String value) {
        cn.set(value);
    }


}
