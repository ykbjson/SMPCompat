package com.drivingassisstantHouse.library.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nostra13.universalimageloader.utils.L;

/**
 * 手机相关操作API
 * 拨电话，发短信，打开相册，拍照拿图片，打开指定连接，跳转到指定系统界面
 *
 * @author sunji
 * @version 1.0
 */
public class ToolPhone {

    /**
     * 直接呼叫指定的号码(需要<uses-permission android:name="android.permission.CALL_PHONE"/>权限)
     *
     * @param mContext    上下文Context
     * @param phoneNumber 需要呼叫的手机号码
     */
    public static void callPhone(Context mContext, String phoneNumber) {
        Uri uri = Uri.parse("tel:" + phoneNumber);
        Intent call = new Intent(Intent.ACTION_CALL, uri);
        mContext.startActivity(call);
    }

    /**
     * 跳转至拨号界面
     *
     * @param mContext    上下文Context
     * @param phoneNumber 需要呼叫的手机号码
     */
    public static void toCallPhoneActivity(Context mContext, String phoneNumber) {
        Uri uri = Uri.parse("tel:" + phoneNumber);
        Intent call = new Intent(Intent.ACTION_DIAL, uri);
        mContext.startActivity(call);
    }

    /**
     * 直接调用短信API发送信息(设置监听发送和接收状态)
     *
     * @param strPhone      手机号码
     * @param strMsgContext 短信内容
     */
    public static void sendMessage(final Context mContext, final String strPhone, final String strMsgContext) {

        //处理返回的发送状态
        String SENT_SMS_ACTION = "SENT_SMS_ACTION";
        Intent sentIntent = new Intent(SENT_SMS_ACTION);
        PendingIntent sendIntent = PendingIntent.getBroadcast(mContext, 0, sentIntent, 0);
        // register the Broadcast Receivers
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context _context, Intent _intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(mContext, "短信发送成功", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        break;
                }
            }
        }, new IntentFilter(SENT_SMS_ACTION));

        //处理返回的接收状态
        String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
        // createSpannableString the deilverIntent parameter
        Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
        PendingIntent backIntent = PendingIntent.getBroadcast(mContext, 0, deliverIntent, 0);
        mContext.registerReceiver(new BroadcastReceiver() {
                                      @Override
                                      public void onReceive(Context _context, Intent _intent) {
                                          Toast.makeText(mContext, strPhone + "已经成功接收", Toast.LENGTH_SHORT).show();
                                      }
                                  },
                new IntentFilter(DELIVERED_SMS_ACTION)
        );

        //拆分短信内容（手机短信长度限制）
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> msgList = smsManager.divideMessage(strMsgContext);
        for (String text : msgList) {
            smsManager.sendTextMessage(strPhone, null, text, sendIntent, backIntent);
        }
    }

    /**
     * 跳转至发送短信界面(自动设置接收方的号码)
     *
     * @param mContext
     * @param strPhone      手机号码
     * @param strMsgContext 短信内容
     */
    public static void toSendMessageActivity(Context mContext, String strPhone, String strMsgContext) {
        if (PhoneNumberUtils.isGlobalPhoneNumber(strPhone)) {
            Uri uri = Uri.parse("smsto:" + strPhone);
            Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);
            sendIntent.putExtra("sms_body", strMsgContext);
            mContext.startActivity(sendIntent);
        }
    }

    /**
     * 使用该方式，非android原生系统短信应用也能拉起。
     * 群发或单人短信；跳转至发送短信界面(自动设置接收方的号码)
     * @param mContext
     * @param strPhone 手机号码，若群发则以分号(;)隔开
     * @param strMsgContext 短信内容
     */
    public static boolean toSendMutiMsgeActivity(Context mContext,String strPhone,String strMsgContext){
        boolean isSuccess=true;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra("address", strPhone);
            intent.putExtra("sms_body", strMsgContext);
            intent.setType("vnd.android-dir/mms-sms");
            mContext.startActivity(intent);
        }
        catch (Exception e){
            e.printStackTrace();
            isSuccess=false;
        }
        return  isSuccess;
    }

    /**
     * 跳转至联系人选择界面
     *
     * @param mContext    上下文
     * @param requestCode 请求返回区分代码
     */
    public static void toChooseContactsList(Activity mContext, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mContext.startActivityForResult(intent, requestCode);
    }


    public static String[] getPhone(Context mContext, Cursor mCursor) {
        String phoneResult[] = new String[2];
        if (mCursor != null && !mCursor.isClosed() && mCursor.getCount() > 0) {
            L.d("choosenumber->mCursor", "不为null");
            if (mCursor.moveToFirst()) {
                L.d("choosenumber->mCursor", "移动到第一个");
                int phoneColumn = mCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                int phoneNameColumn = mCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int phoneNum = mCursor.getInt(phoneColumn);
                if (phoneNum > 0) {
                    L.d("choosenumber->phoneNum", "存在数值");
                    // 获得联系人的ID号
                    int idColumn = mCursor.getColumnIndex(ContactsContract.Contacts._ID);
                    String contactId = mCursor.getString(idColumn);
                    phoneResult[0] = mCursor.getString(phoneNameColumn) + "";
                    L.d("choosenumber->获取到联系人", phoneResult[0]);
                    // 获得联系人的电话号码的cursor;
                    Cursor phones = mContext.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
                                    + contactId, null, null);
                    if (phones != null && phones.moveToFirst()) {
                        L.d("choosenumber->最内层，不为空", "准备循环获取号码");

                        // 遍历所有的电话号码
                        for (; !phones.isAfterLast(); phones.moveToNext()) {
                            int index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            int typeindex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                            int phone_type = phones.getInt(typeindex);
                            String phoneNumber = phones.getString(index);
                            if(!TextUtils.isEmpty(phoneNumber)){
                                L.d("choosenumber->获取到电话号码", phoneNumber);
                                if(phoneNumber.contains("+86")){
                                    phoneNumber=phoneNumber.replace("+86","");//移除开头的+86
                                }
                                phoneResult[1] = phoneNumber + "";
                                break;
                            }
                            //很多手机修改过不同类型值对应的类型，2代表的是手机号，几乎都遵从这个约定，但这里如果联系人存了座机号码，也让其选择
//                            switch (phone_type) {
//                                case 2:
//                                    L.d("choosenumber->获取到电话号码", phoneNumber);
//                                    phoneResult[1] = phoneNumber + "";
//                                    break;
//                            }
                        }
                        if (!phones.isClosed()) {
                            phones.close();
                        }
                    }
                } else {
                    L.d("choosenumber->phoneNum", "不存在");
                }
            } else {
                L.d("choosenumber->mCursor", "moveToFirst失败");
            }
        }
        return phoneResult;
    }


    /**
     * 获取选择的联系人的手机号码
     *
     * @param mContext   上下文
     * @param resultCode 请求返回Result状态区分代码
     * @param data       onActivityResult返回的Intent
     * @return
     */
    public static String[] getChoosedPhoneNumber(Activity mContext, int resultCode, Intent data) {
        //返回结果
        String phoneResult[] = new String[2];
        try {
            if (Activity.RESULT_OK == resultCode && data != null && mContext != null) {
                Uri uri = data.getData();

                Cursor mCursor = mContext.managedQuery(uri, null, null, null, null);
                if (mCursor != null && !mCursor.isClosed() && mCursor.getCount() > 0) {

                    if (mCursor.moveToFirst()) {

                        int phoneColumn = mCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                        int phoneNameColumn = mCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                        int phoneNum = mCursor.getInt(phoneColumn);
                        if (phoneNum > 0) {
                            // 获得联系人的ID号
                            int idColumn = mCursor.getColumnIndex(ContactsContract.Contacts._ID);
                            String contactId = mCursor.getString(idColumn);
                            phoneResult[0] = mCursor.getString(phoneNameColumn) + "";

                            // 获得联系人的电话号码的cursor;
                            Cursor phones = mContext.getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
                                            + contactId, null, null);
                            if (phones != null && phones.moveToFirst()) {
                                // 遍历所有的电话号码
                                for (; !phones.isAfterLast(); phones.moveToNext()) {
                                    int index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                    int typeindex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                                    int phone_type = phones.getInt(typeindex);
                                    String phoneNumber = phones.getString(index);
                                    switch (phone_type) {
                                        case 2:
                                            phoneResult[1] = phoneNumber + "";
                                            break;
                                    }
                                }
                                if (!phones.isClosed()) {
                                    phones.close();
                                }
                            }
                        }
                    }
                }
                //关闭游标
                //https://code.google.com/p/android/issues/detail?id=23746
                //android4.0的managedCursor /managedQuery会自动的close一个cursor
                if (Integer.parseInt(Build.VERSION.SDK) < 11 && mCursor != null) {
                    mCursor.close();
                }
            }
        } catch (CursorIndexOutOfBoundsException exception) {
            exception.printStackTrace();
        }
        return phoneResult;
    }

    /**
     * 跳转至拍照程序界面
     *
     * @param mContext    上下文
     * @param requestCode 请求返回Result区分代码
     */
    public static void toCameraActivity(Activity mContext, int requestCode) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mContext.startActivityForResult(intent, requestCode);
    }

    /**
     * 跳转至相册选择界面
     *
     * @param mContext    上下文
     * @param requestCode
     */
    public static void toImagePickerActivity(Activity mContext, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        mContext.startActivityForResult(intent, requestCode);
    }

    /**
     * 获得选中相册的图片
     *
     * @param mContext 上下文
     * @param data     onActivityResult返回的Intent
     * @return
     */
    public static Bitmap getChoosedImage(Activity mContext, Intent data) {
        if (data == null) {
            return null;
        }

        Bitmap bm = null;

        // 外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
        ContentResolver resolver = mContext.getContentResolver();

        // 此处的用于判断接收的Activity是不是你想要的那个
        try {
            Uri originalUri = data.getData(); // 获得图片的uri
            bm = MediaStore.Images.Media.getBitmap(resolver, originalUri); // 显得到bitmap图片
            // 这里开始的第二部分，获取图片的路径：
            String[] proj = {
                    MediaStore.Images.Media.DATA
            };
            // 好像是android多媒体数据库的封装接口，具体的看Android文档
            Cursor cursor = mContext.managedQuery(originalUri, proj, null, null, null);
            // 按我个人理解 这个是获得用户选择的图片的索引值
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            // 将光标移至开头 ，这个很重要，不小心很容易引起越界
            cursor.moveToFirst();
            // 最后根据索引值获取图片路径
            String path = cursor.getString(column_index);
            //不用了关闭游标
            cursor.close();
        } catch (Exception e) {
            Log.e("ToolPhone", e.getMessage());
        }

        return bm;
    }

    /**
     * 调用本地浏览器打开一个网页
     *
     * @param mContext   上下文
     * @param strSiteUrl 网页地址
     */
    public static void openWebSite(Context mContext, String strSiteUrl) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(strSiteUrl));
        mContext.startActivity(webIntent);
    }

    /**
     * 跳转至系统设置界面
     *
     * @param mContext 上下文
     */
    public static void toSettingActivity(Context mContext) {
        Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
        mContext.startActivity(settingsIntent);
    }

    /**
     * 跳转至WIFI设置界面
     *
     * @param mContext 上下文
     */
    public static void toWIFISettingActivity(Context mContext) {
        Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        mContext.startActivity(wifiSettingsIntent);
    }

    /**
     * 启动本地应用打开PDF
     *
     * @param mContext 上下文
     * @param filePath 文件路径
     */
    public static void openPDFFile(Context mContext, String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                Uri path = Uri.fromFile(file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(path, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "未检测到可打开PDF相关软件", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * 启动本地应用打开PDF
     *
     * @param mContext 上下文
     * @param filePath 文件路径
     */
    public static void openWordFile(Context mContext, String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                Uri path = Uri.fromFile(file);
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.addCategory("android.intent.category.DEFAULT");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(path, "application/msword");
                mContext.startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(mContext, "未检测到可打开Word文档相关软件", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * 调用WPS打开office文档
     * http://bbs.wps.cn/thread-22349340-1-1.html
     *
     * @param mContext 上下文
     * @param filePath 文件路径
     */
    public static void openOfficeByWPS(Context mContext, String filePath) {

        try {

            //文件存在性检查
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(mContext, filePath + "文件路径不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            //检查是否安装WPS
            String wpsPackageEng = "cn.wps.moffice_eng";//普通版与英文版一样
//			String wpsActivity = "cn.wps.moffice.documentmanager.PreStartActivity";
            String wpsActivity2 = "cn.wps.moffice.documentmanager.PreStartActivity2";//默认第三方程序启动

            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setClassName(wpsPackageEng, wpsActivity2);

            Uri uri = Uri.fromFile(new File(filePath));
            intent.setData(uri);
            mContext.startActivity(intent);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, "本地未安装WPS", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(mContext, "打开文档失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 判断是否安装指定包名的APP
     *
     * @param mContext    上下文
     * @param packageName 包路径
     * @return
     */
    public static boolean isInstalledApp(Context mContext, String packageName) {
        if (packageName == null || "".equals(packageName)) {
            return false;
        }

        try {
            ApplicationInfo info = mContext.getPackageManager()
                    .getApplicationInfo(packageName,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 判断是否存在指定的Activity
     *
     * @param mContext    上下文
     * @param packageName 包名
     * @param className   activity全路径类名
     * @return
     */
    public static boolean isExistActivity(Context mContext, String packageName, String className) {

        Boolean result = true;
        Intent intent = new Intent();
        intent.setClassName(packageName, className);

        if (mContext.getPackageManager().resolveActivity(intent, 0) == null) {
            result = false;
        } else if (intent.resolveActivity(mContext.getPackageManager()) == null) {
            result = false;
        } else {
            List<ResolveInfo> list = mContext.getPackageManager()
                    .queryIntentActivities(intent, 0);
            if (list.size() == 0) {
                result = false;
            }
        }

        return result;
    }

}
