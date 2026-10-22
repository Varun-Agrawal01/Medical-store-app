package com.example.project;

import org.junit.Test;

import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.utils.FormatUtils;
import com.example.project.utils.SeedData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MediStoreUnitTest {

    @Test
    public void testMedicineAvailability() {
        Medicine medAvailable = new Medicine("med1", "Dolo 650", "Paracetamol", "Fever reducer", 30.0, 10, "", true, false, "Fever", 0, 0);
        assertTrue(medAvailable.isAvailable());

        Medicine medOutOfStock = new Medicine("med2", "Aspirin", "Aspirin", "Pain reliever", 20.0, 0, "", true, false, "Headache", 0, 0);
        assertFalse(medOutOfStock.isAvailable());

        Medicine medInactive = new Medicine("med3", "TestMed", "Test", "Desc", 10.0, 5, "", false, false, "General", 0, 0);
        assertFalse(medInactive.isAvailable());
    }

    @Test
    public void testCartItemCalculations() {
        CartItem item1 = new CartItem("med1", "Dolo 650", 32.50, 3, false);
        assertEquals(97.50, item1.getItemTotal(), 0.001);
    }

    @Test
    public void testOrderTotalsCalculation() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("med1", "Dolo 650", 32.50, 2)); // 65.00
        items.add(new OrderItem("med2", "Digene", 115.00, 1));  // 115.00

        double subtotal = 180.00;
        double deliveryFee = 50.00; // subtotal <= 500
        double total = subtotal + deliveryFee;

        Order order = new Order("ORD123", "user1", "John", "9876543210", "123 Street", items, subtotal, deliveryFee, total, "Pending", System.currentTimeMillis(), System.currentTimeMillis(), true);

        assertEquals("ORD123", order.getOrderId());
        assertEquals(180.00, order.getSubtotal(), 0.001);
        assertEquals(50.00, order.getDeliveryFee(), 0.001);
        assertEquals(230.00, order.getTotal(), 0.001);
        assertTrue(order.isStockDeducted());
    }

    @Test
    public void testFormatUtilsPrice() {
        String formatted = FormatUtils.formatPrice(150.50);
        assertTrue(formatted.contains("150.50") || formatted.contains("150,50"));
    }

    @Test
    public void testSeedDataValidity() {
        List<Medicine> medicines = SeedData.getSampleMedicines();
        assertNotNull(medicines);
        assertFalse(medicines.isEmpty());
        for (Medicine m : medicines) {
            assertNotNull(m.getId());
            assertNotNull(m.getName());
            assertTrue(m.getPrice() >= 0);
        }
    }
}
