package com.dyngraphs.compositionalplanning;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

class ConstructionHelper {
	static Vec3i WORLD_ORIGIN = new Vec3i(0,4,0); // only y may be changed
	
	Rotation get2DRotationFromFacing(EnumFacing direction) {
		Rotation rotation = Rotation.NONE;
		switch(direction) {
		case DOWN:
			break; // should never occur
		case EAST:
			rotation = Rotation.COUNTERCLOCKWISE_90;
			break;
		case NORTH:
			rotation = Rotation.CLOCKWISE_180;
			break;
		case SOUTH:
			rotation = Rotation.NONE; // default orientation
			break;
		case UP: // should never occur
			break;
		case WEST:
			rotation = Rotation.CLOCKWISE_90;
			break;
		default:
			break;
		}
		return rotation;
	}
	
	EnumFacing get2DFacingFromRotation(Rotation rotation) {
		switch (rotation) {
		case NONE:
			return EnumFacing.SOUTH;
		case CLOCKWISE_90:
			return EnumFacing.WEST;
		case CLOCKWISE_180:
			return EnumFacing.NORTH;
		case COUNTERCLOCKWISE_90:
			return EnumFacing.EAST;
		default:
			return EnumFacing.SOUTH;
		}
	}
	
	/*
	 * Get all the block positions for a Lego.
	 * IMPORTANT: We fill by LR or referring to Minecraft by XZ
	 */
	List<MutableBlockPos> deriveOriginBlockPositions(Lego lego) {
		List<MutableBlockPos> positions = new ArrayList<MutableBlockPos>();
		for(int height = 0; height < lego.getHeight(); height++) {
			for(int width = 0; width < lego.getWidth(); width++) {
				for(int length = 0; length < lego.getLength(); length++) {
					MutableBlockPos legoElement = 
							new MutableBlockPos(WORLD_ORIGIN.getX() + length,
									WORLD_ORIGIN.getY() + height,
									WORLD_ORIGIN.getZ() + width);
					positions.add(legoElement);
				}
			}
		}
		return positions;
	}
	
	void moveOriginalBlockPositions(List<MutableBlockPos> blockPositions, BlockPos sourcePosition, BlockPos destinationPosition) {
		int deltaX = destinationPosition.getX() - sourcePosition.getX();
		int deltaZ = destinationPosition.getZ() - sourcePosition.getZ();
		int deltaY = destinationPosition.getY() - sourcePosition.getY();
		EnumFacing xFacing = deltaX > 0 ? EnumFacing.EAST : EnumFacing.WEST;
		EnumFacing zFacing = deltaZ > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
		EnumFacing yFacing = deltaY > 0 ? EnumFacing.UP : EnumFacing.DOWN;
		for(MutableBlockPos pos : blockPositions) {
			pos.move(xFacing, Math.abs(deltaX));
			pos.move(zFacing, Math.abs(deltaZ));
			pos.move(yFacing, Math.abs(deltaY));
		}
	}
		
	
	void mirrorOnZ(List<MutableBlockPos> blockPositions) {
		for(MutableBlockPos pos : blockPositions) {
			pos.setPos(-1*pos.getX(), pos.getY(), pos.getZ());
		}
	}

	Vec3i getWORLD_ORIGIN() {
		return WORLD_ORIGIN;
	}

}
