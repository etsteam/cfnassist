package tw.com;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import tw.com.exceptions.CannotFindVpcException;
import tw.com.exceptions.CfnAssistException;
import tw.com.exceptions.InvalidParameterException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class TestExecuteScriptsInOrderFromDir {
	
	private static final String THIRD_FILE = "03createRoutes.json";
	Path srcFile = FileSystems.getDefault().getPath(EnvironmentSetupForTests.ORDERED_SCRIPTS_FOLDER, "holding", THIRD_FILE);
	Path destFile = FileSystems.getDefault().getPath(EnvironmentSetupForTests.ORDERED_SCRIPTS_FOLDER, THIRD_FILE);
	private static AmazonCloudFormationClient cfnClient;
	private static AmazonEC2Client ec2Client;
	private DeletesStacks deletesStacks;
	
	private static String env = EnvironmentSetupForTests.ENV;
	private static String proj = EnvironmentSetupForTests.PROJECT;
	private ProjectAndEnv mainProjectAndEnv = new ProjectAndEnv(proj,env);

	ArrayList<String> expectedList = new ArrayList<String>();
	private AwsFacade aws;
	private MonitorStackEvents monitor;
	
	@BeforeClass
	public static void beforeAllTestsOnce() {
		DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();
		ec2Client = EnvironmentSetupForTests.createEC2Client(credentialsProvider);
		cfnClient = EnvironmentSetupForTests.createCFNClient(credentialsProvider);		
	}
	
	@Rule public TestName test = new TestName();
	
	@Before 
	public void beforeAllTestsRun() throws IOException, CannotFindVpcException {
		createExpectedNames();	
		deletesStacks = new DeletesStacks(cfnClient).
				ifPresent("CfnAssistTest01createSubnet").
				ifPresent("CfnAssistTest02createAcls");
		deletesStacks.act();
		
		CfnRepository cfnRepository = new CfnRepository(cfnClient, EnvironmentSetupForTests.PROJECT);
		VpcRepository vpcRepository = new VpcRepository(ec2Client);
		
		monitor = new PollingStackMonitor(cfnRepository);	
		aws = new AwsFacade(monitor, cfnClient, cfnRepository, vpcRepository);
		aws.setCommentTag(test.getMethodName());
		
		Files.deleteIfExists(destFile);
		aws.resetDeltaIndex(mainProjectAndEnv);
	}
	
	@After
	public void afterAllTestsHaveRun() throws IOException, CfnAssistException {	
		try {
			aws.rollbackTemplatesInFolder(EnvironmentSetupForTests.ORDERED_SCRIPTS_FOLDER, mainProjectAndEnv);
		} catch (InvalidParameterException e) {
			System.console().writer().write("Unable to properly rollback");
			e.printStackTrace();
		}
		aws.resetDeltaIndex(mainProjectAndEnv);
		deletesStacks.act();
		Files.deleteIfExists(destFile);
	}

	@Test
	public void shouldCreateTheStacksRequiredOnly() throws CfnAssistException, InterruptedException, FileNotFoundException, InvalidParameterException, IOException {
		List<StackId> stackIds = aws.applyTemplatesFromFolder(EnvironmentSetupForTests.ORDERED_SCRIPTS_FOLDER, mainProjectAndEnv);
		
		assertEquals(expectedList.size(), stackIds.size());
		
		for(int i=0; i<expectedList.size(); i++) {
			StackId stackId = stackIds.get(i);
			assertEquals(expectedList.get(i), stackId.getStackName());
			// TODO should just be a call to get current status because applyTemplatesFromFolder is a blocking call
			String status = monitor.waitForCreateFinished(stackId);
			assertEquals(StackStatus.CREATE_COMPLETE.toString(), status);
		}
		
		// we are up to date, should not apply the files again
		stackIds = aws.applyTemplatesFromFolder(EnvironmentSetupForTests.ORDERED_SCRIPTS_FOLDER, mainProjectAndEnv);
		assertEquals(0, stackIds.size());
		
		// copy in extra files to dir
		FileUtils.copyFile(srcFile.toFile(), destFile.toFile());
		stackIds = aws.applyTemplatesFromFolder(EnvironmentSetupForTests.ORDERED_SCRIPTS_FOLDER, mainProjectAndEnv);
		assertEquals(1, stackIds.size());
		
		expectedList.add(proj+env+"03createRoutes");
		assertEquals(expectedList.get(2), stackIds.get(0).getStackName());
		
		List<String> deletedStacks = aws.rollbackTemplatesInFolder(EnvironmentSetupForTests.ORDERED_SCRIPTS_FOLDER, mainProjectAndEnv);
		assertEquals(3, deletedStacks.size());
		assert(deletedStacks.containsAll(expectedList));
		
		int finalIndex = aws.getDeltaIndex(mainProjectAndEnv);
		assertEquals(0, finalIndex);
	}

	private void createExpectedNames() {
		expectedList.add(proj+env+"01createSubnet");
		expectedList.add(proj+env+"02createAcls");
	}

}
