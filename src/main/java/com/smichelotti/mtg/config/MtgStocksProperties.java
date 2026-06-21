package com.smichelotti.mtg.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mtgstocks")
public record MtgStocksProperties(

        @NotBlank
        String baseUrl,

        @NotBlank
        String setsPath,

        @NotBlank
        String printsPath,

        @NotNull @Valid
        RequestConfig setsIndex,

        @NotNull @Valid
        RequestConfig setDetail,

        boolean debugCards,

        boolean debugJson) {

    public record RequestConfig(

            @NotBlank
            String userAgent,

            @Positive
            int timeoutMs,

            boolean followRedirects) {
    }

    public String setsUrl() {
        return baseUrl + setsPath;
    }

    public String printUrl(String printId) {
        return baseUrl + printsPath + "/" + printId;
    }
}
