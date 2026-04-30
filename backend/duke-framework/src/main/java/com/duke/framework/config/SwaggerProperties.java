package com.duke.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {
    private String title = "API Documentation";
    private String description = "";
    private String version = "1.0.0";
    private boolean enableSecurity = false;
    private String serverUrl = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEnableSecurity() {
        return enableSecurity;
    }

    public void setEnableSecurity(boolean enableSecurity) {
        this.enableSecurity = enableSecurity;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
