package edu.umich.keyword;

import edu.umich.keyword.KeywordMethods;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.List;
import java.util.Scanner;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import org.apache.commons.io.FileUtils;
import org.testng.collections.Lists;


public class TestManager {
	
	private static WebDriver driver;
	private static String application, browser, chromeExecutable, fileDirectory, downloadDirectory, logDirectory,
		mimeTypes, OS, testListFile, variablesPath, xpathFile;
	private static Integer timeout, minTimeout;
	private static Boolean timingData;

	// This method reads the parameters file, creates the logging directory, and reads all tests into
	// an array.  For each test, it starts the browser and calls testRunner with the test to run.
	// After the test is run, it makes an entry to the master log file.
	public static void main(String[] args) throws Exception {
	
		readProperties();
				
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

		// Make sure the download directory is not null, then empty it.
		if (downloadDirectory==null) {
			reportLogPointer.write("fail: you have not specified a download directory in your parameters file.");
			reportLogPointer.close();			
			return;
		}

		// Read the tests from the master file if it exists.
		if (testListFile==null) {
			reportLogPointer.write("fail: you have not specified a master test list in your parameters file, which is located here: " + System.getProperty("user.dir"));
			reportLogPointer.close();			
			return;
		}		
		
		List<String> testList = prepareScriptList(testListFile);
		
		String result = testList.get(testList.size() - 1); 
		
		if (result.startsWith("fail: ")) {
			reportLogPointer.write("fail: one of your test scripts does not exist.  Specifically, \r\n");
			reportLogPointer.write(result.substring(5));
			reportLogPointer.close();
			return;
		}
		
		// How many tests do we need to run?
		int numTests = testList.size();
		
		if (numTests == 0) {
			reportLogPointer.write("Error: there are no scripts in your script path.");
			reportLogPointer.close();
			return;
		}		

		// Start the browser with the proper parameters.
		driver = startDriver(browser, downloadDirectory, mimeTypes, chromeExecutable);		
		
		// Run each test file in the input directory. 
		for (int a = 0; a < numTests; a++) {
			
			// Our tests come from the master test list, specified in parameters.
			testScript = testList.get(a);
			
			//if(File.separator.equals("/")){
			if(testScript.contains("/")){
				testScriptPath = testScript.split("/");	
			} else {
				testScriptPath = testScript.split("\\\\");
			}
			
			testName = testScriptPath[(testScriptPath.length - 1)];

			try {
				// Actually run the test
				testResult = testRunner(testScript, OS, logDirectory, fileDirectory, downloadDirectory, testName, variablesPath, xpathFile);
			} catch (Exception e) {
				testResult = "fail," + e.getMessage();
			}
			
			if (testResult.equals("pass")) {
				passedTests++;
			} else {
				// If the test fails, it will frequently leave the browser in an unknown state.
				// So, shutdown the browser and restart in addition to incrementing the failed
				// tests counter.
				failedTests++;
				
				driver.quit();
				// Start the browser with the proper parameters.
				driver = startDriver(browser, downloadDirectory, mimeTypes, chromeExecutable);				
			}
			
			// Write test results out to our master log file.
			reportLogPointer.write(testResult + "," + testName + "\r\n");
		}
		
		// Shutdown the browser
		//driver.close();
		driver.quit();		
		
	    if (timingData) {
	    	calculateTimings(logDirectory);
	    }
		
		// Close the master report after all the tests are finished.
		reportLogPointer.close();

		// Write out the final results report.
		writeResultReport(passedTests, failedTests);
	}

