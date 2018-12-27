package com.zju.sawdetector.model;

/**
 * Created by Mitsunari on 2017/11/18.
 */

public class WorkflowItem {

	static public enum Type {
		UNKNOWN,
		PUMP("s"),
		INJECT,
		SAMPLE,
		WAIT("s"),
		FLASH(" "),
		HEAT("°C/s", "°C"),
		DATA("s"),
		BAKE("s", "°C"),
		FAN;

		String input1 = null;
		String input2 = null;

		private Type() {
		}

		private Type(String input) {
			this.input1 = input;
		}

		private Type(String input1, String input2) {
			this.input1 = input1;
			this.input2 = input2;
		}

		public String getInput1() {
			return input1;
		}

		public String getInput2() {
			return input2;
		}

		static public Type fromString(String tag) {
			for (Type type : Type.values()) {
				if (type.name().equalsIgnoreCase(tag)) {
					return type;
				}
			}
			return UNKNOWN;
		}
	}

	private CharSequence label;

	private int value1 = 0;
	private int value2 = 0;


	private Type type;

	public CharSequence getLabel() {
		return label;
	}

	public void setLabel(CharSequence label) {
		this.label = label;
	}


	public int getValue2() {
		return value2;
	}

	public void setValue2(int value) {
		this.value2 = value;
	}


	public int getValue1() {
		return value1;
	}

	public void setValue1(int value) {
		this.value1 = value;
	}


	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public void setType(String tag) {
		this.type = Type.fromString(tag);
	}

}
