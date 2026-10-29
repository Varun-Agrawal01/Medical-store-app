package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.HomeActivity;
import com.example.project.R;
import com.example.project.data.MediRepository;
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.utils.FormatUtils;
import com.example.project.utils.PdfGenerator;

import java.io.File;

public class BillActivity extends AppCompatActivity {

    private TextView tvOrderId, tvStatus, tvDate, tvCustName, tvPhone, tvAddress, tvSubtotal, tvDeliveryFee, tvTotal;
    private LinearLayout layoutItemsContainer;
    private Button btnGeneratePdf, btnSharePdf, btnHomeReturn;
    private ImageButton btnBack;

    private MediRepository repository;
    private String orderId;
    private Order currentOrder;
    private File generatedPdfFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bill);

        repository = MediRepository.getInstance(this);

        btnBack = findViewById(R.id.btnBackBill);
        tvOrderId = findViewById(R.id.tvBillOrderId);
        tvStatus = findViewById(R.id.tvBillStatus);
        tvDate = findViewById(R.id.tvBillDate);
        tvCustName = findViewById(R.id.tvBillCustomerName);
        tvPhone = findViewById(R.id.tvBillPhone);
        tvAddress = findViewById(R.id.tvBillAddress);

        layoutItemsContainer = findViewById(R.id.layoutBillItemsContainer);
        tvSubtotal = findViewById(R.id.tvBillSubtotal);
        tvDeliveryFee = findViewById(R.id.tvBillDeliveryFee);
        tvTotal = findViewById(R.id.tvBillTotal);

        btnGeneratePdf = findViewById(R.id.btnGeneratePdf);
        btnSharePdf = findViewById(R.id.btnSharePdf);
        btnHomeReturn = findViewById(R.id.btnHomeReturn);

        btnBack.setOnClickListener(v -> finish());

        if (getIntent().hasExtra("orderId")) {
            orderId = getIntent().getStringExtra("orderId");
            loadOrder();
        } else {
            Toast.makeText(this, "Order ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnGeneratePdf.setOnClickListener(v -> {
            if (currentOrder != null) {
                generatedPdfFile = PdfGenerator.generateInvoicePdf(this, currentOrder);
                if (generatedPdfFile != null) {
                    Toast.makeText(this, "PDF Invoice generated successfully!", Toast.LENGTH_SHORT).show();
                    PdfGenerator.openPdf(this, generatedPdfFile);
                } else {
                    Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSharePdf.setOnClickListener(v -> {
            if (currentOrder != null) {
                if (generatedPdfFile == null) {
                    generatedPdfFile = PdfGenerator.generateInvoicePdf(this, currentOrder);
                }
                PdfGenerator.sharePdf(this, generatedPdfFile);
            }
        });

        btnHomeReturn.setOnClickListener(v -> {
            Intent intent = new Intent(BillActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadOrder() {
        // Direct single-row query by order ID
        repository.getOrderById(orderId, new MediRepository.Callback<Order>() {
            @Override
            public void onSuccess(Order order) {
                currentOrder = order;
                displayOrderDetails(order);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BillActivity.this, "Error loading order: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayOrderDetails(Order o) {
        tvOrderId.setText("Order #" + o.getOrderId());
        tvStatus.setText("Status: " + o.getStatus());
        tvDate.setText("Placed On: " + FormatUtils.formatDate(o.getCreatedAt()));
        tvCustName.setText(o.getCustomerName());
        tvPhone.setText(o.getCustomerPhone());
        tvAddress.setText(o.getDeliveryAddress());

        layoutItemsContainer.removeAllViews();
        if (o.getItems() != null) {
            for (OrderItem item : o.getItems()) {
                TextView itemRow = new TextView(this);
                itemRow.setText(String.format("• %s x%d   -   %s",
                        item.getMedicineName(), item.getQuantity(), FormatUtils.formatPrice(item.getItemTotal())));
                itemRow.setTextColor(getColor(R.color.black));
                itemRow.setTextSize(14f);
                itemRow.setPadding(0, 4, 0, 4);
                layoutItemsContainer.addView(itemRow);
            }
        }

        tvSubtotal.setText(FormatUtils.formatPrice(o.getSubtotal()));
        tvDeliveryFee.setText(FormatUtils.formatPrice(o.getDeliveryFee()));
        tvTotal.setText(FormatUtils.formatPrice(o.getTotal()));
    }
}
