package tw.com.commandline;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.OptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.cloudformation.model.Parameter;

import tw.com.AwsFacade;
import tw.com.ProjectAndEnv;
import tw.com.StackCreateFailed;
import tw.com.exceptions.CannotFindVpcException;
import tw.com.exceptions.InvalidParameterException;
import tw.com.exceptions.WrongNumberOfStacksException;

public class DirAction extends SharedAction {
	private static final Logger logger = LoggerFactory.getLogger(DirAction.class);

	@SuppressWarnings("static-access")
	public DirAction() {
		option = OptionBuilder.withArgName("dir").hasArg().
				withDescription("The directory/folder containing delta templates to apply").create("dir");
	}

	public void invoke(AwsFacade aws, ProjectAndEnv projectAndEnv, String folderPath, Collection<Parameter> cfnParams) throws FileNotFoundException, InvalidParameterException, IOException, WrongNumberOfStacksException, InterruptedException, CannotFindVpcException, StackCreateFailed {
		ArrayList<String> stackNames = aws.applyTemplatesFromFolder(folderPath, projectAndEnv, cfnParams);
		logger.info(String.format("Created %s stacks", stackNames.size()));
		for(String name : stackNames) {
			logger.info("Created stack " +name);
		}
	}

}