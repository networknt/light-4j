package com.networknt.service;

public class InfoImpl implements Info {
    private Contact contact = SingletonServiceFactory.getBean(Contact.class);
    private License license = SingletonServiceFactory.getBean(License.class);

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public License getLicense() {
        return license;
    }
}
