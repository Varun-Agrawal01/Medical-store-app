package com.example.project.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State machine defining valid order lifecycle transitions.
 *
 * Allowed transitions:
 * Awaiting Rx Verification -> Pending | Cancelled
 * Pending -> Confirmed | Cancelled
 * Confirmed -> Preparing | Cancelled
 * Preparing -> Out for Delivery | Cancelled
 * Out for Delivery -> Delivered
 * Delivered and Cancelled are terminal (no further transitions allowed).
 */
public final class OrderStateMachine {

    public static final String STATUS_AWAITING_RX = "Awaiting Rx Verification";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_PREPARING = "Preparing";
    public static final String STATUS_OUT_FOR_DELIVERY = "Out for Delivery";
    public static final String STATUS_DELIVERED = "Delivered";
    public static final String STATUS_CANCELLED = "Cancelled";

    private static final Map<String, List<String>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put(STATUS_AWAITING_RX, Arrays.asList(STATUS_PENDING, STATUS_CANCELLED));
        TRANSITIONS.put(STATUS_PENDING, Arrays.asList(STATUS_CONFIRMED, STATUS_CANCELLED));
        TRANSITIONS.put(STATUS_CONFIRMED, Arrays.asList(STATUS_PREPARING, STATUS_CANCELLED));
        TRANSITIONS.put(STATUS_PREPARING, Arrays.asList(STATUS_OUT_FOR_DELIVERY, STATUS_CANCELLED));
        TRANSITIONS.put(STATUS_OUT_FOR_DELIVERY, Collections.singletonList(STATUS_DELIVERED));
        TRANSITIONS.put(STATUS_DELIVERED, Collections.emptyList());
        TRANSITIONS.put(STATUS_CANCELLED, Collections.emptyList());
    }

    private OrderStateMachine() {
        // Utility class
    }

    /**
     * Returns true if transition from currentStatus to targetStatus is valid.
     */
    public static boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        List<String> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * Returns the list of permitted next statuses for the given status.
     */
    public static List<String> getAllowedTransitions(String currentStatus) {
        if (currentStatus == null) {
            return Collections.emptyList();
        }
        List<String> allowed = TRANSITIONS.get(currentStatus);
        return allowed != null ? Collections.unmodifiableList(allowed) : Collections.emptyList();
    }

    /**
     * Validates transition and throws IllegalStateException if illegal.
     */
    public static void validateTransition(String currentStatus, String targetStatus) {
        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new IllegalStateException("Illegal order transition from '" + currentStatus + "' to '" + targetStatus + "'");
        }
    }

    /**
     * Checks if a status is terminal (cannot transition further).
     */
    public static boolean isTerminal(String status) {
        return STATUS_DELIVERED.equalsIgnoreCase(status) || STATUS_CANCELLED.equalsIgnoreCase(status);
    }
}
