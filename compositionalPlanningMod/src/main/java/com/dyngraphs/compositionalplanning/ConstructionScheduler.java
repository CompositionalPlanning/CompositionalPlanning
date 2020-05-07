package com.dyngraphs.compositionalplanning;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class ConstructionScheduler {
	private List<SnapDescription> constructionSchedule = new ArrayList<>();
	private int MAX_WORKERS = 16;//Integer.MAX_VALUE;
	private Set<Integer> groundPartIds = new HashSet<>();
	
	private Set<Integer> builtParts = new HashSet<>();
	private Set<Set<Integer>> builtAssemblies = new HashSet<>();
	
	ConstructionScheduler(int maxWorkers) {
		MAX_WORKERS = maxWorkers;
	}

	void loadSchedule(String schedulePath) throws FileNotFoundException {
		JsonReader reader = new JsonReader(new FileReader(schedulePath));
		loadSchedule(reader);	
	}
	
	void loadSchedule(Reader scheduleReader) throws FileNotFoundException {
		JsonReader reader = new JsonReader(scheduleReader);
		loadSchedule(reader);	
	}

	private void loadSchedule(JsonReader reader) {
		Gson gson = new Gson();
		List<List<List<List<Integer>>>> schedule = gson.fromJson(reader, new TypeToken<List<List<List<List<Integer>>>>>() {}.getType());
		for(List<List<List<Integer>>> snapDescription : schedule) {
			SnapDescription snap = new SnapDescription(snapDescription.get(1), snapDescription.get(2).get(0));
			constructionSchedule.add(snap);
		}
	}
	
	List<SnapDescription> pullCurrentConstructionElement() {
		List<Integer> pulledSnapIds = new ArrayList<>();
		int usedWorkers = 0;
		for(int i = 0; i < constructionSchedule.size(); i++) {
			if(usedWorkers >= MAX_WORKERS) {	
				break;
			}
			SnapDescription snap = constructionSchedule.get(i);
			boolean isAssemblySnap = (builtAssemblies.contains(new HashSet<>(snap.getSecondInputs())) && builtAssemblies.contains(new HashSet<>(snap.getFirstInputs())));
			boolean isPrimitiveSnap = (builtParts.containsAll(snap.getSecondInputs()) || builtParts.containsAll(snap.getFirstInputs())) && (snap.getSecondInputs().size() == 1 || snap.getFirstInputs().size() == 1);
			boolean isInitSnap = Collections.disjoint(builtParts, snap.getInputs()) && snap.getInputs().size() == 2;

			if(isAssemblySnap || isPrimitiveSnap || isInitSnap) {
				pulledSnapIds.add(i);
				usedWorkers++;
			}
		}
		List<Integer> constructionParts = new ArrayList<>();
		List<SnapDescription> currentSnaps = new ArrayList<>();
		for(int i = pulledSnapIds.size()-1; i >= 0; i--) {
			int snapId = pulledSnapIds.get(i);
			SnapDescription snap = constructionSchedule.get(snapId);
			boolean isInitSnap = Collections.disjoint(builtParts, snap.getInputs()) && snap.getInputs().size() == 2;
			boolean isGroundSnap = isInitSnap && (groundPartIds.contains(snap.getFirstInputs().get(0)) || groundPartIds.contains(snap.getSecondInputs().get(0)));
				if(isInitSnap && !isGroundSnap) {
					constructionParts.addAll(snap.getFirstInputs());
					currentSnaps.add(snap);
				} else {
					List<Integer> notAddedElements = snap.getInputs().stream()
							.filter((elementId) -> !builtParts.contains(elementId)).collect(Collectors.toList());
					constructionParts.addAll(notAddedElements);
					currentSnaps.add(constructionSchedule.remove(snapId));
					builtAssemblies.add(new HashSet<>(snap.getOutputs()));
				}
			}
		builtParts.addAll(constructionParts);
		return currentSnaps;
	}
	
	boolean scheduleIsFinished() {
		return this.constructionSchedule.isEmpty();
	}
	
	int getMaximumNumberOfWorkers() {
		return this.MAX_WORKERS;
	}
	
	void setGroundParts(Collection<Integer> partIds) {
		this.groundPartIds = new HashSet<>(partIds);
	}
	
	
}
