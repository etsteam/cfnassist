package tw.com;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tw.com.exceptions.StackCreateFailed;
import tw.com.exceptions.WrongNumberOfStacksException;

import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;

public class PollingStackMonitor implements MonitorStackEvents {
	private static final Logger logger = LoggerFactory.getLogger(PollingStackMonitor.class);
	private CfnRepository cfnRepository;
	
	public PollingStackMonitor(CfnRepository cfnRepository) {
		this.cfnRepository = cfnRepository;
	}

	@Override
	public String waitForCreateFinished(String stackName) throws WrongNumberOfStacksException, InterruptedException, StackCreateFailed {	 
		String result = cfnRepository.waitForStatusToChangeFrom(stackName, StackStatus.CREATE_IN_PROGRESS);
		if (!result.equals(StackStatus.CREATE_COMPLETE.toString())) {
			logger.error(String.format("Failed to create stack %s, status is %s", stackName, result));
			logStackEvents(cfnRepository.getStackEvents(stackName));
			throw new StackCreateFailed(stackName);
		}
		return result;
	}
	
	public String waitForDeleteFinished(String stackName) throws WrongNumberOfStacksException, InterruptedException {
		StackStatus requiredStatus = StackStatus.DELETE_IN_PROGRESS;
		String result = StackStatus.DELETE_FAILED.toString();
		try {
			result = cfnRepository.waitForStatusToChangeFrom(stackName, requiredStatus);
		}
		catch(com.amazonaws.AmazonServiceException awsException) {
			String errorCode = awsException.getErrorCode();
			if (errorCode.equals("ValidationError")) {
				result = StackStatus.DELETE_COMPLETE.toString();
			} else {
				result = StackStatus.DELETE_FAILED.toString();
			}		
		}	
		
		if (!result.equals(StackStatus.DELETE_COMPLETE.toString())) {
			logger.error("Failed to delete stack, status is " + result);
			logStackEvents(cfnRepository.getStackEvents(stackName));
		}
		return result;
	}

	
	private void logStackEvents(List<StackEvent> stackEvents) {
		for(StackEvent event : stackEvents) {
			logger.info(event.toString());
		}	
	}

}