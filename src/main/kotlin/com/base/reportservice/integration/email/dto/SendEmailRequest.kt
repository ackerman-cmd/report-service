package com.base.reportservice.integration.email.dto

data class SendEmailRequest(
    val fromEmail: String,
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val subject: String,
    val htmlBody: String? = null,
    val textBody: String? = null,
)
