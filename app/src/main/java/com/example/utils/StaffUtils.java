package com.example.utils;

import android.content.Context;
import android.content.Intent;

import com.example.models.Staff;

public class StaffUtils {

    public static void shareOnWhatsapp(
            Context context,
            Staff staff){

        String text=

                "Staff Details\n\n"+
                        "Name : "+staff.getName()+"\n"+
                        "Phone1 : "+staff.getMobile1()+"\n"+
                        "Phone2 : "+staff.getMobile2()+"\n"+
                        "Address : "+staff.getAddress();

        Intent intent=new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");

        intent.putExtra(Intent.EXTRA_TEXT,text);

        intent.setPackage("com.whatsapp");

        context.startActivity(intent);
    }

}