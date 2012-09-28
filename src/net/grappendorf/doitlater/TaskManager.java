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
	void listTasks(String taskList, String[] fields, FilterOptions filter, Activity activity, Handler callback);

	void getTask(String taskList, String taskId, Activity activity, Handler callback);

	void updateTask(String taskList, Task task, Activity activity, Handler callback);

	void deleteTask(String taskList, String taskId, Activity activity, Handler callback);

	void completeTask(String taskList, String taskId, Activity activity, Handler callback);

	void createTask(String taskList, Task task, String previousTaskId, Activity activity, Handler callback);

	void moveTask(String taskList, Task task, String previousTaskId, Activity activity, Handler callback);

	void chooseAccount(Activity activity);

	void onRequestAuthenticateResult(Activity activity, int requestCode);

	void debugDump();
}
