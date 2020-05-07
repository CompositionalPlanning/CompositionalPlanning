package com.dyngraphs.compositionalplanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.util.math.Vec3i;

class AssemblyManager {
	private Map<Integer, Lego> constructionLegos;
	private ConstructionScheduler scheduler;
	private List<LegoAssembly> constructionAssemblies = new ArrayList<>();
	private List<Vec3i> freeAreas = new ArrayList<>();
	private int schedulePhaseCounter = 0;
	private int newPartCounter = 0;
	private int snapCounter = 0;
	private int totalOperations = 0;
	
	
	AssemblyManager(Map<Integer, Lego> constructionLegos, ConstructionScheduler scheduler) {
		this.constructionLegos = constructionLegos;
		this.scheduler = scheduler;
	}
	
	
	
	void updateAssemblies() {
		newPartCounter = 0;
		snapCounter = 0;
		List<Lego> tickLegos = new ArrayList<>();
		List<SnapDescription> scheduledSnaps = this.scheduler.pullCurrentConstructionElement();
		totalOperations += scheduledSnaps.size();
		if(scheduledSnaps.isEmpty()) {
			return;
		}
		schedulePhaseCounter++;
		for(SnapDescription snap : scheduledSnaps) {
			if(snap.isPartSnap()) {
				addToAssembly(snap);
					newPartCounter++;
			} else {
				mergeAssemblies(snap);
				snapCounter++;
			}
		}
	}
	
	// Call after the last updateAssemblies() invokation!
	boolean assemblyIsConstructed() {
		return this.scheduler.scheduleIsFinished();
	}
	
	Collection<Lego> pullFreedLegos() {
		return constructionAssemblies.stream().flatMap(assembly -> assembly.pullFreedLegos().stream()).collect(Collectors.toCollection(ArrayList::new));
	}
	
	Collection<Lego> pullAddedLegos() {
		return constructionAssemblies.stream().flatMap(assembly -> assembly.pullAddedLegos().stream()).collect(Collectors.toCollection(ArrayList::new));
	}
	
	Collection<Lego> getMovedLegos() {
		return constructionAssemblies.stream().flatMap(assembly -> assembly.getMovedLegos().stream()).collect(Collectors.toCollection(ArrayList::new));
	}
	
	private void mergeAssemblies(SnapDescription snap) {
		LegoAssembly firstAssembly = getCorrespondingLegoAssembly(snap.getFirstInputs());
		LegoAssembly secondAssembly = getCorrespondingLegoAssembly(snap.getSecondInputs());
		int removalCounter = 0;
		for(int i =  constructionAssemblies.size()-1; i >= 0; i--) {
			if(constructionAssemblies.get(i) == firstAssembly || constructionAssemblies.get(i) == secondAssembly) {
				LegoAssembly removedAssembly = constructionAssemblies.remove(i);
				if(!removedAssembly.isGrounded()) { // if it's grounded, it's origin has already been added to the free areas
					freeAreas.add(0, removedAssembly.getAssemblyOrigin());
				}
				removalCounter++;
			}
			if(removalCounter >=2) {
				break;
			}
		}
		
		LegoAssembly mergedAssembly = firstAssembly.mergeWithAssembly(secondAssembly);
		if(!mergedAssembly.isGrounded()) {
			freeAreas.remove(freeAreas.indexOf(mergedAssembly.getAssemblyOrigin()));
		}
		constructionAssemblies.add(mergedAssembly);
	}

