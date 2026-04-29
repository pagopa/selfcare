package it.pagopa.selfcare.institution.model;

import lombok.Getter;

@Getter
public class FileMailData {
    byte[] data;
    String name;
    String contentType;

    public FileMailData() {
    }

    public FileMailData(byte[] data, String name, String contentType) {
        this.data = data;
        this.name = name;
        this.contentType = contentType;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
