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

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.google.api.services.tasks.model.Task;

import java.util.List;

public class TaskListAdapter extends ArrayAdapter<Task>
{
	private int itemViewResourceId;

	public TaskListAdapter(Context context, List<Task> tasks, int itemViewResourceId)
	{
		super(context, itemViewResourceId, R.id.title, tasks);
		this.itemViewResourceId = itemViewResourceId;
	}

	private static class ViewHolder
	{
		public TextView title;
		public TextView dueOrCompleted;

		private ViewHolder(View view)
		{
			title = (TextView) view.findViewById(R.id.title);
			dueOrCompleted = (TextView) view.findViewById(R.id.description);
		}

		public static ViewHolder from(View view)
		{
			ViewHolder viewHolder = (ViewHolder) view.getTag();
			if (viewHolder == null)
			{
				viewHolder = new ViewHolder(view);
				view.setTag(viewHolder);
			}
			return viewHolder;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = convertView != null ?
				convertView :
				LayoutInflater.from(getContext()).inflate(itemViewResourceId, parent, false);
		ViewHolder viewHolder = ViewHolder.from(row);
		Task task = getItem(position);
		if (task == null)
		{
			return row;
		}
		createTitle(viewHolder, task);
		createDueOrCompleted(viewHolder, task);
		return row;
	}

	private void createTitle(ViewHolder row, Task task)
	{
		row.title.setText(task.getTitle());
		row.title.setPaintFlags(task.getCompleted() != null ?
				row.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG :
				row.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
		row.title.setTextColor(task.getCompleted() != null ?
				getContext().getResources().getColor(android.R.color.darker_gray) :
				getContext().getResources().getColor(android.R.color.white));
	}

	private void createDueOrCompleted(ViewHolder row, Task task)
	{
		StringBuilder dueOrCompletedText = new StringBuilder();
		if (task.getCompleted() != null)
		{
			dueOrCompletedText.append(
					String.format(getContext().getString(R.string.completed_on),
							DoItLaterApplication.formatDate(task.getCompleted())));
		} else
		{
			if (task.getDue() != null)
			{
				dueOrCompletedText.append(
						String.format(getContext().getString(R.string.due_on),
								DoItLaterApplication.formatDate(task.getDue())));
			} else
			{
				dueOrCompletedText.append(getContext().getString(R.string.open_ended));
			}
		}
		row.dueOrCompleted.setText(dueOrCompletedText);
	}
}
