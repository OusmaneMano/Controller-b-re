package com.controller.controller;

import com.controller.dto.AuthDTO;
import com.controller.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    /**
     * Managers currently sitting at "awaiting payment approval".
     * GET /api/admin/managers/pending
     */
    @GetMapping("/managers/pending")
    public ResponseEntity<List<AuthDTO.UserInfo>> listPendingManagers() {
        return ResponseEntity.ok(adminService.listPendingManagers());
    }

    /**
     * Flip a manager's account to ACTIVE once you've received their payment.
     * POST /api/admin/managers/{userId}/approve
     */
    @PostMapping("/managers/{userId}/approve")
    public ResponseEntity<AuthDTO.UserInfo> approveManager(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.approveManager(userId));
    }
}
