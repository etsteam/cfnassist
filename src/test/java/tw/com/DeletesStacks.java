package tw.com;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tw.com.entity.StackNameAndId;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;

public class DeletesStacks {
	private static final int TIMEOUT_INCREMENT_MS = 500;
	private static final int INITIAL_TIMEOUT_MS = 1000;

	private static final Logger logger = LoggerFactory.getLogger(DeletesStacks.class);

	private AmazonCloudFormationClient cfnClient;
	private LinkedList<String> deleteIfPresent;
	private LinkedList<String> deleteIfPresentNonBlocking;

	public DeletesStacks(AmazonCloudFormationClient cfnClient) {
		this.cfnClient = cfnClient;
		deleteIfPresent = new LinkedList<String>();
		deleteIfPresentNonBlocking = new LinkedList<String>();
	}

	public DeletesStacks ifPresent(String stackName) {
		deleteIfPresent.add(stackName);
		return this;
	}
	
	public DeletesStacks ifPresent(StackNameAndId stackId) {
		return ifPresent(stackId.getStackName());	
	}
	
	public DeletesStacks ifPresentNonBlocking(String stackName) {
		deleteIfPresentNonBlocking.add(stackName);
		return this;
	}

	public void act() {
		List<String> currentStacks = fetchCurrentStacks();
		List<String> deletionList = new LinkedList<String>();
		List<String> deletionListNonBlocking = new LinkedList<String>();
		for(String stackName : currentStacks) {
			if (deleteIfPresent.contains(stackName)) {
				deletionList.add(stackName);
			} else if (deleteIfPresentNonBlocking.contains(stackName)) {
				deletionListNonBlocking.add(stackName);
			}
		}
		try {
			if (deletionList.isEmpty() && deletionListNonBlocking.isEmpty()) {
				logger.info("No stacks need deleting");
			} else {
				requestDeletion(deletionListNonBlocking);
				deleteStacks(deletionList);
			}
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
			logger.error(e.getStackTrace().toString());	
		}
	}

	private List<String> fetchCurrentStacks() {
		List<String> current = new LinkedList<String>();

		DescribeStacksResult result = cfnClient.describeStacks();
		for(Stack stack : result.getStacks()) {
			current.add(stack.getStackName());
		}
		return current;
	}

	private void deleteStacks(List<String> deletionList) throws InterruptedException {
		if (deletionList.isEmpty()) {
			return;
		}
		
		requestDeletion(deletionList);
		
		List<String> present = fetchCurrentStacks();
		int count = EnvironmentSetupForTests.DELETE_RETRY_LIMIT;
		long timeout = INITIAL_TIMEOUT_MS;
		while (containsAnyOf(present,deletionList) && (count>0)) {
			logger.info(String.format("Waiting for stack deletion (check %s/%s). Present: %s Deleting: %s", 
					(EnvironmentSetupForTests.DELETE_RETRY_LIMIT-count), EnvironmentSetupForTests.DELETE_RETRY_LIMIT, 
					present.size(), deletionList.size()));
			Thread.sleep(timeout);
			count--;
			if (timeout<EnvironmentSetupForTests.DELETE_RETRY_MAX_TIMEOUT_MS) {
				timeout = timeout + TIMEOUT_INCREMENT_MS;
			}
			present = fetchCurrentStacks();
		}
		if (count==0) {
			logger.warn("Timed out waiting for deletions");
		} else {
			logger.info("Deleted stacks");
		}
		for(String unwanted : deletionList) {
			if (present.contains(unwanted)) {
				logger.error("Stack was not deleted yet: " + unwanted);
			}
		}	
	}

	private void requestDeletion(List<String> deletionList) {
		for(String stackName : deletionList) {
			DeleteStackRequest request = new DeleteStackRequest();
			request.setStackName(stackName);
			cfnClient.deleteStack(request);
			logger.info("Requested deletion of " + stackName);
		}
	}

	private boolean containsAnyOf(List<String> present,
			List<String> deletionList) {
		for(String unwanted : deletionList) {
			if (present.contains(unwanted)) {
				return true;
			}
		}
		return false;
	}


}
