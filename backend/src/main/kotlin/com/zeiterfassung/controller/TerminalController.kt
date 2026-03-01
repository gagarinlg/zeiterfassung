package com.zeiterfassung.controller

import com.zeiterfassung.model.dto.TerminalScanRequest
import com.zeiterfassung.model.dto.TerminalScanResponse
import com.zeiterfassung.service.TerminalService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/terminal")
@Tag(name = "Terminal")
class TerminalController(
    private val terminalService: TerminalService,
) {
    @PostMapping("/scan")
    @Operation(
        summary = "Process RFID scan",
        description = "Processes an RFID tag scan from a terminal device. Toggles clock-in/out state.",
    )
    @ApiResponse(responseCode = "200", description = "Scan processed successfully")
    @ApiResponse(responseCode = "404", description = "RFID tag not recognized")
    @ApiResponse(responseCode = "409", description = "Concurrent scan conflict — retry")
    fun scan(
        @RequestBody request: TerminalScanRequest,
    ): ResponseEntity<TerminalScanResponse> = ResponseEntity.ok(terminalService.scan(request.rfidTagId, request.terminalId))

    @GetMapping("/heartbeat")
    @Operation(
        summary = "Terminal heartbeat",
        description = "Simple health check endpoint polled by terminals to detect backend connectivity.",
    )
    @ApiResponse(responseCode = "200", description = "Backend is reachable")
    fun heartbeat(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(mapOf("status" to "ok"))
}
