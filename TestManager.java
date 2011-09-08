package edu.umich.keyword2;

import edu.umich.keyword2.KeywordMethods;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.List;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import org.apache.commons.io.FileUtils;
import org.testng.collections.Lists;


public class TestManager {
	
	private static WebDriver driver;
	private static String application;

	// This method reads the parameters file, creates the logging directory, and reads all tests into
	// an array.  For each test, it starts the browser and calls testRunner with the test to run.
	// After the test is run, it makes an entry to the master log file.
	public static void main(String[] args) throws Exception {
	
		// This prints the user directory out to the console and the java window
		// Useful for understanding where the parameters file needs to be.
		System.out.println("Your test properties file should be located here: " + System.getProperty("user.dir"));
		
		// This next section confirms that the parameters file exists,
		// then opens and reads it.
		File file = new File("parameters.txt");
		
		if (!file.exists()) {
			throw new Exception("Parameters file does not exist here: " + System.getProperty("user.dir"));
		}
		
		Properties propReader = new Properties();
		FileInputStream propertiesStream = new FileInputStream("parameters.txt");
		
		// Read our properties, with defaults.
		propReader.load(propertiesStream);
		String browser = propReader.getProperty("browser", "firefox");
		String chromeExecutable = propReader.getProperty("chromeExecutable", "none");
		String fileDirectory = propReader.getProperty("fileDirectory");
		String downloadDirectory = propReader.getProperty("downloadDirectory");
		String logDirectory = propReader.getProperty("logDirectory");
		String mimeTypes = propReader.getProperty("mimeTypes");		
		String OS = propReader.getProperty("os", "mac");
		String testListFile = propReader.getProperty("testList", null);
		String variablesPath = propReader.getProperty("variablesFile", null);
		String xpathFile = propReader.getProperty("xpathFile");
		
		// Close the properties file
		propertiesStream.close();
		
 		// Check that the user has ended the path to their test scripts with a slash, add one if they haven't put it there.
		if (!fileDirectory.endsWith("/")) {
			fileDirectory = fileDirectory.concat("/");
			}		
		
 		// Check that the user has ended the path to their test scripts with a slash, add one if they haven't put it there.
		if (!logDirectory.endsWith("/")) {
			logDirectory = logDirectory.concat("/");
			}
			
		//format of the output date for log directory
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss");
		//get current date time with Date()
		Date date = new Date();
		// Formatted date to create unique sub-directory
		String dateForLogDir = dateFormat.format(date);
		// class-level variable will house all logs from this test run.		
		logDirectory = logDirectory.concat(dateForLogDir + "/");
		// Make the sub-directory
		new File(logDirectory).mkdirs();

		// These variables will hold each line from the input directory.
		// and hold the placement of the log directory
		String testName, testScript, testResult;
		String[] testScriptPath; 
		
		// These variables will hold the overall stats.
		int passedTests = 0;
		int failedTests = 0;


		// This will be the reference to our master log file,
		// which houses the results of all of our tests.
		BufferedWriter reportLogPointer = new BufferedWriter(new FileWriter(logDirectory + "report.txt"));		
		
		List<String> testList = Lists.newArrayList();
		
		// Read the tests from the master file if it exists.
		if (testListFile==null) {
			reportLogPointer.write("fail: you have not specified a master test list in your parameters file, which is located here: " + System.getProperty("user.dir"));
		}
			
		// Create the variables to read the test files
		// test files have csv extension, but are pipe delimited.
		FileInputStream testListFileStream = new FileInputStream(testListFile);
		BufferedReader testListFilePointer = new BufferedReader(new InputStreamReader(testListFileStream));
		String line = null;
		
		// Read the next line while there is a line to read and the last command did NOT fail.
		while ((line = testListFilePointer.readLine()) != null) {
			if (!line.isEmpty()) {
				testList.add(line);
			}
		}
		
		// How many tests do we need to run?
		int numTests = testList.size();
		
		if (numTests == 0) {
			reportLogPointer.write("Error: there are no scripts in your script path.");
			reportLogPointer.close();
			return;
		}		
		
		// Run each test file in the input directory. 
		for (int a = 0; a < numTests; a++) {
			
			// Start the browser with the proper parameters.
			driver = startDriver(browser, downloadDirectory, mimeTypes, chromeExecutable);
			
			// Our tests come from the master test list, specified in parameters.
			testScript = testList.get(a); //.substring(0, testName.length() - 4);
			testScriptPath = testScript.split("/");
			testName = testScriptPath[(testScriptPath.length - 1)];

			// Actually run the test
			testResult = testRunner(OS, fileDirectory, testScript, logDirectory, testName, variablesPath, xpathFile);
			// Shutdown the browser
			driver.close();
			
			if (testResult.equals("pass")) {
				passedTests++;
			}
			else {
				failedTests++;
			}
			// Write test results out to our master log file.
			reportLogPointer.write(testResult + "," + testName + "\r\n");
			}
		
		reportLogPointer.write("\r\n" + passedTests + " tests passed\r\n");
		reportLogPointer.write(failedTests + " tests failed");
		// Close the master report after all the tests are finished.
		reportLogPointer.close();
	}
	
