package com.zju.sawdetector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.zju.sawdetector.model.WorkflowItem;

import org.w3c.dom.Text;

public class WorkflowSetting extends AppCompatActivity {

	ListView workflowView;
	ArrayAdapter<WorkflowItem> workflow;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_workflow_setting);

		workflow = new ArrayAdapter<WorkflowItem>(this, R.layout.listitem_workflow) {
			@Override
			public View getView(int position, View view, ViewGroup parent) {
				WorkflowItem item = getItem(position);

				if (view == null) {
					view = LayoutInflater.from(getContext()).inflate(R.layout.listitem_workflow, parent, false);
				}

				TextView button = view.findViewById(R.id.workflow_label);
				button.setText(item.getLabel());

				WorkflowItem.Type type = item.getType();

				String input1 = type.getInput1();
				View inputView1 = view.findViewById(R.id.workflow_item1_input);
				if (input1 != null) {
					TextView unitView = inputView1.findViewById(R.id.workflow_item1_unit);
					unitView.setText(input1);
				} else {
					inputView1.setVisibility(View.GONE);
				}

				String input2 = type.getInput2();
				View inputView2 = view.findViewById(R.id.workflow_item2_input);
				if (input2 != null) {
					TextView unitView = inputView2.findViewById(R.id.workflow_item2_unit);
					unitView.setText(input2);
				} else {
					inputView2.setVisibility(View.GONE);
				}

				return view;
			}
		};
		workflowView = (ListView) findViewById(R.id.listViewWorkflow);
		workflowView.setAdapter(workflow);
	}

	public void onWorkflowButtonClick(View view) {
		Button button = (Button) view;
		WorkflowItem item = new WorkflowItem();
		item.setLabel(button.getText());
		item.setType((String) button.getTag());
		workflow.add(item);
	}
}
