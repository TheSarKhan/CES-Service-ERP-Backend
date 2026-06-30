package com.ces.service.module.branch.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.branch.dto.BranchRequest;
import com.ces.service.module.branch.dto.BranchResponse;
import com.ces.service.module.branch.service.BranchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Branch CRUD (SRS §5). Reads require {@code BRANCH_VIEW_ALL}; mutations require
 * {@code BRANCH_MANAGE}.
 */
@RestController
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches", description = "Branch (şöbə) management")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW_ALL','BRANCH_MANAGE')")
    public ApiResponse<PageResponse<BranchResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at") String sort,
            @RequestParam(defaultValue = "desc") String dir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // Translate API snake_case sort key to the entity property.
        String property = "created_at".equals(sort) ? "createdAt" : sort;
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.min(Math.max(1, size), 100);

        Page<BranchResponse> result = branchService.list(
                PageRequest.of(pageIndex, pageSize, Sort.by(direction, property)));
        PageResponse<BranchResponse> body = PageResponse.of(result);
        return ApiResponse.ok(body, body.meta());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW_ALL','BRANCH_MANAGE')")
    public ApiResponse<BranchResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(branchService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BRANCH_MANAGE')")
    public ApiResponse<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        return ApiResponse.created(branchService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCH_MANAGE')")
    public ApiResponse<BranchResponse> update(@PathVariable UUID id,
                                              @Valid @RequestBody BranchRequest request) {
        return ApiResponse.ok(branchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('BRANCH_MANAGE')")
    public void delete(@PathVariable UUID id) {
        branchService.delete(id);
    }
}