	private static WebDriver startDriver(String browser, String downloadDirectory, String mimeTypes, String chromeExecutable) throws Exception {
		
			if (browser.contains("firefox")){
				prepFirefoxProfile(downloadDirectory, mimeTypes);
			} else if (browser.contains("explore")){
				driver = new InternetExplorerDriver();
			} else if (browser.contains("google")){
				prepChromeProfile(downloadDirectory, mimeTypes, chromeExecutable);
			}

			return driver;
	}

	// This method reads the test file line by line and calls commandRunner with the command to run.
	private static String testRunner (String OS, String filePath, String testScript, String logDir, String testName, String variablePath, String xpathFile) throws Exception {
		
		// Reset the application back to null, so the user can set it in a script.
		application = "";
		
		// Create a log file for receiving the results of the commands as we process them.
		BufferedWriter logPointer = new BufferedWriter(new FileWriter(logDir + testName + ".log"));
		
		// Create the variables to read the test files
		// test files have csv extension, but are pipe delimited.
		FileInputStream fis1 = new FileInputStream(testScript);	
			
		if (fis1.available()==0) {
			return "fail: " + testName + " does not exist at the provided path.  Please check your test list.";
		}
		
		BufferedReader scriptPointer = new BufferedReader(new InputStreamReader(fis1));
	
		// scriptLine holds single line from a test script.
		// testStatus holds the status of a test.
		String scriptLine, testStatus = "";	
		
		// Read the next line while there is a line to read and the last command did NOT fail.
		while (((scriptLine = scriptPointer.readLine()) != null) && (!testStatus.equals("fail"))) {
		
			// If the line is blank, write blank lines to log to keep in sync with script.
			// Otherwise, execute the line.
			if ( scriptLine.trim().length() == 0 ) {
				logPointer.write("\r\n"); 
			}
			else { 
				testStatus = commandRunner(OS, filePath, logDir, testName, scriptPointer, logPointer, scriptLine, variablePath, xpathFile);
			}
		}
		
		//Close logging file at the end successful test run.
	    logPointer.close();
		
		return testStatus;	
		}

