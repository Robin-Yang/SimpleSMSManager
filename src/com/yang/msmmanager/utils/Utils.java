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
	 * ����α���
	 * @param cursor
	 */
	public static void printCursor(Cursor cursor)
	{
		if (cursor != null && cursor.getCount() > 0)
		{
			String columnName;
			String columnValue;  //column ��
			while (cursor.moveToNext())
			{
				for (int i = 0; i < cursor.getColumnCount(); i++)
				{
					 columnName = cursor.getColumnName(i);
					 columnValue = cursor.getString(i);
					Log.i(TAG,"��" + cursor.getPosition() + "�У�"+columnName+"="+columnValue);
				}
			}
			cursor.close();
		}
	}
	/**
	 * ������ϵ�����ȡ��ϵ������
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
	 * ������ϵ�˵ĺ����ѯ��ϵ�˵�ͷ��
	 * @param resolver
	 * @param address
	 * @return
	 */
	public static Bitmap getContactIcon(ContentResolver resolver,String address)
	{
		// 1. ���ݺ���ȡ����ϵ�˵�id
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,address);
		Cursor cursor = resolver.query(uri,new String[]{"_id"},null,null,null);
		if (cursor != null && cursor.moveToFirst())
		{
			long id = cursor.getLong(0);
			cursor.close();
			//2. ����id�����ϵ�˵�ͷ��
			uri = ContentUris.withAppendedId(Contacts.CONTENT_URI,id);
			InputStream is = Contacts.openContactPhotoInputStream(resolver,uri);
			return BitmapFactory.decodeStream(is);
		}
		return null;
	}
	
	/**
	 * ���Ͷ���
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
					address,   //�Է��ֻ���
					null,   //��������
					text,
					sentIntent,  // �����ŷ��ͳɹ�ʱ�ص����ص���ʽ��������ͼ��������ͼָ����ǹ㲥������
					null  //���Է��յ�����ʱ�ص�
					);
		}
		//������������ӵ����ݿ�
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
	 * �����ϵ��id ----���ݸ�����uri��ѯ��ϵ�˵�id
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
		
		return -1;  //Ȣ����ʱ����-1
		
	}
	
	/**
	 * ������ϵ�˵�idȡ��ϵ�˵��ֻ���
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
