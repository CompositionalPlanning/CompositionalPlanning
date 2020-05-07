package com.dyngraphs.compositionalplanning;

import java.util.List;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.EnumFaceDirection;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

public class Lego {
	private int id;
	private final int length;
	private final int width;
	private final int height;
	private BlockPos[] legoBlockPositions;
	private EnumFacing direction = EnumFacing.SOUTH; //default orientation in MC
	private Block blockType = Blocks.CONCRETE;
	private EnumDyeColor color = EnumDyeColor.GRAY;
	private boolean isGroundLego = false;
	
	public Lego(int id, int length, int width) throws IllegalArgumentException {
		this(id, length, width, 1);
	}
	public Lego(int id, int length, int width, int height) throws IllegalArgumentException {
		if(length < 1 || width < 1 || height < 1) {
			throw new IllegalArgumentException("Neither length, width nor height of a lego may be smaller than 1.");
		}
		this.id = id;
		this.length = length;
		this.width = width;
		this.height = height;
		legoBlockPositions = new BlockPos[length*width*height];
	}
	
	public static Lego CreateGroundLego(int id) {
		Lego lego = new Lego();
		lego.id = id;
		lego.isGroundLego = true;
		return lego;
	}
	
	private Lego() {
		length = 0;
		width = 0;
		height = 0;
		
	}
	
	public Lego(Lego lego) {
		this(lego.id, lego.length, lego.width, lego.height);
		this.direction = lego.direction;
		this.color = lego.color;
		this.blockType = lego.blockType;
		int i = 0;
		for(BlockPos pos : lego.legoBlockPositions) {
			legoBlockPositions[i] = new BlockPos(pos);
			i++;
		}
	}
	
	public BlockPos getOriginBlockPos() {
		return legoBlockPositions[0];
	}
	
	public int getId() {
		return id;
	}
	
	public int getLength() {
		return length;
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	public EnumFacing getDirection() {
		return direction;
	}

	public void setDirection(EnumFacing direction) {
		if(direction == EnumFacing.DOWN || direction == EnumFacing.UP) {
			throw new IllegalArgumentException("Lego cannot face up or down as it is positioned in the plane.");
		}
		this.direction = direction;
	}

	public Block getBlockType() {
		return blockType;
	}

	public void setBlockType(Block blockType) {
		this.blockType = blockType;
	}
	/*
	 * Copies every element of the List. Using only immutable blocks.
	 */
	public void setLegoBlocks(List<? extends BlockPos> blockPositions) {
		if(legoBlockPositions.length != blockPositions.size()) {
			throw new IllegalArgumentException("Provided list of block positions does not match dimension of the lego");
		}
		int i = 0;
		for(BlockPos block : blockPositions) {
			this.legoBlockPositions[i] = block.toImmutable();
			i++;
		}
	}
	/*
	 * Get the BlockPos from the coordinates used in the model.
	 * Assumption: Map (Array) is filled LR
	 */
	public BlockPos GetTransformedBlockPos(int x, int z) {
		if(this.height > 1) {
			throw new IllegalArgumentException("Cannot access an element with a height greater than 1 when only providing length and width coordinates");
		}
		if(z*this.length + x >= legoBlockPositions.length) {
			CompositionalPlanningMod.logger.warn("Tried accessing element {} but there are only {} for the lego {} witch dimensions {}x{} ", z*this.length + x, this.legoBlockPositions.length,this.id, this.length, this.width);
		}
		return legoBlockPositions[z*this.length + x];
	}
	
	public List<BlockPos> getBlockPositions() {
		return Arrays.asList(this.legoBlockPositions);
	}
	
	public EnumDyeColor getColor() {
		return color;
	}
	public void setColor(EnumDyeColor color) {
		this.color = color;
	}
	
	public boolean isGroundLego() {
		return this.isGroundLego;
	}

}
