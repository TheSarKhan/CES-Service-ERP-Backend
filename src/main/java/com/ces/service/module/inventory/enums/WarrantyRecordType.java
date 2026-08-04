package com.ces.service.module.inventory.enums;

/**
 * What a warranty search row actually is.
 *
 * <p>Warranty lives in two places: on a serialized unit (one physical thing with its own serial)
 * and on a non-serialized product (one batch bought under one warranty). Both need answering the
 * same question, so the search returns them side by side and this says which kind you're looking
 * at.
 */
public enum WarrantyRecordType {

    /** A non-serialized product — its own warranty window covers the whole quantity. */
    ITEM,

    /** One serialized unit, warranty-tracked individually. */
    UNIT
}
