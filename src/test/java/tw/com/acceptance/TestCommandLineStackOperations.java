package tw.com.acceptance;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;

import tw.com.CLIArgBuilder;
import tw.com.DeletesStacks;
import tw.com.EnvironmentSetupForTests;
import tw.com.FilesForTesting;
import tw.com.commandline.Main;
import tw.com.entity.ProjectAndEnv;
import tw.com.exceptions.CannotFindVpcException;
import tw.com.providers.CloudClient;
import tw.com.repository.VpcRepository;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Vpc;

public class TestCommandLineStackOperations {

	private Vpc altEnvVPC;
	private VpcRepository vpcRepository;
	private ProjectAndEnv altProjectAndEnv;
	private static AmazonEC2Client ec2Client;
	private static AmazonCloudFormationClient cfnClient;
	private DeletesStacks deletesStacks;
	
	@BeforeClass
	public static void beforeAllTestsRun() {
		DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();
		ec2Client = EnvironmentSetupForTests.createEC2Client(credentialsProvider);
		cfnClient = EnvironmentSetupForTests.createCFNClient(credentialsProvider);	
	}
	
	@Rule public TestName test = new TestName();
	String testName = "";
	
	@Rule
    public Timeout globalTimeout = new Timeout(180*1000);
	
	@Before
	public void beforeEveryTestRun() {
		vpcRepository = new VpcRepository(new CloudClient(ec2Client));
		altProjectAndEnv = EnvironmentSetupForTests.getAltProjectAndEnv();
		EnvironmentSetupForTests.getMainProjectAndEnv();
		
		altEnvVPC = EnvironmentSetupForTests.findAltVpc(vpcRepository);	
		deletesStacks = new DeletesStacks(cfnClient);
		deletesStacks.ifPresent(EnvironmentSetupForTests.TEMPORARY_STACK)
			.ifPresent("CfnAssistTest01createSubnet")
			.ifPresent("CfnAssistTest02createAcls")
			.ifPresent("CfnAssistTestsimpleStack")
			.ifPresent("CfnAssistTestsubnet");
		deletesStacks.act();
		testName = test.getMethodName();
	}
	
	@After
	public void afterEachTestIsRun() {
		deletesStacks.act();
	}
	
	@Test
	public void testInvokeViaCommandLineDeployWithFile() throws InterruptedException, TimeoutException {
		String[] args = CLIArgBuilder.createSimpleStack(testName);

		Main main = new Main(args);
		int result = main.parse();
		deletesStacks.ifPresent("CfnAssistTestsimpleStack");
		assertEquals(0,result);
	}
		
	@Test
	public void testInvokeViaCommandLineDeployWithFileAndBuildNumber() throws InterruptedException, TimeoutException {
		String[] args = CLIArgBuilder.createSimpleStackWithBuildNumber(testName, "876");
		Main main = new Main(args);
		int result = main.parse();
		deletesStacks.ifPresent("CfnAssist876TestsimpleStack");
		assertEquals(0,result);
	}
	
	@Test
	public void testListStacks() throws InterruptedException, TimeoutException {
        PrintStream origStream = System.out;

		String[] create = CLIArgBuilder.createSimpleStack(testName);
		Main main = new Main(create);
		int status = main.parse();
		
		String[] list = CLIArgBuilder.listStacks();
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		PrintStream output = new PrintStream(stream);
		System.setOut(output);
		
		main = new Main(list);
		status = main.parse();

		System.setOut(origStream);
	
		CLIArgBuilder.checkForExpectedLine("CfnAssistTestsimpleStack", "CfnAssist", "Test", stream);

		deletesStacks.ifPresent("CfnAssistTestsimpleStack");
		assertEquals(0, status);
	}
	
	@Test
	public void testInvokeDiagramGenViaCLI() {
		String[] create = CLIArgBuilder.createDiagrams("./diagrams");
		Main main = new Main(create);
		int status = main.parse();
		assertEquals(0, status);
	}
	
	@Test
	public void testDeleteViaCommandLineDeployWithFileAndBuildNumber() throws InterruptedException, TimeoutException {
		String buildNumber = "0915";
		
		String[] createArgs = CLIArgBuilder.createSimpleStackWithBuildNumber(testName, buildNumber);
		Main main = new Main(createArgs);
		int createResult = main.parse();
		assertEquals(0,createResult);
		
		String[] deleteArgs = CLIArgBuilder.deleteSimpleStackWithBuildNumber(buildNumber);
		main = new Main(deleteArgs);
		int deleteResult = main.parse();
		assertEquals(0,deleteResult);
		
		deletesStacks.ifPresent("CfnAssistTestsimpleStack");
	}
	
	@Test
	public void testDeleteViaCommandLineDeployWithFile() throws InterruptedException, TimeoutException {
		String[] createArgs = CLIArgBuilder.createSimpleStack(testName);
		Main main = new Main(createArgs);
		int createResult = main.parse();
		assertEquals(0,createResult);
		
		String[] deleteArgs = CLIArgBuilder.deleteSimpleStack();
		main = new Main(deleteArgs);
		int deleteResult = main.parse();
		assertEquals(0,deleteResult);
		
		deletesStacks.ifPresent("CfnAssistTestsimpleStack");
	}
	
