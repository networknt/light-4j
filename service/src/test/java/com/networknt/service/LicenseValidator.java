package com.networknt.service;

public class LicenseValidator extends ValidatorBase<License> {
    @Override
    public boolean validate(License license) {
        return "license".equals(license.getName());
    }
}
