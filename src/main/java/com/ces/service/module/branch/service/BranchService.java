package com.ces.service.module.branch.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.module.branch.dto.BranchRequest;
import com.ces.service.module.branch.dto.BranchResponse;
import com.ces.service.module.branch.entity.Branch;
import com.ces.service.module.branch.repository.BranchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * CRUD operations over {@link Branch}. Branch creation/modification is gated by
 * {@code BRANCH_MANAGE} at the controller layer; reads by {@code BRANCH_VIEW_*}.
 */
@Service
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public Page<BranchResponse> list(Pageable pageable) {
        return branchRepository.findAllByDeletedAtIsNull(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BranchResponse get(UUID id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        if (branchRepository.existsByCodeAndDeletedAtIsNull(request.code())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROLE_CODE,
                    "Branch code already exists: " + request.code());
        }
        Branch branch = new Branch();
        branch.setName(request.name());
        branch.setCode(request.code());
        branch.setAddress(request.address());
        branch.setPhone(request.phone());
        branch.setActive(request.active() == null || request.active());
        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse update(UUID id, BranchRequest request) {
        Branch branch = getEntity(id);
        if (!branch.getCode().equals(request.code())
                && branchRepository.existsByCodeAndDeletedAtIsNull(request.code())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ROLE_CODE,
                    "Branch code already exists: " + request.code());
        }
        branch.setName(request.name());
        branch.setCode(request.code());
        branch.setAddress(request.address());
        branch.setPhone(request.phone());
        if (request.active() != null) {
            branch.setActive(request.active());
        }
        return toResponse(branchRepository.save(branch));
    }

    @Transactional
    public void delete(UUID id) {
        Branch branch = getEntity(id);
        branch.setDeletedAt(Instant.now());
        branchRepository.save(branch);
    }

    private Branch getEntity(UUID id) {
        return branchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
    }

    private BranchResponse toResponse(Branch b) {
        return new BranchResponse(
                b.getId(),
                b.getName(),
                b.getCode(),
                b.getAddress(),
                b.getPhone(),
                b.isActive(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
