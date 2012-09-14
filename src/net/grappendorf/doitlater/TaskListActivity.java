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
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.api.services.tasks.model.Task;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import java.util.List;

public class TaskListActivity extends ListActivity
{
	private static final int REQUEST_TASK_EDIT = GlobalActivityCodes.REQUEST_FIRST_USER;

	private Activity activity;

	private List<Task> tasks;

	public TaskListActivity()
	{
		activity = this;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_list);
		registerForContextMenu(getListView());
		loadTaskItems();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.task_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.switch_account:
				((DoItLaterApplication) getApplication()).getTaskManager().chooseAccount(this);
				return true;

			case R.id.debug_dump:
				((DoItLaterApplication) getApplication()).getTaskManager().debugDump();
				return true;

			case R.id.preferences:
				Intent preferencesActivity = new Intent(getBaseContext(), PreferenceEditor.class);
				startActivity(preferencesActivity);
				return true;

			case R.id.exit:
				finish();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		getMenuInflater().inflate(R.menu.task_list_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
			case R.id.edit:
				onEdit(((Task) getListAdapter().getItem(info.position)).getId());
				break;

			case R.id.complete:
				break;

			case R.id.delete:
				onDelete(((Task) getListAdapter().getItem(info.position)).getId(),
						((Task) getListAdapter().getItem(info.position)).getTitle());
				break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		onEdit(((Task) getListAdapter().getItem(position)).getId());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case GlobalActivityCodes.TASK_MANAGER_REQUEST_AUTHENTICATE:
				((DoItLaterApplication) getApplication()).getTaskManager().onRequestAuthenticateResult(this, requestCode);
				break;

			case REQUEST_TASK_EDIT:
				switch (resultCode)
				{
					case GlobalActivityCodes.RESULT_SAVED:
						updateTaskItem(data.getStringExtra("taskId"));
						break;

					case GlobalActivityCodes.RESULT_DELETED:
						deleteTaskItem(data.getStringExtra("taskId"));
						break;
				}
				break;
		}
	}

	public void onReload(@SuppressWarnings("unused") View source)
	{
		loadTaskItems();
	}

	public void onAdd(@SuppressWarnings("unused") View source)
	{
	}

	private void onDelete(final String taskId, String taskTitle)
	{
		new AlertDialog.Builder(this)
				.setIcon(R.drawable.task)
				.setTitle(R.string.delete_task)
				.setMessage(getResources().getString(R.string.delete_task_confirm, taskTitle))
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						((DoItLaterApplication) getApplication()).getTaskManager().deleteTask("@default", taskId, activity, new Handler()
						{
							@Override
							@SuppressWarnings("unchecked")
							public void handleMessage(Message msg)
							{
								if (msg.obj != null)
								{
									String taskId = (String) msg.obj;
									if (taskId != null)
									{
										deleteTaskItem(taskId);
									}
								}
								else
								{
									showErrorMessageInList();
								}
							}
						});
					}
				})
				.show();
	}

	private void onEdit(String taskId)
	{
		Intent intent = new Intent(this, TaskEditorActivity.class);
		intent.putExtra("taskId", taskId);
		startActivityForResult(intent, REQUEST_TASK_EDIT);
	}

	private void loadTaskItems()
	{
		((DoItLaterApplication) getApplication()).getTaskManager().listTasks("@default",
				new String[]{"title", "due", "completed"}, this, new Handler()
		{
			@Override
			@SuppressWarnings("unchecked")
			public void handleMessage(Message msg)
			{
				if (msg.obj != null)
				{
					tasks = (List<Task>) msg.obj;
					setListAdapter(new TaskListAdapter(activity, tasks));
				}
				else
				{
					showErrorMessageInList();
				}
			}
		});
	}

	private void updateTaskItem(final String taskId)
	{
		((DoItLaterApplication) getApplication()).getTaskManager().getTask("@default", taskId, this, new Handler()
		{
			@Override
			@SuppressWarnings("unchecked")
			public void handleMessage(Message msg)
			{
				if (msg.obj != null)
				{
					Task task = (Task) msg.obj;
					int taskIndex = Iterables.indexOf(tasks, new Predicate<Task>()
					{
						@Override
						public boolean apply(@Nullable Task task)
						{
							return task != null && task.getId().equals(taskId);
						}
					});
					if (taskIndex >= 0)
					{
						tasks.set(taskIndex, task);
						((ArrayAdapter<Task>) getListAdapter()).notifyDataSetChanged();
					}
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void deleteTaskItem(final String taskId)
	{
		int taskIndex = Iterables.indexOf(tasks, new Predicate<Task>()
		{
			@Override
			public boolean apply(@Nullable Task task)
			{
				return task != null && task.getId().equals(taskId);
			}
		});
		if (taskIndex >= 0)
		{
			tasks.remove(taskIndex);
			((ArrayAdapter<Task>) getListAdapter()).notifyDataSetChanged();
		}
	}

	private void showErrorMessageInList()
	{
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
				new String[]{"Unable to load task list"}));
	}
}
