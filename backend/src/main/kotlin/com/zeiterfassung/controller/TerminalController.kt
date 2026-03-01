package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.TerminalScanRequest
import com.zeiterfassung.model.dto.TerminalScanResponse
import com.zeiterfassung.service.TerminalService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/terminal")
class TerminalController(
    private val terminalService: TerminalService,
) {
    @PostMapping("/scan")
    fun scan(
        @RequestBody request: TerminalScanRequest,
    ): ResponseEntity<TerminalScanResponse> = ResponseEntity.ok(terminalService.scan(request.rfidTagId, request.terminalId))

    /** Simple heartbeat endpoint — terminals poll this to detect backend connectivity. */
    @GetMapping("/heartbeat")
    fun heartbeat(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(mapOf("status" to "ok"))
}
