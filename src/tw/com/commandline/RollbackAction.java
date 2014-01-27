package tw.com.commandline;

import java.util.Collection;

import org.apache.commons.cli.OptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.cloudformation.model.Parameter;

import tw.com.AwsFacade;
import tw.com.ProjectAndEnv;
import tw.com.exceptions.CannotFindVpcException;
import tw.com.exceptions.InvalidParameterException;

public class RollbackAction extends SharedAction {
	private static final Logger logger = LoggerFactory.getLogger(RollbackAction.class);

	@SuppressWarnings("static-access")
	public RollbackAction() {
		option = OptionBuilder.withArgName("rollback").hasArg().
					withDescription("Warning: Rollback all current deltas and reset index accordingly").create("rollback");
	}

	public void invoke(AwsFacade aws, ProjectAndEnv projectAndEnv, String folder, Collection<Parameter> unused) throws InvalidParameterException, CannotFindVpcException {
		logger.info("Invoking rollback for " + projectAndEnv);
		aws.rollbackTemplatesInFolder(folder, projectAndEnv);
	}

}