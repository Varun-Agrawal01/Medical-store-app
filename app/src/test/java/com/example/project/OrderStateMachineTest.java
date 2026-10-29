package com.example.project;

import com.example.project.utils.OrderStateMachine;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class OrderStateMachineTest {

    @Test
    public void testAllowedTransitionsPass() {
        // Awaiting Rx Verification -> Pending | Cancelled
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_AWAITING_RX, OrderStateMachine.STATUS_PENDING));
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_AWAITING_RX, OrderStateMachine.STATUS_CANCELLED));

        // Pending -> Confirmed | Cancelled
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_PENDING, OrderStateMachine.STATUS_CONFIRMED));
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_PENDING, OrderStateMachine.STATUS_CANCELLED));

        // Confirmed -> Preparing | Cancelled
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_CONFIRMED, OrderStateMachine.STATUS_PREPARING));
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_CONFIRMED, OrderStateMachine.STATUS_CANCELLED));

        // Preparing -> Out for Delivery | Cancelled
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_PREPARING, OrderStateMachine.STATUS_OUT_FOR_DELIVERY));
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_PREPARING, OrderStateMachine.STATUS_CANCELLED));

        // Out for Delivery -> Delivered
        assertTrue(OrderStateMachine.isValidTransition(
                OrderStateMachine.STATUS_OUT_FOR_DELIVERY, OrderStateMachine.STATUS_DELIVERED));
    }

    @Test
    public void testDeliveredAndCancelledAreTerminal() {
        assertTrue(OrderStateMachine.isTerminal(OrderStateMachine.STATUS_DELIVERED));
        assertTrue(OrderStateMachine.isTerminal(OrderStateMachine.STATUS_CANCELLED));

        List<String> allStatuses = Arrays.asList(
                OrderStateMachine.STATUS_AWAITING_RX,
                OrderStateMachine.STATUS_PENDING,
                OrderStateMachine.STATUS_CONFIRMED,
                OrderStateMachine.STATUS_PREPARING,
                OrderStateMachine.STATUS_OUT_FOR_DELIVERY,
                OrderStateMachine.STATUS_DELIVERED,
                OrderStateMachine.STATUS_CANCELLED
        );

        // Delivered -> anything must be rejected
        for (String target : allStatuses) {
            assertFalse("Delivered should not transition to " + target,
                    OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_DELIVERED, target));
        }

        // Cancelled -> anything must be rejected
        for (String target : allStatuses) {
            assertFalse("Cancelled should not transition to " + target,
                    OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_CANCELLED, target));
        }
    }

    @Test
    public void testIllegalTransitionsAreRejected() {
        // Cannot jump steps: Pending -> Delivered
        assertFalse(OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_PENDING, OrderStateMachine.STATUS_DELIVERED));

        // Cannot go backwards: Delivered -> Pending
        assertFalse(OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_DELIVERED, OrderStateMachine.STATUS_PENDING));
        assertFalse(OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_PREPARING, OrderStateMachine.STATUS_PENDING));
        assertFalse(OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_CONFIRMED, OrderStateMachine.STATUS_AWAITING_RX));

        // Out for delivery cannot be cancelled directly
        assertFalse(OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_OUT_FOR_DELIVERY, OrderStateMachine.STATUS_CANCELLED));

        // Null checks
        assertFalse(OrderStateMachine.isValidTransition(null, OrderStateMachine.STATUS_PENDING));
        assertFalse(OrderStateMachine.isValidTransition(OrderStateMachine.STATUS_PENDING, null));
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateTransitionThrowsOnIllegal() {
        OrderStateMachine.validateTransition(OrderStateMachine.STATUS_DELIVERED, OrderStateMachine.STATUS_PENDING);
    }
}
