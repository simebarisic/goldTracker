package com.simebarisic.goldtracker.dto;

import jakarta.validation.constraints.NotBlank;

public class CreatePageRequest {
    @NotBlank(message = "label i url su obavezni")
    private String label;

    @NotBlank(message = "label i url su obavezni")
    private String url;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
