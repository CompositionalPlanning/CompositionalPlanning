package com.dyngraphs.compositionalplanning;

public class LegoVertex {
	private int id;
	private int x;
	private int y;
	private int z;
	private int rotation;
	private int height;
	private int length;
	private int width;
	private String ref;
	private String type;
	private String color;
	private boolean isGroundVertex = false;
	
	LegoVertex(int id, int x, int y, int z, int rotation, int height, int length, int width, String ref, String type, String color) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotation = rotation;
		this.height = height;
		this.length = length;
		this.width = width;
		this.ref = ref;
		this.type = type;
		this.color = color;
	}
	
	LegoVertex(int id, int x, int y, int z, String type) {
		this(id, x, y, z, -1, -1, -1, -1, "groundDummy", type, "none");
		this.isGroundVertex = true;
	}
	
	
	
	int getId() {
		return id;
	}
	int getX() {
		return x;
	}
	int getY() {
		return y;
	}
	int getZ() {
		return z;
	}
	int getRotation() {
		return rotation;
	}

	int getHeight() {
		return height;
	}
	int getLength() {
		return length;
	}
	int getWidth() {
		return width;
	}
	String getType() {
		return type;
	}
	String getColor() {
		return color;
	}
	
	boolean isGroundVertex() {
		return this.isGroundVertex;
	}

	
}
