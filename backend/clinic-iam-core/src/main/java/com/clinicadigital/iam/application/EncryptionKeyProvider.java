package com.clinicadigital.iam.application;

public interface EncryptionKeyProvider {

    String activeKeyVersion();

    String keyMaterialForVersion(String version);
}
