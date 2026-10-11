package com.controller.controller;

import com.controller.dto.AuthDTO;
import com.controller.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/admin/managers/pending")
    public ResponseEntity<List<AuthDTO.UserInfo>> listPendingManagers() {
        return ResponseEntity.ok(adminService.listPendingManagers());
    }

    @GetMapping("/admin/accounts")
    public ResponseEntity<List<Map<String, Object>>> accounts() {
        return ResponseEntity.ok(adminService.listAccounts());
    }

    @PostMapping("/admin/managers/{userId}/approve")
    public ResponseEntity<AuthDTO.UserInfo> approveManager(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "2") int months,
            @RequestParam(defaultValue = "3") int employeeLimit) {
        return ResponseEntity.ok(adminService.approveManager(userId, months, employeeLimit));
    }

    @PostMapping("/admin/managers/{userId}/staff")
    public ResponseEntity<AuthDTO.UserInfo> setStaff(@PathVariable Long userId, @RequestParam int employeeLimit) {
        return ResponseEntity.ok(adminService.setStaffLimit(userId, employeeLimit));
    }

    @PostMapping("/admin/managers/{userId}/increment")
    public ResponseEntity<AuthDTO.UserInfo> increment(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.increment(userId));
    }

    @PostMapping("/admin/managers/{userId}/decrement")
    public ResponseEntity<AuthDTO.UserInfo> decrement(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.decrement(userId));
    }

    @PostMapping("/admin/users/{userId}/block")
    public ResponseEntity<AuthDTO.UserInfo> block(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.block(userId));
    }

    @PostMapping("/admin/users/{userId}/unblock")
    public ResponseEntity<AuthDTO.UserInfo> unblock(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.unblock(userId));
    }

    @GetMapping({"/manager/payment", "/employee/payment"})
    public ResponseEntity<Map<String, Object>> myPayment(Principal principal) {
        return ResponseEntity.ok(adminService.paymentFor(principal.getName()));
    }

    @DeleteMapping("/admin/users/{userId}")
public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
    adminService.deleteUser(userId);
    return ResponseEntity.noContent().build();
}
}