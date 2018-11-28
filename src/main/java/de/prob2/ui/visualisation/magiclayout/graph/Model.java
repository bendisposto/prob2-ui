package de.prob2.ui.visualisation.magiclayout.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Model {
	private Set<Vertex> vertices = new HashSet<>();
	private Set<Edge> edges = new HashSet<>();
	
	private Set<Vertex> addedVertices = new HashSet<>();
	private Set<Vertex> removedVertices = new HashSet<>();
	private Set<Edge> addedEdges = new HashSet<>();
	private Set<Edge> removedEdges = new HashSet<>();
	
	public void addVertex(Vertex vertex) {
		addedVertices.add(vertex);
		removedVertices.remove(vertex); // vertex should not be in added and removed set
		vertices.add(vertex);
	}
	
	public void removeVertex(Vertex vertex) {
		removedVertices.add(vertex);
		addedVertices.remove(vertex); // vertex should not be in added and removed set
		vertices.remove(vertex);
	}
	
	public void addEdge(Edge edge) {
		addedEdges.add(edge);
		removedEdges.remove(edge); // edge should not be in added and removed set
		edges.add(edge);
	}

	public void removeEdge(Edge edge) {
		removedEdges.add(edge);
		addedEdges.remove(edge); // edge should not be in added and removed set
		edges.remove(edge);
	}
	
	public Set<Vertex> getVertices() {
		return Collections.unmodifiableSet(vertices);
	}
	
	public Set<Vertex> getAddedVertices() {
		return Collections.unmodifiableSet(addedVertices);
	}
	
	public Set<Vertex> getRemovedVertices() {
		return Collections.unmodifiableSet(removedVertices);
	}
	
	public Set<Edge> getEdges() {
		return Collections.unmodifiableSet(edges);
	}
	
	public Set<Edge> getAddedEdges() {
		return Collections.unmodifiableSet(addedEdges);
	}
	
	public Set<Edge> getRemovedEdges() {
		return Collections.unmodifiableSet(removedEdges);
	}
	
	void finishUpdate() {
		addedVertices.clear();
		removedVertices.clear();
		addedEdges.clear();
		removedVertices.clear();
	}
}
