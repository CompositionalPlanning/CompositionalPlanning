package com.dyngraphs.compositionalplanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;

class LegoAssembly {
	private Vec3i assemblyOrigin;
	private Set<Lego> assemblyLegos = new HashSet<>();
	private Set<Lego> freedLegos = new HashSet<>();
	private Set<Lego> movedAssemblyLegos = new HashSet<>();
	private Set<Lego> addedAssemblyLegos = new HashSet<>();
	private Lego rootPiece; //this is the lowest Lego of the assembly (there might be several ones with the same height)
	private ConstructionHelper constructionHelper = new ConstructionHelper();
	private boolean isGrounded = false;
	private Set<Lego> groundLegos = new HashSet<>();
	
	LegoAssembly(Vec3i assemblyOrigin) {
		this.assemblyOrigin = assemblyOrigin;
	}
	
	LegoAssembly(Vec3i assemblyOrigin, Lego lego) {
		this(assemblyOrigin);
		if(lego.isGroundLego()) {
			isGrounded = true;
			groundLegos.add(lego);
		} else {
			addLego(lego);
		};
	}
	
	void addLego(Lego lego) {
		if(lego.isGroundLego()) {
			groundLegos.add(lego);
			freedLegos = new HashSet(movedAssemblyLegos);
			movedAssemblyLegos = new HashSet(assemblyLegos);
			if(!this.isGrounded) {
				addedAssemblyLegos = new HashSet(assemblyLegos);
			}
			this.isGrounded = true;
			return;
		}
		assemblyLegos.add(lego);
		if(!isGrounded && (rootPiece == null || rootPiece.getOriginBlockPos().getY() > lego.getOriginBlockPos().getY())) {
			rootPiece = new Lego(lego);
			freedLegos.addAll(movedAssemblyLegos);
			movedAssemblyLegos = moveAssembly(this.rootPiece, this.assemblyLegos);
		}
		if(!isGrounded) {
			Lego movedLego = moveLego(this.rootPiece, lego);
			movedAssemblyLegos.add(movedLego);
			addedAssemblyLegos.add(movedLego);
		} else {
			movedAssemblyLegos.add(lego);
			addedAssemblyLegos.add(lego);
		}
	}
	
	boolean isPartOfAssembly(Lego lego) {
		return assemblyLegos.contains(lego) || groundLegos.contains(lego);
	}
	
	private Lego moveLego(Lego rootLego, Lego targetLego) {
		Set<Lego> singleElementSet = moveAssembly(rootLego, Collections.singleton(targetLego));
		for(Lego movedLego : singleElementSet) {
			return movedLego;
		}
		return null; // should never happen!
	}
	
	private Set<Lego> moveAssembly(Lego rootLego, Set<Lego> targetAssembly) {
		Vec3i offset = rootLego.getOriginBlockPos().subtract(assemblyOrigin);
		Set<Lego> movedAssembly = new HashSet<>();
		for(Lego lego : targetAssembly) {
			Lego movedLego = new Lego(lego);
			List<BlockPos> legoPositions = new ArrayList<>();
			for(BlockPos pos : lego.getBlockPositions()) {
				BlockPos movedPos = pos.subtract(offset);
				legoPositions.add(movedPos);
			}
			movedLego.setLegoBlocks(legoPositions);
			movedAssembly.add(movedLego);
		}
		return movedAssembly;
	}
	
	LegoAssembly mergeWithAssembly(LegoAssembly legoAssembly) {
		boolean mergesGrounded = this.isGrounded || (!this.isGrounded && !legoAssembly.isGrounded);
		if(mergesGrounded) {
			if(!this.isGrounded && legoAssembly.rootPiece.getOriginBlockPos().getY() < this.rootPiece.getOriginBlockPos().getY()) {
				return legoAssembly.mergeWithAssembly(this);
			} else {
				//HashSet<Lego> freedLegosTmp = new HashSet(this.freedLegos); // In case we merge two sub-assemblies in the same step with a third sub-assembly
				for(Lego lego : legoAssembly.assemblyLegos) {
					addLego(lego);
				}
				//this.freedLegos = freedLegosTmp;
				this.freedLegos.addAll(legoAssembly.movedAssemblyLegos);
				//this.addedAssemblyLegos = Collections.EMPTY_SET;
				return this;
			}
		} else {
			return legoAssembly.mergeWithAssembly(this);
		}
	}
	
	Collection<Lego> getFreedLegos() {
		return this.freedLegos;
	}
	
	Collection<Lego> pullFreedLegos() {
		Set<Lego> freedLegosTmp = this.freedLegos;
		this.freedLegos = new HashSet<>();
		return freedLegosTmp;
	}
	
	// Only contains newly added legos - these are also contained in movedAssemblyLegos
	Collection<Lego> getAddedLegos() {
		return this.addedAssemblyLegos;
	}
	
	Collection<Lego> pullAddedLegos() {
		Set<Lego> addedLegosTmp = this.addedAssemblyLegos;
		this.addedAssemblyLegos = new HashSet<>();
		return addedLegosTmp;
	}
	
	Collection<Lego> getMovedLegos() {
		return this.movedAssemblyLegos;
	}
	
	 Vec3i getAssemblyOrigin() {
		return assemblyOrigin;
	}
	 
	 boolean isGrounded() {
		 return this.isGrounded;
	 }
	
}
