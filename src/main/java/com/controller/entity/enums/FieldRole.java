package com.controller.entity.enums;

/**
 * Marks whether a TableColumn plays a special role in Amount calculation.
 * A company may have at most one column of each of these roles:
 *   - QUANTITY + UNIT_PRICE together  -> Amount is auto-calculated (Quantity * Unit Price)
 *   - AMOUNT alone (no Quantity/Unit Price) -> Amount is entered directly by the employee
 *   - NONE -> an ordinary custom column, not involved in Amount calculation at all
 */
public enum FieldRole {
    NONE,
    QUANTITY,
    UNIT_PRICE,
    AMOUNT
}