	@Test
	public void testInvokeViaCommandLineDeploySwitchELBInstances() throws InterruptedException, TimeoutException {		
		String buildNumber = "876";
		String typeTag = "web";
		
		String[] createIns = { 
				"-env", EnvironmentSetupForTests.ENV, 
				"-project", EnvironmentSetupForTests.PROJECT, 
				"-region", EnvironmentSetupForTests.getRegion().toString(),
				"-build", buildNumber,
				"-file", FilesForTesting.ELB_AND_INSTANCE,
				"-comment", testName
				};
		Main main = new Main(createIns);
		main.parse();
				
		String[] args = CLIArgBuilder.updateELB(typeTag, buildNumber);
		main = new Main(args);
		int result = main.parse();
		
		deletesStacks.ifPresent("CfnAssist876TestelbAndInstance");
		assertEquals(0,result);
	}
	
	@Test
	public void testInvokeViaCommandLineDeployWithFileAndSNS() throws InterruptedException, TimeoutException {
		String[] args = CLIArgBuilder.createSimpleStackWithSNS(testName);
		Main main = new Main(args);
		int result = main.parse();
		deletesStacks.ifPresent("CfnAssistTestsimpleStack");
		assertEquals(0,result);
	}
	
	@Test
	public void testUpdateViaCommandLineDeployWithFileAndSNS() throws InterruptedException, TimeoutException {
		String[] create = CLIArgBuilder.createSubnetStack(testName); // no sns
		Main main = new Main(create);
		int result = main.parse();
		
		String[] update = CLIArgBuilder.updateSimpleStack(testName, "-sns");
		main = new Main(update);
		result = main.parse();
		
		deletesStacks.ifPresent("CfnAssistTestsubnet");
		assertEquals(0,result);
	}
	
	@Test
	public void testInvokeViaCommandLineDeployWithFilePassedInParam() throws InterruptedException, TimeoutException {
		String[] args = CLIArgBuilder.createSubnetStackWithParams(testName);
		Main main = new Main(args);
		int result = main.parse();
		deletesStacks.ifPresent("CfnAssistTestsubnetWithParam");
		assertEquals(0,result);
	}
	
	@Test
	public void testInvokeViaCommandLineDeployWholeDirAndThenRollback() throws CannotFindVpcException, InterruptedException, TimeoutException {
		ProjectAndEnv projAndEnv = new ProjectAndEnv(EnvironmentSetupForTests.PROJECT, EnvironmentSetupForTests.ENV);		
		invokeForDirAndThenRollback(projAndEnv, "", FilesForTesting.ORDERED_SCRIPTS_FOLDER);
	}
	
	@Test
	public void testInvokeViaCommandLineDeployWholeDirAndThenRollbackWithSNS() throws CannotFindVpcException, InterruptedException, TimeoutException {
		ProjectAndEnv projAndEnv = new ProjectAndEnv(EnvironmentSetupForTests.PROJECT, EnvironmentSetupForTests.ENV);		
		invokeForDirAndThenRollback(projAndEnv, "-sns", FilesForTesting.ORDERED_SCRIPTS_FOLDER);
	}
	
	@Test
	public void testInvokeViaCommandLineDeployWholeDirDeltasAndThenRollback() throws CannotFindVpcException, InterruptedException, TimeoutException {
		ProjectAndEnv projAndEnv = new ProjectAndEnv(EnvironmentSetupForTests.PROJECT, EnvironmentSetupForTests.ENV);		
		invokeForDirAndThenRollback(projAndEnv, "", FilesForTesting.ORDERED_SCRIPTS_WITH_DELTAS_FOLDER);
	}
	
	@Test
	public void testInvokeViaCommandLineDeployWholeDirDeltasAndThenRollbackWithSNS() throws CannotFindVpcException, InterruptedException, TimeoutException {
		ProjectAndEnv projAndEnv = new ProjectAndEnv(EnvironmentSetupForTests.PROJECT, EnvironmentSetupForTests.ENV);		
		invokeForDirAndThenRollback(projAndEnv, "-sns", FilesForTesting.ORDERED_SCRIPTS_WITH_DELTAS_FOLDER);
	}


	private void invokeForDirAndThenRollback(ProjectAndEnv projAndEnv,
			String sns, String orderedScriptsFolder) throws CannotFindVpcException {
		vpcRepository.setVpcIndexTag(projAndEnv, "0");
		
		String[] argsDeploy = CLIArgBuilder.deployFromDir(orderedScriptsFolder, sns, testName);
		Main main = new Main(argsDeploy);
		int result = main.parse();
		assertEquals("deploy failed",0,result);
		
		String[] rollbackDeploy = CLIArgBuilder.rollbackFromDir(orderedScriptsFolder, sns);
		main = new Main(rollbackDeploy);
		result = main.parse();
		
		//clean up as needed
		vpcRepository.initAllTags(altEnvVPC.getVpcId(), altProjectAndEnv);
		cfnClient.setRegion(EnvironmentSetupForTests.getRegion());
		
		// check
		assertEquals("rollback failed",0,result);
	}
	
	@Ignore("cant find way to label at existing stack via apis")
	@Test
	public void testInvokeViaCommandLineTagExistingStack() throws IOException {
		EnvironmentSetupForTests.createTemporarySimpleStack(cfnClient, altEnvVPC.getVpcId(),"");
		
		String[] argslabelStack = {
				"-env", EnvironmentSetupForTests.ENV,
				"-project", EnvironmentSetupForTests.PROJECT,
				"-region", EnvironmentSetupForTests.getRegion().toString(),
				"-labelstack", "todoNotWorking"
		};
		Main main = new Main(argslabelStack);
		int result = main.parse();	
		deletesStacks.ifPresent("CfnAssistTestsimpleStack");		
		assertEquals(0, result);
	}

}
