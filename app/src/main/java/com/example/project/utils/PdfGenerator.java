package com.example.project.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.project.models.Order;
import com.example.project.models.OrderItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfGenerator {

    public static File generateInvoicePdf(Context context, Order order) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#00796B"));
        headerPaint.setTextSize(22f);
        headerPaint.setFakeBoldText(true);

        Paint subHeaderPaint = new Paint();
        subHeaderPaint.setColor(Color.DARKGRAY);
        subHeaderPaint.setTextSize(14f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12f);

        Paint boldTextPaint = new Paint();
        boldTextPaint.setColor(Color.BLACK);
        boldTextPaint.setTextSize(12f);
        boldTextPaint.setFakeBoldText(true);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.LTGRAY);
        linePaint.setStrokeWidth(1f);

        int startX = 40;
        int startY = 50;

        // Title
        canvas.drawText("MEDI STORE - TAX INVOICE", startX, startY, headerPaint);
        startY += 25;
        canvas.drawText("Your Trusted Health Partner", startX, startY, subHeaderPaint);
        startY += 20;

        canvas.drawLine(startX, startY, 555, startY, linePaint);
        startY += 25;

        // Order & Customer Info
        canvas.drawText("Order ID: " + order.getOrderId(), startX, startY, boldTextPaint);
        canvas.drawText("Date: " + FormatUtils.formatDate(order.getCreatedAt()), 350, startY, textPaint);
        startY += 20;

        canvas.drawText("Customer Name: " + order.getCustomerName(), startX, startY, textPaint);
        canvas.drawText("Status: " + order.getStatus(), 350, startY, boldTextPaint);
        startY += 20;

        canvas.drawText("Phone: " + order.getCustomerPhone(), startX, startY, textPaint);
        startY += 20;

        canvas.drawText("Address: " + order.getDeliveryAddress(), startX, startY, textPaint);
        startY += 25;

        canvas.drawLine(startX, startY, 555, startY, linePaint);
        startY += 25;

        // Table Header
        canvas.drawText("Item Description", startX, startY, boldTextPaint);
        canvas.drawText("Qty", 320, startY, boldTextPaint);
        canvas.drawText("Unit Price", 400, startY, boldTextPaint);
        canvas.drawText("Total", 490, startY, boldTextPaint);
        startY += 15;

        canvas.drawLine(startX, startY, 555, startY, linePaint);
        startY += 20;

        // Table Items
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                canvas.drawText(item.getMedicineName(), startX, startY, textPaint);
                canvas.drawText(String.valueOf(item.getQuantity()), 320, startY, textPaint);
                canvas.drawText(FormatUtils.formatPrice(item.getUnitPrice()), 400, startY, textPaint);
                canvas.drawText(FormatUtils.formatPrice(item.getItemTotal()), 490, startY, textPaint);
                startY += 20;
            }
        }

        startY += 10;
        canvas.drawLine(startX, startY, 555, startY, linePaint);
        startY += 25;

        // Totals
        canvas.drawText("Subtotal:", 350, startY, textPaint);
        canvas.drawText(FormatUtils.formatPrice(order.getSubtotal()), 490, startY, textPaint);
        startY += 20;

        canvas.drawText("Delivery Fee:", 350, startY, textPaint);
        canvas.drawText(FormatUtils.formatPrice(order.getDeliveryFee()), 490, startY, textPaint);
        startY += 20;

        canvas.drawText("Grand Total:", 350, startY, boldTextPaint);
        canvas.drawText(FormatUtils.formatPrice(order.getTotal()), 490, startY, boldTextPaint);
        startY += 30;

        canvas.drawLine(startX, startY, 555, startY, linePaint);
        startY += 25;

        // Disclaimer Note
        Paint disclaimerPaint = new Paint();
        disclaimerPaint.setColor(Color.GRAY);
        disclaimerPaint.setTextSize(10f);
        canvas.drawText("Note: Thank you for ordering from Medi Store! Keep this invoice for your records.", startX, startY, disclaimerPaint);
        startY += 15;
        canvas.drawText("This invoice is electronically generated and valid without signature.", startX, startY, disclaimerPaint);

        pdfDocument.finishPage(page);

        File pdfFile = new File(context.getExternalFilesDir("invoices"), "Invoice_" + order.getOrderId() + ".pdf");
        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            fos.close();
            pdfDocument.close();
            return pdfFile;
        } catch (IOException e) {
            e.printStackTrace();
            pdfDocument.close();
            return null;
        }
    }

    public static void sharePdf(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "PDF file not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                pdfFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(shareIntent, "Share Bill Invoice"));
    }

    public static void openPdf(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "PDF file not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                pdfFile
        );

        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(contentUri, "application/pdf");
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(openIntent);
        } catch (Exception e) {
            Toast.makeText(context, "No app available to open PDF files", Toast.LENGTH_SHORT).show();
        }
    }
}
