package com.dyngraphs.compositionalplanning;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

import org.apache.logging.log4j.Logger;

@Mod(modid = CompositionalPlanningMod.MODID, name = CompositionalPlanningMod.NAME, version = CompositionalPlanningMod.VERSION)
@Mod.EventBusSubscriber
public class CompositionalPlanningMod
{
    public static final String MODID = "compositionalplanning";
    public static final String NAME = "Compositional Planning";
    public static final String VERSION = "1.0";
    
    public static Logger logger;
    

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ConstructionRunner());
    }
    
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
    	// register server commands
    	event.registerServerCommand(new AssemblyCommand());
    }
        

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	logger.info("Loaded Compositional Planning mod");
    }
    
    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> event) {
    }
    
    @SubscribeEvent
    public void registerItemBlocks(RegistryEvent.Register<Item> event) {
    }
}
