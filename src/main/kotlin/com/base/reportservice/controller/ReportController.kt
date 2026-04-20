package com.base.reportservice.controller

import com.base.reportservice.domain.ReportHistory
import com.base.reportservice.domain.SlaPolicy
import com.base.reportservice.repository.ReportHistoryRepository
import com.base.reportservice.repository.SlaPolicyRepository
import com.base.reportservice.service.ReportEmailService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/reports")
class ReportController(
    private val reportEmailService: ReportEmailService,
    private val reportHistoryRepository: ReportHistoryRepository,
    private val slaPolicyRepository: SlaPolicyRepository,
) {
    /** Ручной запуск генерации и отправки отчёта (для тестирования). */
    @PostMapping("/trigger")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun trigger() {
        reportEmailService.generateAndSend()
    }

    /** История отправленных отчётов. */
    @GetMapping("/history")
    fun history(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<ReportHistory> =
        reportHistoryRepository.findAll(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt")),
        )

    /** Текущие SLA-политики. */
    @GetMapping("/sla-policies")
    fun slaPolicies(): List<SlaPolicy> = slaPolicyRepository.findAll()
}
