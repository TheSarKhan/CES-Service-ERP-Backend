package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryNodeRequest;
import com.ces.service.module.inventory.dto.InventoryNodeResponse;
import com.ces.service.module.inventory.entity.InventoryCategory;
import com.ces.service.module.inventory.entity.InventoryNode;
import com.ces.service.module.inventory.repository.InventoryCategoryRepository;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import com.ces.service.module.inventory.repository.InventoryNodeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dynamic physical storage tree (Layer) management.
 *
 * <p>Adjacency list only (parent_id), unbounded depth — mirrors the Folder-tree pattern from the
 * reference Arxiv project. A node may hold items directly and have child nodes at the same time
 * (e.g. a shelf with loose products on it and boxes on top of it). Business rules:
 * <ul>
 *   <li>Sibling names must be unique under the same parent → {@link ErrorCode#DUPLICATE_NODE_NAME}.</li>
 *   <li>A node cannot be moved under itself or one of its own descendants →
 *       {@link ErrorCode#NODE_INVALID_PARENT}.</li>
 *   <li>A node with children or items cannot be deleted → {@link ErrorCode#NODE_NOT_EMPTY}.</li>
 * </ul>
 */
@Service
@Transactional
public class InventoryNodeService {

    private final InventoryNodeRepository nodeRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryCategoryRepository categoryRepository;

    public InventoryNodeService(
            InventoryNodeRepository nodeRepository,
            InventoryItemRepository itemRepository,
            InventoryCategoryRepository categoryRepository) {
        this.nodeRepository = nodeRepository;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryNodeResponse> listChildren(UUID parentId) {
        UUID branchId = BranchContext.get();
        if (parentId != null) {
            loadNode(parentId); // 404 if the parent doesn't exist / isn't visible in this branch
        }
        List<InventoryNode> children = nodeRepository
                .findByBranchIdAndParentIdAndDeletedAtIsNullOrderByNameAsc(branchId, parentId);
        return toResponses(children);
    }

    @Transactional(readOnly = true)
    public InventoryNodeResponse get(UUID id) {
        InventoryNode node = loadNode(id);
        boolean hasChildren = nodeRepository.existsByParentIdAndDeletedAtIsNull(node.getId());
        return InventoryNodeResponse.from(node, hasChildren);
    }

    /** Root-first ancestor chain (including {@code id} itself) — lets a client rebuild a breadcrumb. */
    @Transactional(readOnly = true)
    public List<InventoryNodeResponse> getPath(UUID id) {
        List<InventoryNode> chain = new ArrayList<>();
        UUID currentId = id;
        int guard = 0;
        while (currentId != null && guard++ < 1000) {
            InventoryNode node = loadNode(currentId);
            chain.add(node);
            currentId = node.getParentId();
        }
        Collections.reverse(chain);
        List<UUID> ids = chain.stream().map(InventoryNode::getId).collect(Collectors.toList());
        Set<UUID> withChildren = Set.copyOf(nodeRepository.findIdsWithChildren(ids));
        return chain.stream()
                .map(n -> InventoryNodeResponse.from(n, withChildren.contains(n.getId())))
                .collect(Collectors.toList());
    }

    public InventoryNodeResponse create(InventoryNodeRequest request) {
        UUID branchId = BranchContext.get();

        if (request.getParentId() != null) {
            loadNode(request.getParentId()); // ensures parent exists in this branch
        }

        if (nodeRepository.existsByBranchIdAndParentIdAndNameAndDeletedAtIsNull(
                branchId, request.getParentId(), request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NODE_NAME);
        }

        InventoryNode node = InventoryNode.builder()
                .parentId(request.getParentId())
                .name(request.getName())
                .code(request.getCode())
                .notes(request.getNotes())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .build();
        node.setBranchId(branchId);
        node.setQrCode(generateCode());
        node.setBarcode(generateCode());
        if (request.getCategoryIds() != null) {
            node.setAllowedCategories(resolveCategories(request.getCategoryIds()));
        }

        InventoryNode saved = nodeRepository.save(node);
        return InventoryNodeResponse.from(saved, false);
    }

    public InventoryNodeResponse update(UUID id, InventoryNodeRequest request) {
        InventoryNode node = loadNode(id);
        UUID branchId = node.getBranchId();

        UUID newParentId = request.getParentId();
        boolean isMove = !java.util.Objects.equals(node.getParentId(), newParentId);

        if (isMove) {
            if (newParentId != null) {
                if (newParentId.equals(node.getId()) || isDescendant(node.getId(), newParentId)) {
                    throw new BusinessException(ErrorCode.NODE_INVALID_PARENT);
                }
                loadNode(newParentId);
            }
        }

        if (nodeRepository.existsByBranchIdAndParentIdAndNameAndDeletedAtIsNullAndIdNot(
                branchId, newParentId, request.getName(), node.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NODE_NAME);
        }

        node.setName(request.getName());
        node.setCode(request.getCode());
        node.setNotes(request.getNotes());
        node.setParentId(newParentId);
        if (request.getIsActive() != null) {
            node.setIsActive(request.getIsActive());
        }
        if (request.getCategoryIds() != null) {
            node.setAllowedCategories(resolveCategories(request.getCategoryIds()));
        }

        boolean hasChildren = nodeRepository.existsByParentIdAndDeletedAtIsNull(node.getId());
        return InventoryNodeResponse.from(node, hasChildren);
    }

    public void delete(UUID id) {
        InventoryNode node = loadNode(id);

        if (nodeRepository.existsByParentIdAndDeletedAtIsNull(node.getId())
                || itemRepository.existsByNodeIdAndDeletedAtIsNull(node.getId())) {
            throw new BusinessException(ErrorCode.NODE_NOT_EMPTY);
        }

        node.setDeletedAt(Instant.now());
        nodeRepository.save(node);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private InventoryNode loadNode(UUID id) {
        UUID branchId = BranchContext.get();
        return nodeRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory node not found: " + id));
    }

    /** True if {@code candidateAncestorId} is an ancestor of (or equal to) {@code startId}. */
    private boolean isDescendant(UUID candidateAncestorId, UUID startId) {
        UUID currentId = startId;
        int guard = 0;
        while (currentId != null && guard++ < 1000) {
            if (currentId.equals(candidateAncestorId)) {
                return true;
            }
            InventoryNode current = nodeRepository.findById(currentId).orElse(null);
            currentId = current == null ? null : current.getParentId();
        }
        return false;
    }

    private List<InventoryNodeResponse> toResponses(List<InventoryNode> nodes) {
        if (nodes.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = nodes.stream().map(InventoryNode::getId).collect(Collectors.toList());
        Set<UUID> withChildren = Set.copyOf(nodeRepository.findIdsWithChildren(ids));
        return nodes.stream()
                .map(n -> InventoryNodeResponse.from(n, withChildren.contains(n.getId())))
                .collect(Collectors.toList());
    }

    private String generateCode() {
        return UUID.randomUUID().toString();
    }

    private Set<InventoryCategory> resolveCategories(List<UUID> categoryIds) {
        if (categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<UUID> distinctIds = new HashSet<>(categoryIds);
        List<InventoryCategory> found = categoryRepository.findAllById(new ArrayList<>(distinctIds));
        if (found.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("One or more categories not found");
        }
        return new HashSet<>(found);
    }
}
