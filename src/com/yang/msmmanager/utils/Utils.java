package com.yang.msmmanager.utils;

import java.io.InputStream;
import java.util.ArrayList;


import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsManager;
import android.util.Log;

public class Utils
{
 
	private static final String TAG = "Utils";
	/**
	 * 输出游标结果
	 * @param cursor
	 */
	public static void printCursor(Cursor cursor)
	{
		if (cursor != null && cursor.getCount() > 0)
		{
			String columnName;
			String columnValue;  //column 列
			while (cursor.moveToNext())
			{
				for (int i = 0; i < cursor.getColumnCount(); i++)
				{
					 columnName = cursor.getColumnName(i);
					 columnValue = cursor.getString(i);
					Log.i(TAG,"第" + cursor.getPosition() + "行："+columnName+"="+columnValue);
				}
			}
			cursor.close();
		}
	}
	/**
	 * 根据联系号码获取联系人姓名
	 * @param address
	 * @return
	 */
	public static String getContactName(ContentResolver resolver,String address)
	{
		//content://com.android.contacts/phone_lookup/95556
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,address);
		
		Cursor cursor = resolver.query(uri,new String[]{"display_name"},null,null,null);
		if (cursor != null && cursor.moveToFirst())
		{
			String contactName = cursor.getString(0);
			cursor.close();
			return contactName;
		}
		return null;
	}
	
	/**
	 * 根据联系人的号码查询联系人的头像
	 * @param resolver
	 * @param address
	 * @return
	 */
	public static Bitmap getContactIcon(ContentResolver resolver,String address)
	{
		// 1. 根据号码取得联系人的id
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,address);
		Cursor cursor = resolver.query(uri,new String[]{"_id"},null,null,null);
		if (cursor != null && cursor.moveToFirst())
		{
			long id = cursor.getLong(0);
			cursor.close();
			//2. 根据id获得联系人的头像
			uri = ContentUris.withAppendedId(Contacts.CONTENT_URI,id);
			InputStream is = Contacts.openContactPhotoInputStream(resolver,uri);
			return BitmapFactory.decodeStream(is);
		}
		return null;
	}
	
	/**
	 * 发送短信
	 */
	public static void sendMessage(Context context,String address,String content)
	{
		SmsManager smsManager = SmsManager.getDefault();
		
		ArrayList<String> divideMessage = smsManager.divideMessage(content);
		
		Intent intent = new Intent("com.example.heima.SendMessageBroadcasrReceive");
		PendingIntent sentIntent = PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_ONE_SHOT);
	
		for (String text : divideMessage)
		{
			smsManager.sendTextMessage(
					address,   //对方手机号
					null,   //短信中心
					text,
					sentIntent,  // 当短信发送成功时回调，回调方式：延期意图（延期意图指向的是广播接收者
					null  //当对方收到短信时回调
					);
		}
		//把整条短信添加到数据库
		writeMessage(context,address,content);
	}
	
	private static void writeMessage(Context context, String address,String content)
	{
		ContentValues values = new ContentValues();
		values.put("address",address);
		values.put("body",content);
		context.getContentResolver().insert(Sms.SMS_URI,values);
	}
	
	/**
	 * 获得联系人id ----根据给定的uri查询联系人的id
	 */
	
	public static int getContactID(ContentResolver resolver,Uri uri)
	{
		Cursor cursor = resolver.query(uri,new String[]{"has_phone_number","_id"},null,null,null);
		if (cursor != null && cursor.moveToFirst())  
		{
			int hasPhoneNumber = cursor.getInt(0);
			if (hasPhoneNumber > 0)
			{
				int contactID = cursor.getInt(1);
				cursor.close();
				return contactID;
			}
		}
		
		return -1;  //娶不到时返回-1
		
	}
	
	/**
	 * 根据联系人的id取联系人的手机号
	 * @param resolver
	 * @param contact_id
	 * @return
	 */
	public static String getContactAddress(ContentResolver resolver,int contact_id)
	{
		String selection = "contact_id";
		String[] selectionArgs = {String.valueOf(contact_id)};
		
		Cursor cursor = resolver.query(Phone.CONTENT_URI,new String[]{Phone.NUMBER},selection,selectionArgs,null);
//		Utils.printCursor(cursor);
		if (cursor != null && cursor.moveToFirst())
		{
			String address = cursor.getString(0);
			cursor.close();
			return address;
		}
		
		return null;
	}
	
	
	
	
}
