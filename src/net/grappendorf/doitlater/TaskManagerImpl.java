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

import android.accounts.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.GoogleKeyInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskManagerImpl implements TaskManager
{
	private static final String PREF_NAME = "taskManager";

	private static final String AUTH_TOKEN_TYPE = "Manage your tasks";

	private static final String PREF_AUTH_TOKEN = "authToken";

	private static final String PREF_ACCOUNT_NAME = "accountName";

	private GoogleCredential credential;

	private Tasks tasksService;

	private GoogleAccountManager accountManager;

	private Map<Activity, AsyncTask<Void, Void, ?>> asyncTaskByActivity;

	private SharedPreferences preferences;

	private String accountName;

	private boolean received401;

	public TaskManagerImpl(Context context)
	{
		SharedPreferences globalPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		String apiKey = globalPrefs.getString("api_key", "");
		this.credential = new GoogleCredential();
		this.accountManager = new GoogleAccountManager(context);
		HttpTransport transport = AndroidHttp.newCompatibleTransport();
		JsonFactory jsonFactory = new GsonFactory();
		this.tasksService = new Tasks.Builder(transport, jsonFactory, credential)
				.setApplicationName(DoItLaterApplication.NAME)
				.setJsonHttpRequestInitializer(new GoogleKeyInitializer(apiKey))
				.build();
		asyncTaskByActivity = new HashMap<Activity, AsyncTask<Void, Void, ?>>();
		preferences = context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
		credential.setAccessToken(preferences.getString(PREF_AUTH_TOKEN, null));
		accountName = preferences.getString(PREF_ACCOUNT_NAME, null);
		received401 = false;
	}

	private void executeAsyncTaskWhenAuthenticated(final Activity activity)
	{
		Account account = accountManager.getAccountByName(accountName);
		if (account == null)
		{
			chooseAccount(activity);
			return;
		}
		if (credential.getAccessToken() != null)
		{
			onAuthToken(activity);
			return;
		}
		accountManager.getAccountManager()
				.getAuthToken(account, AUTH_TOKEN_TYPE, true, new AccountManagerCallback<Bundle>()
				{
					public void run(AccountManagerFuture<Bundle> future)
					{
						try
						{
							Bundle bundle = future.getResult();
							if (bundle.containsKey(AccountManager.KEY_INTENT))
							{
								Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
								intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
								activity.startActivityForResult(intent, GlobalActivityCodes.TASK_MANAGER_REQUEST_AUTHENTICATE);
							}
							else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN))
							{
								setAuthToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
								onAuthToken(activity);
							}
						}
						catch (Exception e)
						{
							Log.e(DoItLaterApplication.LOG_TAG, e.getMessage(), e);
							onRequestAborted(activity);
						}
					}
				}, null);
	}

	@Override
	public void chooseAccount(final Activity activity)
	{
		accountManager.getAccountManager().getAuthTokenByFeatures(GoogleAccountManager.ACCOUNT_TYPE,
				AUTH_TOKEN_TYPE, null, activity, null, null,
				new AccountManagerCallback<Bundle>()
				{
					public void run(AccountManagerFuture<Bundle> future)
					{
						try
						{
							Bundle bundle = future.getResult();
							setAccountName(bundle.getString(AccountManager.KEY_ACCOUNT_NAME));
							setAuthToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
							onAuthToken(activity);
						}
						catch (OperationCanceledException e)
						{
							onRequestAborted(activity);
						}
						catch (AuthenticatorException e)
						{
							Log.e(DoItLaterApplication.NAME, e.getMessage(), e);
							onRequestAborted(activity);
						}
						catch (IOException e)
						{
							Log.e(DoItLaterApplication.NAME, e.getMessage(), e);
							onRequestAborted(activity);
						}
					}
				},
				null);
	}

	private void setAccountName(String accountName)
	{
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREF_ACCOUNT_NAME, accountName);
		editor.commit();
		this.accountName = accountName;
	}

	private void setAuthToken(String authToken)
	{
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREF_AUTH_TOKEN, authToken);
		editor.commit();
		credential.setAccessToken(authToken);
	}

	private void onRequestCompleted()
	{
		received401 = false;
	}

	private void onRequestAborted(Activity activity)
	{
		asyncTaskByActivity.remove(activity);
	}

	private void handleApiException(Activity activity, Handler callback, IOException e)
	{
		if (e instanceof GoogleJsonResponseException)
		{
			GoogleJsonResponseException exception = (GoogleJsonResponseException) e;
			if (exception.getStatusCode() == 401 && !received401)
			{
				received401 = true;
				accountManager.invalidateAuthToken(credential.getAccessToken());
				credential.setAccessToken(null);
				SharedPreferences.Editor editor2 = preferences.edit();
				editor2.remove(PREF_AUTH_TOKEN);
				editor2.commit();
				executeAsyncTaskWhenAuthenticated(activity);
				return;
			}
		}
		Log.e(DoItLaterApplication.LOG_TAG, e.getMessage(), e);
		onRequestAborted(activity);
		callback.sendMessage(callback.obtainMessage(0, null));
	}

	void onAuthToken(Activity activity)
	{
		asyncTaskByActivity.get(activity).execute((Void) null);
	}

	@Override
	public void onRequestAuthenticateResult(Activity activity, int requestCode)
	{
		if (requestCode == Activity.RESULT_OK)
		{
			executeAsyncTaskWhenAuthenticated(activity);
		}
		else
		{
			chooseAccount(activity);
		}
	}

	@Override
	public void listTasks(final String taskList, final String[] fields, final Activity activity, final Handler callback)
	{
		AsyncTask<Void, Void, Void> asyncTask = new AsyncTaskWithProgressDialog<Void, Void, Void>(activity, R.string.loading)
		{
			@Override
			protected Void doInBackground(Void... voids)
			{
				try
				{
					StringBuilder fieldsSpec = new StringBuilder("items(id");
					for (String field : fields)
					{
						fieldsSpec.append(",");
						fieldsSpec.append(field);
					}
					fieldsSpec.append(")");
					List<Task> tasks = tasksService.tasks().list(taskList)
							.setFields(fieldsSpec.toString())
							.setShowDeleted(true)
							.execute().getItems();
					callback.sendMessage(callback.obtainMessage(0, tasks));
					asyncTaskByActivity.remove(activity);
					onRequestCompleted();
				}
				catch (IOException e)
				{
					handleApiException(activity, callback, e);
				}
				return null;
			}
		};
		asyncTaskByActivity.put(activity, asyncTask);
		executeAsyncTaskWhenAuthenticated(activity);
	}

	@Override
	public void getTask(final String taskList, final String taskId, final Activity activity, final Handler callback)
	{
		AsyncTask<Void, Void, Void> asyncTask = new AsyncTaskWithProgressDialog<Void, Void, Void>(activity, R.string.loading)
		{
			@Override
			protected Void doInBackground(Void... voids)
			{
				try
				{
					Task task = tasksService.tasks().get(taskList, taskId).execute();
					callback.sendMessage(callback.obtainMessage(0, task));
					asyncTaskByActivity.remove(activity);
					onRequestCompleted();
				}
				catch (IOException e)
				{
					handleApiException(activity, callback, e);
				}
				return null;
			}
		};
		asyncTaskByActivity.put(activity, asyncTask);
		executeAsyncTaskWhenAuthenticated(activity);
	}

	@Override
	public void updateTask(final String taskList, final Task task, final Activity activity, final Handler callback)
	{
		AsyncTask<Void, Void, Void> asyncTask = new AsyncTaskWithProgressDialog<Void, Void, Void>(activity, R.string.saving)
		{
			@Override
			protected Void doInBackground(Void... voids)
			{
				try
				{
					Task result = tasksService.tasks().update(taskList, task.getId(), task).execute();
					callback.sendMessage(callback.obtainMessage(0, result));
					asyncTaskByActivity.remove(activity);
					onRequestCompleted();
				}
				catch (IOException e)
				{
					handleApiException(activity, callback, e);
				}
				return null;
			}
		};
		asyncTaskByActivity.put(activity, asyncTask);
		executeAsyncTaskWhenAuthenticated(activity);
	}

	@Override
	public void deleteTask(final String taskList, final String taskId, final Activity activity, final Handler callback)
	{
		AsyncTask<Void, Void, Void> asyncTask = new AsyncTaskWithProgressDialog<Void, Void, Void>(activity, R.string.deleting)
		{
			@Override
			protected Void doInBackground(Void... voids)
			{
				try
				{
					tasksService.tasks().delete(taskList, taskId).execute();
					callback.sendMessage(callback.obtainMessage(0, taskId));
					asyncTaskByActivity.remove(activity);
					onRequestCompleted();
				}
				catch (IOException e)
				{
					handleApiException(activity, callback, e);
				}
				return null;
			}
		};
		asyncTaskByActivity.put(activity, asyncTask);
		executeAsyncTaskWhenAuthenticated(activity);
	}

	@Override
	public void debugDump()
	{
		Log.d(DoItLaterApplication.LOG_TAG, "TaskManager debug dump:");
		Log.d(DoItLaterApplication.LOG_TAG, "\tAsync tasks by activity:");
		if (asyncTaskByActivity.size() > 0)
		{
			for (Map.Entry<Activity, AsyncTask<Void, Void, ?>> entry : asyncTaskByActivity.entrySet())
			{
				Log.d(DoItLaterApplication.LOG_TAG, "\t\t" + entry.getKey().getLocalClassName() + " => " +
						entry.getValue().toString());
			}
		}
		else
		{
			Log.d(DoItLaterApplication.LOG_TAG, "\t\tNone");
		}
	}
}
