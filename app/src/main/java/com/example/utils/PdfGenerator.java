package com.example.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.example.models.Customer;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfGenerator {

    public static File generateInvoice(
            Context context,
            Customer customer,
            int days,
            double amount,
            String status) {

        PdfDocument document = new PdfDocument();

        //  Page Size: A4 (595 x 842)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();

        //  Background - Light Gray
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(0, 0, 595, 842, bgPaint);

        //  Title Background - Green
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRect(0, 0, 595, 80, headerBg);

        //  Title
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(28);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🧾 PAYMENT SLIP", 297, 50, titlePaint);

        //  Subtitle
        Paint subPaint = new Paint();
        subPaint.setColor(Color.parseColor("#E8F5E9"));
        subPaint.setTextSize(14);
        subPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Milk Delivery Management", 297, 70, subPaint);

        //  Divider Line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E0E0E0"));
        linePaint.setStrokeWidth(2);
        canvas.drawLine(40, 100, 555, 100, linePaint);

        //  Customer Info
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#546E7A"));
        labelPaint.setTextSize(14);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#1A237E"));
        valuePaint.setTextSize(16);
        valuePaint.setTypeface(Typeface.DEFAULT_BOLD);

        int x = 40;
        int y = 140;

        // Customer Name
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("👤 Customer Name", x, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(customer.getName(), 555, y, valuePaint);
        y += 40;

        // Mobile
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("📱 Mobile", x, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(customer.getMobile(), 555, y, valuePaint);
        y += 40;

        // Address
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("📍 Address", x, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        String address = customer.getAddress() != null && !customer.getAddress().isEmpty()
                ? customer.getAddress() : "Not Specified";
        canvas.drawText(address, 555, y, valuePaint);
        y += 40;

        // Divider
        canvas.drawLine(40, y, 555, y, linePaint);
        y += 30;

        //  Milk Details
        Paint sectionPaint = new Paint();
        sectionPaint.setColor(Color.parseColor("#004D40"));
        sectionPaint.setTextSize(18);
        sectionPaint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("📊 Billing Details", x, y, sectionPaint);
        y += 30;

        // Rate
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("💵 Rate (Per Litre)", x, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("₹ " + String.format(Locale.getDefault(), "%.2f", customer.getMilkRate()), 555, y, valuePaint);
        y += 35;

        // Quantity
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("📦 Quantity (Litre/Day)", x, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.format(Locale.getDefault(), "%.2f", customer.getMilkQuantity()) + " L", 555, y, valuePaint);
        y += 35;

        // Delivered Days
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("✅ Delivered Days", x, y, labelPaint);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.valueOf(days) + " Days", 555, y, valuePaint);
        y += 35;

        // Divider
        Paint thickLine = new Paint();
        thickLine.setColor(Color.parseColor("#1B5E20"));
        thickLine.setStrokeWidth(3);
        canvas.drawLine(40, y, 555, y, thickLine);
        y += 30;

        //  Total Amount - Highlighted
        Paint totalLabel = new Paint();
        totalLabel.setColor(Color.parseColor("#1A237E"));
        totalLabel.setTextSize(20);
        totalLabel.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("💰 Total Amount", x, y, totalLabel);

        Paint totalValue = new Paint();
        totalValue.setColor(Color.parseColor("#1B5E20"));
        totalValue.setTextSize(28);
        totalValue.setTypeface(Typeface.DEFAULT_BOLD);
        totalValue.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("₹ " + String.format(Locale.getDefault(), "%.2f", amount), 555, y, totalValue);
        y += 40;

        //  Payment Status - with Color
        Paint statusLabel = new Paint();
        statusLabel.setColor(Color.parseColor("#546E7A"));
        statusLabel.setTextSize(16);
        statusLabel.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("📌 Payment Status", x, y, statusLabel);

        Paint statusValue = new Paint();
        statusValue.setTextSize(20);
        statusValue.setTypeface(Typeface.DEFAULT_BOLD);
        statusValue.setTextAlign(Paint.Align.RIGHT);

        if ("Paid".equalsIgnoreCase(status)) {
            statusValue.setColor(Color.parseColor("#2E7D32"));
            canvas.drawText("✅ PAID", 555, y, statusValue);
        } else {
            statusValue.setColor(Color.parseColor("#C62828"));
            canvas.drawText("⏳ PENDING", 555, y, statusValue);
        }
        y += 50;

        //  Divider
        canvas.drawLine(40, y, 555, y, linePaint);
        y += 30;

        //  Footer
        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#78909C"));
        footerPaint.setTextSize(11);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, 297, y, footerPaint);
        y += 20;

        footerPaint.setTextSize(10);
        footerPaint.setColor(Color.parseColor("#B0BEC5"));
        canvas.drawText("This is a computer-generated invoice | Thank you for your business!", 297, y, footerPaint);

        //  Border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#1B5E20"));
        borderPaint.setStrokeWidth(3);
        borderPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(15, 15, 580, 820, borderPaint);

        // Finish Page
        document.finishPage(page);

        //  Save PDF
        String fileName = "Payment_Slip_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault()).format(new Date()) + ".pdf";

        File folder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        //  Create subfolder "MilkDelivery/Invoices"
        File invoiceFolder = new File(folder, "MilkDelivery/Invoices");
        if (!invoiceFolder.exists()) {
            invoiceFolder.mkdirs();
        }

        File file = new File(invoiceFolder, fileName);

        try {
            FileOutputStream out = new FileOutputStream(file);
            document.writeTo(out);
            out.close();

            //  Notify MediaScanner
            android.media.MediaScannerConnection.scanFile(
                    context,
                    new String[]{file.getAbsolutePath()},
                    new String[]{"application/pdf"},
                    null
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        document.close();
        return file;
    }
}