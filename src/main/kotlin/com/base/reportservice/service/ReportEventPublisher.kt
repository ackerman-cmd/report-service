package com.base.reportservice.service

import com.base.reportservice.event.DailyReportEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class ReportEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @Value("\${app.kafka.topics.daily-report}") private val topic: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishDailyReport(event: DailyReportEvent) {
        val payload = objectMapper.writeValueAsString(event)
        val message = MessageBuilder.withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, topic)
            .setHeader("__TypeId__", "DAILY_REPORT")
            .build()

        kafkaTemplate.send(message)
            .whenComplete { result, ex ->
                if (ex != null) {
                    log.error("Failed to publish daily-report event: reportId={}", event.reportId, ex)
                } else {
                    log.info(
                        "Daily-report event published: reportId={}, topic={}, offset={}",
                        event.reportId, topic, result.recordMetadata.offset(),
                    )
                }
            }
    }
}
