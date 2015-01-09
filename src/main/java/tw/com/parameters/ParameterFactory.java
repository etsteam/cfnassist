package tw.com.parameters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tw.com.entity.ProjectAndEnv;
import tw.com.exceptions.CannotFindVpcException;
import tw.com.exceptions.InvalidParameterException;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.TemplateParameter;

public class ParameterFactory {
	private static final Logger logger = LoggerFactory.getLogger(ParameterFactory.class);

	private List<String> reservedParameters;
	private List<PopulatesParameters> populators;

	public ParameterFactory(List<PopulatesParameters> populators) {
		this.populators = populators;
		reservedParameters = new LinkedList<String>();
		reservedParameters.add(PopulatesParameters.PARAMETER_ENV);
		reservedParameters.add(PopulatesParameters.PARAMETER_VPC);
		reservedParameters.add(PopulatesParameters.PARAMETER_BUILD_NUMBER);
	}

	public Collection<Parameter> createRequiredParameters(ProjectAndEnv projAndEnv, Collection<Parameter> userParameters, List<TemplateParameter> declaredParameters)
			throws InvalidParameterException, FileNotFoundException,
			IOException, CannotFindVpcException {
		
		Collection<Parameter> result  = new LinkedList<Parameter>();
		result.addAll(userParameters);
		
		checkNoClashWithBuiltInParameters(result);
		for(PopulatesParameters populator : populators) {
			populator.addParameters(result, declaredParameters, projAndEnv);
		}
		
		logAllParameters(result, declaredParameters);
		return result;
	}
	
	private void logAllParameters(Collection<Parameter> parameters, List<TemplateParameter> declaredParameters) {
		logger.info("Invoking with following parameters");
		for(Parameter param : parameters) {
			if (noEchoIsSet(param, declaredParameters)) {
				logger.info(String.format("Parameter key='%s' value=<NoEchoIsSet>", param.getParameterKey()));				
			}
			else {
				logger.info(String.format("Parameter key='%s' value='%s'", param.getParameterKey(), param.getParameterValue()));
			}			
		}
	}

	private boolean noEchoIsSet(Parameter param,
			List<TemplateParameter> declaredParameters) {
		for(TemplateParameter declared : declaredParameters) {
			if (declared.getParameterKey().equals(param.getParameterKey())) {
				if (declared.getNoEcho()==null) {
					return false; 
				}
				return declared.getNoEcho();
			}
		}
		return false;
	}

	private void checkNoClashWithBuiltInParameters(Collection<Parameter> parameters) throws InvalidParameterException {
		for(Parameter param : parameters) {
			String parameterKey = param.getParameterKey();
			if (reservedParameters.contains(parameterKey)) {
				logger.error("Attempt to overide built in and autoset parameter called " + parameterKey);
				throw new InvalidParameterException(parameterKey);
			}
		}	
	}

}