	private void addToAssembly(SnapDescription snap) {
		boolean isNewAssembly = !isPartOfAssembly(snap.getFirstInputs().get(0)) && !isPartOfAssembly(snap.getSecondInputs().get(0));
		if(isNewAssembly) {
			int legoId = snap.getFirstInputs().get(0);
			Lego constructionLego = constructionLegos.get(legoId);
			LegoAssembly legoAssembly = new LegoAssembly(deriveAssemblyArea(), constructionLego);
			if(legoAssembly.isGrounded()) {
				freeAreas.add(0, legoAssembly.getAssemblyOrigin());
			}
			constructionAssemblies.add(legoAssembly);
			// for snaps containing a ground piece we add both parts at the same time
			if(constructionLegos.get(snap.getFirstInputs().get(0)).isGroundLego() || constructionLegos.get(snap.getSecondInputs().get(0)).isGroundLego()) {
				addToAssembly(snap);
			}
		} else {
			int legoId = snap.getSecondInputs().size() ==  1 ? snap.getSecondInputs().get(0) : snap.getFirstInputs().get(0);
			List<Integer> assemblyIds = snap.getSecondInputs().size() ==  1 ? snap.getFirstInputs() : snap.getSecondInputs();
			LegoAssembly legoAssembly = getCorrespondingLegoAssembly(assemblyIds);
			boolean wasGroundAssembly = legoAssembly.isGrounded();
			legoAssembly.addLego(constructionLegos.get(legoId));
			if(!wasGroundAssembly && legoAssembly.isGrounded()) {
				freeAreas.add(0, legoAssembly.getAssemblyOrigin());
			}
		}
	}
	
	private LegoAssembly getCorrespondingLegoAssembly(List<Integer> assemblyIds) {
		Lego lego = constructionLegos.get(assemblyIds.get(0));
		LegoAssembly correspondingAssembly = null;
		for(LegoAssembly assembly : constructionAssemblies) {
			if(assembly.isPartOfAssembly(lego)) {
				correspondingAssembly = assembly;
				break;
			}
		}
		return correspondingAssembly; // core assumption: assembly assignment is correct
	}
	
	private boolean isPartOfAssembly(int legoId) {
		Lego lego = constructionLegos.get(legoId);
		for(LegoAssembly assembly : constructionAssemblies) {
			if(assembly.isPartOfAssembly(lego)) {
				return true;
			}
		}
		return false;
	}
	
	private Vec3i deriveAssemblyArea() {
		final int areaDistance = 20;
		Vec3i worldOrigin = ConstructionHelper.WORLD_ORIGIN;
		int numberOfAreas = constructionAssemblies.size() + freeAreas.size();
		int xCoord = areaDistance * ((numberOfAreas+1)%2) * ((int)(numberOfAreas/4) + 1);
		int zCoord = areaDistance * ((numberOfAreas)%2) * ((int)(numberOfAreas/4) + 1);
		switch(numberOfAreas%4) {
		case 0:
			xCoord *= -1;
			break;
		case 1:
			break;
		case 2:
			break;
		case 3:
			zCoord *= -1;
			break;
		}
		// *-1 because the LDraw model was mirrored but the MC origin may have been moved
		freeAreas.add(new Vec3i(xCoord + -1*worldOrigin.getX(), worldOrigin.getY(), zCoord + worldOrigin.getZ()));
		// we sort according to the x/y distance to the center so areas close to the assembly origin are used first
		Collections.sort(freeAreas, (v1, v2) -> 
		Math.abs(Math.abs(-1*worldOrigin.getX() - v1.getX()) -  Math.abs(worldOrigin.getZ() - v1.getZ())) < 
		Math.abs(Math.abs(-1*worldOrigin.getX() - v2.getX()) -  Math.abs(worldOrigin.getZ() - v2.getZ())) ?
				- 1 :
		Math.abs(Math.abs(-1*worldOrigin.getX() - v1.getX()) -  Math.abs(worldOrigin.getZ() - v1.getZ())) == 
		Math.abs(Math.abs(-1*worldOrigin.getX() - v2.getX()) -  Math.abs(worldOrigin.getZ() - v2.getZ())) ?
				0 : 1
				);
		return 	freeAreas.remove(0);
	}
	
	int getSchedulePhaseCounter() {
		return this.schedulePhaseCounter;
	}
	
	// Counts the number of assembly snaps for the previous schedule step
	int getSnapCount() {
		return this.snapCounter;
	}
	
	// Counts the number of part snaps/new part introductions for the previous schedule step
	int getAddedPartCount() {
		return this.newPartCounter;
	}
	
	int getTotalOperations() {
		return this.totalOperations;
	}
	
	int getNumberOfMaximumWorkers() {
		return this.scheduler.getMaximumNumberOfWorkers();
	}
}