	// This receives the command to call from testRunner, makes the appropriate call to selenium and logs
	// the results in the script-level log file.
	protected static String commandRunner (String OS, String filePath, String logDirectory, String testName, 
			BufferedReader scriptPointer, BufferedWriter logPointer, String scriptLine, String variablesPath, String xpathFile) throws Exception {
	
		//Assume there are no variables unless explicitly stated.
		boolean hasVariables = false;
		
		String testStatus = "";
		// Split the script line into it's constituent commands.
	    String[] vars = scriptLine.split("\\|");
	
	    // Get the number of commands and check for variables.
	    // Ignore the command if it is a comment.
	    int numVariables = vars.length;		
		
	    for (int i=0; i < numVariables; i++) {
	    	if ((vars[i].startsWith("$")) && (!vars[0].equalsIgnoreCase("comment"))) {
	    		hasVariables = true;
	    		break;
	    	}
	    }
	    
	    // If there are variables in the command line, lookup them up from the variables file and throw an error
	    // if the variables file or variable itself doesn't exist.
	    if (hasVariables) {
	    	
			if (variablesPath==null) {
				logPointer.write("Fail: Your script uses variables, but you didn't declare a variables file in your parameters file.");				
				return "fail";
			}	    	
	    	
			// This next section confirms that the variables file exists, then opens and reads it.
			File variablesFile = new File(variablesPath);
			
			if (!variablesFile.exists()) {
				logPointer.write("Fail: Your variables file: " + variablesPath + " does not exist.");				
				return "fail";
			}
			
			//Define the variables streams and load the variables.
			Properties variablesReader = new Properties();
			FileInputStream variablesStream = new FileInputStream(variablesPath);
			variablesReader.load(variablesStream);
	    	
			// Replace the command with a value from the variables file if it exists.
		    for (int i=0; i < numVariables; i++) {
		    	if (vars[i].startsWith("$")) {
		    		if (!variablesReader.containsKey(vars[i].substring(1))) {
		    			logPointer.write("Fail: There is no variable named " + vars[i].substring(1));
		    			return "fail";
		    		} else {
		    			vars[i] = variablesReader.getProperty(vars[i].substring(1));
		    		}
		    	}
		    }
	    	
		    //Close the stream to the variables.
	    	variablesStream.close();
	    }
	    
	    if (vars[0].equalsIgnoreCase("addtext") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.addText(driver, application, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("addtext") && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.addText(driver, application, xpathFile, vars[1], vars[2], vars[3]);
	    	}
	    else if (vars[0].equalsIgnoreCase("application")) { 
	    	application = vars[1];
	    	testStatus = "pass";
	    	}	
	    else if (vars[0].equalsIgnoreCase("capturetext")) { 
	    	testStatus = KeywordMethods.captureText(driver, application, xpathFile, variablesPath, vars[1], vars[2], vars[3]);
	    	}		    
	    else if (vars[0].equalsIgnoreCase("click") && (vars.length == 2)) { 
	    	testStatus = KeywordMethods.click(driver, application, xpathFile, vars[1]);
	    	}	
	    else if (vars[0].equalsIgnoreCase("click") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.click(driver, application, xpathFile, vars[1], vars[2]);
	    	}			 	    
	    else if (vars[0].equalsIgnoreCase("clickxpath")) { 
	    	testStatus = KeywordMethods.clickXPath(driver, vars[1]);
	    	}
	    else if (vars[0].equalsIgnoreCase("closePopUp")) {
	    	testStatus = KeywordMethods.closePopUp (driver);
	    }
	    else if (vars[0].equalsIgnoreCase("comment")) {
	    	// this is done to include comments in the log file.
	    	testStatus = "comment";
	    	}		    
	    else if (vars[0].equalsIgnoreCase("entertext") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.enterText(driver, application, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("entertext") && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.enterText(driver, application, xpathFile, vars[1], vars[2], vars[3]);
	    	}	    
	    else if (vars[0].equalsIgnoreCase("fckEnter")) {
	    	testStatus = KeywordMethods.fckEnter(driver, application, xpathFile, vars[1], vars[2]);
	    }
	    else if (vars[0].equalsIgnoreCase("loopwhile")) { 
	    	testStatus = KeywordMethods.loopWhile(vars[1], OS, filePath, logDirectory, testName, scriptPointer, logPointer, variablesPath, xpathFile);
	    	}
	    else if (vars[0].equalsIgnoreCase("mceEnter") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.mceEnter(driver, application, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("mceEnter") && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.mceEnter(driver, application, xpathFile, vars[1], vars[2], vars[3]);
	    	}	    	    
	    else if (vars[0].equalsIgnoreCase("openurl")) {
   			testStatus = KeywordMethods.openUrl(driver, vars[1]);
	    	}	
	    else if (vars[0].equalsIgnoreCase("selectcheckbox")) { 
	    	testStatus = KeywordMethods.selectCheckbox(driver, application, xpathFile, vars[1]);
	    	}		    
	    else if (vars[0].equalsIgnoreCase("selectlist")) { 
	    	testStatus = KeywordMethods.selectList(driver, application, xpathFile, vars[1],vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("selectradiobutton") || vars[0].equalsIgnoreCase("selectradio")) { 
	    	testStatus = KeywordMethods.selectRadio(driver, application, xpathFile, vars[1]);
	    	}	
	    else if (vars[0].equalsIgnoreCase("selectwindow")) { 
	    	testStatus = KeywordMethods.selectWindow(driver, vars[1]);
	    	}
	    else if (vars[0].equalsIgnoreCase("setvariable")) {
	    	testStatus = KeywordMethods.setVariable(variablesPath, vars[1],vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("uploadfile")) { 
	    	testStatus = KeywordMethods.uploadFile(driver, application, xpathFile, vars[1], filePath, OS);
	    	}			    
	    else if (vars[0].equalsIgnoreCase("verifyfile")) { 
	    	if (vars.length != 3) {
	    		logPointer.write("Usage: verifyFile|fileToVerify|textToVerifyInFile\r\n");
	    		testStatus = "fail: incorrect method call.";
	    	} else {
	    		testStatus = KeywordMethods.verifyFile(driver, application, xpathFile, vars[1], vars[2]);
	    	  }
	    	}	
	    else if (vars[0].equalsIgnoreCase("verifytext")) { 
	    	testStatus = KeywordMethods.verifyText(driver, application, xpathFile, vars[1]);
	    	}
	    else if (vars[0].equalsIgnoreCase("verifytextnotpresent")) { 
	    	testStatus = KeywordMethods.verifyTextNotPresent(driver, application, xpathFile, vars[1]);
	    	}	    
	    else if ((vars[0].equalsIgnoreCase("verifyvalue")) && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.verifyValue(driver, application, xpathFile, vars[1],vars[2]);
	    	}
	    else if ((vars[0].equalsIgnoreCase("verifyvalue")) && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.verifyValue(driver, application, xpathFile, vars[1],vars[2],vars[3]);
	    	}	    
	    else if ((vars[0].equalsIgnoreCase("waitforpopup"))) { 
	    	testStatus = KeywordMethods.waitForPopUp(driver, vars[1]);
	    	}	    
	    else {
	    	testStatus = "fail: " + vars[0] + " is not a valid method.";
	    }		    	
	    
	    // Logging action status to log file.		    
	    if (testStatus.equals("pass")) {
		    logPointer.write(testStatus);
		    for(int i=0; i < vars.length; i++) {
		    	logPointer.write("," + vars[i]); 
		    	}
		    logPointer.write("\r\n");
	    	}
	    else if (testStatus.equals("comment")) {
	
	    	logPointer.write("comment," + vars[1] + "\r\n"); 
	
	    	}
	    else if (testStatus.startsWith("fail", 0)) {
	
	    	// Take screen shot if the script fails.
		    String screenshotFile = logDirectory + testName + ".png";
		    File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		    FileUtils.copyFile(scrFile, new File(screenshotFile)); 
	    	
	    	logPointer.write("fail");
	    	
		    for(int i=0; i < vars.length; i++) {
		    	logPointer.write("," + vars[i]); 
		    	}
		    logPointer.write("\r\n");
	
	    	// Print additional text when test status contains it.
		    if (testStatus.length() > 5) {
		    	logPointer.write(testStatus.substring(5));		    
		    }
			//Close logging file as the test has failed.  
		    logPointer.close();
		    return "fail";
	    	}		    
		    	    		    
		return "pass";	
		}
	
	private static void prepFirefoxProfile (String downloadDirectory, String mimeTypes)  throws Exception {
		
		FirefoxProfile profile = new FirefoxProfile(); 

		profile.setPreference("browser.download.folderList", 2);
		profile.setPreference("browser.download.dir", downloadDirectory); 
		
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", mimeTypes);

		driver = new FirefoxDriver(profile); 
        }	
	
	private static void prepChromeProfile (String downloadDirectory, String mimeTypes, String driverPath)  throws Exception {
		
		// Find the chrome driver path in the user's parameter file.
		System.setProperty("webdriver.chrome.driver", driverPath);

		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		//capabilities.setCapability("chrome.switches", Arrays.asList("--user-download-dir=" + downloadDirectory));
		capabilities.setCapability("chrome.switches", Arrays.asList("--download.prompt_for_download=false"));

		driver = new ChromeDriver(capabilities);
		}
	
	private static void prepIEProfile (String downloadDirectory, String mimeTypes)  throws Exception {
		
		DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		//capabilities.setCapability("chrome.switches", Arrays.asList("--user-download-dir=" + downloadDirectory));
		capabilities.setCapability("chrome.switches", Arrays.asList("--download.prompt_for_download=false"));

		driver = new ChromeDriver(capabilities);
		}		
	
}