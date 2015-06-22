package com.yang.msmmanager.utils;

import android.net.Uri;

public class Sms
{
	/**
	 * 查询会话uri
	 */
	public static final Uri CONVERSATION_URI = Uri.parse("content://sms/conversations");
	
	/**
	 * 操作SMS表的uri
	 */
	public static final Uri SMS_URI = Uri.parse("content://sms/");
	
	public static final int RECEVIE_TYPE = 1;  ///短信类型 ： 接收的
	public static final int SEND_TYPE = 2;   //发送的
	
	
}
