package com.fulinlin.model.enums;

public enum TypeDisplayStyleEnum {
    DROP_DOWN("Drop Down"),
    SELECTION("Selection"),
    MIXING("Mixing");

    private final String name;


    TypeDisplayStyleEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
