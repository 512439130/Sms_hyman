package com.yy.sms_hyman.dao;

import android.content.ContentValues;
import android.content.Context;

import com.yy.sms_hyman.bean.SendedMsg;
import com.yy.sms_hyman.db.SmsProvider;

import java.util.Date;

/**
 * Created by 13160677911 on 2016-12-25.
 */

public class SmsDao {
    private Context context;
    public SmsDao(Context context){
        this.context = context;
    }
    public void save(SendedMsg sendedMsg){
        sendedMsg.setDate(new Date());
        ContentValues values = new ContentValues();
        values.put(SendedMsg.COLUMN_DATE , sendedMsg.getDate().getTime());
        values.put(SendedMsg.COLUMN_FES_NAME, sendedMsg.getFestivalName());
        values.put(SendedMsg.COLUMN_MSG , sendedMsg.getMsg());
        values.put(SendedMsg.COLUMN_NAMES , sendedMsg.getNames());
        values.put(SendedMsg.COLUMN_NUMBERS , sendedMsg.getNumbers());

        context.getContentResolver().insert(SmsProvider.URI_SMS_ALL, values);



    }
}
