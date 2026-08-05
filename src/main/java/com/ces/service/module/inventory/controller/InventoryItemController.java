package com.ces.service.module.inventory.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.dto.PageResponse;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.service.ApprovalService;
import com.ces.service.module.inventory.dto.InventoryItemRequest;
import com.ces.service.module.inventory.dto.InventoryItemResponse;
import com.ces.service.module.inventory.dto.MoveItemRequest;
import com.ces.service.module.inventory.dto.StockQuantityRequest;
import com.ces.service.module.inventory.service.InventoryItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
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

/**
 * Inventory product (Məhsul) endpoints — Stok İdarəetməsi.
 *
 * <p>Every mutating action here is deferred: instead of applying the change, it is parked as an
 * approval request and answered with {@code 202 Accepted}. A second person approves it in the
 * Təsdiqləmələr module, which replays the stored payload through this same service. Creating a
 * product is deliberately exempt — only edits, moves, deletions and stock movements are reviewed.
 */
@RestController
@RequestMapping("/api/v1/inventory/items")
public class InventoryItemController {

    private final InventoryItemService itemService;
    private final ApprovalService approvalService;

    public InventoryItemController(InventoryItemService itemService, ApprovalService approvalService) {
        this.itemService = itemService;
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryItemResponse>>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID nodeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            // Repository.search() is a native query (needed to search inside the attributes
            // JSONB column), so this must be an actual DB column name, not a Java property name.
            @RequestParam(defaultValue = "created_at") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Pageable pageable = toPageable(page, size, sort, dir);
        Page<InventoryItemResponse> result = itemService.list(categoryId, nodeId, search, pageable);
        PageResponse<InventoryItemResponse> body = PageResponse.of(result);
        return ResponseEntity.ok(ApiResponse.ok(body, body.meta()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(itemService.get(id)));
    }

    /** Distinct category ids present at a node — see {@code InventoryItemService.listCategoryIdsAtNode}. */
    @GetMapping("/category-ids")
    @PreAuthorize("hasAuthority('WH_READ')")
    public ResponseEntity<ApiResponse<List<UUID>>> categoryIds(@RequestParam UUID nodeId) {
        return ResponseEntity.ok(ApiResponse.ok(itemService.listCategoryIdsAtNode(nodeId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> create(
            @Valid @RequestBody InventoryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(itemService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody InventoryItemRequest request) {
        itemService.assertTrackingChangeAllowed(id, request);
        return accepted(submit(id, ApprovalOperation.UPDATE, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> delete(@PathVariable UUID id) {
        return accepted(submit(id, ApprovalOperation.DELETE, null));
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('WH_MANAGE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> move(
            @PathVariable UUID id, @Valid @RequestBody MoveItemRequest request) {
        itemService.assertCanReceiveStock(id, request.getToNodeId());
        itemService.assertMovable(id, request.getFromNodeId(), request.getToNodeId(), request.getQuantity());
        return accepted(submit(id, ApprovalOperation.MOVE, request));
    }

    @PostMapping("/{id}/stock-in")
    @PreAuthorize("hasAuthority('WH_STOCK_IN')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> stockIn(
            @PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
        itemService.assertCanReceiveStock(id, request.getNodeId());
        return accepted(submit(id, ApprovalOperation.STOCK_IN, request));
    }

    @PostMapping("/{id}/stock-out")
    @PreAuthorize("hasAuthority('WH_USE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> stockOut(
            @PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
        return accepted(submit(id, ApprovalOperation.STOCK_OUT, request));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('WH_ADJUST')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> adjust(
            @PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
        itemService.assertCanReceiveStock(id, request.getNodeId());
        return accepted(submit(id, ApprovalOperation.STOCK_ADJUST, request));
    }

    /** Parks the operation and snapshots the item as it stands, so the reviewer can see the diff. */
    private ApprovalRequestResponse submit(UUID id, ApprovalOperation operation, Object payload) {
        InventoryItemResponse before = itemService.get(id);
        return approvalService.submit(
                ApprovalEntityType.INVENTORY_ITEM, id, before.getName(), operation, payload, before);
    }

    private ResponseEntity<ApiResponse<ApprovalRequestResponse>> accepted(ApprovalRequestResponse request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(request));
    }

    /**
     * Columns the product table may order by, as real {@code inventory_items} column names.
     *
     * <p>The whitelist earns its keep here more than anywhere else: {@code search()} is a native
     * query — it has to be, to reach inside the attributes JSONB — so the sort string is spliced
     * into SQL as an identifier rather than resolved against a mapped entity. Now that column
     * headers send this value, it is client-controlled input reaching the query planner.
     *
     * <p>Quantity is not in this list because it is not a column — stock lives in
     * {@code inventory_stock}, one row per folder. It is still sortable, via {@link #TOTAL_QUANTITY}
     * below, which orders by the same sum the response reports.
     */
    private static final Set<String> SORTABLE =
            Set.of("created_at", "name", "sku", "barcode", "unit", "purchase_price", "supplier");

    /**
     * The product's stock across every folder, as an expression rather than a column.
     *
     * <p>Kept identical to what {@code InventoryItemResponse.totalQuantity} reports — if the two
     * ever drift, the table would order by a number it does not display.
     */
    private static final String TOTAL_QUANTITY =
            "(coalesce((select sum(s.quantity) from ces_service.inventory_stock s"
                    + " where s.item_id = i.id and s.deleted_at is null), 0))";

    private Pageable toPageable(int page, int size, String sort, String dir) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort order = "totalQuantity".equals(sort)
                ? JpaSort.unsafe(direction, TOTAL_QUANTITY)
                : Sort.by(direction, SORTABLE.contains(sort) ? sort : "created_at");
        return PageRequest.of(pageIndex, pageSize, order);
    }
}
