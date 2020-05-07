package com.dyngraphs.compositionalplanning;

import java.io.File;
import java.io.Reader;
import java.util.HashMap;

import org.jgrapht.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.io.*;
import org.jgrapht.io.GraphMLExporter.*;
import org.jgrapht.util.*;

public class GraphMLLoader {
	
	public Graph<LegoVertex, DefaultWeightedEdge> loadGraphML(String fileLocation) throws ImportException {
		Graph<LegoVertex, DefaultWeightedEdge> graph = new DefaultDirectedGraph(DefaultEdge.class);
		
		GraphMLImporter<LegoVertex, DefaultWeightedEdge> importer = getLEgoImporter();
		importer.importGraph(graph, new File(fileLocation));
		return graph;
	}
	
	public Graph<LegoVertex, DefaultWeightedEdge> loadGraphML(Reader fileReader) throws ImportException {
		Graph<LegoVertex, DefaultWeightedEdge> graph = new DefaultDirectedGraph(DefaultEdge.class);
		
		GraphMLImporter<LegoVertex, DefaultWeightedEdge> importer = getLEgoImporter();
		importer.importGraph(graph, fileReader);
		return graph;
	}

	private GraphMLImporter<LegoVertex, DefaultWeightedEdge> getLEgoImporter() {
		VertexProvider<LegoVertex> vertexProvider = (id, attributes) -> {
			if(attributes.get("type").getValue().equals("Ground")) {
				return new LegoVertex(
						Integer.parseInt(id), 
						Integer.parseInt(attributes.get("x").getValue()),
						Integer.parseInt(attributes.get("y").getValue()),
						Integer.parseInt(attributes.get("z").getValue()),
						attributes.get("type").getValue()
						);
			} else {
				return new LegoVertex(
						Integer.parseInt(id), 
						Integer.parseInt(attributes.get("x").getValue()),
						Integer.parseInt(attributes.get("y").getValue()),
						Integer.parseInt(attributes.get("z").getValue()),
						Integer.parseInt(attributes.get("rotation").getValue()),
						Integer.parseInt(attributes.get("height").getValue()),
						Integer.parseInt(attributes.get("length").getValue()),
						Integer.parseInt(attributes.get("width").getValue()),
						attributes.get("ref").getValue(),
						attributes.get("type").getValue(),
						attributes.get("color").getValue());
			}
		};
		EdgeProvider<LegoVertex, DefaultWeightedEdge> edgeProvider = (from, to, label, attributes) -> new DefaultWeightedEdge();
		GraphMLImporter<LegoVertex, DefaultWeightedEdge> importer = new GraphMLImporter(vertexProvider, edgeProvider);
		return importer;
	}
	
	
}
