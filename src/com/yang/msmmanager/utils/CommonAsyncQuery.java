package com.yang.msmmanager.utils;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.widget.CursorAdapter;

public class CommonAsyncQuery extends AsyncQueryHandler
{

	private static final String TAG = "CommonAsyncQuenry";
	private OnQueryNotifyCompleteListener mOnQueryNotifyCompleteListener;
	
	public CommonAsyncQuery(ContentResolver cr)
	{
		super(cr);
	}

	/**
	 * ������startQuery��ʼ�첽��ѯ����ʱ����ѯ��Ϻ��ѯ�������α�����cursor�ش��ݵ��η���
	 * ִ�������߳��У��������ݣ�
	 * @param token startQuery ��������token
	 * @param cookie startQuery ��������cookie��CursorAdapter��
	 * @param cursor ��ѯ���������½����
	 */
	@Override
	protected void onQueryComplete(int token, Object cookie, Cursor cursor)
	{
//		Log.i(TAG,"onQueryComplete is calling");
//		Utils.printCursor(cursor);
		//��ˢ��֮ǰ�����û���һЩ׼������
		if (mOnQueryNotifyCompleteListener != null)
		{
			mOnQueryNotifyCompleteListener.onPreNotify(token,cookie,cursor);
		}
		
		//ˢ������
		if (cookie != null)
		{
			notifyAdapter((CursorAdapter) cookie,cursor);
		}
		
		//֪ͨ�û�ˢ����ɣ��û����Բ���һЩ����
		if (mOnQueryNotifyCompleteListener != null)
		{
			mOnQueryNotifyCompleteListener.onPostNotify(token,cookie,cursor);
		}
	}
	
	/**
	 * ��������
	 * @param adapter
	 * @param cursor
	 */
	private void notifyAdapter(CursorAdapter adapter,Cursor cursor)
	{
		//��adapter ˢ�����ݣ�����BaseAdapter �е�notifyDataSetchang
		adapter.changeCursor(cursor);
	}
	
	public void setOnQueryNotifyCompleteListener(OnQueryNotifyCompleteListener l)
	{
		
	}
	
	public interface OnQueryNotifyCompleteListener
	{
		//��adapter ����֮ǰ�ص��˷���----�û���һЩ��������֮ǰ��׼������
		void onPreNotify(int token,Object cookie,Cursor cursor);
		
		//��ˢ���������֮��ص��˷���
		void onPostNotify(int token,Object cookie,Cursor cursor);
	}

}
