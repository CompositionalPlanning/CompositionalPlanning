package com.dyngraphs.compositionalplanning;

import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class ConstructionRunner {
	
	public static boolean isActivated = false;
	public static Graph<LegoVertex, DefaultWeightedEdge> connectivityGraph;
	public static ConstructionScheduler scheduler;
	
	private int tickCounter = 0;
	public static int TICK_INTERVAL = 20; // 20 ticks equal 1 second (if the PC can keep up with that speed)
	private boolean isInitialized = false; 
	private AssemblyManager assemblyManager;
	
	

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if(!isActivated) {
			return;
		}
		if(!isInitialized) {
			initialize(connectivityGraph);
			isInitialized = true;
			//buildLegos();
		}
		if(!event.player.getEntityWorld().isRemote && event.phase == Phase.START) {
			tickCounter++;
			if(tickCounter >= TICK_INTERVAL) {
				updateWorld(event.player);
				tickCounter = 0; //reset
			}
		}
	}
	
	private void updateWorld(EntityPlayer player) {
		assemblyManager.updateAssemblies(); // updating is mandatory
		player.sendMessage(new TextComponentString("Assembly construction step " + assemblyManager.getSchedulePhaseCounter()));
		//player.sendMessage(new TextComponentString("Added " + assemblyManager.getAddedPartCount() + " legos."));
		//player.sendMessage(new TextComponentString("Performed " + assemblyManager.getSnapCount() + " snaps."));
		Collection<Lego> freedLegos = assemblyManager.pullFreedLegos();
		Collection<Lego> addedLegos = assemblyManager.pullAddedLegos();
		Collection<Lego> movedLegos = assemblyManager.getMovedLegos();
		removeLegos(player, freedLegos);
		spawnLegos(player, addedLegos);
		if(assemblyManager.assemblyIsConstructed()) {
			player.sendMessage(new TextComponentString("Assembly construction took " + assemblyManager.getSchedulePhaseCounter() + " steps."));
			player.sendMessage(new TextComponentString("A total of " + assemblyManager.getTotalOperations() + " operations were executed with at maximum " + assemblyManager.getNumberOfMaximumWorkers() + " workers"));
			isActivated = false;
			isInitialized = false;
		}
	}
	
	private void spawnLegos(EntityPlayer player, Collection<Lego> constructionLegos) {
		World world = player.world;
        for(Lego legoBlock : constructionLegos) {
        	//player.sendMessage(new TextComponentString("Added lego " + legoBlock.getId() + " (" + legoBlock.getLength() + "x" + legoBlock.getWidth()+")"));
        	for(BlockPos pos : legoBlock.getBlockPositions()) {
        		world.setBlockState(pos ,legoBlock.getBlockType().getDefaultState().withProperty(BlockColored.COLOR, legoBlock.getColor()), 2);
        	}
        }
	}
	
	
	private void removeLegos(EntityPlayer player, Collection<Lego> constructionLegos) {
		World world = player.world;
        for(Lego legoBlock : constructionLegos) {
        	for(BlockPos pos : legoBlock.getBlockPositions()) {
        		world.setBlockState(pos ,Blocks.AIR.getDefaultState(), 2);
        	}
        }}
	
	// plug file or whatever input in here
	// This method uses the old model for constructing the legos - replaced by graph-based approach
	@Deprecated
	private void buildLegos() {
		ConstructionManager constructionManager = new ConstructionManager();
        List<Lego> legos = constructionManager.buildLegoWithStandardCoordinates(2, 2, 3, new Vec3i(0, 0, 2), new Vec3i(1, 0, 2), 1, 3, 4, new Vec3i(0, 0, 3), new Vec3i(0, 0, 2));
        legos.addAll(constructionManager.buildLegoWithStandardCoordinates(3, 2, 4, new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), 2, 2, 3, new Vec3i(1, 	0, 1), new Vec3i(1, 0, 0)));
        legos.addAll(constructionManager.buildLegoWithStandardCoordinates(4, 10, 1, new Vec3i(0, 0, 0), new Vec3i(1, 0, 1), 3, 2, 4, new Vec3i(1, 0, 2), new Vec3i(2, 0, 2)));
        legos.addAll(constructionManager.buildLegoWithStandardCoordinates(101, 3, 3, new Vec3i(0, 0, 0), new Vec3i(1, 0, 1), 100, 5, 5, new Vec3i(1, 0, 1), new Vec3i(2, 0, 2)));
        legos.addAll(constructionManager.buildLegoWithStandardCoordinates(102, 2, 1, new Vec3i(0, 0, 0), new Vec3i(1, 0, 0), 101, 3, 3, new Vec3i(1, 0, 0), new Vec3i(1, 0, 2)));
        legos.addAll(constructionManager.buildLegoWithStandardCoordinates(4, 10, 1, new Vec3i(9, 0, 0), new Vec3i(10, 0, 0), 102, 2, 1, new Vec3i(0, 0, 0), new Vec3i(1, 0, 0)));
        Map<Integer, Lego> constructionLegos = constructionManager.getLegosWithId();
	}
	
	private void initialize(Graph<LegoVertex, DefaultWeightedEdge> graph) {
		ConstructionManager constructionManager = new ConstructionManager();
		constructionManager.buildLegos(graph);
		Map<Integer, Lego> constructionLegos = constructionManager.getLegosWithId();
		this.scheduler.setGroundParts(constructionLegos.entrySet().stream().filter(idLegoPair -> idLegoPair.getValue().isGroundLego()).map(Entry::getKey).collect(Collectors.toCollection(HashSet::new)));
		assemblyManager = new AssemblyManager(constructionLegos, this.scheduler);
	} 
}
