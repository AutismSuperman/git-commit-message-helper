package com.fulinlin.model.enums;

public enum TypeDisplayStyleEnum {
    CHECKBOX("Checkbox"),
    RADIO("Radio"),
    MIXING("Mixing");

    private final String name;


    TypeDisplayStyleEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
