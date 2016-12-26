package com.yy.sms_hyman.biz;

import android.app.PendingIntent;
import android.content.Context;
import android.telephony.SmsManager;

import com.yy.sms_hyman.bean.SendedMsg;
import com.yy.sms_hyman.dao.SmsDao;

import java.util.ArrayList;

import java.util.Set;

/**
 * Created by 13160677911 on 2016-12-25.
 */

public class SmsBiz {
    private Context context;
    public SmsBiz(Context context){
        this.context = context;
    }
    /**
     * 发送单条短信
     *
     * @param number
     * @param msg
     * @param sentPi
     * @param deliverPi
     * @return 返回短信条数
     */
    public int sendMsg(String number, String msg, PendingIntent sentPi, PendingIntent deliverPi) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> contents = smsManager.divideMessage(msg);
        for (String content : contents) {
            smsManager.sendTextMessage(number, null, content, sentPi, deliverPi);
        }
        return contents.size();
    }
    public int sendMsg(Set<String> numbers, SendedMsg msg, PendingIntent sentPi, PendingIntent deliverPi) {
        SmsDao mSmsDao = new SmsDao(context);
        mSmsDao.save(msg);
        int result = 0;
        for (String number : numbers) {
            int count = sendMsg(number, msg.getMsg(), sentPi,deliverPi);
            result += count;
        }
        return result;
    }

}
