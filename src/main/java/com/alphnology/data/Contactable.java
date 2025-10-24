package com.alphnology.data;

public record Contactable(Long code, String fullName, String company, String title, String email, String phone,
                          byte[] photo) {


    public Contactable(Speaker speaker) {
        this(speaker.getCode(), speaker.getName(), speaker.getCompany(), speaker.getTitle(), speaker.getEmail(), speaker.getPhone(), speaker.getPhoto());
    }

    public Contactable(Attender attender) {
        this(attender.getCode(), attender.getName() + " " + attender.getLastName(), attender.getCompany(), attender.getTitle(), attender.getEmail(), attender.getPhone(), null);
    }
}
