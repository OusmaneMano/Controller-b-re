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

    @GetMapping("/pending-managers")
    public ResponseEntity<List<AuthDTO.UserInfo>> pending() {
        return ResponseEntity.ok(adminService.pendingManagers());
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> accounts() {
        return ResponseEntity.ok(adminService.listAccounts());
    }

    @PostMapping("/managers/{userId}/approve")
    public ResponseEntity<AuthDTO.UserInfo> approve(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "1") int months,
                                                     @RequestParam(defaultValue = "3") int employeeLimit) {
        return ResponseEntity.ok(adminService.approveManager(userId, months, employeeLimit));
    }

    @PostMapping("/managers/{userId}/increment")
    public ResponseEntity<AuthDTO.UserInfo> increment(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.increment(userId));
    }

    @PostMapping("/managers/{userId}/decrement")
    public ResponseEntity<AuthDTO.UserInfo> decrement(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.decrement(userId));
    }

    @PostMapping("/managers/{userId}/staff")
    public ResponseEntity<Void> setStaff(@PathVariable Long userId, @RequestParam int limit) {
        adminService.setStaffLimit(userId, limit);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/block")
    public ResponseEntity<Void> block(@PathVariable Long userId) {
        adminService.block(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable Long userId) {
        adminService.unblock(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}