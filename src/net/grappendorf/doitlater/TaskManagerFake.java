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
import android.content.Context;
import android.os.Handler;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class TaskManagerFake implements TaskManager
{
	private List<Task> tasks = new LinkedList<Task>();

	private Map<String, Task> tasksById = new HashMap<String, Task>();

	private List<TaskList> taskLists = new LinkedList<TaskList>();

	private Map<String, TaskList> taskListsById = new HashMap<String, TaskList>();

	public TaskManagerFake(@SuppressWarnings("unused") Context context)
	{
		TaskList taskList = new TaskList();
		taskList.setTitle("Default");
		taskList.setId("@default");
		taskLists.add(taskList);
		taskList = new TaskList();
		taskList.setTitle("Very Important");
		taskList.setId("1");
		taskLists.add(taskList);

		Task task = new Task();
		task.setId("1");
		task.setTitle("Invent a new application");
		task.setDue(new DateTime(5000000));
		task.setNotes(("A way to manage all the things\nyou want or need to do."));
		tasks.add(task);
		task = new Task();
		task.setId("2");
		task.setTitle("Design the architecture");
		task.setDue(new DateTime(5000000));
		task.setCompleted(new DateTime(10000000));
		tasks.add(task);
		task = new Task();
		task.setId("3");
		task.setTitle("Implement the software");
		tasks.add(task);
		task = new Task();
		task.setId("4");
		task.setTitle("Become famous");
		task.setCompleted(new DateTime(50000000));
		tasks.add(task);

		for (Task t : tasks)
		{
			tasksById.put(t.getId(), t);
		}
	}

	@Override
	public void onRequestAuthenticateResult(Activity activity, int requestCode)
	{
	}

	@Override
	public String getDefaultTaskListId()
	{
		return "@default";
	}

	@Override
	public void listTaskLists(Activity activity, Handler callback)
	{
		callback.sendMessage(callback.obtainMessage(0, taskLists));
	}

	@Override
	public void getTaskList(String taskListId, Activity activity, Handler callback)
	{
		callback.sendMessage(callback.obtainMessage(0, taskListsById.get(taskListId)));
	}

	@Override
	public void listTasks(String taskListId, String[] fields, FilterOptions filter, Activity activity, Handler callback)
	{
		callback.sendMessage(callback.obtainMessage(0, tasks));
	}

	@Override
	public void getTask(String taskListId, String taskId, Activity activity, Handler callback)
	{
		callback.sendMessage(callback.obtainMessage(0, tasksById.get(taskId)));
	}

	@Override
	public void updateTask(String taskListId, Task task, Activity activity, Handler callback)
	{
	}

	@Override
	public void deleteTask(String taskListId, String taskId, Activity activity, Handler callback)
	{
	}

	@Override
	public void completeTask(String taskListId, String taskId, Activity activity, Handler callback)
	{
	}

	@Override
	public void createTask(String taskListId, Task task, String previousTaskId, Activity activity, Handler callback)
	{
	}

	@Override
	public void moveTask(String taskListId, Task task, String previousTaskId, Activity activity, Handler callback)
	{
	}

	@Override
	public void chooseAccount(Activity activity)
	{
	}

	@Override
	public void debugDump()
	{
	}
}
