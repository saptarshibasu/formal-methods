package com.formalmethods.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Concrete seam onto the external inventory system's release API
 * (FR-006, plan.md's "Inventory release seam"). Deliberately a single
 * concrete class with no interface extracted (constitution Article V — no
 * second implementation exists yet); the real external HTTP call replaces
 * this method body later. Retry/compensation on the inventory system's own
 * side is explicitly out of scope for this feature (spec.md's Out of Scope).
 */
@Component
public class InventoryReleaseClient {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReleaseClient.class);

    /** FR-006: releases inventory reserved for {@code orderId}, exactly once per qualifying cancellation. */
    public void release(UUID orderId) {
        LOG.info("action=inventoryRelease order={} outcome=released", orderId);
    }
}
