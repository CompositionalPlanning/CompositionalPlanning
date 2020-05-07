package com.dyngraphs.compositionalplanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Source;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class LegoConstructor extends BaseLegoConstructor{
	private Vec3i spawnOrigin;
	
	
	public LegoConstructor(Vec3i spawnOrigin) {
		this.spawnOrigin = spawnOrigin;
	}
		
	public void positionLego(Lego sourceLego, Vec3i source0, Vec3i source1, 
												   Lego destinationLego, Vec3i destination0, Vec3i destination1) {
		
		destinationLego = constructionElements.get(destinationLego.getId());
		Rotation rotation = get2DRotationFromCoordinates(
				source0.getX(), source0.getZ(), source1.getX(), source1.getZ(), 
				destination0.getX(), destination0.getZ(), destination1.getX(), destination1.getZ());
		List<MutableBlockPos> sourceLegoPositions = constructionHelper.deriveOriginBlockPositions(sourceLego);
		// rotate to reference frame
		rotateOriginalBlockPositions(sourceLegoPositions, destinationLego.getDirection());
		// rotate for connection on reference frame
		moveRotationCenterToOrigin(sourceLegoPositions, source0);
		for (int i = 0; i < sourceLegoPositions.size(); i++) {
			BlockPos rotatetPos = sourceLegoPositions.get(i).rotate(rotation);
			sourceLegoPositions.set(i, new MutableBlockPos(rotatetPos));
		}
		// apply offset of reference frame
		moveOriginalBlockPositions(sourceLegoPositions, destinationLego);
		// apply offset for connection
		sourceLego.setLegoBlocks(sourceLegoPositions); //
		moveOriginalBlocksToConnection(sourceLegoPositions, sourceLego, destinationLego, source0, destination0);
		
		sourceLego.setLegoBlocks(sourceLegoPositions);
		EnumFacing sourceFacing =  constructionHelper.get2DFacingFromRotation(rotation.add(constructionHelper.get2DRotationFromFacing(destinationLego.getDirection())));
		sourceLego.setDirection(sourceFacing);
		constructionElements.put(sourceLego.getId(), sourceLego);
		return;
	}
	
	public void positionBaseLego(Lego destinationLego) {
		List<MutableBlockPos> destinationLegoPositions = constructionHelper.deriveOriginBlockPositions(destinationLego);
		constructionHelper.moveOriginalBlockPositions(destinationLegoPositions, destinationLegoPositions.get(0), new BlockPos(spawnOrigin));
		destinationLego.setLegoBlocks(destinationLegoPositions);
		destinationLego.setDirection(EnumFacing.SOUTH); //default
		constructionElements.put(destinationLego.getId(), destinationLego);
	}
		
	private void rotateOriginalBlockPositions(List<MutableBlockPos> positions, EnumFacing direction) {
		for(int i = 0; i < positions.size(); i++) {
			BlockPos rotatePos = positions.get(i).rotate(constructionHelper.get2DRotationFromFacing(direction));
			positions.set(i, new MutableBlockPos(rotatePos));
		}
	}
	
	
	private void moveRotationCenterToOrigin(List<MutableBlockPos> positions, Vec3i rotationCenter) {
		BlockPos source = new BlockPos(rotationCenter);
		BlockPos destination = new BlockPos(0,0,0);
		constructionHelper.moveOriginalBlockPositions(positions, source, destination);
	}
	
	private void moveOriginalBlockPositions(List<MutableBlockPos> positions, Lego connectionLego) {
		BlockPos connectionLegoOrigin = connectionLego.getOriginBlockPos();
		BlockPos attachingLegoOrigin = positions.get(0); // should be 0,0
		//if(attachingLegoOrigin.getX() != 0 || attachingLegoOrigin.getZ() != 0) {
		//	throw new IllegalArgumentException("The provided list of positions does not start with an"
		//			+ " element position at (0,0). This is an assumption that has to hold.");
		//}
		constructionHelper.moveOriginalBlockPositions(positions, attachingLegoOrigin, connectionLegoOrigin);
	}
	
	private void moveOriginalBlocksToConnection(List<MutableBlockPos> sourcePositions,Lego sourceLego, Lego destinationLego, Vec3i sourceConnection, Vec3i destinationConnection) {
		BlockPos sourceBlockPos = sourceLego.GetTransformedBlockPos(sourceConnection.getX(), sourceConnection.getZ());
		BlockPos destinationBlockPos = destinationLego.GetTransformedBlockPos(destinationConnection.getX(), destinationConnection.getZ());
		constructionHelper.moveOriginalBlockPositions(sourcePositions, sourceBlockPos, destinationBlockPos);
		int yOffset = (destinationBlockPos.getY() - sourceBlockPos.getY()) + 1; // since we connect on top!
		for(MutableBlockPos pos : sourcePositions) {
			pos.move(EnumFacing.UP, yOffset);
		}	
	}
	
	public void moveBodyToDestination(Lego sourceLego, Lego destinationLego, Vec3i sourceConnection, Vec3i destinationConnection) {
		BlockPos sourceBlockPos = sourceLego.GetTransformedBlockPos(sourceConnection.getX(), sourceConnection.getZ());
		BlockPos destinationBlockPos = destinationLego.GetTransformedBlockPos(destinationConnection.getX(), destinationConnection.getZ());
		for(Lego legoBlock : constructionElements.values()) {
			List<MutableBlockPos> sourcePositions = new ArrayList<>();
			for(BlockPos blockPos : legoBlock.getBlockPositions()) {
				sourcePositions.add(new MutableBlockPos(blockPos));
			}
			constructionHelper.moveOriginalBlockPositions(sourcePositions, sourceBlockPos, destinationBlockPos);
			legoBlock.setLegoBlocks(sourcePositions);
		}
	}
	
	
	private Rotation get2DRotationFromCoordinates
	(int sourceX0, int sourceZ0, int sourceX1, int sourceZ1,
			int destinationX0, int destinationZ0, int destinationX1, int destinationZ1) {
		Vec3d sourceVector = new Vec3d(sourceX0 - sourceX1,sourceZ0 - sourceZ1, 0); //2d Space
		Vec3d destinationVector = new Vec3d(destinationX0 - destinationX1, destinationZ0 - destinationZ1, 0);
		
		// angle from source -> destination
		double dotProduct = destinationVector.dotProduct(sourceVector);
		double determinant2d = destinationVector.x * sourceVector.y - destinationVector.y * sourceVector.x;
		
		double angle = Math.atan2(determinant2d, dotProduct); // (-pi ,pi]
		if(Math.abs(angle) >= 0.75*Math.PI) { //180 degree
			return Rotation.CLOCKWISE_180;
		}
		if(angle < 0.75*Math.PI && angle >= 0.25*Math.PI) { // 90 degree
			return Rotation.COUNTERCLOCKWISE_90;
		}
		if(angle < 0.25*Math.PI && angle >= -0.25*Math.PI) { // stay
			return Rotation.NONE;
		}
		if(angle < -0.25*Math.PI && angle > -0.75*Math.PI) { // -90 degree
			return Rotation.CLOCKWISE_90;
		}
		return Rotation.NONE;
	}
	
	
	public boolean hasBuiltLego(int legoId) {
		return constructionElements.containsKey(legoId);
	}
		
	public Lego getLego(int id) {
		return constructionElements.get(id);
	}
	
	public Vec3i getSpawnOrigin() {
		return spawnOrigin;
	}

}
