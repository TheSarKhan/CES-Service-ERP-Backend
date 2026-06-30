package com.ces.service.module.role.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.role.dto.AssignPermissionsRequest;
import com.ces.service.module.role.dto.PermissionMatrixResponse;
import com.ces.service.module.role.dto.PermissionResponse;
import com.ces.service.module.role.dto.RoleRequest;
import com.ces.service.module.role.dto.RoleResponse;
import com.ces.service.module.role.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RBAC role & permission endpoints (SRS M16.4). */
@RestController
@RequestMapping("/api/v1")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = toPageable(page, size, sort, dir);
        Page<RoleResponse> result = roleService.list(pageable);
        PageResponse<RoleResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<RoleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.get(id)));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(roleService.create(request)));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.update(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> rolePermissions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getRolePermissions(id)));
    }

    @PostMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermissions(
            @PathVariable UUID id, @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.addPermissions(id, request)));
    }

    @DeleteMapping("/roles/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermission(
            @PathVariable UUID id, @PathVariable UUID permissionId) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.removePermission(id, permissionId)));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> permissions(
            @RequestParam(required = false) String module) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.listPermissions(module)));
    }

    @GetMapping("/permissions/matrix")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<PermissionMatrixResponse>> matrix() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.permissionMatrix()));
    }

    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, sort));
    }
}
