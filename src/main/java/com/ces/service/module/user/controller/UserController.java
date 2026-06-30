package com.ces.service.module.user.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.user.dto.AssignRoleRequest;
import com.ces.service.module.user.dto.ResetPasswordRequest;
import com.ces.service.module.user.dto.UserActivityResponse;
import com.ces.service.module.user.dto.UserDetailResponse;
import com.ces.service.module.user.dto.UserRequest;
import com.ces.service.module.user.dto.UserResponse;
import com.ces.service.module.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** User administration endpoints (SRS M15.2). */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Page<UserResponse> result = userService.list(isActive, toPageable(page, size, sort, dir));
        PageResponse<UserResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.activate(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.deactivate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_ROLE_MANAGE')")
    public ResponseEntity<Void> assignRole(
            @PathVariable UUID id, @Valid @RequestBody AssignRoleRequest request) {
        userService.assignRole(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('USER_ROLE_MANAGE')")
    public ResponseEntity<Void> revokeRole(
            @PathVariable UUID id,
            @PathVariable UUID roleId,
            @RequestParam UUID branchId) {
        userService.revokeRole(id, roleId, branchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/branches")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Void> addBranch(@PathVariable UUID id, @RequestParam UUID branchId) {
        userService.addBranch(id, branchId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/branches/{branchId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Void> removeBranch(@PathVariable UUID id, @PathVariable UUID branchId) {
        userService.removeBranch(id, branchId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Void> resetPassword(
            @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/activity")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<UserActivityResponse>> activity(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.activity(id)));
    }

    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, sort));
    }
}