	private static void writeResultReport(Integer passedTests, Integer failedTests) {
		
		FileWriter outputStream = null;
		String file = logDirectory + "report.txt";
		
		try {
            // Read the current file contents into a string
			String results = new Scanner(new File(file)).useDelimiter("\\Z").next();
			
            // open our file for output.
            outputStream = new FileWriter(file);			
			
			outputStream.write(passedTests + " tests passed\r\n");
			outputStream.write(failedTests + " tests failed\r\n\r\n");
			outputStream.write(results);

			outputStream.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void readProperties() throws Exception {
		
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
		String myProperties = readFile("parameters.txt");
		// This is necessary for windows.
		propReader.load(new StringReader(myProperties.replace("\\", "\\\\"))); 

		//Read our properties values, with default values.
		browser = propReader.getProperty("browser", "firefox");
		chromeExecutable = propReader.getProperty("chromeExecutable", "none");
		fileDirectory = propReader.getProperty("fileDirectory");
		downloadDirectory = propReader.getProperty("downloadDirectory");
		logDirectory = propReader.getProperty("logDirectory");
		mimeTypes = propReader.getProperty("mimeTypes");		
		OS = propReader.getProperty("os", "mac");
		testListFile = propReader.getProperty("testList", null);
		variablesPath = propReader.getProperty("variablesFile", null);
		xpathFile = propReader.getProperty("xpathFile");
		timeout = Integer.parseInt(propReader.getProperty("timeout", "10"));
		minTimeout = Integer.parseInt(propReader.getProperty("minTimeout", "3"));
		timingData = Boolean.parseBoolean(propReader.getProperty("timingData", "false"));
		
		// Check that the user has ended the path to their test scripts with a slash, add one if they haven't put it there.
		if (!fileDirectory.endsWith(File.separator)) {
			fileDirectory = fileDirectory.concat(File.separator);
			}		
		
 		// Check that the user has ended the path to their test scripts with a slash, add one if they haven't put it there.
		if (!logDirectory.endsWith(File.separator)) {
			logDirectory = logDirectory.concat(File.separator);
			}
		
		if (!downloadDirectory.endsWith(File.separator)) {
			downloadDirectory = downloadDirectory.concat(File.separator);
			}
		
	}

	
	private static String readFile(String path) throws IOException {
		  FileInputStream stream = new FileInputStream(new File(path));
		  try {
		    FileChannel fc = stream.getChannel();
		    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		    /* Instead of using default, pass in a decoder. */
		    return Charset.defaultCharset().decode(bb).toString();
		  }
		  finally {
		    stream.close();
		  }
		}
	

	private static List<String> prepareScriptList(String scriptList) throws Exception {

		List<String> testList = Lists.newArrayList();

		// Create the variables to read the test files
		// We will read the test list one line at a time and verify that the target file
		// exists before proceeding.
		FileInputStream testListFileStream = new FileInputStream(testListFile);
		BufferedReader testListFilePointer = new BufferedReader(new InputStreamReader(testListFileStream));
		String line = null;		
		
		try {
		
			// Read the contents of the test list into a list object,
			// then verify the scripts within the list exist before proceeding.
			while ((line = testListFilePointer.readLine()) != null) {
				if (!line.isEmpty()) {
					
					// read the target file from one line of the test list.
					FileInputStream stream = new FileInputStream(new File(line));
					
					// If the file has bytes--meaning that it exists--add it to the test list.
					if (stream.available() > 0) {
						testList.add(line);
					}
				}
			}
			
			return testList;
		
		} catch (FileNotFoundException e) {
			
			testList.add("fail: " + line + " does not exist.");
			return testList;
		} 
	}	
	
	
	private static WebDriver startDriver(String browser, String downloadDirectory, String mimeTypes, String chromeExecutable) throws Exception {
		
			if (browser.contains("firefox")){
				prepFirefoxProfile(downloadDirectory, mimeTypes);
			} else if (browser.contains("explore")){
				prepIEProfile(downloadDirectory, mimeTypes);
			} else if (browser.contains("chrome")){
				prepChromeProfile(downloadDirectory, mimeTypes, chromeExecutable);
			} else throw new Exception("You have entered an invalid browser string.  Must be either firefox, explorer or chrome.");
			
			return driver;
	}

	
	
	// This method reads the test file line by line and calls commandRunner with the command to run.
	private static String testRunner (String testScript, String OS, String logDirectory, String fileDirectory, 
			String downloadDirectory, String testName, String variablePath, String xpathFile) throws Exception {
		
		// Reset the application back to null, so the user can set it in a script.
		application = "";
		
		// Create a log file for receiving the results of the commands as we process them.
		BufferedWriter logPointer = new BufferedWriter(new FileWriter(logDirectory + testName + ".log"));
		
		// Create a log file for receiving the timing information from the click command as it is processed.
		BufferedWriter timingPointer = new BufferedWriter(new FileWriter(logDirectory + "timingData.log", true));		
		
		// Create the variables to read the test files
		// test files have csv extension, but are pipe delimited.
		FileInputStream fis1 = new FileInputStream(testScript);	
			
		BufferedReader scriptPointer = new BufferedReader(new InputStreamReader(fis1));
	
		// scriptLine holds single line from a test script.
		// testStatus holds the status of a test.
		String scriptLine, testStatus = "";	
		
		// Empty the downloads directory, so that previously download files won't be used by this script.
		File dir = new File(downloadDirectory);
	    String[] files = dir.list();
	    
	    if (files == null) {
	    	
	    } else {
	    
		    for (int i = 0; i < files.length; i++) {
		      
		    	File downloadFile = new File(downloadDirectory + files[i]);
		      
		    	if (downloadFile.isFile()) { // skip ., .., other directories too
		    		downloadFile.delete();
		    	}
		    }
	    }
	    
	    
		
		// Read the next line while there is a line to read and the last command did NOT fail.
		while (((scriptLine = scriptPointer.readLine()) != null) && (!testStatus.equals("fail"))) {
		
			// If the line is blank, write blank lines to log to keep in sync with script.
			// Otherwise, execute the line.
			if ( scriptLine.trim().length() == 0 ) {
				logPointer.write("\r\n"); 
			}
			else { 
				testStatus = commandRunner(scriptLine, OS, logDirectory, fileDirectory, downloadDirectory, testName, variablePath, xpathFile, scriptPointer, logPointer, timingPointer);
			}
		}
		
		//Close logging file at the end successful test run.
	    logPointer.close();
	    timingPointer.close();
		
		return testStatus;	
		}

	// This receives the command to call from testRunner, makes the appropriate call to selenium and logs
	// the results in the script-level log file.
	protected static String commandRunner (String scriptLine, String OS, String logDirectory, String fileDirectory, String downloadDirectory, 
			String testName, String variablePath, String xpathFile, BufferedReader scriptPointer, BufferedWriter logPointer, BufferedWriter timingPointer) throws Exception {
	
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
	    	
			if (variablePath==null) {
				logPointer.write("Fail: Your script uses variables, but you didn't declare a variables file in your parameters file.");				
				return "fail";
			}	    	
	    	
			// This next section confirms that the variables file exists, then opens and reads it.
			File variablesFile = new File(variablePath);
			
			if (!variablesFile.exists()) {
				logPointer.write("Fail: Your variables file: " + variablePath + " does not exist.");				
				return "fail";
			}
			
			//Define the variables streams and load the variables.
			Properties variablesReader = new Properties();
			FileInputStream variablesStream = new FileInputStream(variablePath);
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
	    	testStatus = KeywordMethods.addText(driver, application, timeout, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("addtext") && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.addText(driver, application, timeout, xpathFile, vars[1], vars[2], vars[3]);
	    	}
	    else if (vars[0].equalsIgnoreCase("application")) { 
	    	application = vars[1];
	    	testStatus = "pass";
	    	}	
	    else if (vars[0].equalsIgnoreCase("capturetext")) { 
	    	testStatus = KeywordMethods.captureText(driver, application, timeout, xpathFile, variablePath, vars[1], vars[2], vars[3]);
	    	}
	    else if (vars[0].equalsIgnoreCase("ckEnter")) {
	    	testStatus = KeywordMethods.ckEnter(driver, application, timeout, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("click") && (vars.length == 2)) { 
	    	testStatus = KeywordMethods.click(driver, application, timeout, xpathFile, timingData, timingPointer, vars[1]);
	    	}	
	    else if (vars[0].equalsIgnoreCase("click") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.click(driver, application, timeout, xpathFile, timingData, timingPointer, vars[1], vars[2]);
	    	}			 	    
	    else if (vars[0].equalsIgnoreCase("clickxpath")) { 
	    	testStatus = KeywordMethods.clickXPath(driver, vars[1]);
	    	}
	    else if (vars[0].equalsIgnoreCase("closePopUp")) {
	    	testStatus = KeywordMethods.closePopUp(driver);
	    	}
	    else if (vars[0].equalsIgnoreCase("comment")) {
	    	// this is done to include comments in the log file.
	    	testStatus = "comment";
	    	}	
	    else if (vars[0].equalsIgnoreCase("downloadFile") && (vars.length == 2)) { 
	    	testStatus = KeywordMethods.downloadFile(driver, application, timeout, downloadDirectory, xpathFile, vars[1]);
	    	}	    
	    else if (vars[0].equalsIgnoreCase("entertext") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.enterText(driver, application, timeout, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("entertext") && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.enterText(driver, application, timeout, xpathFile, vars[1], vars[2], vars[3]);
	    	}
	    else if (vars[0].equalsIgnoreCase("enteractionkeys")) { 
	    	testStatus = KeywordMethods.enterActionKeys(driver, vars[1]);
	    	}	    
	    else if (vars[0].equalsIgnoreCase("fckEnter")) {
	    	testStatus = KeywordMethods.fckEnter(driver, application, timeout, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("loopwhile")) { 
	    	testStatus = KeywordMethods.loopWhile(vars[1], OS, logDirectory, fileDirectory, downloadDirectory, testName, variablePath, xpathFile, scriptPointer, logPointer, timingPointer);
	    	}
	    else if (vars[0].equalsIgnoreCase("mceEnter") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.mceEnter(driver, application, timeout, xpathFile, vars[1], vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("mceEnter") && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.mceEnter(driver, application, timeout, xpathFile, vars[1], vars[2], vars[3]);
	    	}
	    else if (vars[0].equalsIgnoreCase("modalClick")) {
   			testStatus = KeywordMethods.modalClick(driver, vars[1], timeout);
	    	}		    
	    else if (vars[0].equalsIgnoreCase("openurl")) {
	    	testStatus = KeywordMethods.openUrl(driver, vars[1]);
	    	}	
	    else if (vars[0].equalsIgnoreCase("selectcheckbox") && (vars.length == 2)) { 
	    	testStatus = KeywordMethods.selectCheckbox(driver, application, timeout, xpathFile, vars[1]);
	    	}
	    else if (vars[0].equalsIgnoreCase("selectcheckbox") && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.selectCheckbox(driver, application, timeout, xpathFile, vars[1], vars[2]);
	    	}		    
	    else if (vars[0].equalsIgnoreCase("selectlist")) { 
	    	testStatus = KeywordMethods.selectList(driver, application, timeout, xpathFile, vars[1],vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("selectradiobutton") || vars[0].equalsIgnoreCase("selectradio")) { 
	    	testStatus = KeywordMethods.selectRadio(driver, application, timeout, xpathFile, vars[1]);
	    	}	
	    else if (vars[0].equalsIgnoreCase("selectwindow")) { 
	    	testStatus = KeywordMethods.selectWindow(driver, vars[1]);
	    	}
	    else if (vars[0].equalsIgnoreCase("setvariable")) {
	    	testStatus = KeywordMethods.setVariable(variablePath, vars[1],vars[2]);
	    	}
	    else if (vars[0].equalsIgnoreCase("uploadfile")) { 
	    	testStatus = KeywordMethods.uploadFile(driver, application, timeout, xpathFile, vars[1], fileDirectory, OS);
	    	}			    
	    else if (vars[0].equalsIgnoreCase("verifyfile")) { 
	    	if (vars.length != 2) {
	    		logPointer.write("Usage: verifyFile|fileToVerify|\r\n");
	    		testStatus = "fail: incorrect method call.";
	    	} else {
	    		testStatus = KeywordMethods.verifyFile(downloadDirectory, fileDirectory, vars[1], timeout);
	    	  }
	    	}
	    else if (vars[0].equalsIgnoreCase("verifyfileexists")) { 
	    	if (vars.length != 2) {
	    		logPointer.write("Usage: verifyFileExists|fileToVerify|\r\n");
	    		testStatus = "fail: incorrect method call.";
	    	} else {
	    		testStatus = KeywordMethods.verifyFileExists(downloadDirectory, vars[1], timeout);
	    	  }
	    	}	    
	    else if (vars[0].equalsIgnoreCase("verifytext")) { 
	    	testStatus = KeywordMethods.verifyText(driver, application, timeout, xpathFile, vars[1]);
	    	}
	    else if (vars[0].equalsIgnoreCase("verifytextnotpresent")) { 
	    	testStatus = KeywordMethods.verifyTextNotPresent(driver, application, minTimeout, xpathFile, vars[1]);
	    	}	    
	    else if (vars[0].equalsIgnoreCase("verifycontents")) { 
	    	testStatus = KeywordMethods.verifyContents(driver, application, timeout, xpathFile, vars[1],vars[2]);
	    	}
	    else if ((vars[0].equalsIgnoreCase("verifyvalue")) && (vars.length == 3)) { 
	    	testStatus = KeywordMethods.verifyValue(driver, application, timeout, xpathFile, vars[1],vars[2]);
	    	}
	    else if ((vars[0].equalsIgnoreCase("verifyvalue")) && (vars.length == 4)) { 
	    	testStatus = KeywordMethods.verifyValue(driver, application, timeout, xpathFile, vars[1],vars[2],vars[3]);
	    	}	    
	    else if ((vars[0].equalsIgnoreCase("wait"))) { 
	    	testStatus = KeywordMethods.wait(vars[1]);
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
	
	private static void calculateTimings(String logDirectory) throws Exception {

		String timingLine;
		Integer totalTiming = 0;
		Integer totalRows = 0;
		StringBuilder text = new StringBuilder();
		
		// Open and read the file containing all of the timing information.
		FileInputStream fis1 = new FileInputStream(logDirectory + "timingData.log");	
		BufferedReader timingReader = new BufferedReader(new InputStreamReader(fis1));
		
		// Read the timing file one line at a time.  Also keep track of the number of rows as we will need that to calculate an average.
		while ((timingLine = timingReader.readLine()) != null) {
			Integer time = Integer.parseInt(timingLine);
			totalTiming = totalTiming + time;
			totalRows ++;
			text.append(timingLine + "\n");
		}

		BufferedWriter timingWriter = new BufferedWriter(new FileWriter(logDirectory + "timingData.log", false));		
		
		// If we have collected some timing data, write it to the log file.
		if(totalRows > 0) {
			Integer averageTiming = totalTiming / totalRows;		
		
			timingWriter.write("Total wait time after clicks: " + totalTiming + "\n");
			timingWriter.write("Sample size: " + totalRows + "\n");
			timingWriter.write("Average time: " + averageTiming + "\n\n");
			
			timingWriter.write("Timing Samples:\n");
			timingWriter.write(text.toString());
			timingWriter.close(); 
		} else {
			timingWriter.write("You have collected no timing samples.\n");
			timingWriter.close(); 
		}		
	}
	
	private static void prepFirefoxProfile (String downloadDirectory, String mimeTypes)  throws Exception {
		
		FirefoxProfile profile = new FirefoxProfile(); 
		
		// This section tells firefox to download all files of the user specified
		// mimeTypes to the user specified downloadDirectory
		profile.setPreference("browser.download.folderList", 2);
		profile.setPreference("browser.download.dir", downloadDirectory); 
		
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", mimeTypes);
        
        // Attempt to disable annoying security messages on windows 
		profile.setPreference("network.cookie.cookieBehavior", 1);
		
		// Cannot set these. WebDriver freezes these by default.
		//profile.setPreference("security.warn_viewing_mixed", "false");
		//profile.setPreference("security.warn_viewing_mixed.show_once", "false");   

		driver = new FirefoxDriver(profile); 
        }	
	
	private static void prepChromeProfile (String downloadDirectory, String mimeTypes, String driverPath)  throws Exception {
				
		System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, driverPath);
		
		ChromeDriverService service = ChromeDriverService.createDefaultService();
		
		ChromeOptions options = new ChromeOptions();

		// The following option is ineffectual.  Workaround is to set the downloadDirectory
		// equal to the user's current download directory setting
		//options.addArguments("--download.default_directory=" + downloadDirectory);
		//options.addArguments("--download.prompt_for_download=false");
		//options.addArguments("--plugins.plugins_disabled=Chrome PDF Viewer");
		//options.addArguments("--plugins.plugins_disabled=C:/Program Files (x86)/Adobe/Reader 10.0/Reader/Browser/nppdf32.dll");
		//options.addArguments("--plugins.plugins_disabled=C:/Program Files (x86)/Google/Chrome/Application/19.0.1084.52/pdf.dll");		
		//options.addArguments("plugins_disabled=C:/Program Files (x86)/Google/Chrome/Application/19.0.1084.52/pdf.dll");		
		
		driver = new ChromeDriver(service, options);
		}
	
	private static void prepIEProfile (String downloadDirectory, String mimeTypes)  throws Exception {
		
        DesiredCapabilities ieCapabilities = DesiredCapabilities.internetExplorer();
        ieCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
		
		driver = new InternetExplorerDriver(ieCapabilities);
		}
	
	private static void prepSafariProfile (String downloadDirectory, String mimeTypes, String driverPath)  throws Exception {
		
		ChromeDriverService service = ChromeDriverService.createDefaultService();
		
		ChromeOptions options = new ChromeOptions();

		// The following option is ineffectual.  Workaround is to set the downloadDirectory
		// equal to the user's current download directory setting
		//options.addArguments("--download.default_directory=" + downloadDirectory);
		//options.addArguments("--download.prompt_for_download=false");
		//options.addArguments("--plugins.plugins_disabled=Chrome PDF Viewer");
		//options.addArguments("--plugins.plugins_disabled=C:/Program Files (x86)/Adobe/Reader 10.0/Reader/Browser/nppdf32.dll");
		//options.addArguments("--plugins.plugins_disabled=C:/Program Files (x86)/Google/Chrome/Application/19.0.1084.52/pdf.dll");		
		//options.addArguments("plugins_disabled=C:/Program Files (x86)/Google/Chrome/Application/19.0.1084.52/pdf.dll");		
		
		driver = new ChromeDriver(service, options);
		}
	
	
}