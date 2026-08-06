package in.esmartsolution.milkflow.utils;

import android.content.Context;

import in.esmartsolution.milkflow.models.Customer;
import in.esmartsolution.milkflow.models.Payment;
import in.esmartsolution.milkflow.viewmodel.MilkViewModel;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelExporter {

    public static File exportExcel(
            Context context,
            List<Customer> customers,
            MilkViewModel viewModel) {

        // STEP 9.18.6 — Workbook create
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Milk Report");

        // STEP 9.18.7 — Header row
        XSSFRow header = sheet.createRow(0);

        header.createCell(0).setCellValue("Customer");
        header.createCell(1).setCellValue("Days");
        header.createCell(2).setCellValue("Qty");
        header.createCell(3).setCellValue("Rate");
        header.createCell(4).setCellValue("Amount");
        header.createCell(5).setCellValue("Status");

        // Month
        String month = new java.text.SimpleDateFormat(
                "yyyy-MM",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());

        // STEP 9.18.8 — Data fill
        int rowIndex = 1;

        for (Customer c : customers) {

            Payment p = viewModel.getPayment(c.getId(), month);

            int days = viewModel.getDeliveredDaysCount(
                    c.getId(),
                    month
            );
            double amount = days * c.getMilkRate() * c.getMilkQuantity();

            XSSFRow row = sheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(c.getName());
            row.createCell(1).setCellValue(days);
            row.createCell(2).setCellValue(c.getMilkQuantity());
            row.createCell(3).setCellValue(c.getMilkRate());
            row.createCell(4).setCellValue(amount);

            if (p != null && "Paid".equalsIgnoreCase(p.getStatus())) {
                row.createCell(5).setCellValue("Paid");
            } else {
                row.createCell(5).setCellValue("Pending");
            }
        }

        // STEP 9.18.9 — Save file
        File file = new File(
                context.getExternalFilesDir(null),
                "Milk_Report_" + month + ".xlsx"
        );

        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }
}