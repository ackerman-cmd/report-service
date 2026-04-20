package com.base.reportservice.integration.email

import com.base.reportservice.config.ReportProperties
import com.base.reportservice.integration.email.dto.SendEmailRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Service
class EmailServiceClient(
    @Qualifier("emailServiceRestClient") private val restClient: RestClient,
    private val reportProperties: ReportProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendEmail(to: List<String>, subject: String, htmlBody: String) {
        val request = SendEmailRequest(
            fromEmail = reportProperties.fromEmail,
            to = to,
            subject = subject,
            htmlBody = htmlBody,
        )
        try {
            restClient.post()
                .uri("/internal/emails/send")
                .body(request)
                .retrieve()
                .toBodilessEntity()
            log.info("Report email sent to {} recipients, subject: {}", to.size, subject)
        } catch (ex: Exception) {
            log.error("Failed to send report email to {}: {}", to, ex.message, ex)
            throw ex
        }
    }

    fun sendEmailWithAttachment(
        to: List<String>,
        subject: String,
        htmlBody: String,
        attachmentName: String,
        attachmentBytes: ByteArray,
        attachmentContentType: String,
    ) {
        val parts = LinkedMultiValueMap<String, Any>()
        parts.add("fromEmail", reportProperties.fromEmail)
        to.forEach { parts.add("to", it) }
        parts.add("subject", subject)
        parts.add("htmlBody", htmlBody)
        val resource = object : ByteArrayResource(attachmentBytes) {
            override fun getFilename() = attachmentName
        }
        parts.add("files", resource)

        try {
            restClient.post()
                .uri("/internal/emails/send-with-attachments")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .toBodilessEntity()
            log.info("Report email with PDF sent to {} recipients, subject: {}", to.size, subject)
        } catch (ex: Exception) {
            log.error("Failed to send report email with attachment to {}: {}", to, ex.message, ex)
            throw ex
        }
    }
}
