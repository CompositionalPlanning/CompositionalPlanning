package com.dyngraphs.compositionalplanning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.io.ImportException;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class AssemblyCommand extends  CommandBase{
	private int scheduleWorkers = 16;
	
	private final List<String> aliases = new ArrayList<String>() {{
		add("assembly"); 
		add("ass");}};


	@Override
	public String getName() {
		return "assembly";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "This command constructs a specified assembly. "
				+ "By using \"/construction <filename>\" or \"/construction <connectivity_graph_file.graphml> <schedule_file.json>\" the provided assembly will be constructed. "
				+ "Provide absolte paths. If only a single argument is provided, the argument will be resolved to \"<arg>.graphml\" and \"<arg>.json\" to retrieve the connectivity graph and the correspondign schedule."
				+ "Angle brackets indicate arguments. Do not include them. ";
	}

	@Override
	public List<String> getAliases() {
		return aliases;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		
        sender.sendMessage(new TextComponentString("Assembly command called"));
        String connectivityGraphLocation;
        String scheduleLocation;
        if(args.length == 1) {
        	if("house".equalsIgnoreCase(args[0])) {
        		sender.sendMessage(new TextComponentString("Using House model example with parallel schedule"));
        	    BufferedReader connectivityGraphReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/house.graphml")));
        	    BufferedReader scheduleReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/house_parallel_schedule.json")));
        	    launchAssemblyProcess(sender, connectivityGraphReader, scheduleReader);
        	}
        	else if("columns".equalsIgnoreCase(args[0])) {
        		sender.sendMessage(new TextComponentString("Using Columns model example with parallel schedule"));
        		BufferedReader connectivityGraphReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/columns.graphml")));
        		BufferedReader scheduleReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/columns_parallel_schedule.json")));
        		launchAssemblyProcess(sender, connectivityGraphReader, scheduleReader);
        	}
        	else {
        		connectivityGraphLocation = args[0] + ".graphml";
        		scheduleLocation = args[0] + ".json";
        		sender.sendMessage(new TextComponentString("Using assembly file: " + connectivityGraphLocation + " and schedule file: " + scheduleLocation));
        		launchAssemblyProcess(sender, connectivityGraphLocation, scheduleLocation);
        	}
        }
        if(args.length == 2) {
        	if("workers".equalsIgnoreCase(args[0])) {
        		this.scheduleWorkers = (Long.parseLong(args[1]) > 0 && Long.parseLong(args[1]) <= Integer.MAX_VALUE) ? Integer.parseInt(args[1]) : this.scheduleWorkers;
        		sender.sendMessage(new TextComponentString("Set number of workers to " + this.scheduleWorkers));
        	} else if("ticks".equalsIgnoreCase(args[0])) {
        		ConstructionRunner.TICK_INTERVAL = (Long.parseLong(args[1]) > 0 && Long.parseLong(args[1]) <= 2000) ? Integer.parseInt(args[1]) : ConstructionRunner.TICK_INTERVAL;
        		sender.sendMessage(new TextComponentString("Set number of construction ticks to " + ConstructionRunner.TICK_INTERVAL));
        	} else if("house".equalsIgnoreCase(args[0])) {
        		BufferedReader connectivityReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/house.graphml")));
        		BufferedReader scheduleReader;
        		if("parallel".equalsIgnoreCase(args[1])) {
        			sender.sendMessage(new TextComponentString("Using House model example with parallel schedule"));
        			scheduleReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/house_parallel_schedule.json")));
        			launchAssemblyProcess(sender, connectivityReader, scheduleReader);
        		} else if("sequence".equalsIgnoreCase(args[1])) {
        			sender.sendMessage(new TextComponentString("Using House model example with sequential schedule"));
        			scheduleReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/house_sequential_schedule.json")));
        			launchAssemblyProcess(sender, connectivityReader, scheduleReader);
        		}        		
        	}
        	else if("columns".equalsIgnoreCase(args[0])) {
        		BufferedReader connectivityReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/columns.graphml")));
        		BufferedReader scheduleReader;
        		if("parallel".equalsIgnoreCase(args[1])) {
        			sender.sendMessage(new TextComponentString("Using Columns model example with parallel schedule"));
        			scheduleReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/columns_parallel_schedule.json")));
        			launchAssemblyProcess(sender, connectivityReader, scheduleReader);
        		} else if("sequence".equalsIgnoreCase(args[1])) {
        			sender.sendMessage(new TextComponentString("Using Columns model example with sequential schedule"));
        			scheduleReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("assets/compositionalplanning/columns_sequential_schedule.json")));
        			launchAssemblyProcess(sender, connectivityReader, scheduleReader);
        		}
        	} else {
        		connectivityGraphLocation = args[0];
        		scheduleLocation = args[1];
        		sender.sendMessage(new TextComponentString("Using assembly file: " + connectivityGraphLocation + " and schedule file: " + scheduleLocation));
        		launchAssemblyProcess(sender, connectivityGraphLocation, scheduleLocation);
        	}
        }
        if(args.length == 4) {
        	try {
        		if("origin".equalsIgnoreCase(args[0])) {
        			int x = Integer.parseInt(args[1]);
        			int y = Integer.parseInt(args[2]);
        			int z = Integer.parseInt(args[3]);
        			sender.sendMessage(new TextComponentString("Setting assembly origin to (" + x + "," + y + "," + z + ")"));
        			ConstructionHelper.WORLD_ORIGIN = new Vec3i(-1*x, y, z);
        		}
        	}
        	catch (Exception e) {
        		sender.sendMessage(new TextComponentString("Tried setting origin for the assembly but encountered a problem. Please provide the coordinates in the form \"1 2 3\" after \"origin\". The command has the form \"oirigin x y z\""));
        	}
        }
        
	}

	// Sets the variables of the ConstructionRunner and activates it
	private void launchAssemblyProcess(ICommandSender sender, String connectivityGraphLocation,
			String scheduleLocation) {
        GraphMLLoader loader = new GraphMLLoader();
        ConstructionScheduler scheduler = new ConstructionScheduler(scheduleWorkers);
        try {
			ConstructionRunner.connectivityGraph = loader.loadGraphML(connectivityGraphLocation);
			scheduler.loadSchedule(scheduleLocation);
			ConstructionRunner.scheduler = scheduler;
			ConstructionRunner.isActivated = true;
		} catch (ImportException e) {
			sender.sendMessage(new TextComponentString("Aborting command: could not load construction plan: " + connectivityGraphLocation));
		} catch (FileNotFoundException e) {
			sender.sendMessage(new TextComponentString("Aborting command: could not load schedule: " + scheduleLocation));
		}
	}
	
	private void launchAssemblyProcess(ICommandSender sender, Reader connectivityGraphReader,
			Reader scheduleReader) {
        GraphMLLoader loader = new GraphMLLoader();
        ConstructionScheduler scheduler = new ConstructionScheduler(scheduleWorkers);
        try {
			ConstructionRunner.connectivityGraph = loader.loadGraphML(connectivityGraphReader);
			scheduler.loadSchedule(scheduleReader);
			ConstructionRunner.scheduler = scheduler;
			ConstructionRunner.isActivated = true;
		} catch (ImportException e) {
			sender.sendMessage(new TextComponentString("Aborting command: could not load construction plan."));
		} catch (FileNotFoundException e) {
			sender.sendMessage(new TextComponentString("Aborting command: could not load schedule"));
		}
	}
	

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos targetPos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		// TODO Auto-generated method stub
		return false;
	}

}
