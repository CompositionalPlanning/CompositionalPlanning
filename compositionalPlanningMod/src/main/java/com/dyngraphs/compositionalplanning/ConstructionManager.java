package com.dyngraphs.compositionalplanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class ConstructionManager {
	private List<BaseLegoConstructor> spawnAreaConstructors= new ArrayList<BaseLegoConstructor>();
	private final Vec3i SPAWN_AREA_DISTANCE = new Vec3i(6, 0, 0);
	
	// Out of use. Switched to graph based construction
	@Deprecated
	private List<Lego> buildLego(Lego sourceLego, Vec3i source0, Vec3i source1, Lego destinationLego,
			Vec3i destination0, Vec3i destination1) {
		List<Lego> legos = new ArrayList<Lego>();
		LegoConstructor destinationLegoConstructor = tryGetLegoConstructor(destinationLego);
		LegoConstructor sourceLegoConstructor = tryGetLegoConstructor(sourceLego);
		if(destinationLegoConstructor == null) {
			destinationLegoConstructor = new LegoConstructor(deriveNewOrigin());
			destinationLegoConstructor.positionBaseLego(destinationLego);
			spawnAreaConstructors.add(destinationLegoConstructor);
			legos.add(destinationLego);
		}
		if(destinationLegoConstructor != null && sourceLegoConstructor != null) {
			destinationLego = destinationLegoConstructor.getLego(destinationLego.getId());
			sourceLego = sourceLegoConstructor.getLego(sourceLego.getId());
			sourceLegoConstructor.moveBodyToDestination(sourceLego, destinationLego, source0, destination0);
		} else {
			destinationLegoConstructor.positionLego(sourceLego, source0, source1, destinationLego, destination0, destination1);
			legos.add(sourceLego);
		}
		return legos;
	}

	// Out of use. Switched to graph based construction
	@Deprecated
	public List<Lego> buildLego(
			int sourceLegoId, int sourceLength, int sourceWidth, Vec3i source0, Vec3i source1, 
			int destinationLegoId, int destinationLength, int destinationWidth, Vec3i destination0, Vec3i destination1) {
		Lego destinationLego = new Lego(destinationLegoId, destinationLength, destinationWidth);
		Lego sourceLego = new Lego(sourceLegoId, sourceLength, sourceWidth);
		
		return buildLego(sourceLego, source0, source1, destinationLego, destination0, destination1);
	}
	
	// Out of use. Switched to graph based construction
	/*
	 * A helper function that takes "natural" coordinates and transforms them to the MC coordinate system before building blocks
	 */
	@Deprecated
	public List<Lego> buildLegoWithStandardCoordinates(
			int sourceLegoId, int sourceLength, int sourceWidth, Vec3i source0, Vec3i source1, 
			int destinationLegoId, int destinationLength, int destinationWidth, Vec3i destination0, Vec3i destination1) {
		Vec3i newSource0 = new Vec3i(source0.getX(), source0.getY(), sourceWidth-1 - source0.getZ());
		Vec3i newSource1 = new Vec3i(source1.getX(), source1.getY(), sourceWidth-1 - source1.getZ());
		Vec3i newDestination0 = new Vec3i(destination0.getX(), destination0.getY(), destinationWidth-1 - destination0.getZ());
		Vec3i newDestination1 = new Vec3i(destination1.getX(), destination1.getY(), destinationWidth-1 - destination1.getZ());
		
		return buildLego(sourceLegoId, sourceLength, sourceWidth, newSource0, newSource1, destinationLegoId, destinationLength, destinationWidth, newDestination0, newDestination1);
	}
	
	/*
	 * Returns a Lego object containing the corresponding MC Block Positions. The supplied legoVertex is based on a GraphML vertex derived from LDRaw
	 */
	public List<Lego>buildLegos(Graph<LegoVertex, DefaultWeightedEdge> graph) {
		LDrawLegoConstructor constructor = new LDrawLegoConstructor();
		spawnAreaConstructors.add(constructor);
		List<Lego> builtLegos = new ArrayList<>();
		for(LegoVertex legoVertex : graph.vertexSet()) {
			builtLegos.add(constructor.buildLego(legoVertex));
		}
		return builtLegos;
	}
	
	@Nullable
	private LegoConstructor tryGetLegoConstructor(Lego destinationLego) {
		if(spawnAreaConstructors.isEmpty()) {
			return null;
		} else {
			for (BaseLegoConstructor legoConstructor : spawnAreaConstructors) {
				if(legoConstructor.hasBuiltLego(destinationLego)) {
					if(legoConstructor instanceof LegoConstructor) {
					return (LegoConstructor) legoConstructor;
					}
					else {
						throw new IllegalArgumentException("Tried accessing a LegoConstructor although a LDrawConstrucor is used. This should not occur.");
					}
				}
			}
		}
		return null;
	}
	
	public Map<Integer, Lego> getLegosWithId() {
		if(spawnAreaConstructors.isEmpty()) {
			throw new NullPointerException("There are no lego constructors. Build all assemblies before accessing their constructors.");
		}
		Map<Integer, Lego> mappedLegos = spawnAreaConstructors.stream().flatMap((constructor) -> constructor.getAllLegosWithId().entrySet().stream()).collect(Collectors.toMap(Entry<Integer, Lego>::getKey, Entry<Integer, Lego>::getValue));
		return mappedLegos;
	}
	
	public Vec3i deriveNewOrigin() {
		if(spawnAreaConstructors.isEmpty()) {
			return ConstructionHelper.WORLD_ORIGIN;
		} else {
			Vec3i precedingSpawnOrigin = ((LegoConstructor) spawnAreaConstructors.get(spawnAreaConstructors.size()-1)).getSpawnOrigin();
			return new Vec3i(precedingSpawnOrigin.getX() + SPAWN_AREA_DISTANCE.getX() , precedingSpawnOrigin.getY() + SPAWN_AREA_DISTANCE.getY(), precedingSpawnOrigin.getZ() + SPAWN_AREA_DISTANCE.getZ());
		}
	}

}
