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
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import org.apache.http.impl.entity.EntityDeserializer;

import java.text.ParseException;
import java.util.Calendar;

public class TaskEditorActivity extends Activity
{
	private Task task;

	private CheckBox completed;

	private EditText title;

	private EditText dueDate;

	private EditText notes;

	private Spinner insertPosition;

	public static final int INSERT_TOP = 0;

	public static final int INSERT_BOTTOM = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.task_editor);
		setResult(RESULT_CANCELED);
		String taskId = getIntent().getStringExtra("taskId");
		completed = (CheckBox) findViewById(R.id.completed);
		title = (EditText) findViewById(R.id.title);
		dueDate = (EditText) findViewById(R.id.due_date);
		notes = (EditText) findViewById(R.id.notes);
		insertPosition = (Spinner) findViewById(R.id.insert_position);
		insertPosition.setVisibility(taskId == null ? View.VISIBLE : View.GONE);
		if (taskId != null)
		{
			((DoItLaterApplication) getApplication()).getTaskManager().getTask("@default", taskId, this, new Handler()
			{
				@Override
				public void handleMessage(Message msg)
				{
					task = (Task) msg.obj;
					updateCompletedView();
					updateTitleView();
					updateDueDateView();
					updateNotesView();
				}
			});
		}
		else
		{
			((Button) findViewById(R.id.save)).setText(R.string.create);
			task = new Task();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.task_editor_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.delete:
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == GlobalActivityCodes.TASK_MANAGER_REQUEST_AUTHENTICATE)
		{
			((DoItLaterApplication) getApplication()).getTaskManager().onRequestAuthenticateResult(this, requestCode);
		}
	}

	public void onDueDatePopup(@SuppressWarnings("unused") View source)
	{
		final Calendar cal = Calendar.getInstance();
		if (task.getDue() != null)
		{
			cal.setTimeInMillis(task.getDue().getValue());
		}
		new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener()
		{
			@Override
			public void onDateSet(DatePicker datePicker, int year, int month, int day)
			{
				cal.set(Calendar.YEAR, year);
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH, day);
				dueDate.setText(DoItLaterApplication.formatDate(cal.getTime()));
			}
		}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
	}

	public void onNotesClicked(@SuppressWarnings("unused") View source)
	{
	}

	public void onSave(@SuppressWarnings("unused") View source)
	{
		try
		{
			updateTaskTitle();
			updateTaskCompleted();
			updateTaskDue();
			updateTaskNotes();
			if (task.getId() != null)
			{
				updateTask();
			}
			else
			{
				createTask();
			}
		} catch (ValidationException e)
		{
			Toast.makeText(this, e.getErrorResourceId(), Toast.LENGTH_LONG).show();
		}
	}

	private void updateTask()
	{
		((DoItLaterApplication) getApplication()).getTaskManager().updateTask("@default", task, this, new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				if (msg.obj != null)
				{
					Intent intent = new Intent();
					intent.putExtra("taskId", task.getId());
					setResult(RESULT_OK, intent);
					finish();
				}
				else
				{
					Toast.makeText(getApplicationContext(), R.string.save_error, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	private void createTask()
	{
		final int insertAt = insertPosition.getSelectedItemPosition();
		String previousTaskId = insertAt == INSERT_TOP ? null : getIntent().getStringExtra("lastTaskId");
		((DoItLaterApplication) getApplication()).getTaskManager().createTask("@default", task, previousTaskId, this, new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				if (msg.obj != null)
				{
					task = (Task) msg.obj;
					Intent intent = new Intent();
					intent.putExtra("taskId", task.getId());
					intent.putExtra("insertedAt", insertAt);
					setResult(RESULT_OK, intent);
					finish();
				}
				else
				{
					Toast.makeText(getApplicationContext(), R.string.save_error, Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	public void onCancel(@SuppressWarnings("unused") View source)
	{
		setResult(RESULT_CANCELED);
		finish();
	}

	public void onCompletedClicked(@SuppressWarnings("unused") View source)
	{
		updateTitleView();
	}

	private void updateCompletedView()
	{
		completed.setChecked(task.getCompleted() != null);
	}

	private void updateTaskCompleted() throws ValidationException
	{
		if (task.getCompleted() == null && completed.isChecked())
		{
			task.setCompleted(new DateTime(System.currentTimeMillis(), 0));
			task.setStatus("completed");
		}
		else
			if (!completed.isChecked())
			{
				task.setCompleted(null);
				task.setStatus("needsAction");
			}
	}

	private void updateTitleView()
	{
		title.setText(task.getTitle());
		title.setPaintFlags(completed.isChecked() ?
				title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG :
				title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
	}

	private void updateTaskTitle() throws ValidationException
	{
		task.setTitle(title.getText().toString());
	}

	private void updateDueDateView()
	{
		dueDate.setText(task.getDue() != null ?
				DoItLaterApplication.formatDate(task.getDue()) : "");
	}

	private boolean isDueDateSet()
	{
		return !dueDate.getText().toString().trim().isEmpty() &&
				!dueDate.getText().toString().equals(getResources().getString(R.string.due_date));
	}

	private void updateTaskDue() throws ValidationException
	{
		if (isDueDateSet())
		{
			try
			{
				task.setDue(new DateTime(DoItLaterApplication.parseDate(dueDate.getText().toString().trim()).getTime(), 0));
			} catch (ParseException e)
			{
				throw new ValidationException(R.string.error_invalud_due_date);
			}
		}
		else
		{
			task.setDue(null);
		}
	}

	private void updateNotesView()
	{
		notes.setText(task.getNotes() != null ? task.getNotes() : "");
	}

	private void updateTaskNotes() throws ValidationException
	{
		task.setNotes(notes.getText().toString());
	}
}
