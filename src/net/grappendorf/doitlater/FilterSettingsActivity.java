/*
 * This file is part of the Do it later! Android application.
 *
 * Copyright 2011-2012 Dirk Grappendorf, www.grappendorf.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.grappendorf.doitlater;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.api.services.tasks.model.TaskList;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

public class FilterSettingsActivity extends Activity
{
	private SharedPreferences preferences;

	private CheckBox showCompleted;

	private Spinner taskList;

	private Activity activity;

	private List<TaskList> taskLists;

	public FilterSettingsActivity()
	{
		this.activity = this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filter_settings);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		showCompleted = (CheckBox) findViewById(R.id.show_completed);
		showCompleted.setChecked(preferences.getBoolean("showCompleted", false));
		taskList = (Spinner) findViewById(R.id.taskList);
		loadTaskLists();
	}

	private void loadTaskLists()
	{
		((DoItLaterApplication) getApplication()).getTaskManager().listTaskLists(this, new Handler()
		{
			@Override
			@SuppressWarnings("unchecked")
			public void handleMessage(Message msg)
			{
				if (msg.obj != null)
				{
					taskLists = (List<TaskList>) msg.obj;
					List<String> taskListTitles = Lists.transform(taskLists, new Function<TaskList, String>()
					{
						@Override
						public String apply(@Nullable TaskList taskList)
						{
							return taskList != null ? taskList.getTitle() : "";
						}
					});
					taskList.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, taskListTitles));
					String taskListId = preferences.getString("taskListId", ((DoItLaterApplication) getApplication()).getTaskManager().getDefaultTaskListId());
					int pos = 0;
					for (TaskList tl : taskLists)
					{
						if (tl.getId().equals(taskListId))
						{
							taskList.setSelection(pos);
							break;
						}
						++pos;
					}
				}
				else
				{
					Toast.makeText(activity.getApplicationContext(), R.string.load_tasklists_error, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	public void onApply(@SuppressWarnings("unused") View source)
	{
		SharedPreferences.Editor prefsEdit = preferences.edit();
		prefsEdit.putBoolean("showCompleted", showCompleted.isChecked());
		prefsEdit.putString("taskListId", taskLists.get(taskList.getSelectedItemPosition()).getId());
		prefsEdit.commit();
		setResult(RESULT_OK);
		finish();
	}

	public void onCancel(@SuppressWarnings("unused") View source)
	{
		setResult(RESULT_CANCELED);
		finish();
	}
}