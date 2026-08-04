-- V41__create_inventory_transfers.sql
-- Qovluqlar arası transfer, iki addımda: göndər → yolda → qəbul et.
--
-- Niyə iki addım: fiziki köçürmə vaxt aparır. Tək addımlı model malın mənbədən çıxdığı və
-- təyinata çatdığı anı eyni sayır — yəni real həyatda malın heç bir yerdə görünmədiyi bir
-- müddət olur və sayım vaxtı o boşluq izah edilə bilmir. «Yolda» statusu həmin müddəti qeyd edir.
--
-- Miqdar GÖNDƏRİLƏNDƏ mənbədən çıxılır (ona görə mənbədə iki dəfə vəd edilə bilməz) və yalnız
-- QƏBUL EDİLƏNDƏ təyinata əlavə olunur. Aradakı fərq transferin öz sətrindədir.

CREATE TABLE ces_service.inventory_transfers (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id         UUID          NOT NULL REFERENCES ces_service.branches(id),

    from_node_id      UUID          NOT NULL REFERENCES ces_service.inventory_nodes(id),
    to_node_id        UUID          NOT NULL REFERENCES ces_service.inventory_nodes(id),

    status            VARCHAR(20)   NOT NULL DEFAULT 'IN_TRANSIT',
    notes             TEXT,

    -- Kim göndərdi / kim qəbul etdi. created_by ilə eyni deyil: qəbul ayrı bir hadisədir və
    -- «başqa şəxs qəbul etməlidir» qaydası məhz bu iki sütunun müqayisəsidir.
    sent_by           UUID,
    sent_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    received_by       UUID,
    received_at       TIMESTAMPTZ,
    cancelled_by      UUID,
    cancelled_at      TIMESTAMPTZ,

    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT inventory_transfers_status_chk
        CHECK (status IN ('IN_TRANSIT', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT inventory_transfers_nodes_chk
        CHECK (from_node_id <> to_node_id)
);

CREATE INDEX ix_inventory_transfers_status
    ON ces_service.inventory_transfers (branch_id, status, sent_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_inventory_transfers_to_node
    ON ces_service.inventory_transfers (to_node_id, status)
    WHERE deleted_at IS NULL;

-- ── Sətirlər ────────────────────────────────────────────────────────────────
-- Bir transferdə bir neçə məhsul ola bilər: anbardar rəfi boşaldanda hər məhsul üçün ayrıca
-- sənəd doldurmur.
CREATE TABLE ces_service.inventory_transfer_lines (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    transfer_id  UUID          NOT NULL REFERENCES ces_service.inventory_transfers(id) ON DELETE CASCADE,
    item_id      UUID          NOT NULL REFERENCES ces_service.inventory_items(id),

    quantity     NUMERIC(12,3) NOT NULL,

    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ,
    created_by   UUID,
    updated_by   UUID,

    CONSTRAINT inventory_transfer_lines_quantity_chk CHECK (quantity > 0)
);

-- Eyni məhsul bir transferdə iki sətir kimi görünməməlidir — qəbul edən adam nəyi
-- saydığını bilməz.
CREATE UNIQUE INDEX ux_transfer_lines_transfer_item
    ON ces_service.inventory_transfer_lines (transfer_id, item_id)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_transfer_lines_item
    ON ces_service.inventory_transfer_lines (item_id)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE ces_service.inventory_transfers IS
    'Qovluqlar arası köçürmə. IN_TRANSIT = mənbədən çıxıb, təyinata hələ çatmayıb.';
