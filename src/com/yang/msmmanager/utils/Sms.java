package com.yang.msmmanager.utils;

import android.net.Uri;

public class Sms
{
	/**
	 * ��ѯ�Ựuri
	 */
	public static final Uri CONVERSATION_URI = Uri.parse("content://sms/conversations");
	
	/**
	 * ����SMS���uri
	 */
	public static final Uri SMS_URI = Uri.parse("content://sms/");
	
	public static final int RECEVIE_TYPE = 1;  ///�������� �� ���յ�
	public static final int SEND_TYPE = 2;   //���͵�
	
	
}
