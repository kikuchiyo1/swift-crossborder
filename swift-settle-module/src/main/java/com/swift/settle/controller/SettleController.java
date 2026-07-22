package com.swift.settle.controller;

import com.swift.settle.dto.ReceiveReq;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/swift/settle")
public class SettleController {

    @PostMapping("/receive")
    public ResponseEntity<Void> settlePayment(@RequestBody ReceiveReq request) {
        return ResponseEntity.ok().build();
    }
}
