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
import android.os.Handler;
import com.google.api.services.tasks.model.Task;

public interface TaskManager
{
	/**
	 * Fetch a list of all tasks in the specified task list. Only the task id plus the attributes
	 * specified in fields are retrieved.
	 * This method is asynchronous and the result is passed backed through the provided callback as the
	 * message object. In case of an error, the message object will be null.
	 *
	 * @param taskList The name of the task list to fetch
	 * @param showCompleted Completed tasks are only fetched if this is true
	 * @param activity The activity that calls the service method
	 * @param callback A handler to be called back with the retrieved task list
	 */
	void listTasks(String taskList, String[] fields, boolean showCompleted, Activity activity, Handler callback);

	/**
	 * Retrieve a specific task. This method is asynchronous and the result is passed backed
	 * through the provided callback as the message object. In case of an error, the message
	 * object will be null.
	 *
	 * @param taskList The name of the task list that contains the task
	 * @param taskId   The id of the task to fetch
	 * @param activity The activity that calls the service method
	 * @param callback A handler to be called back with the retrieved task
	 */
	void getTask(String taskList, String taskId, Activity activity, Handler callback);

	/**
	 * Update the specific task. This method is asynchronous and the updated task is passed back
	 * through the provided callback as the message object. In case of an error, the message
	 * object will be null.
	 *
	 * @param taskList The name of the task list that contains the task
	 * @param task     The task containing the updated attributes
	 * @param activity The activity that calls the service method
	 * @param callback A handler to be called back with the updated task
	 */
	void updateTask(String taskList, Task task, Activity activity, Handler callback);

	/**
	 * Delete the specified task. This method is asynchronous and the id of the deleted task is
	 * passed back through the provided callback as the message object. In case of an error, the
	 * message object will be null.
	 *
	 * @param taskList The name of the task list that contains the task
	 * @param taskId   The id of the task to delete
	 * @param activity The activity that calls the service method
	 * @param callback A handler to be called back with the id of the deleted task
	 */
	void deleteTask(String taskList, String taskId, Activity activity, Handler callback);

	/**
	 * Complete the specific task. This method is asynchronous and the completed task is passed
	 * back through the provided callback as the message object. In case of an error, the message
	 * object will be null.
	 *
	 * @param taskList The name of the task list that contains the task
	 * @param taskId   The id of the task to delete
	 * @param activity The activity that calls the service method
	 * @param callback A handler to be called back with the completed task
	 */
	void completeTask(String taskList, String taskId, Activity activity, Handler callback);

	/**
	 * Create a new task. This method is asynchronous and the created task is passed
	 * back through the provided callback as the message object. In case of an error, the message
	 * object will be null.
	 *
	 * @param taskList       The name of the task list that will contane task
	 * @param task           The task to create
	 * @param previousTaskId Id of the task, after which the new one should be created. If null,
	 *                       the new task is created at the top of the list
	 * @param activity       The activity that calls the service method
	 * @param callback       A handler to be called back with the completed task
	 */
	void createTask(String taskList, Task task, String previousTaskId, Activity activity, Handler callback);

	void moveTask(String taskList, Task task, String previousTaskId, Activity activity, Handler callback);

	void chooseAccount(Activity activity);

	void onRequestAuthenticateResult(Activity activity, int requestCode);

	void debugDump();
}
