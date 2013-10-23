package tw.com;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.TemplateParameter;

public interface AwsProvider {
	
	List<TemplateParameter> validateTemplate(File file) throws FileNotFoundException, IOException;
	String applyTemplate(File file, String env) throws FileNotFoundException, IOException, 
	InvalidParameterException;
	String applyTemplate(File file, String stackName,  Collection<Parameter> parameters) throws FileNotFoundException, IOException, 
		InvalidParameterException;
	
	void deleteStack(String stackName);
	
	String waitForDeleteFinished(String stackName) throws WrongNumberOfStacksException, InterruptedException;
	String waitForCreateFinished(String stackName) throws WrongNumberOfStacksException, InterruptedException;
	String createStackName(File templateFile, String env);
	List<Parameter> fetchParametersFor(File file, String env) throws FileNotFoundException, IOException, InvalidParameterException;
}
