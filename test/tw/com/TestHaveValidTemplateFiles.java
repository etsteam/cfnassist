package tw.com;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class TestHaveValidTemplateFiles {

	private AWSCredentialsProvider credentialsProvider;
	
	@Before
	public void beforeTestsRun() {
		credentialsProvider = new DefaultAWSCredentialsProviderChain();
	}
	
	@Test
	public void testAllTestCfnFilesAreValid() throws FileNotFoundException, IOException {
		AmazonCloudFormationClient cfnClient = EnvironmentSetupForTests.createCFNClient(credentialsProvider);
		AmazonEC2Client ec2Client = EnvironmentSetupForTests.createEC2Client(credentialsProvider);
		
		CfnRepository cfnRepository = new CfnRepository(cfnClient, EnvironmentSetupForTests.PROJECT);
		VpcRepository vpcRepository = new VpcRepository(ec2Client);
		
		PollingStackMonitor monitor = new PollingStackMonitor(cfnRepository);	
		AwsFacade aws = new AwsFacade(monitor, cfnClient, cfnRepository, vpcRepository);
	
		File folder = new File("src/cfnScripts");
		
		assertTrue(folder.exists());	
		validateFolder(aws, folder);
	}

	private void validateFolder(AwsProvider facade, File folder)
			throws FileNotFoundException, IOException {
		File[] files = folder.listFiles();
		for(File file : files) {
			if (file.isDirectory()) {
				validateFolder(facade, file);
			} else
			{
				facade.validateTemplate(file);
			}
		}
	}

}
