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

public interface GlobalActivityCodes
{
	public static final int TASK_MANAGER_REQUEST_AUTHENTICATE = 0;

	public static final int REQUEST_FIRST_USER = 1;

	public static final int RESULT_SAVED = Activity.RESULT_FIRST_USER + 0;

	public static final int RESULT_DELETED = Activity.RESULT_FIRST_USER + 1;

	public static final int RESULT_FIRST_USER = Activity.RESULT_FIRST_USER + 2;
}