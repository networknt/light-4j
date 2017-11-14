package com.networknt.service;

public class InfoValidator extends ValidatorBase<Info> {
    Validator<Contact> contactValidator = SingletonServiceFactory.getBean(Validator.class, Contact.class);
    Validator<License> licenseValidator = SingletonServiceFactory.getBean(Validator.class, License.class);

    @Override
    public boolean validate(Info info) {
        return contactValidator.validate(info.getContact()) && licenseValidator.validate(info.getLicense());
    }
}
