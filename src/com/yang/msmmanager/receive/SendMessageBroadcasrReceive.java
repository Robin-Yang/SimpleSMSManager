package com.yang.msmmanager.receive;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class SendMessageBroadcasrReceive extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{

		int resultCode = getResultCode();
		if (resultCode == Activity.RESULT_OK)
		{
			Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show();
		} else
		{
			Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show();

		}

	}

}
