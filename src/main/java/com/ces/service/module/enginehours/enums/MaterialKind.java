package com.ces.service.module.enginehours.enums;

/** Which Anbar concept a completion's material line points at. */
public enum MaterialKind {
    /** A quantity-tracked item (yağ, filtr) — submits an ordinary Anbar stock-out. */
    CONSUMABLE,
    /** A specific serialized unit (e.g. hidravlik nasos) — marked IN_USE and mirrored as a component. */
    SERIALIZED
}
