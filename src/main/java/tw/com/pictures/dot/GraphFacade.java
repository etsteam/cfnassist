package tw.com.pictures.dot;

import tw.com.exceptions.CfnAssistException;
import tw.com.pictures.ChildDiagram;
import tw.com.pictures.Diagram;
import tw.com.pictures.HasDiagramId;

public class GraphFacade implements Diagram {
	public static final int SUBNET_TITLE_FONT_SIZE = 12;

	private Graph graph;
	private CommonElements commonElements;
	
	public GraphFacade() {
		graph = new Graph();
		commonElements = new CommonDiagramElements(graph);
	}
	
	@Override
	public ChildDiagram createSubDiagram(String uniqueId, String label) throws CfnAssistException {
		SubGraph cluster = graph.createDiagramCluster(uniqueId, label, SUBNET_TITLE_FONT_SIZE);
		cluster.addNode(uniqueId).makeInvisible();
		ChildDiagram child = new SubGraphFacade(cluster);
		return child;
	}

	@Override
	public void addTitle(String title) {
		graph.addTitle(title);	
	}

	@Override
	public void render(Recorder recorder) {
		graph.render(recorder);	
	}

	@Override
	public void addRouteTable(String routeTableId, String label) throws CfnAssistException {
		graph.addNode(routeTableId).addLabel(label);		
	}

	@Override
	public void addConnectionBetween(String uniqueIdA, String uniqueIdB) {
		graph.addEdge(uniqueIdA, uniqueIdB);
	}
	
	@Override
	public void associate(String uniqueIdA, String uniqueIdB) {
		graph.addEdge(uniqueIdA, uniqueIdB).withDottedLine().withDot();	
	}

	@Override
	public void addPublicIPAddress(String uniqueId, String label) throws CfnAssistException {
		graph.addNode(uniqueId).withShape(Shape.Diamond).withLabel(label);	
	}

	@Override
	public void addLoadBalancer(String uniqueId, String label) throws CfnAssistException {
		graph.addNode(uniqueId).withShape(Shape.InvHouse).withLabel(label);	
	}
	

	private void addConnectionFromSubDiagram(String target, String start,
			HasDiagramId childDigram, String edgeLabel, boolean blocked) throws CfnAssistException {
		Edge edge = graph.addEdge(start, target).beginsAt(childDigram.getIdAsString()).withLabel(edgeLabel);	
		if (blocked) {
			edge.withBox().withColour(Colour.Red);
		}
	}
	
	private void addConnectionToSubDiagram(String start, String end, HasDiagramId childDigram, String label, boolean blocked) throws CfnAssistException {
		Edge edge = graph.addEdge(start, end).endsAt(childDigram.getIdAsString()).withLabel(label);
		if (blocked) {
			edge.withBox().withColour(Colour.Red);
		}
	}
	
	@Override
	public void addConnectionFromSubDiagram(String start, String end,
			HasDiagramId childDigram, String label) throws CfnAssistException {
		addConnectionFromSubDiagram(start, end, childDigram, label, false);	
	}

	@Override
	public void addConnectionToSubDiagram(String start, String end,
			HasDiagramId childDigram, String label) throws CfnAssistException {
		addConnectionToSubDiagram(start, end, childDigram, label, false);	
	}
	
	@Override
	public void addBlockedConnectionFromSubDiagram(String start, String end,
			HasDiagramId childDigram, String label) throws CfnAssistException {
		addConnectionFromSubDiagram(start, end, childDigram, label, true);	
	}

	@Override
	public void addBlockedConnectionToSubDiagram(String start, String end,
			HasDiagramId childDigram, String label) throws CfnAssistException {
		addConnectionToSubDiagram(start, end, childDigram, label, true);
		
	}

	@Override
	public void associateWithSubDiagram(String begin, String end, HasDiagramId childDiagram) {
		graph.addEdge(begin, end).withDot().endsAt(childDiagram.getIdAsString()).withDottedLine();		
	}

	@Override
	public void addDBInstance(String uniqueId, String label) throws CfnAssistException {
		graph.addNode(uniqueId).withShape(Shape.Octogon).withLabel(label);	
	}

	@Override
	public void addACL(String uniqueId, String label) throws CfnAssistException {
		graph.addNode(uniqueId).withLabel(label).withShape(Shape.Box);		
	}

	@Override
	public void addCidr(String uniqueId, String label) throws CfnAssistException {
		graph.addNode(uniqueId).withLabel(label).withShape(Shape.Diamond);	
	}

	@Override
	public void addSecurityGroup(String id, String label) throws CfnAssistException {
		commonElements.addSecurityGroup(id, label);	
	}

	@Override
	public void addPortRange(String uniqueId, String label) throws CfnAssistException {
		commonElements.addPortRange(uniqueId, label);
		
	}

	@Override
	public void connectWithLabel(String uniqueIdA, String uniqueIdB, String label) {
		commonElements.connectWithLabel(uniqueIdA, uniqueIdB, label);
		
	}
}
