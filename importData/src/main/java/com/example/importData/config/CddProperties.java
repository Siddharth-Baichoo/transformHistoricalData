package com.example.importData.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdd")
public class CddProperties {
    private String baseUrl = "https://www.cryptodatadownload.com";
    private String defaultExchange = "Binance";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getDefaultExchange() { return defaultExchange; }
    public void setDefaultExchange(String defaultExchange) { this.defaultExchange = defaultExchange; }
}
