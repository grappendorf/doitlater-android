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

import android.app.Application;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class DoItLaterApplication extends android.app.Application
{
	public static final String NAME = "Do it later!";

	public static final String LOG_TAG = "DoItLater";

	private static Application application;

	private static DateFormat dateFormat;

	private TaskManager taskManager;

	public TaskManager getTaskManager()
	{
		return taskManager;
	}

	@Override
	public void onCreate()
	{
		application = this;
		dateFormat = android.text.format.DateFormat.getDateFormat(this);
		taskManager = new TaskManagerImpl(getApplicationContext());
	}

	public static String formatDate(Date date)
	{
		try
		{
			return dateFormat.format(date);
		}
		catch (IllegalArgumentException x)
		{
			return application.getString(R.string.unknown_date);
		}
	}

	public static String formatDate(com.google.api.client.util.DateTime dateTime)
	{
		return formatDate(new Date(dateTime.getValue()));
	}

	public static Date parseDate(String dateString) throws ParseException
	{
		return dateFormat.parse(dateString);
	}
}