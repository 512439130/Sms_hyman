package com.yy.sms_hyman;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yy.sms_hyman.bean.Festival;
import com.yy.sms_hyman.bean.FestivalLab;
import com.yy.sms_hyman.bean.Msg;
import com.yy.sms_hyman.bean.SendedMsg;
import com.yy.sms_hyman.biz.SmsBiz;
import com.yy.sms_hyman.view.FlowLayout;

import java.util.HashSet;

public class SendMsgActivity extends AppCompatActivity {

    private static final String KEY_FESTIVAL_ID = "festivalId";
    private static final String KEY_MSG_ID = "msgId";
    private static final int CODE_REQUEST = 1;

    private int mFestivalId;
    private int msgId;

    private Festival mFestival;
    private Msg mMsg;

    private EditText mEdMsg;  //编辑短信相关
    private Button mBtnAdd;  //添加联系人相关
    private FlowLayout mFlContacts;  //存储Contacts相关
    private FloatingActionButton mFabSend;  //发送短信相关
    private View mLayoutLoading;  //当点击发送时，显示Loading的信息

    private HashSet<String> mContactNames = new HashSet<>();
    private HashSet<String> mContactNums = new HashSet<>();


    private LayoutInflater mInflater;

    public static final String ACTION_SEND_MSG = "ACTION_SEND_MSG";
    public static final String ACTION_DELIVER_MSG = "ACTION_DELIVER_MSG";
    private PendingIntent mSendPi;
    private PendingIntent mDeliverPi;
    private BroadcastReceiver mSendBroadcastReceiver;
    private BroadcastReceiver mDeliverBroadcastReceiver;

    private SmsBiz mSmsBiz;

    //记录已经成功发送多少短信
    private int mMsgSendCount;
    private int mTotalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_msg);
        mInflater = LayoutInflater.from(this);  //初始化
        mSmsBiz = new SmsBiz(this);
        initDatas();

        initViews();

        initEvents();

        initRececiver();
    }

    private void initRececiver() {
        Intent sendIntent = new Intent(ACTION_SEND_MSG);
        mSendPi = PendingIntent.getBroadcast(this, 0, sendIntent, 0);

        Intent deliverIntent = new Intent(ACTION_DELIVER_MSG);
        mDeliverPi = PendingIntent.getBroadcast(this, 0, deliverIntent, 0);

        //注册发送短信广播
        registerReceiver(mSendBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mMsgSendCount++;
                if (getResultCode() == RESULT_OK) {
                    Log.e("TAG", "短信发送成功" + (mMsgSendCount + "/" + mTotalCount));
                } else {
                    Log.e("TAG", "短信发送失败");
                }
                Toast.makeText(SendMsgActivity.this, (mMsgSendCount + "/" + mTotalCount) + "短信发送成功", Toast.LENGTH_SHORT).show();

                if (mMsgSendCount == mTotalCount) {
                    finish();
                }

            }
        }, new IntentFilter(ACTION_SEND_MSG));
        //注册联系人接收广播
        registerReceiver(mDeliverBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e("TAG", "联系人已经成功接收到短信");
            }
        }, new IntentFilter(ACTION_DELIVER_MSG));

        //切记，注册广播后需要在onDestory去unregisterReceiver,否则会引起内存泄露问题
    }

    private void initEvents() {
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //读取联系人
                //通过Intent去启动系统通讯录App
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, CODE_REQUEST);
            }
        });
        mFabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //发送短信
                //发送广播
                if (mContactNums.size() == 0) {
                    Toast.makeText(SendMsgActivity.this, "请先选择联系人", Toast.LENGTH_SHORT).show();
                    return;
                }
                String msg = mEdMsg.getText().toString();
                if (TextUtils.isEmpty(msg)) {
                    Toast.makeText(SendMsgActivity.this, "短信内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                mLayoutLoading.setVisibility(View.VISIBLE);
                mTotalCount = mSmsBiz.sendMsg(mContactNums, buildSendMsg(msg), mSendPi, mDeliverPi);
                mMsgSendCount = 0;
            }
        });
    }

    private SendedMsg buildSendMsg(String msg) {
        SendedMsg sendedMsg = new SendedMsg();
        sendedMsg.setMsg(msg);
        sendedMsg.setFestivalName(mFestival.getName());
        String names = "";
        for(String name : mContactNames){
            names += name + ":";
        }
        String numbers = "";
        for(String number : mContactNums){
            numbers += number + ":";
        }
        sendedMsg.setNames(names.substring(0,names.length()-1));
        sendedMsg.setNumbers(numbers.substring(0,numbers.length()-1));

        return sendedMsg;
    }

    //当用户选择一个通讯人的时候需要回调onActivityResult方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_REQUEST) {
            if (resultCode == RESULT_OK) {
                //通过返回的data去获取联系人的姓名和电话号码
                Uri contactUri = data.getData();
                Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
                //拿到联系人的姓名
                cursor.moveToFirst();
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                //拿到电话号码
                String number = getContactNumber(cursor);
                if (!TextUtils.isEmpty(number)) {
                    mContactNums.add(number);
                    mContactNames.add(contactName);

                    addTag(contactName);
                    System.out.println("选择联系人");
                    System.out.println("联系人姓名" + contactName);
                    System.out.println("电话" + number);
                }
            }
        }
    }

    /**
     * 显示选择联系人TAG（FlowLayout）
     *
     * @param contactName 联系人name
     */
    private void addTag(String contactName) {
        TextView view = (TextView) mInflater.inflate(R.layout.tag, mFlContacts, false);
        view.setText(contactName);
        mFlContacts.addView(view);

    }

    /**
     * 获取电话号码
     *
     * @param cursor
     * @return
     */
    private String getContactNumber(Cursor cursor) {
        int numberCount = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
        String number = null;
        if (numberCount > 0) {
            int contactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            //通过ID查询
            Cursor phoneCursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
            phoneCursor.moveToFirst();
            number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            phoneCursor.close();
        }
        cursor.close();  //关闭cursor，防止内存泄露
        return number;
    }

    private void initViews() {
        mEdMsg = (EditText) findViewById(R.id.id_et_content);
        mBtnAdd = (Button) findViewById(R.id.id_bt_add);
        mFlContacts = (FlowLayout) findViewById(R.id.id_fl_contacts);
        mFabSend = (FloatingActionButton) findViewById(R.id.id_fab_send);
        mLayoutLoading = findViewById(R.id.id_layout_loading);

        mLayoutLoading.setVisibility(View.GONE);  //先隐藏发送进度条

        //给Msg赋值
        if (msgId != -1) {
            mMsg = FestivalLab.getInstance().getMsgByMsgId(msgId);
            mEdMsg.setText(mMsg.getContent());
        }
    }

    private void initDatas() {
        mFestivalId = getIntent().getIntExtra(KEY_FESTIVAL_ID, -1);
        msgId = getIntent().getIntExtra(KEY_MSG_ID, -1);
        mFestival = FestivalLab.getInstance().getFestivalById(mFestivalId);
        setTitle(mFestival.getName());
    }


    /**
     * 把startActivity的方法写在目标Activity的类里面
     * 目标Activity跳转方法提供给调用者
     *
     * @param context
     * @param festivalId
     * @param msgId
     */
    public static void toActivity(Context context, int festivalId, int msgId) {
        Intent intent = new Intent(context, SendMsgActivity.class);
        intent.putExtra(KEY_FESTIVAL_ID, festivalId);
        intent.putExtra(KEY_MSG_ID, msgId);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSendBroadcastReceiver);
        unregisterReceiver(mDeliverBroadcastReceiver);
    }
}
