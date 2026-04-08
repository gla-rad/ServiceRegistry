package net.maritimeconnectivity.serviceregistry.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class PingController {

    @GetMapping("/v2/ping")
    public ResponseEntity<Void> ping() {
        return ResponseEntity.ok().build();
    }
}