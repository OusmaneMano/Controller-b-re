package com.controller.controller;

import com.controller.dto.AuthDTO;
import com.controller.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/managers/pending")
    public ResponseEntity<List<AuthDTO.UserInfo>> listPendingManagers() {
        return ResponseEntity.ok(adminService.listPendingManagers());
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> accounts() {
        return ResponseEntity.ok(adminService.listAccounts());
    }

    @PostMapping("/managers/{userId}/approve")
    public ResponseEntity<AuthDTO.UserInfo> approveManager(@PathVariable Long userId, @RequestParam(defaultValue = "1") int months) {
        return ResponseEntity.ok(adminService.approveManager(userId, months));
    }

    @PostMapping("/users/{userId}/block")
    public ResponseEntity<AuthDTO.UserInfo> block(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.block(userId));
    }

    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<AuthDTO.UserInfo> unblock(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.unblock(userId));
    }
}