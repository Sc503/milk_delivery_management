package com.example.utils;

import android.content.Context;

import com.example.models.Staff;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.util.List;

public class StaffPdfExporter {

    public static File export(
            Context context,
            List<Staff> staffList)
            throws Exception {

        File file=
                new File(
                        context.getExternalFilesDir(null),
                        "staff_list.pdf");

        PdfWriter writer=
                new PdfWriter(file);

        PdfDocument pdf=
                new PdfDocument(writer);

        Document document=
                new Document(pdf);

        document.add(
                new Paragraph("STAFF LIST"));

        for(Staff s:staffList){

            document.add(

                    new Paragraph(

                            s.getName()
                                    +"\n"+
                                    s.getMobile1()
                                    +"\n"+
                                    s.getAddress()

                    )
            );

        }

        document.close();

        return file;
    }

}