package com.example.project;

import com.example.project.utils.PricingUtils;

import org.junit.Test;

import static org.junit.Assert.*;

public class PricingUtilsTest {

    @Test
    public void testDeliveryFeeThresholdBoundary() {
        // Just below Rs 500 -> delivery fee is standard (Rs 50)
        double justBelow = 499.99;
        assertEquals(50.0, PricingUtils.calculateDeliveryFee(justBelow), 0.001);

        // Exactly at Rs 500 -> free delivery (Rs 0)
        double exactlyAt = 500.00;
        assertEquals(0.0, PricingUtils.calculateDeliveryFee(exactlyAt), 0.001);

        // Just above Rs 500 -> free delivery (Rs 0)
        double justAbove = 500.01;
        assertEquals(0.0, PricingUtils.calculateDeliveryFee(justAbove), 0.001);

        // Zero or negative subtotal -> Rs 0 fee
        assertEquals(0.0, PricingUtils.calculateDeliveryFee(0.0), 0.001);
        assertEquals(0.0, PricingUtils.calculateDeliveryFee(-10.0), 0.001);

        // Well above Rs 500 -> free delivery
        assertEquals(0.0, PricingUtils.calculateDeliveryFee(1500.0), 0.001);
    }

    @Test
    public void testGstSplitAndCalculation() {
        // 12% GST on Rs 200 is Rs 24.00
        double gstAmount = PricingUtils.calculateGstAmount(200.0, 12.0);
        assertEquals(24.0, gstAmount, 0.001);

        // GST split (Intra-state CGST 50% + SGST 50%)
        PricingUtils.GstSplit split = PricingUtils.calculateGstSplit(gstAmount);
        assertNotNull(split);
        assertEquals(12.0, split.getCgst(), 0.001);
        assertEquals(12.0, split.getSgst(), 0.001);
        assertEquals(24.0, split.getTotal(), 0.001);

        // Odd GST amount: Rs 15.25 -> 7.63 + 7.63 = 15.26
        PricingUtils.GstSplit oddSplit = PricingUtils.calculateGstSplit(15.25);
        assertEquals(7.63, oddSplit.getCgst(), 0.001);
        assertEquals(7.63, oddSplit.getSgst(), 0.001);

        // Zero GST
        PricingUtils.GstSplit zeroSplit = PricingUtils.calculateGstSplit(0.0);
        assertEquals(0.0, zeroSplit.getCgst(), 0.001);
        assertEquals(0.0, zeroSplit.getSgst(), 0.001);
    }

    @Test
    public void testRoundingToTwoDecimals() {
        assertEquals(12.35, PricingUtils.roundToTwoDecimals(12.346), 0.001);
        assertEquals(12.34, PricingUtils.roundToTwoDecimals(12.344), 0.001);
        assertEquals(10.50, PricingUtils.roundToTwoDecimals(10.5), 0.001);
        assertEquals(0.00, PricingUtils.roundToTwoDecimals(0.00001), 0.001);
    }

    @Test
    public void testCalculateTotal() {
        double subtotal = 450.00;
        double delivery = PricingUtils.calculateDeliveryFee(subtotal); // 50.0
        double total = PricingUtils.calculateTotal(subtotal, delivery);
        assertEquals(500.00, total, 0.001);
    }

    @Test
    public void testAmountInWords() {
        String words500 = PricingUtils.formatAmountInWords(500.00);
        assertEquals("Five Hundred Rupees Only", words500);

        String wordsZero = PricingUtils.formatAmountInWords(0.0);
        assertEquals("Zero Rupees Only", wordsZero);

        String wordsPaise = PricingUtils.formatAmountInWords(1234.50);
        assertEquals("One Thousand Two Hundred Thirty Four Rupees and Fifty Paise Only", wordsPaise);
    }
}
