package org.syncany.tests.cli;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;

public class WatchCommandTest {	
	@Test
	public void testWatchCommand() throws Exception {
		final Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		final Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		final Map<String, String> clientB = TestCliUtil.createLocalTestEnvAndConnect("B", connectionSettings);

		Thread clientThreadA = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new CommandLineClient(new String[] { 
						 "--localdir", clientA.get("localdir"),
						 "watch",
						 "--interval", "1"						 
					}).start();
				} 				
				catch (Exception e) {					
					System.out.println("Interrupted.");
				}
			}			
		});
		
		// Client A: Start 'watch'
		clientThreadA.start();
		
		// Client B: New file and up
		TestFileUtil.createRandomFile(new File(clientB.get("localdir")+"/file1"), 20*1024);
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientB.get("localdir"),
			 "up"				 
		}).start();
		
		// Client A: Wait for client A to sync it
		Thread.sleep(2000); 
		assertFileEquals(new File(clientB.get("localdir")+"/file1"), new File(clientA.get("localdir")+"/file1"));
		assertFileListEquals(new File(clientB.get("localdir")), new File(clientA.get("localdir")));
		
		// Client A: New file, wait for it to sync it
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file2"), 20*1024);
		Thread.sleep(2000); 
		assertTrue(new File(clientB.get("repopath")+"/db-A-1").exists());
		
		clientThreadA.interrupt();		
		
		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}	
}