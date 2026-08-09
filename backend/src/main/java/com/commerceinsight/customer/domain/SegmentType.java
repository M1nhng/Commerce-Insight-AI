package com.commerceinsight.customer.domain;

/**
 * SegmentType — classification of how a customer segment is defined.
 *
 * <p>MANUAL: Segment is manually curated by an admin.
 * <p>RULE_BASED: Segment is auto-populated by configurable business rules.
 * <p>AI_GENERATED: Segment is produced by AI/ML analysis (future sprint).
 */
public enum SegmentType {
    MANUAL,
    RULE_BASED,
    AI_GENERATED
}
