package com.dyngraphs.compositionalplanning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;

public class LDrawLegoConstructor extends BaseLegoConstructor{
	private final int UNIT_HEIGHT = 24; // 24 LDU for a Brick (Plates only have 8 LDU)
	private final int UNIT_DEPTH_WIDTH = 20; // brick depth & width 20 LDU
	private ConstructionHelper constructionHelper = new ConstructionHelper();
	
	public Lego buildLego(LegoVertex legoVertex) {
		if(legoVertex.isGroundVertex()) {
			Lego groundLego = Lego.CreateGroundLego(legoVertex.getId());
			constructionElements.put(groundLego.getId(), groundLego);
			return groundLego;
		}
		int roundedHeight = legoVertex.getHeight() < UNIT_HEIGHT ? UNIT_HEIGHT : legoVertex.getHeight(); // only needed for Plates that are 8 LDU high
		
		Lego lego = new Lego(legoVertex.getId(), legoVertex.getLength()/UNIT_DEPTH_WIDTH, legoVertex.getWidth()/UNIT_DEPTH_WIDTH, roundedHeight/UNIT_HEIGHT);
		lego.setColor(deriveColor(legoVertex.getColor()));
		List<MutableBlockPos> blockPositions = constructionHelper.deriveOriginBlockPositions(lego);
		// Don't rotate the blocks if they were already rotated when exported from LDraw!
		int targetX = (legoVertex.getX() - legoVertex.getLength()/2)/UNIT_DEPTH_WIDTH + constructionHelper.getWORLD_ORIGIN().getX();
		int targetY = (-1 * legoVertex.getY() - roundedHeight)/UNIT_HEIGHT + constructionHelper.getWORLD_ORIGIN().getY();
		int targetZ = (legoVertex.getZ() - legoVertex.getWidth()/2)/UNIT_DEPTH_WIDTH + constructionHelper.getWORLD_ORIGIN().getZ();
		
		BlockPos legoOrigin = new BlockPos(targetX, targetY, targetZ);
		constructionHelper.moveOriginalBlockPositions(blockPositions, blockPositions.get(0), legoOrigin);
		constructionHelper.mirrorOnZ(blockPositions); //LDraw models and minecraft representation are mirrored
		lego.setLegoBlocks(blockPositions);
		constructionElements.put(lego.getId(), lego);
		return lego;
	}
	
	/*
	 *  Not used at the moment because graphML vertices are already rotated when provided
	 */
	@Deprecated
	private Rotation getRotationDirection(int rotationAngel) {
		Rotation rotation = Rotation.NONE;
		switch(rotationAngel) {
		case 0:
			break;
		case 90:
			rotation = Rotation.CLOCKWISE_90;
			break;
		case 180:
			rotation = Rotation.CLOCKWISE_180;
			break;
		case 270:
			rotation = Rotation.COUNTERCLOCKWISE_90;
			break;
		}
		return rotation;
	}
	
	private EnumDyeColor deriveColor(String colorName) {
		final Map<String, EnumDyeColor> colorMapping = new HashMap<String, EnumDyeColor>() {{
	        put("black", EnumDyeColor.BLACK);
	        put("blue", EnumDyeColor.BLUE);
	        put("green", EnumDyeColor.GREEN);
	        put("lime", EnumDyeColor.LIME);
	        put("red", EnumDyeColor.RED);
	        put("pink", EnumDyeColor.PINK);
	        put("brown", EnumDyeColor.BROWN);
	        put("grey", EnumDyeColor.GRAY);
	        put("gray", EnumDyeColor.GRAY);
	        put("orange", EnumDyeColor.ORANGE);
	        put("yellow", EnumDyeColor.YELLOW);
	        put("white", EnumDyeColor.WHITE);
	    }};
	    for(String color : colorMapping.keySet()) {
	    	if(colorName.contains(color)) {
	    		return colorMapping.get(color);
	    	}
	    }
	    return EnumDyeColor.GRAY; // Default color
	}
}
