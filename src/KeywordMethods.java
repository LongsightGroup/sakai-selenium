package edu.umich.keyword;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.FileUtils;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.Select;

public class KeywordMethods {
	
	// enter text into a text field
	// use: enterText|object|text where object is the id for the text field and 
	// text is the string to be entered.		
	protected static String addText (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {

		String textToEnter = object[1];
		String iteration = "1";
		if (object.length > 2) 
			iteration = object[2];	

		HashMap<String, String> paramHash = new HashMap<String, String>();
		
		try {
			
			String [] elementTypes = {"textbox", "textarea"};
			
			paramHash.put("application", application);
	        paramHash.put("objectID", object[0]); 
	        paramHash.put("iteration", iteration);
	        paramHash.put("xpathFile", xpathFile);
	        
	        WebElement objectInQuestion = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
	        
			objectInQuestion.sendKeys(textToEnter);
			return "pass";
			
		} catch (Exception e) {
			return "fail: " + e.toString();
		}
		
	}
	
 	//captureText stores all text between leftText and rightText into a variable
	//use: captureText|leftText|rightText|varName where varName will be the file name
	//houses the variable.  Stored in the log file directory.
	protected static String captureText (WebDriver driver, String application, Integer timeout, String xpathFile, String variablesPath, String leftText, String rightText, String varName)  throws Exception {
		
		HashMap<String, String> paramHash = new HashMap<String, String>();
		
		try {
			
			String [] elementTypes = {"bodytext"};
			
			paramHash.put("application", application);
	        paramHash.put("objectID", leftText); 
	        paramHash.put("iteration", "1");
	        paramHash.put("xpathFile", xpathFile);
			
	        // The call to pathFinder should move us to the correct frame.  We don't need the element itself.
	        // So if this call doesn't throw an exception, we can execute the call to getPageSource.
	        WebElement frameWithText = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
			
			// Capture the text into a variable to be written to the variables file.
			String allText = frameWithText.getText(); //driver.getPageSource();
			String capturedText = StringUtils.substringBetween(allText, leftText, rightText).trim();
			
			//Define the variables stream, load the variables, and close the stream.
			Properties currentVariables = new Properties();
			FileInputStream curVarStream = new FileInputStream(variablesPath);
			currentVariables.load(curVarStream);
			curVarStream.close();

			//Add the new key to the properties hash.
	        currentVariables.setProperty(varName, capturedText);
		 
	        //Reload the properties file, save new values and close stream.
			FileOutputStream newVarStream = new FileOutputStream(variablesPath);		
			currentVariables.store(newVarStream, null);
			newVarStream.close();

			return "pass";
			
			} catch (Exception e) {
				return "fail: " + e.toString();
			}  
		}


	// Enter text to a ckEditor.  Is the iteration variable if their are multiple editors on the page.
	// use: ckEnter|text|iteration where text is the text string to be entered.  Iteration is optional		
    protected static String ckEnter (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object) throws Exception {
    	
			try {
				
				String textToEnter = object[1];
				String iteration = "1";
				if (object.length > 2) 
					iteration = object[2];	

				HashMap<String, String> paramHash = new HashMap<String, String>();
				
				String [] elementTypes = {"ckeditor"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);
			
		        // This locates the sub-frame within which the fckEditor resides from the preceding label
				driver.switchTo().frame(ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes));
				
				driver.findElement(By.xpath("//body")).sendKeys(textToEnter);
				
				return "pass";

			} catch (Exception e) {	
				
				return "fail: " + e.toString();
				
			}
		}		
	
	
	// click clicks on an interface element.
	// use: click|object where object is the id, name or label of an element to be clicked on.
	protected static String click (WebDriver driver, String application, Integer timeout, String xpathFile, Boolean timingData, BufferedWriter timingPointer, String ... object)  throws Exception {
		
		String iteration = "1";
		if (object.length >= 2) 
			iteration = object[1];
		
		HashMap<String, String> paramHash = new HashMap<String, String>();
		
		try {
			
			String [] elementTypes = {"button", "anchor"};
			
			paramHash.put("application", application);
	        paramHash.put("objectID", object[0]); 
	        paramHash.put("iteration", iteration);
	        paramHash.put("xpathFile", xpathFile);
	        
	        long start = System.currentTimeMillis();

			ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes).click();

	        long end = System.currentTimeMillis();
	        long duration = end - start;
	        
	        if (timingData) {
	        	//System.out.println(duration);
	        	timingPointer.write(String.valueOf(duration) + "\n");
	        }
			
			return "pass";
			
			}  catch (Exception e) {
				return "fail: " + e.toString();
			}			
		}			

	// click clicks on an interface element.
	// use: click|xPath where xPath is the specific xPath to the element being clicked on.		
		protected static String clickXPath (WebDriver driver, String xPath)  throws Exception {
			
			try {
				
				driver.findElement(By.xpath(xPath)).click();
				return "pass";
				
				} catch (Exception e) {
					
						return "fail: " + e.toString();
				}
			
		}	
		
		//close a pop up window and return to the main window
		//use: closePopUp|
		protected static String closePopUp (WebDriver driver, String variablesPath) throws Exception {
			
			String origWindowHandle;
			
			try {
				
				//Define the variables stream, load the variables, and close the stream.
				Properties currentVariables = new Properties();
				FileInputStream curVarStream = new FileInputStream(variablesPath);
				currentVariables.load(curVarStream);

	    		if (!currentVariables.containsKey("origWindowHandle")) {
	    			return "fail: The original window handle was not recorded.";
	    		} else {
	    			origWindowHandle = currentVariables.getProperty("origWindowHandle");
	    		}
				
				curVarStream.close();

				driver.close();
				//String myWindow = driver.getWindowHandles().iterator().next();
				//driver.switchTo().window(myWindow);
				driver.switchTo().window(origWindowHandle);

				return "pass";
				
			} catch (Exception e) {
				
					return "fail: " + e.toString();
			}
			
			
		}
		
		// enter text into a text field
		// use: enterText|object|text where object is the id for the text field and 
		// text is the string to be entered.		
		protected static String enterText (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {

			String textToEnter = object[1];
			String iteration = "1";
			if (object.length > 2) 
				iteration = object[2];	

			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"textbox", "textarea"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);
		        
		        WebElement objectInQuestion = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
		        
				// This clears all text from the existing field before typing the new...
				objectInQuestion.sendKeys(Keys.chord(Keys.CONTROL, "a"), textToEnter);

				return "pass";
				
			} catch (Exception e) {
				return "fail: " + e.toString();
			}
			
		}	
		

		// Enter text to the first fckEditor on a page.
		// use: fckEnter|text where text is the text string to be entered.		
	    protected static String fckEnter (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object) throws Exception {
	    	
				try {
					
					String textToEnter = object[1];
					String iteration = "1";
					if (object.length > 2) 
						iteration = object[2];	
	
					HashMap<String, String> paramHash = new HashMap<String, String>();
					
					String [] elementTypes = {"fckeditor"};
					
					paramHash.put("application", application);
			        paramHash.put("objectID", object[0]); 
			        paramHash.put("iteration", iteration);
			        paramHash.put("xpathFile", xpathFile);
				
			        // This locates the sub-frame within which the fckEditor resides from the preceding label
					driver.switchTo().frame(ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes));
					
					WebElement fckFrame = null;
					
					// But we're not done there, as the text area resides in another nested sub-frame.
					// This sub-frame will sometimes take a long time to load on slow connections.
					for(int i=0; i<timeout; i++) {
						try {
							fckFrame = driver.findElement(By.id("xEditingArea")).findElement(By.xpath("//iframe"));
							
							if (fckFrame.isDisplayed()){
								break;
							}
						} catch (NoSuchElementException ex) {
							continue;
						}
					
					}
					//driver.switchTo().frame(driver.findElement(By.id("xEditingArea")).findElement(By.xpath("//iframe")));
					driver.switchTo().frame(fckFrame);
					
					driver.findElement(By.xpath("//body")).sendKeys(Keys.chord(Keys.CONTROL, "a"), textToEnter);
					
					return "pass";
	
				} catch (Exception e) {	
					
					return "fail: " + e.toString();
					
				}
			}		
		
		
		// Executes the following block of code x number of times as specified by the value of varName
		// use:
		// loopWhile|varName
		// loop|click|sample button name
		// loop|verifyText|sample text to verify
		// endloop		
		protected static String loopWhile (String varName, String OS, String logDirectory, String fileDirectory, String downloadDirectory, String testName, 
				String variablePath, String xpathFile, BufferedReader scriptPointer, BufferedWriter logPointer, BufferedWriter timingPointer)  throws Exception {
			
			try {
				
				String scriptLine; // will hold an individual line from the input file
				String testStatus; // Holds the return value from commandRunner sub-loop
				String[] vars; // array to the split contents of scriptLine.
				
				//Open the variables file and read how many times we will be looping
				//Define the variables stream, load the variables, and close the stream.
				Properties currentVariables = new Properties();
				FileInputStream curVarStream = new FileInputStream(variablePath);
				currentVariables.load(curVarStream);
				curVarStream.close();

				//Add the new key to the properties hash.
				String upperString = currentVariables.getProperty(varName);
		        
				// cast the variable from the file to an integer for use in the for loop later on
				int upperLimit = Integer.parseInt(upperString);
				
				// Read lines from the test script until they are no longer prefaced
				// with 'loop'. 
				scriptPointer.mark(65536);			
				
				for(int i = 0; i < upperLimit; i++) {
					
					// Reset the test script back to where the loop begins.
					scriptPointer.reset();
		
					scriptLine = scriptPointer.readLine();
				    vars = scriptLine.split("\\|");
					
					while (vars[0].equals("loop")) {
						
						//This will send the scriptLine on, minus the loop prefix.
						//which is 5 characters long.
						scriptLine = scriptLine.substring(5);
						
						testStatus = TestManager.commandRunner(scriptLine, OS, logDirectory, fileDirectory, downloadDirectory, testName, variablePath, xpathFile, scriptPointer, logPointer, timingPointer);
		
						if (testStatus.equalsIgnoreCase("fail")) {
							return "fail";
						}
											
						// Read lines from the test script until they are 
						// no longer prefaced with 'loop'. 
						scriptLine = scriptPointer.readLine();
					    vars = scriptLine.split("\\|");
					    
				    } 
				}
				
					return "pass";
				
				} catch (Exception e) {
					
						return "fail: " + e.toString();
				}
			
		}


		// enter text into an MCE Rich-Text Editor
		// use: mceEnter|object|text where object is the id for the text field and 
		// text is the string to be entered.  Optionally you can specify an iteration.
		protected static String mceEnter (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {

			String textToEnter = object[1];
			String iteration = "1";
			if (object.length > 2) 
				iteration = object[2];	

			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"mceeditor"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);
			
				// Find the frame the contains the mceEditor and switch to it. 
				WebElement enterPath = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
				
				driver.switchTo().frame(enterPath);

				// Either of these work.
				//driver.findElement(By.id("tinymce")).sendKeys(textToEnter);
				driver.findElement(By.className("mceContentBody")).sendKeys(textToEnter);

				return "pass";	
			
			} catch (Exception e) {
				return "fail: " + e.toString();
			}		
		}		
	    

		// Handle modal dialogs
		// use: modalClick|action where action is the desired button to push.
		protected static String modalClick (WebDriver driver, String action)  throws Exception {
			
			try {
				
				if(action.equalsIgnoreCase("ok")) {
					driver.switchTo().alert().accept();
					return "pass";
				} else if (action.equalsIgnoreCase("continue")) {
					driver.switchTo().alert().accept();
					return "pass";					
				} else {
					driver.switchTo().alert().dismiss();
					return "pass";
				}
			
			} catch (Exception e) {
				
				return "fail: " + e.toString();
			}
		}	
		

		// opens the url of the application to test.
		// use: openUrl|url
		protected static String openUrl (WebDriver driver, String variablesPath, String url)  throws Exception {
			
			try {
				
				// Open the targeted URL
				driver.get(url);
				
				String origWindowHandle = driver.getWindowHandle();
				
				//Define the variables stream, load the variables, and close the stream.
				Properties currentVariables = new Properties();
				FileInputStream curVarStream = new FileInputStream(variablesPath);
				currentVariables.load(curVarStream);
				curVarStream.close();

				//Add the new key to the properties hash.
		        currentVariables.setProperty("origWindowHandle" , origWindowHandle);
			 
		        //Reload the properties file, save new values and close stream.
				FileOutputStream newVarStream = new FileOutputStream(variablesPath);		
				currentVariables.store(newVarStream, null);
				newVarStream.close();
				
				return "pass";
				
				} catch (Exception e) {
					
						return "fail: " + e.toString();
				}
			
		}


		// Click within a given checkbox, identified either by the ID or label of the box.
		// Will either select or de-select, depending on the original value.
		// use: selectCheckbox|checkBox		
		protected static String selectCheckbox (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {
			
			String iteration = "1";
			if (object.length >= 2) 
				iteration = object[1];
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"checkbox"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);

				ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes).click();
				return "pass";
				
			}  catch (Exception e) {
				return "fail: " + e.toString();
			}			
		}
		
		
		// Select a given value from a drop-down list box, which is specified by id
		// use: selectList|IDofListBox|SelectedOption		
		protected static String selectList (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {
			
			String iteration = "1";
			if (object.length > 2) 
				iteration = object[2];
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"listbox"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);

		        WebElement myElement = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
		        
		        Select mySelect = new Select(myElement);
		        
		        mySelect.selectByVisibleText(object[1]);
		        
		    	return "pass";
		        
				}  catch (Exception e) {
					return "fail: " + e.toString();
				}			
			}
		
		
		// Select a radio button, which is specified by its label
		// use: selectRadio|labelOfRadio
		protected static String selectRadio (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {
			
			String iteration = "1";
			if (object.length >= 2) 
				iteration = object[1];
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"radio"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);

				ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes).click();
				return "pass";
				
				}  catch (Exception e) {
					return "fail: " + e.toString();
				}			
			}
		
		
		//select a given open window
		//use: selectWindow|windowIndex where windowIndex is 0 for original and 1 for a pop-up window. 
		protected static String selectWindow (WebDriver driver, String windowIndex) throws Exception {
			
			try {
				
				// The following code selects a window by index.  So the base (original) window
				// would be 0, and 1 would be for a popUp Window.
				Integer intWindowIndex = Integer.parseInt(windowIndex);
				//selenium.selectWindow(selenium.getAllWindowTitles()[intWindowIndex]);
				//selenium.windowFocus();
				Object[] handles  = driver.getWindowHandles().toArray();
				driver.switchTo().window(handles[intWindowIndex].toString());
				
				return "pass";
				
			}   catch (Exception e) {
				
					return "fail: " + e.toString();
			}
			
		}
		
		protected static String setVariable (String variablesPath, String variable, String value)  throws Exception {
			
			try {
				
				//Define the variables stream, load the variables, and close the stream.
				Properties currentVariables = new Properties();
				FileInputStream curVarStream = new FileInputStream(variablesPath);
				currentVariables.load(curVarStream);
				curVarStream.close();

				//Add the new key to the properties hash.
		        currentVariables.setProperty(variable, value);
			 
		        //Reload the properties file, save new values and close stream.
				FileOutputStream newVarStream = new FileOutputStream(variablesPath);		
				currentVariables.store(newVarStream, null);
				newVarStream.close();

				return "pass";
				
				} catch (Exception e) {
					return "fail: " + e.toString();
				}  
			}		

		
		// Within sakai, this will browse to a file within the default directory and upload it.
		// Should be followed by a waitFor|Page
		// use: uploadFile|fileName
		protected static String uploadFile (WebDriver driver, String application, Integer timeout, String xpathFile, String fileName, String filePath, String OS)  throws Exception {
			
			String iteration = "1";
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				// Build a path to the file out of the file path and file name.
				// The file path is the files sub-directory of the test directory.  
				// fileName is specified in the method call in the script file.
				String uploadPath = filePath + fileName;
				
				if (OS.equalsIgnoreCase("windows")) {
					uploadPath = uploadPath.replace('/', '\\');
				}
				
				File file=new File(uploadPath);
				
				// Check to see if the file exists.  Write a log entry and fail if it does not.
				if (!file.isFile()) {
					return "fail: File does not exist at the following path: " + uploadPath;
				}
				
				String [] elementTypes = {"uploadpath"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", ""); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);

				WebElement myElement = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
				
				myElement.sendKeys(uploadPath);

				return "pass";
				
				}  catch (Exception e) {
					return "fail: " + e.toString();
				}			
			}			
			

		
		protected static String verifyFile ( String downloadDirectory,  String fileDirectory, String fileName, Integer timeout)  throws Exception {
			
			try {
				
				File[] baselineFiles, testFiles;
				File baselineFile, testFile = null;
				File badFileDirectory = new File(downloadDirectory + "/errors");

				// Construct a filter, for use later if the file contains a wildcard.
				FileFilter fileFilter = new WildcardFileFilter(fileName);				
				
				// Verify that our baseline file exists
				if (fileName.contains("*")) {
					
					File baselineDirectory = new File(fileDirectory);
					baselineFiles = baselineDirectory.listFiles(fileFilter);
					
					if (baselineFiles.length > 0) {
						baselineFile = baselineFiles[0];
					} else {
						return "fail: your baseline file does not exist at this path: " + fileDirectory + fileName;
					}
					
				} else {
					baselineFile = new File(fileDirectory + fileName);
					
					if (!baselineFile.exists()) {
						return "fail: your baseline file does not exist at this path: " + fileDirectory + fileName;
					}	
				}
				
				// Verify that our test file has been download and exists.
				// Wait for as long as the timeout is specified.
				// First, account for cases where we are checking for a wild-carded file.				
				if (fileName.contains("*")) {
						
					File downloadFileDirectory = new File(downloadDirectory);
					
					for (int i=0; i<timeout; i++) {
						
						testFiles = downloadFileDirectory.listFiles(fileFilter);
						testFile = testFiles[0];
						
						if (testFile.length() > 0) {
								break;
						}
						// Sleep for a second if the file does not exist yet.							
						Thread.sleep(1000);	
					}
						
				} else {

					for (int i=0; i<timeout; i++) {
					
						testFile = new File(downloadDirectory + fileName);
						
						if (testFile.length() > 0) {
							break;
						}
					// Sleep for a second if the file does not exist yet.
					Thread.sleep(1000);
					}
				}
					
				if (!testFile.exists()) {
					return "fail: your test file does not exist at this path: " + downloadDirectory + fileName;
				}
				
				// Verify that the length of the two files matches.
				if (baselineFile.length() == testFile.length()) {
					return "pass"; 
				} else {
					// copy test file to a holding directory for comparison after the test.
					FileUtils.copyFileToDirectory(testFile, badFileDirectory);
					
					return "fail: the files do not match.";
				}
				
				} catch (Exception e) {
					
						return "fail: " + e.toString();
				}
			
		}		

		
		// Useful for files with a variable length, you just want to verify that a non-zero file has been downloaded.
		protected static String verifyFileExists ( String downloadDirectory,  String fileName, Integer timeout)  throws Exception {
			
			try {
				
				File[] testFiles;
				File testFile = null;
				
				// Construct a filter, for use later if the file contains a wildcard.
				FileFilter fileFilter = new WildcardFileFilter(fileName);								
				
				// Verify that our test file has been download and exists.
				// Wait for as long as the timeout is specified.
				// First, account for cases where we are checking for a wild-carded file.
				if (fileName.contains("*")) {
						
					File downloadFileDirectory = new File(downloadDirectory);
					
					for (int i=0; i<timeout; i++) {
						
						testFiles = downloadFileDirectory.listFiles(fileFilter);
						testFile = testFiles[0];
						
						if (testFile.length() > 0) {
								break;
						}
						// Sleep for a second if the file does not exist yet.						
						Thread.sleep(1000);	
					}

				// Check our target file where there are no wildcard characters to account for.
				} else {

					for (int i=0; i<timeout; i++) {
					
						testFile = new File(downloadDirectory + fileName);
						
						if (testFile.length() > 0) {
							break;
						}
					// Sleep for a second if the file does not exist yet.
					Thread.sleep(1000);
					}
				}
					
				if (!testFile.canRead()) {
					return "fail: your test file does not exist at this path: " + downloadDirectory + fileName;
				} else {
					return "pass";
				}
				
				} catch (Exception e) {
					
						return "fail: " + e.toString();
				}
			
		}
		
		
		protected static String verifyText (WebDriver driver, String application, Integer timeout, String xpathFile, String text)  throws Exception {
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"bodytext"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", text); 
		        paramHash.put("iteration", "1");
		        paramHash.put("xpathFile", xpathFile);

				ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
				// If pathFinder has found the element, we assume the text is present on the page.		
				return "pass";
				
				} catch (Exception e) {
					return "fail: " + e.toString();
				}  
			}
		
		protected static String verifyTextNotPresent (WebDriver driver, String application, Integer timeout, String xpathFile, String text)  throws Exception {
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"bodytext"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", text); 
		        paramHash.put("iteration", "1");
		        paramHash.put("xpathFile", xpathFile);

				ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes);
				// If pathFinder has found the element, we assume the text is present on the page.		
				return "fail: " + text + " is visible on the page.";
				
				} catch (Exception e) {
					// We should only get here if the text is not present, which is what we are looking for.
					return "pass";
				}  
			}		


		protected static String verifyContents (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {
			
			String valueToVerify = object[1];
			String iteration = "1";
			if (object.length > 2) 
				iteration = object[2];
			
			// Empty text boxes will not match with a space value. So we need to recast to an empty string
			if (object[1].equals(" ")) {
				object[1]="";
			}
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"listbox"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);
		        
		        //Get our webElement
		        WebElement objectInQuestion = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes); 
		        
		        Select selectInQuestion = new Select(objectInQuestion);
			        
		        List<WebElement> selectedOptions = selectInQuestion.getOptions();
		        
		        ListIterator<WebElement> optionList = selectedOptions.listIterator();
		        
		        while(optionList.hasNext()) {

		            if(optionList.next().getText().equals(valueToVerify)){
		            	return "pass";
		            }
		        } 
					
				return "fail";
							
			} catch (Exception e) {
				return "fail: " + e.toString();
			}
		}	
		
		
		protected static String verifyValue (WebDriver driver, String application, Integer timeout, String xpathFile, String ... object)  throws Exception {
			
			String iteration = "1";
			if (object.length > 2) 
				iteration = object[2];
	
			// Empty text boxes will not match with a space value. So we need to recast to an empty string
			if (object[1].equals(" ")) {
				object[1]="";
			}

			String valueToVerify = object[1];			
			
			HashMap<String, String> paramHash = new HashMap<String, String>();
			
			try {
				
				String [] elementTypes = {"radio", "checkbox", "listbox", "textbox"};
				
				paramHash.put("application", application);
		        paramHash.put("objectID", object[0]); 
		        paramHash.put("iteration", iteration);
		        paramHash.put("xpathFile", xpathFile);
		        
		        //Get our webElement
		        WebElement objectInQuestion = ElementLocator.pathFinder(driver, timeout, paramHash, elementTypes); 
		        
				// For radios and check boxes, we verify if they are checked--which returns true--or not--which returns false.
				if ((objectInQuestion.getAttribute("type").contains("radio")) || (objectInQuestion.getAttribute("type").contains("checkbox")))	{
			
					// Check to see if the specified element is checked or not and compare it with the user's expectations
					String objectSelected = ((Boolean) objectInQuestion.isSelected()).toString().trim();
					
					if (objectSelected.equalsIgnoreCase(valueToVerify)) {
						return "pass";
					// Fail if the stated criteria doesn't match
					} else {
						return "fail";
					}
				// For selects selects, we grab their selected value.
				} else if (objectInQuestion.getAttribute("type").contains("select")) {
					
			        Select selectInQuestion = new Select(objectInQuestion);
			        
			        List<WebElement> selectedOptions = selectInQuestion.getAllSelectedOptions();
			        
					if (selectedOptions.get(0).getText().trim().equals(valueToVerify)) {
						return "pass";
					// Fail if the stated criteria doesn't match
					} else {
						return "fail";
					}
				// For selects text boxes, we grab their text value.					
				} else {
					if (objectInQuestion.getAttribute("value").trim().equals(valueToVerify)) {
						return "pass";
					// Fail if the stated criteria doesn't match
					} else {
						return "fail: Cannot find the following text: " + valueToVerify;
					}
				}
				
			} catch (Exception e) {
				return "fail: " + e.toString();
			}
		}	


		//wait for a defined period of time.
		protected static String wait(String timeout) throws Exception{
			
			try {
			
			Integer intTimeout = Integer.parseInt(timeout + "000");
			Thread.sleep(intTimeout);
			
			return "pass";
			
			} catch (Exception e) {
				return "fail: " + e.toString();
			}

		}		

		

		//wait for a pop up window to open.  Timeout is in milliseconds.
		protected static String waitForPopUp(WebDriver driver, String variablesPath, String timeout) throws Exception {
			
			Integer intTimeout = Integer.parseInt(timeout);
			String origWindowHandle;
			
			try {
				
				//Define the variables stream, load the variables, and close the stream.
				Properties currentVariables = new Properties();
				FileInputStream curVarStream = new FileInputStream(variablesPath);
				currentVariables.load(curVarStream);

	    		if (!currentVariables.containsKey("origWindowHandle")) {
	    			return "fail: The original window handle was not recorded.";
	    		} else {
	    			origWindowHandle = currentVariables.getProperty("origWindowHandle");
	    		}
				
				curVarStream.close();
				
				//Object[] windowHandles  = driver.getWindowHandles().toArray();				
				
				// Grab the current window handle and remove it from the list
				// of all available window handles.  This should either yield the
				// pop-up window or nothing, depending on whether the pop-up has appeared.
				// String currentHandle = driver.getWindowHandle();
				Set<String> windowHandleSet = driver.getWindowHandles();
				//System.out.println("Current list is: " + windowHandleSet.toString());
				windowHandleSet.remove(origWindowHandle);
				
				//System.out.println("Current Handle was: " + currentHandle);
				//System.out.println("What's left is: " + windowHandleSet.toString());
				//System.out.println("The end");
	        
				// Try to find the correct path, once a second until the timeout value is reached.
				// By default this is 30 seconds.
				for (int timer = 1; timer <= intTimeout; timer++) {

			        // If there is more than a single window, attempt to switch to the newest one.
					//if (windowHandles.length > 1)
					if (windowHandleSet.size() > 0)
			        {
						//driver.switchTo().window(windowHandles[windowHandles.length - 1].toString());
						driver.switchTo().window(windowHandleSet.iterator().next());
						return "pass";
			        }
					
					//Wait for one second if the popup is not found
					Thread.sleep(1000);
					
					//windowHandles  = driver.getWindowHandles().toArray();
					// Repeat the earlier process after waiting a second.
					windowHandleSet = driver.getWindowHandles();
					windowHandleSet.remove(origWindowHandle);
					
				}
				
			return "fail: no pop-up was found in the time interval specified.";
				
			} catch (Exception e) {
				
					return "fail: " + e.toString();
			}
		}		
	    

}
