package com.yang.msmmanager;

import java.util.HashSet;
import java.util.Iterator;

import com.yang.msmmanager.utils.CommonAsyncQuery;
import com.yang.msmmanager.utils.Sms;
import com.yang.msmmanager.utils.Utils;
import com.yang.sms.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener,
		OnItemClickListener
{
	private static final String TAG = "ConverationUI";
	
	private static final int SEARCH_ID = 0;
	private static final int EDIT_ID = 1;
	private static final int CANCLE_EDIT_ID = 2;

	private final int LIST_STATE = -1;
	private final int EDIT_STATE = -2;
	private int currentState = LIST_STATE; // 当前默认状态为列表状态

	private HashSet<Integer> mMultiDeleteSet;

	private String[] projection =
	{ "sms.thread_id AS _id", "sms.body AS body", "groups.msg_count AS count",
			"sms.date AS date", "sms.address AS address" };

	private final int THREAD_ID_COLUMN_INDEX = 0;
	private final int BODY_COLUMN_INDEX = 1;
	private final int COUNT_ID_COLUMN_INDEX = 2;
	private final int DATE_ID_COLUMN_INDEX = 3;
	private final int ADDRESS_ID_COLUMN_INDEX = 4;

	private ConversationAdapter mAdapter;

	private Button btnNewMessage;
	private Button btnSelectAll;
	private Button btnCancelSelect;
	private Button btnDeleteMessage;
	private ListView mListView;
	private Cursor cursor;

	private ProgressDialog mProgressDialog;
	
	private boolean isStop = false; //是否停止删除

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.conversation);

		initView();
		prepareData();
	}

	/**
	 * 按返回键的设置
	 */
	@Override
	public void onBackPressed()
	{
		if (currentState == EDIT_STATE)
		{
			currentState = LIST_STATE;
			mMultiDeleteSet.clear();
			refreshState();
			return;
		}
		super.onBackPressed();
	}


	/**
	 * 次方法是创建options 菜单调用 ，只会调用一次
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, SEARCH_ID, 0, "搜索");
		menu.add(0, EDIT_ID, 0, "编辑");
		menu.add(0, CANCLE_EDIT_ID, 0, "取消编辑");
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * 当菜单将要显示在屏幕上时，回调此方法 控制显示哪一个菜单
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (currentState == EDIT_STATE)
		{
			// 显示取消编辑，隐藏另外两个
			menu.findItem(SEARCH_ID).setVisible(false);
			menu.findItem(EDIT_ID).setVisible(false);
			menu.findItem(CANCLE_EDIT_ID).setVisible(true);
		} else
		{
			menu.findItem(SEARCH_ID).setVisible(true);
			menu.findItem(EDIT_ID).setVisible(true);
			menu.findItem(CANCLE_EDIT_ID).setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * 当options菜单被选中时回调
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case SEARCH_ID: // 搜索菜单被选中
			//未能实现
			break;
			
		case EDIT_ID: // 编辑菜单
			currentState = EDIT_STATE;
			refreshState();
			break;
			
		case CANCLE_EDIT_ID: // 取消菜单
			currentState = LIST_STATE;
			mMultiDeleteSet.clear();
			refreshState();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 刷新状态
	 */
	private void refreshState()
	{
		if (currentState == EDIT_STATE)
		{
			// 新建信息隐藏，其他按钮显示，每一个item都要显示一个checkBox
			btnNewMessage.setVisibility(View.GONE);
			btnSelectAll.setVisibility(View.VISIBLE);
			btnCancelSelect.setVisibility(View.VISIBLE);
			btnDeleteMessage.setVisibility(View.VISIBLE);

			if (mMultiDeleteSet.size() == 0)
			{
				// 没有选中任何checkbox
				btnCancelSelect.setEnabled(false);
				btnDeleteMessage.setEnabled(false);
			} else
			{
				btnCancelSelect.setEnabled(true);
				btnDeleteMessage.setEnabled(true);
			}
			// 全选
			btnSelectAll.setEnabled(mMultiDeleteSet.size() != mListView
					.getCount());

		} else
		{
			// 新建信息显示，其他隐藏
			btnNewMessage.setVisibility(View.VISIBLE);
			btnSelectAll.setVisibility(View.GONE);
			btnCancelSelect.setVisibility(View.GONE);
			btnDeleteMessage.setVisibility(View.GONE);
		}
	}

	private void initView()
	{
		mMultiDeleteSet = new HashSet<Integer>();

		mListView = (ListView) findViewById(R.id.lv_conversation);
		btnNewMessage = (Button) findViewById(R.id.btn_conversation_new_message);
		btnSelectAll = (Button) findViewById(R.id.btn_conversation_select_all);
		btnCancelSelect = (Button) findViewById(R.id.btn_conversation_cancel_select);
		btnDeleteMessage = (Button) findViewById(R.id.btn_conversation_delete_message);

		btnNewMessage.setOnClickListener(this);
		btnSelectAll.setOnClickListener(this);
		btnCancelSelect.setOnClickListener(this);
		btnDeleteMessage.setOnClickListener(this);

		mAdapter = new ConversationAdapter(this, null);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
	}

	/**
	 * 异步查询数据
	 */
	private void prepareData()
	{
		CommonAsyncQuery asyncQuery = new CommonAsyncQuery(
				getContentResolver());
		asyncQuery.startQuery(0, mAdapter, Sms.CONVERSATION_URI, projection,
				null, null, "date desc");

	}

	class ConversationAdapter extends CursorAdapter
	{

		private ConversationHolerView mHolder;

		public ConversationAdapter(Context context, Cursor c)
		{
			super(context, c);
		}

		/**
		 * 创建一个View
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			View view = View.inflate(context, R.layout.conversation_item, null);
			mHolder = new ConversationHolerView();
			mHolder.checkBox = (CheckBox) view
					.findViewById(R.id.cb_conversation_item);
			mHolder.ivIcon = (ImageView) view
					.findViewById(R.id.iv_conversation_item_icon);
			mHolder.tvBody = (TextView) view
					.findViewById(R.id.tv_conversation_item_body);
			mHolder.tvDate = (TextView) view
					.findViewById(R.id.tv_conversation_item_date);
			mHolder.tvName = (TextView) view
					.findViewById(R.id.tv_conversation_item_name);

			view.setTag(mHolder);
			return view;
		}

		/**
		 * 绑定数据
		 */
		@SuppressWarnings("deprecation")
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			mHolder = (ConversationHolerView) view.getTag();

			// 取出数据
			String address = cursor.getString(ADDRESS_ID_COLUMN_INDEX);
			int count = cursor.getInt(COUNT_ID_COLUMN_INDEX);
			long date = cursor.getLong(DATE_ID_COLUMN_INDEX);
			String body = cursor.getString(BODY_COLUMN_INDEX);
			int id = cursor.getInt(THREAD_ID_COLUMN_INDEX);

			// 判断当前的状态是否编辑
			if (currentState == EDIT_STATE)
			{
				// 显示checkbox
				mHolder.checkBox.setVisibility(View.VISIBLE);

				// 当前的会话id是否存在与deleteSet 集合中
				mHolder.checkBox.setChecked(mMultiDeleteSet.contains(id));
			} else
			{
				// 隐藏checkbox
				mHolder.checkBox.setVisibility(View.GONE);
			}

			String contactName = Utils.getContactName(getContentResolver(),
					address);
			if (TextUtils.isEmpty(contactName))
			{ // 显示号码
				mHolder.tvName.setText(address + "(" + count + ")");
				mHolder.ivIcon
						.setBackgroundResource(R.drawable.ic_unknow_contact_picture);
			} else
			{
				// 显示名称
				mHolder.tvName.setText(contactName + "(" + count + ")");

				Bitmap contactIcon = Utils.getContactIcon(getContentResolver(),
						address);
				if (contactIcon != null)
				{
					mHolder.ivIcon.setBackgroundDrawable(new BitmapDrawable(
							contactIcon));
				} else
				{
					mHolder.ivIcon
							.setBackgroundResource(R.drawable.ic_contact_picture);
				}
			}

			String strDate = null;
			if (DateUtils.isToday(date))
			{
				// 显示时间
				strDate = DateFormat.getTimeFormat(context).format(date);
			} else
			{
				// 显示日期
				strDate = DateFormat.getDateFormat(context).format(date);
			}
			// 把数据传到显示 的文本上
			mHolder.tvDate.setText(strDate);

			mHolder.tvBody.setText(body);
		}

	}

	public class ConversationHolerView
	{
		public CheckBox checkBox;
		public ImageView ivIcon;
		public TextView tvName;
		public TextView tvDate;
		public TextView tvBody;
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.btn_conversation_cancel_select: // 取消选中
			mMultiDeleteSet.clear();
			mAdapter.notifyDataSetChanged(); // 刷新数据
			refreshState();
			break;
			
		case R.id.btn_conversation_delete_message: // 删除信息
			showConfirmDeleteDialog();
			break;
			
		case R.id.btn_conversation_new_message: // 新建信息
			startActivity(new Intent(this,NewMessageActivity.class));
			break;
			
		case R.id.btn_conversation_select_all: // 全选
			cursor = mAdapter.getCursor();
			cursor.moveToPosition(-1); // 复位到初始的位置
			while (cursor.moveToNext())
			{
				mMultiDeleteSet.add(cursor.getInt(THREAD_ID_COLUMN_INDEX));
			}
			mAdapter.notifyDataSetChanged(); // 刷新数据
			refreshState();
			break;

		default:
			break;
		}

	}

	/**
	 * 确认删除对话框
	 */
	private void showConfirmDeleteDialog()
	{
		AlertDialog.Builder builder = new Builder(this);
		//设置左上角的删除图标
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle("删除");
		builder.setMessage("确认删除选中的会话吗？");
		builder.setPositiveButton("OK",new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Log.i(TAG,"确认删除");
				//弹出进度对话框
				showDeleteProgressDialog();
				isStop = false;
				//开启子线程，真正删除短信，每删除一条信息，更新进度
				new Thread(new DeleteRunnable()).start();
			}
		});
		builder.setNegativeButton("Cancel",null);
		builder.show();
		
	}
	
	/**
	 * 弹出删除对话框
	 */
	@SuppressWarnings("deprecation")
	private void showDeleteProgressDialog()
	{
		mProgressDialog = new ProgressDialog(this);
		//设置最大值
		mProgressDialog.setMax(mMultiDeleteSet.size());
		//设置进度条的演示为长度
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setButton("取消",new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Log.i(TAG,"终止删除");
				isStop = true;
			}
		});
		mProgressDialog.show();
		mProgressDialog.setOnDismissListener(new OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface arg0)
			{
				currentState = LIST_STATE;
				refreshState();
			}
		});
	}
	 
	/**
	 * 删除会话的任务
	 */
	class DeleteRunnable implements Runnable{
		@Override
		public void run()
		{
			//删除会话
			Iterator<Integer> iterator = mMultiDeleteSet.iterator();
			int thread_id;
			String where;
			String[] selectionArgs;
			while (iterator.hasNext())
			{
				if (isStop)
				{
					break;
				}
				thread_id = iterator.next();
				where = "thread_id = ?";
				selectionArgs = new String[]{String.valueOf(thread_id)};
				getContentResolver().delete(Sms.SMS_URI,where,selectionArgs);
				
				SystemClock.sleep(2000);
				
				//更新进度条
				mProgressDialog.incrementProgressBy(1);
			}
			mMultiDeleteSet.clear();
			mProgressDialog.dismiss();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id)
	{
		// 把当前被点击的item的会话id添加到集合中，刷新checkbox
		cursor = mAdapter.getCursor();
		// 移动到当前被点击的索引
		cursor.moveToPosition(position);
		// 会话id
		int thread_id = cursor.getInt(THREAD_ID_COLUMN_INDEX);
		String address = cursor.getString(ADDRESS_ID_COLUMN_INDEX);
		
		// 判断编辑状态才执行，不然不处于编辑状态也会被记录的
		if (currentState == EDIT_STATE)
		{
			CheckBox checkBox = (CheckBox) view
					.findViewById(R.id.cb_conversation_item);
			if (checkBox.isChecked())
			{
				// 移除id
				mMultiDeleteSet.remove(thread_id);
			} else
			{
				mMultiDeleteSet.add(thread_id);
			}
			checkBox.setChecked(!checkBox.isChecked());

			// 每一次点击判断刷新一下按钮状态
			refreshState();
		}else {
			Intent intent = new Intent(this,ConversationDetailActivity.class);
			intent.putExtra("thread_id",thread_id);
			intent.putExtra("address",address);
			startActivity(intent);
		}

	}

	

}
