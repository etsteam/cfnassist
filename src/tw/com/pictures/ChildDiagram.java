package tw.com.pictures;

import tw.com.exceptions.CfnAssistException;
import tw.com.pictures.dot.Recorder;

public interface ChildDiagram {

	void addInstance(String instanceId, String label) throws CfnAssistException;

	void render(Recorder recorder);

	void addRouteTable(String routeTableId, String label) throws CfnAssistException;

	String getId();

}