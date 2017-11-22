package com.zju.sawdetector;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zju.sawdetector.model.WorkflowItem;



public class WorkflowSetting extends AppCompatActivity {

	ListView workflowView;
	ArrayAdapter<WorkflowItem> workflow;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_workflow_setting);


		workflow = new ArrayAdapter<WorkflowItem>(this, R.layout.listitem_workflow) {
			@RequiresApi(api = Build.VERSION_CODES.O)
			@NonNull
			@Override
			public View getView(int position, View view, @NonNull ViewGroup parent) {

				final WorkflowItem item = getItem(position);
				if (view == null) {
					view = LayoutInflater.from(getContext()).inflate(R.layout.listitem_workflow, parent, false);
				}

				final TextView button = view.findViewById(R.id.workflow_label);
				assert item != null;
				button.setText(item.getLabel());

				WorkflowItem.Type type = item.getType();

				final EditText editValue1 = view.findViewById(R.id.workflow_item1_value);
				if (item.getValue1() !=0 && editValue1.getText().equals(null))
				{
					editValue1.setText(item.getValue1());
				}


				editValue1.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {

					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {

					}

					@Override
					public void afterTextChanged(Editable s) {

						if (item.getLabel().equals(button.getText()))
						{
							item.setValue1(Integer.parseInt(s.toString()));
						}
					}

				});

				if(item.getValue1()!=0)
				{
					editValue1.setText(String.valueOf(item.getValue1()));
				}

				final EditText editValue2 = view.findViewById(R.id.workflow_item2_value);
				editValue2.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {

					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {

					}

					@Override
					public void afterTextChanged(Editable s) {

						if (item.getLabel().equals(button.getText()))
						{
							item.setValue2(Integer.parseInt(s.toString()));
						}
					}
				});

				if(item.getValue2()!=0)
				{
					editValue2.setText(String.valueOf(item.getValue2()));
				}


				String input1 = type.getInput1();
				View inputView1 = view.findViewById(R.id.workflow_item1_input);
				if (input1 != null) {
					TextView unitView = inputView1.findViewById(R.id.workflow_item1_unit);
					unitView.setText(input1);
					//editValue1.setText(item.getValue1());
					inputView1.setVisibility(View.VISIBLE);
				} else {
					inputView1.setVisibility(View.INVISIBLE);
				}


				String input2 = type.getInput2();
				View inputView2 = view.findViewById(R.id.workflow_item2_input);
				if (input2 != null) {
					TextView unitView = inputView2.findViewById(R.id.workflow_item2_unit);
					unitView.setText(input2);
					//editValue2.setText(item.getValue2());
					inputView2.setVisibility(View.VISIBLE);
				} else {
					inputView2.setVisibility(View.INVISIBLE);
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


	public void NewFile(View view){
		workflow.clear();

	}

	public void WorkFlowSave(View view){
		int num = workflow.getCount();
		Log.d("WorkflowSetting",String.valueOf( num));

		SharedPreferences.Editor WorkflowNum = getSharedPreferences("Workflow",MODE_PRIVATE).edit();
		WorkflowNum.clear();
		WorkflowNum.putInt("Tag",num);
		WorkflowNum.apply();

		for (int i=0;i<num;i++)
		{
			WorkflowItem item = workflow.getItem(i);
			assert item != null;
			String itemName = item.getLabel().toString();

			int itemValue1 = item.getValue1();
			int itemValue2 = item.getValue2();
			SharedPreferences.Editor WorkflowEditor = getSharedPreferences("Workflow",MODE_PRIVATE).edit();
			WorkflowEditor.putString(String.valueOf(i*3+1),itemName);
			WorkflowEditor.putInt(String.valueOf(i*3+2),itemValue1);
			WorkflowEditor.putInt(String.valueOf(i*3+3),itemValue2);
			WorkflowEditor.apply();
		}

		Toast.makeText(getApplicationContext(), "流程设置已保存", Toast.LENGTH_SHORT).show();


	}

	public void WorkFlowPresent(View view)
	{

		SharedPreferences WorkflowTag = getSharedPreferences("Workflow",MODE_PRIVATE);
		int num = WorkflowTag.getInt("Tag",0);



		for (int i=0; i<num;i++)
		{
			SharedPreferences WorkflowReader = getSharedPreferences("Workflow",MODE_PRIVATE);
			String itemName = WorkflowReader.getString(String.valueOf(i*3+1),"Error");
			int itemValue1 = WorkflowReader.getInt(String.valueOf(i*3+2),0);
			int itemValue2 = WorkflowReader.getInt(String.valueOf(i*3+3),0);

			WorkflowItem item = new WorkflowItem();
			item.setLabel(itemName);
			switch (itemName)
			{
				case "抽气":
					itemName = "PUMP";
					break;
				case "注射":
					itemName = "INJECT";
					break;
				case "样品":
					itemName = "SAMPLE";
					break;
				case "等待":
					itemName = "WAIT";
					break;
				case "闪蒸":
					itemName = "FLASH";
					break;
				case "升温":
					itemName = "HEAT";
					break;
				case "数据":
					itemName = "DATA";
					break;
				case "清洁":
					itemName = "BAKE";
					break;
				case "风扇":
					itemName = "FAN";
					break;
				default:
					itemName = "UNKNOWN";
					break;
			}
			Log.d("WorkflowSetting",itemName);

			item.setType(itemName);
			item.setValue1(itemValue1);
			item.setValue2(itemValue2);
			workflow.add(item);
		}
	}
}
