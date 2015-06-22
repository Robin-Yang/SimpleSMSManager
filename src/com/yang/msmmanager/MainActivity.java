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
	private int currentState = LIST_STATE; // ��ǰĬ��״̬Ϊ�б�״̬

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
	
	private boolean isStop = false; //�Ƿ�ֹͣɾ��

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
	 * �����ؼ�������
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
	 * �η����Ǵ���options �˵����� ��ֻ�����һ��
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, SEARCH_ID, 0, "����");
		menu.add(0, EDIT_ID, 0, "�༭");
		menu.add(0, CANCLE_EDIT_ID, 0, "ȡ���༭");
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * ���˵���Ҫ��ʾ����Ļ��ʱ���ص��˷��� ������ʾ��һ���˵�
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (currentState == EDIT_STATE)
		{
			// ��ʾȡ���༭��������������
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
	 * ��options�˵���ѡ��ʱ�ص�
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case SEARCH_ID: // �����˵���ѡ��
			//δ��ʵ��
			break;
			
		case EDIT_ID: // �༭�˵�
			currentState = EDIT_STATE;
			refreshState();
			break;
			
		case CANCLE_EDIT_ID: // ȡ���˵�
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
	 * ˢ��״̬
	 */
	private void refreshState()
	{
		if (currentState == EDIT_STATE)
		{
			// �½���Ϣ���أ�������ť��ʾ��ÿһ��item��Ҫ��ʾһ��checkBox
			btnNewMessage.setVisibility(View.GONE);
			btnSelectAll.setVisibility(View.VISIBLE);
			btnCancelSelect.setVisibility(View.VISIBLE);
			btnDeleteMessage.setVisibility(View.VISIBLE);

			if (mMultiDeleteSet.size() == 0)
			{
				// û��ѡ���κ�checkbox
				btnCancelSelect.setEnabled(false);
				btnDeleteMessage.setEnabled(false);
			} else
			{
				btnCancelSelect.setEnabled(true);
				btnDeleteMessage.setEnabled(true);
			}
			// ȫѡ
			btnSelectAll.setEnabled(mMultiDeleteSet.size() != mListView
					.getCount());

		} else
		{
			// �½���Ϣ��ʾ����������
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
	 * �첽��ѯ����
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
		 * ����һ��View
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
		 * ������
		 */
		@SuppressWarnings("deprecation")
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			mHolder = (ConversationHolerView) view.getTag();

			// ȡ������
			String address = cursor.getString(ADDRESS_ID_COLUMN_INDEX);
			int count = cursor.getInt(COUNT_ID_COLUMN_INDEX);
			long date = cursor.getLong(DATE_ID_COLUMN_INDEX);
			String body = cursor.getString(BODY_COLUMN_INDEX);
			int id = cursor.getInt(THREAD_ID_COLUMN_INDEX);

			// �жϵ�ǰ��״̬�Ƿ�༭
			if (currentState == EDIT_STATE)
			{
				// ��ʾcheckbox
				mHolder.checkBox.setVisibility(View.VISIBLE);

				// ��ǰ�ĻỰid�Ƿ������deleteSet ������
				mHolder.checkBox.setChecked(mMultiDeleteSet.contains(id));
			} else
			{
				// ����checkbox
				mHolder.checkBox.setVisibility(View.GONE);
			}

			String contactName = Utils.getContactName(getContentResolver(),
					address);
			if (TextUtils.isEmpty(contactName))
			{ // ��ʾ����
				mHolder.tvName.setText(address + "(" + count + ")");
				mHolder.ivIcon
						.setBackgroundResource(R.drawable.ic_unknow_contact_picture);
			} else
			{
				// ��ʾ����
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
				// ��ʾʱ��
				strDate = DateFormat.getTimeFormat(context).format(date);
			} else
			{
				// ��ʾ����
				strDate = DateFormat.getDateFormat(context).format(date);
			}
			// �����ݴ�����ʾ ���ı���
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
		case R.id.btn_conversation_cancel_select: // ȡ��ѡ��
			mMultiDeleteSet.clear();
			mAdapter.notifyDataSetChanged(); // ˢ������
			refreshState();
			break;
			
		case R.id.btn_conversation_delete_message: // ɾ����Ϣ
			showConfirmDeleteDialog();
			break;
			
		case R.id.btn_conversation_new_message: // �½���Ϣ
			startActivity(new Intent(this,NewMessageActivity.class));
			break;
			
		case R.id.btn_conversation_select_all: // ȫѡ
			cursor = mAdapter.getCursor();
			cursor.moveToPosition(-1); // ��λ����ʼ��λ��
			while (cursor.moveToNext())
			{
				mMultiDeleteSet.add(cursor.getInt(THREAD_ID_COLUMN_INDEX));
			}
			mAdapter.notifyDataSetChanged(); // ˢ������
			refreshState();
			break;

		default:
			break;
		}

	}

	/**
	 * ȷ��ɾ���Ի���
	 */
	private void showConfirmDeleteDialog()
	{
		AlertDialog.Builder builder = new Builder(this);
		//�������Ͻǵ�ɾ��ͼ��
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle("ɾ��");
		builder.setMessage("ȷ��ɾ��ѡ�еĻỰ��");
		builder.setPositiveButton("OK",new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Log.i(TAG,"ȷ��ɾ��");
				//�������ȶԻ���
				showDeleteProgressDialog();
				isStop = false;
				//�������̣߳�����ɾ�����ţ�ÿɾ��һ����Ϣ�����½���
				new Thread(new DeleteRunnable()).start();
			}
		});
		builder.setNegativeButton("Cancel",null);
		builder.show();
		
	}
	
	/**
	 * ����ɾ���Ի���
	 */
	@SuppressWarnings("deprecation")
	private void showDeleteProgressDialog()
	{
		mProgressDialog = new ProgressDialog(this);
		//�������ֵ
		mProgressDialog.setMax(mMultiDeleteSet.size());
		//���ý���������ʾΪ����
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setButton("ȡ��",new DialogInterface.OnClickListener()
		{
			
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Log.i(TAG,"��ֹɾ��");
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
	 * ɾ���Ự������
	 */
	class DeleteRunnable implements Runnable{
		@Override
		public void run()
		{
			//ɾ���Ự
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
				
				//���½�����
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
		// �ѵ�ǰ�������item�ĻỰid��ӵ������У�ˢ��checkbox
		cursor = mAdapter.getCursor();
		// �ƶ�����ǰ�����������
		cursor.moveToPosition(position);
		// �Ựid
		int thread_id = cursor.getInt(THREAD_ID_COLUMN_INDEX);
		String address = cursor.getString(ADDRESS_ID_COLUMN_INDEX);
		
		// �жϱ༭״̬��ִ�У���Ȼ�����ڱ༭״̬Ҳ�ᱻ��¼��
		if (currentState == EDIT_STATE)
		{
			CheckBox checkBox = (CheckBox) view
					.findViewById(R.id.cb_conversation_item);
			if (checkBox.isChecked())
			{
				// �Ƴ�id
				mMultiDeleteSet.remove(thread_id);
			} else
			{
				mMultiDeleteSet.add(thread_id);
			}
			checkBox.setChecked(!checkBox.isChecked());

			// ÿһ�ε���ж�ˢ��һ�°�ť״̬
			refreshState();
		}else {
			Intent intent = new Intent(this,ConversationDetailActivity.class);
			intent.putExtra("thread_id",thread_id);
			intent.putExtra("address",address);
			startActivity(intent);
		}

	}

	

}
