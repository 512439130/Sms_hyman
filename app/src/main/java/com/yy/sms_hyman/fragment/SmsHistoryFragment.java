package com.yy.sms_hyman.fragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yy.sms_hyman.R;
import com.yy.sms_hyman.bean.SendedMsg;
import com.yy.sms_hyman.db.SmsProvider;
import com.yy.sms_hyman.view.FlowLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by 13160677911 on 2016-12-25.
 */

public class SmsHistoryFragment extends ListFragment {
    private static final int LOADER_ID = 1;
    private LayoutInflater mInflater;
    private CursorAdapter mCursorAdapter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
        initLoader();
        setupListAdapter();
    }


    private void initLoader() {
        getLoaderManager().initLoader(LOADER_ID, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            //onCreateLoader
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                CursorLoader loader = new CursorLoader(getActivity(), SmsProvider.URI_SMS_ALL, null, null, null, null);

                return loader;
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                if (loader.getId() == LOADER_ID) {
                    mCursorAdapter.swapCursor(data);
                }
            }

            //change
            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mCursorAdapter.swapCursor(null);
            }
        });
    }

    private void setupListAdapter() {
        mCursorAdapter = new CursorAdapter(getActivity(), null, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {  //返回item的布局
                View view = mInflater.inflate(R.layout.item_sended_msg, parent, false);
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {  //拿到item的布局为其设置数据
                TextView msg = (TextView) view.findViewById(R.id.id_tv_msg);
                TextView fes = (TextView) view.findViewById(R.id.id_tv_fes);
                TextView date = (TextView) view.findViewById(R.id.id_tv_date);
                FlowLayout fl = (FlowLayout) view.findViewById(R.id.id_fl_contacts);
                //为view赋值
                msg.setText(cursor.getString(cursor.getColumnIndex(SendedMsg.COLUMN_MSG)));
                fes.setText(cursor.getString(cursor.getColumnIndex(SendedMsg.COLUMN_FES_NAME)));

                //日期转换格式
                long dateVal =cursor.getLong(cursor.getColumnIndex(SendedMsg.COLUMN_DATE));

                date.setText(parseDate(dateVal));

                String names = cursor.getString(cursor.getColumnIndex(SendedMsg.COLUMN_NAMES));

                if(TextUtils.isEmpty(names)){
                    return;
                }
                fl.removeAllViews();
                for (String name : names.split(":")){
                    addTag(name, fl);
                }


            }
        };
        setListAdapter(mCursorAdapter);
    }

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Date格式转换
     * @param dateVal
     * @return
     */
    private String parseDate(long dateVal) {
        return df.format(dateVal);
    }

    private void addTag(String name, FlowLayout fl) {
        TextView tv = (TextView) mInflater.inflate(R.layout.tag, fl, false);
        tv.setText(name);
        fl.addView(tv);
    }
}
