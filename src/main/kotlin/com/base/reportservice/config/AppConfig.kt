package com.base.reportservice.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ReportProperties::class, EmailServiceProperties::class)
class AppConfig {

    @Bean
    fun emailServiceRestClient(properties: EmailServiceProperties): RestClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .build()
}
