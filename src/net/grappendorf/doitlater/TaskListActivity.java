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
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.google.api.services.tasks.model.Task;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.List;

public class TaskListActivity extends ListActivity
{
	private static final int REQUEST_TASK_EDIT = GlobalActivityCodes.REQUEST_FIRST_USER;

	private static final int REQUEST_TASK_CREATE = GlobalActivityCodes.REQUEST_FIRST_USER + 1;

	private Activity activity;

	private List<Task> tasks;

	private DragSortListView listView;

	private boolean dragEnabled;

	public TaskListActivity()
	{
		activity = this;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		dragEnabled = false;
		setContentView(R.layout.task_list);
		registerForContextMenu(getListView());
		listView = (DragSortListView) getListView();
		listView.setDropListener(onDragDrop);
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

			case R.id.about:
				Intent aboutActivity = new Intent(getBaseContext(), AboutActivity.class);
				startActivity(aboutActivity);
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
				onEditTask(((Task) getListAdapter().getItem(info.position)).getId());
				break;

			case R.id.complete:
				onCompleteTask(((Task) getListAdapter().getItem(info.position)).getId(),
						((Task) getListAdapter().getItem(info.position)).getTitle());
				break;

			case R.id.delete:
				onDeleteTask(((Task) getListAdapter().getItem(info.position)).getId(),
						((Task) getListAdapter().getItem(info.position)).getTitle());
				break;
		}
		return super.onContextItemSelected(item);
	}

	private DragSortListView.DropListener onDragDrop =
			new DragSortListView.DropListener()
			{
				@Override
				public void drop(final int from, final int to)
				{
					if (from == to)
					{
						return;
					}

					final Task task = tasks.get(from);
					int previousPos = (to > from) ? to : to - 1;
					String previousTaksId = (previousPos >= 0) ? tasks.get(previousPos).getId() : null;

					final TaskListAdapter listAdapter = (TaskListAdapter) getListAdapter();
					listAdapter.remove(task);
					listAdapter.insert(task, to);

					((DoItLaterApplication) getApplication()).getTaskManager().moveTask("@default", task,
							previousTaksId, activity, new Handler()
					{
						@Override
						@SuppressWarnings("unchecked")
						public void handleMessage(Message msg)
						{
							if (msg.obj == null)
							{
								listAdapter.remove(task);
								listAdapter.insert(task, from);
								Toast.makeText(activity.getApplicationContext(), R.string.move_task_error, Toast.LENGTH_LONG).show();
							}
						}
					});
				}
			};

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		onEditTask(((Task) getListAdapter().getItem(position)).getId());
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
				if (resultCode == RESULT_OK)
				{
					updateTaskItem(data.getStringExtra("taskId"));
				}
				break;

			case REQUEST_TASK_CREATE:
				if (resultCode == RESULT_OK)
				{
					ceateTaskItem(data.getStringExtra("taskId"), data.getIntExtra("insertedAt", 0));
				}
				break;
		}
	}

	public void onReload(@SuppressWarnings("unused") View source)
	{
		loadTaskItems();
	}

	public void onFilter(@SuppressWarnings("unused") View source)
	{
	}

	public void onSort(@SuppressWarnings("unused") View source)
	{
	}

	public void onDrag(@SuppressWarnings("unused") View source)
	{
		dragEnabled = !dragEnabled;
		showTasksItems();
	}

	public void onCreateTask(@SuppressWarnings("unused") View source)
	{
		Intent intent = new Intent(this, TaskEditorActivity.class);
		if (tasks.size() > 0)
		{
			intent.putExtra("lastTaskId", tasks.get(tasks.size() - 1).getId());
		}
		startActivityForResult(intent, REQUEST_TASK_CREATE);
	}

	private void onEditTask(String taskId)
	{
		Intent intent = new Intent(this, TaskEditorActivity.class);
		intent.putExtra("taskId", taskId);
		startActivityForResult(intent, REQUEST_TASK_EDIT);
	}

	private void onDeleteTask(final String taskId, String taskTitle)
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
									Toast.makeText(activity.getApplicationContext(), R.string.delete_task_error, Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				})
				.show();
	}

	private void onCompleteTask(final String taskId, String taskTitle)
	{
		new AlertDialog.Builder(this)
				.setIcon(R.drawable.task)
				.setTitle(R.string.complete_task)
				.setMessage(getResources().getString(R.string.complete_task_confirm, taskTitle))
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.complete, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						((DoItLaterApplication) getApplication()).getTaskManager().completeTask("@default", taskId, activity, new Handler()
						{
							@Override
							@SuppressWarnings("unchecked")
							public void handleMessage(Message msg)
							{
								if (msg.obj != null)
								{
									Task task = (Task) msg.obj;
									if (task != null)
									{
										completeTaskItem(task);
									}
								}
								else
								{
									Toast.makeText(activity.getApplicationContext(), R.string.complete_task_error, Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				})
				.show();
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
				tasks = (List<Task>) msg.obj;
				showTasksItems();
			}
		});
	}

	private void showTasksItems()
	{
		if (tasks != null)
		{
			int topIndex = listView.getFirstVisiblePosition();
			View topView = listView.getChildAt(0);
			int topPos = (topView == null) ? 0 : topView.getTop();
			setListAdapter(new TaskListAdapter(activity, tasks,
					dragEnabled ? R.layout.task_list_item_dragable : R.layout.task_list_item));
			listView.setSelectionFromTop(topIndex, topPos);
		}
		else
		{
			showErrorMessageInList();
		}
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

	private void ceateTaskItem(String taskId, final int insertedAt)
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
					if (insertedAt == TaskEditorActivity.INSERT_TOP)
					{
						tasks.add(0, task);
						((ArrayAdapter<Task>) getListAdapter()).notifyDataSetChanged();
						listView.setSelection(0);
					}
					else
					{
						tasks.add(task);
						((ArrayAdapter<Task>) getListAdapter()).notifyDataSetChanged();
						listView.setSelection(tasks.size() - 1);
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

	@SuppressWarnings("unchecked")
	private void completeTaskItem(final Task task)
	{
		int taskIndex = Iterables.indexOf(tasks, new Predicate<Task>()
		{
			@Override
			public boolean apply(@Nullable Task aTask)
			{
				return aTask != null && aTask.getId().equals(task.getId());
			}
		});
		if (taskIndex >= 0)
		{
			tasks.set(taskIndex, task);
			((ArrayAdapter<Task>) getListAdapter()).notifyDataSetChanged();
		}
	}

	private void showErrorMessageInList()
	{
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
				new String[]{"Unable to load task list"}));
	}
}
