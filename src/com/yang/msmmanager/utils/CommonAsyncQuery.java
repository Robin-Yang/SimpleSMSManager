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
	 * 当调用startQuery开始异步查询数据时，查询完毕后查询出来的游标结果集cursor回传递到次方法
	 * 执行在主线程中（更新数据）
	 * @param token startQuery 传进来的token
	 * @param cookie startQuery 传进来的cookie（CursorAdapter）
	 * @param cursor 查询出来的最新结果集
	 */
	@Override
	protected void onQueryComplete(int token, Object cookie, Cursor cursor)
	{
//		Log.i(TAG,"onQueryComplete is calling");
//		Utils.printCursor(cursor);
		//再刷新之前，让用户做一些准备操作
		if (mOnQueryNotifyCompleteListener != null)
		{
			mOnQueryNotifyCompleteListener.onPreNotify(token,cookie,cursor);
		}
		
		//刷新数据
		if (cookie != null)
		{
			notifyAdapter((CursorAdapter) cookie,cursor);
		}
		
		//通知用户刷新完成，用户可以操作一些事情
		if (mOnQueryNotifyCompleteListener != null)
		{
			mOnQueryNotifyCompleteListener.onPostNotify(token,cookie,cursor);
		}
	}
	
	/**
	 * 更新数据
	 * @param adapter
	 * @param cursor
	 */
	private void notifyAdapter(CursorAdapter adapter,Cursor cursor)
	{
		//给adapter 刷新数据，类似BaseAdapter 中的notifyDataSetchang
		adapter.changeCursor(cursor);
	}
	
	public void setOnQueryNotifyCompleteListener(OnQueryNotifyCompleteListener l)
	{
		
	}
	
	public interface OnQueryNotifyCompleteListener
	{
		//当adapter 更新之前回调此方法----用户做一些适配数据之前的准备操作
		void onPreNotify(int token,Object cookie,Cursor cursor);
		
		//当刷新完成数据之后回调此方法
		void onPostNotify(int token,Object cookie,Cursor cursor);
	}

}
