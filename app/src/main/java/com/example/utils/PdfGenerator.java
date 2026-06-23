package com.example.utils;

import android.content.Context;
import android.graphics.Paint;
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

        PdfDocument document =
                new PdfDocument();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(
                        595,
                        842,
                        1)
                        .create();

        PdfDocument.Page page =
                document.startPage(pageInfo);

        android.graphics.Canvas canvas =
                page.getCanvas();

        Paint paint =
                new Paint();

        paint.setTextSize(18);

        // Header
        int x = 40;
        int y = 60;

        canvas.drawText(
                "Milk Delivery Management",
                x,
                y,
                paint);

        y += 50;

        // Customer Information

        canvas.drawText(
                "Customer : "
                        + customer.getName(),
                x,
                y,
                paint);

        y += 40;

        canvas.drawText(
                "Mobile : "
                        + customer.getMobile(),
                x,
                y,
                paint);

        y += 40;

        canvas.drawText(
                "Milk Quantity : "
                        + customer.getMilkQuantity()
                        + " L",
                x,
                y,
                paint);

        y += 40;

        canvas.drawText(
                "Milk Rate : ₹"
                        + customer.getMilkRate(),
                x,
                y,
                paint);

        y += 40;

        canvas.drawText(
                "Delivered Days : "
                        + days,
                x,
                y,
                paint);

        y += 40;

        canvas.drawText(
                "Total Amount : ₹"
                        + amount,
                x,
                y,
                paint);

        y += 40;

        canvas.drawText(
                "Status : "
                        + status,
                x,
                y,
                paint);

        // Finish Page
        document.finishPage(page);

        // Create file in Downloads folder

        String fileName =
                "Invoice_"
                        + new SimpleDateFormat(
                        "yyyy_MM",
                        Locale.getDefault())
                        .format(new Date())
                        + ".pdf";

        File downloadsFolder =
                context.getExternalFilesDir(
                        Environment.DIRECTORY_DOWNLOADS);

        if (!downloadsFolder.exists()) {

            downloadsFolder.mkdirs();

        }

        File file =
                new File(
                        downloadsFolder,
                        fileName);

        // Save PDF

        try {

            FileOutputStream out =
                    new FileOutputStream(file);

            document.writeTo(out);

            out.close();

            // Notify MediaScanner about the new file so it appears in "Downloads"
            android.media.MediaScannerConnection.scanFile(
                    context,
                    new String[]{file.getAbsolutePath()},
                    new String[]{"application/pdf"},
                    null
            );

        } catch (Exception e) {

            e.printStackTrace();

        }

        // Close and return

        document.close();

        return file;
    }
}