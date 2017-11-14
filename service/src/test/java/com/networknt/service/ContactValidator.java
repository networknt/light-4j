package com.networknt.service;

public class ContactValidator extends ValidatorBase<Contact> {
    @Override
    public boolean validate(Contact contact) {
        return "contact".equals(contact.getName());
    }
}
