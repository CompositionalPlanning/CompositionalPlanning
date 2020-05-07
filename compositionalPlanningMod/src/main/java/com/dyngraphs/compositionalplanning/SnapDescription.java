package com.dyngraphs.compositionalplanning;

import java.util.List;
import java.util.stream.Collectors;

class SnapDescription {
	
	
	private List<List<Integer>> inputs;
	private List<Integer> outputs;

	SnapDescription(List<List<Integer>> inputs, List<Integer> outputs) {
		this.inputs = inputs;
		this.outputs = outputs;
	}
	
	List<Integer> getInputs() {
		List<Integer> newList = inputs.stream()
		        .flatMap(List::stream)
		        .collect(Collectors.toList());
		return newList;
		
	}
	
	List<Integer> getFirstInputs() {
		return inputs.get(0);
	}
	
	List<Integer> getSecondInputs() {
		return inputs.get(1);
	}
	
	List<Integer> getOutputs() {
		return this.outputs;
	}
	
	boolean isPartSnap() {
		return inputs.get(0).size() == 1 || inputs.get(1).size() == 1; 
	}
	
	boolean isAssemblySnap() {
		return inputs.get(0).size() > 1 && inputs.get(1).size() > 1; 
	}
	

}
