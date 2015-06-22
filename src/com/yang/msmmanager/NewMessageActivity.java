package com.yang.msmmanager;

import com.yang.msmmanager.utils.Utils;
import com.yang.sms.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.TextView;
import android.widget.Toast;

public class NewMessageActivity extends Activity implements OnClickListener
{
	private static final String TAG = "NewMessageUI";

	// contact接触，联系 _projection 投射 /推测
	private final String[] CONTACT_PROJECTION =
	{ 
			"_id",
			"display_name", 
			"datal" };

	private final int NAME_COLUMN_INDEX = 1;
	private final int ADDRESS_COLUMN_INDEX = 2;

	private AutoCompleteTextView actvNumber;

	private EditText etContent;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.new_message);

		initView();

	}
	

	private void initView()
	{
		actvNumber = (AutoCompleteTextView) findViewById(R.id.actv_new_message_number);
		etContent = (EditText) findViewById(R.id.et_new_message_content);

		findViewById(R.id.ib_new_message_select_contact).setOnClickListener(
				this);
		findViewById(R.id.btn_new_message_send).setOnClickListener(this);

		ContactsAdapter mAdapter = new ContactsAdapter(this, null);
		actvNumber.setAdapter(mAdapter);

		mAdapter.setFilterQueryProvider(new FilterQueryProvider()
		{
			/**
			 * 当自动提示文本框开始过滤查询时回调
			 * 
			 * @param constraint
			 *            自动提示文本框输入的内容
			 */
			@Override
			public Cursor runQuery(CharSequence constraint)
			{
				Log.i(TAG, "开始过滤查询: " + constraint);

				// 根据constraint字段查询联系人的数据, 返回cursor

				// 选择条件: where data1 like '%12%'
				String selection = "data1 like ?";
				String selectionArgs[] =
				{ "%" + constraint + "%" };
				Cursor cursor = getContentResolver().query(Phone.CONTENT_URI,
						CONTACT_PROJECTION, selection, selectionArgs, null);
				return cursor;
			}
		});
	}

	class ContactsAdapter extends CursorAdapter
	{

		public ContactsAdapter(Context context, Cursor c)
		{
			super(context, c);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return LayoutInflater.from(context).inflate(R.layout.contact_item,
					null);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			String name = cursor.getString(NAME_COLUMN_INDEX);
			String address = cursor.getString(ADDRESS_COLUMN_INDEX);

			TextView tvName = (TextView) view
					.findViewById(R.id.tv_contact_item_name);
			TextView tvAddress = (TextView) view
					.findViewById(R.id.tv_contact_item_address);

			tvName.setText(name);
			tvAddress.setText(address);
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		// 选择联系人 -----调用系统存在的
		case R.id.ib_new_message_select_contact:
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setData(Contacts.CONTENT_URI);
			startActivityForResult(intent, 100);
			break;
		case R.id.btn_new_message_send:
			String address = actvNumber.getText().toString();
			String content = etContent.getText().toString();

			if (TextUtils.isEmpty(address))
			{
				Toast.makeText(this, "请输入手机号码", Toast.LENGTH_SHORT).show();
				break;
			}
			if (TextUtils.isEmpty(content))
			{
				Toast.makeText(this, "请输入短信内容", Toast.LENGTH_SHORT).show();
				break;
			}
			// 发送短信
			Utils.sendMessage(this, address, content);
			finish();
			break;
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == 100 && resultCode == Activity.RESULT_OK)
		{
			// 跳转到系统联系人页面后，点击返回的数据是一个uri值
			Uri uri = data.getData();
			Log.i(TAG, "onActivityResult:" + uri);
			// 得到的是一个联系人的姓名
			// Cursor cursor =
			// getContentResolver().query(uri,null,null,null,null);
			// Utils.printCursor(cursor);
			// content 内容、目录 、容量 ------------Resolver 解析服务器、解决[答]问题者
			int contactID = Utils.getContactID(getContentResolver(), uri);
			if (contactID != -1)
			{
				// 当前联系人有号码
				String contactAddress = Utils.getContactAddress(
						getContentResolver(), contactID);
				actvNumber.setText(contactAddress); // 取到号码后传到文本框上
				etContent.requestFocus(); // 当号码填充文本框，需把光标移到内容输入框中
			} else
			{
				Toast.makeText(this, "当前联系人尚未添加手机号码", Toast.LENGTH_SHORT)
						.show();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

}
