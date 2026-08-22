package com.simebarisic.goldtracker.dto;

public class ChangeDto {
    private String type;
    private String name;
    private Double oldValue;
    private Double newValue;

    public ChangeDto(String type, String name, Double oldValue, Double newValue) {
        this.type = type;
        this.name = name;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public Double getOldValue() { return oldValue; }
    public Double getNewValue() { return newValue; }
}
