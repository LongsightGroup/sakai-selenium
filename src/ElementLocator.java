package edu.umich.keyword;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ElementLocator {
	
	// Internal function which finds the the desired interface element for the calling action method to use.
	// returns the proper path to the interface element to the calling action method.
	// Calling functions will give the requesting identifier and an element type(s)
	
	protected static WebElement foundObject;
	private static String application;
	private static Integer frameLevel;
	
	// The next two definitions mean you can only support 8 levels of 8 frames apiece.
	// Increase the array size to support a larger number of frames.
	//private static Integer [] numFrames = new Integer[8]; 
	//private static Integer [] currentFrame = new Integer[8];
	
	static Map<Integer, WebElement> frameMap = new HashMap<Integer, WebElement>();
	
	protected static WebElement pathFinder (WebDriver driver, Integer timeout, HashMap<String, String> paramHash, String [] elementTypes)  throws Exception {
		
		String objectID = paramHash.get("objectID");
		String xpathFile = paramHash.get("xpathFile");
		application = paramHash.get("application");
		Integer iteration = Integer.parseInt(paramHash.get("iteration"));
		
		try {

			for (int i=0; i < timeout; i++) {
				// Get the list of paths for the first element type, like button, for instance.
				List<String> locatorList = getAllLocators(xpathFile, elementTypes, objectID);
				
				// Start at the top frame to look for the element in question.
				driver.switchTo().defaultContent();
				
				// Look for the element, and return it if you find it.
				if(objectLocator(driver, locatorList, iteration)) {
					return foundObject;
				}
				
				// If you can't find it, call frameCrawler to look through the frames for it.
				// return it if you find it.
				frameLevel = 0;
				
				try {
					
					if(frameCrawler(driver, locatorList, iteration)) {
						return foundObject;
					}
					Thread.sleep(1000);

				} catch (StaleElementReferenceException e) {
					// This typically occurs because the DOM has not caught up.
					// Wait for a second and continue.
					// System.out.println("Ahh, napping.");
					Thread.sleep(1000);
					continue;
				}
			}
	
			// Return an exception if you still can't find it.
			throw new Exception("fail: " + objectID + " is not found.");
			
		}  catch (Exception e) {
			throw e;
		}
	}

	private static List<String> getAllLocators (String xpathFile, String [] elementTypes, String objectID)  throws Exception {
		
		List<String> myLocators = new ArrayList<String>();
		
		try {

			// Iterate through the list of element types, returning a consolidated list of string containing all 
			// candidate locators
			for (int i=0; i < elementTypes.length; i++) {

				// This list should contain all locators for a given interface element type, for instance all buttons.
				// We now want to go through the list, taking the generic locators and those specific to our application,
				// and return this as a string list.
				NodeList listOfElements = getLocatorListPerElement(xpathFile, elementTypes[i]);
				
				// Grab each element from our list and see if it is either generic or application specific. 
				for (int xpathIterator = 0; xpathIterator < listOfElements.getLength(); xpathIterator++) {
					
					// This should take a single node and recast it as an element.
				    Element uiElement = (Element) listOfElements.item(xpathIterator);
				    
				    // Store all generic and app-specific elements in our list.
					if (uiElement.getElementsByTagName("generic").getLength() > 0) {
//						myLocators.add(uiElement.getElementsByTagName("generic").item(0).getChildNodes().item(0).getNodeValue().replace("element", "'"+ objectID + "'"));
						myLocators.add(uiElement.getElementsByTagName("generic").item(0).getChildNodes().item(0).getNodeValue().replace("element", "\""+ objectID + "\""));
					} else if (!application.isEmpty()) {
						if (uiElement.getElementsByTagName(application).getLength() > 0) {
//							myLocators.add(uiElement.getElementsByTagName(application).item(0).getChildNodes().item(0).getNodeValue().replace("element", "'"+ objectID + "'"));
							myLocators.add(uiElement.getElementsByTagName(application).item(0).getChildNodes().item(0).getNodeValue().replace("element", "\""+ objectID + "\""));
						}
					}
				}
			}

			// This should give us a string list with every locator that matches either generic or our application
			// and is one of our element types.
			return myLocators;
			
		} catch (Exception e) {
			throw e;
		}	
	}	
	
	private static NodeList getLocatorListPerElement (String xpathFile, String elementType)  throws Exception {
		
		try {
		
			//Open the xml file
	        File xmlPaths = new File(xpathFile);
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	        Document doc = docBuilder.parse(xmlPaths);
	        doc.getDocumentElement().normalize();
	        
	        return  doc.getElementsByTagName(elementType);
			
		} catch (Exception e) {
			throw e;
		}	
	}

/*	private static Boolean frameCrawlerOrig (WebDriver driver, List<String> locatorList, Integer iteration) throws Exception {
		
		try {
			
			// We are being told to look for frames, so we will descend one layer into the frame stack and populate
			// an array with the number of frames we find.
			frameLevel++;
			
			
			//Some frames are not iframes, determine what type of frame is contained on the page.
			numFrames[frameLevel] = driver.findElements(By.xpath("//iframe")).size();
			
			if (numFrames[frameLevel] == 0) {
				numFrames[frameLevel] = driver.findElements(By.xpath("//frame")).size();
			}

			// We will get here if there are no frames, that means it will throw an exception for the iframes and never get
			// to the other frames.  Will this ever happen?
			} catch (NoSuchElementException e) {
				// continue;
			}	

			// Switch to each of the frames and look for the current xpath within it.
			for(currentFrame[frameLevel]=0; currentFrame[frameLevel] < numFrames[frameLevel]; currentFrame[frameLevel]++) {

				// Switch to the default content prior to iteratively switching into the frames.  This is based on the
				// WebDriver limitation of not being able to switch up 1 frame level.
				driver.switchTo().defaultContent();
				
				for(int i=1; i <= frameLevel; i++) {
					// Switch into the current frame in the level we are currently working in.
					driver.switchTo().frame(currentFrame[i]);
				}

				// This will return true if it finds an enabled object, and false if it doesn't.
				// If we don't find the object, look for sub-frames and return true if the object is found within them.
				if (objectLocator(driver, locatorList, iteration)) {
					return true;
				} else {
					if(frameCrawler(driver, locatorList, iteration)) {
						return true;
					}
				}
				// There are no sub-frames, so we need to navigate back up to the parent frame before going down to the next frame.
				// This command takes us to the root frame, instead of just up one-level
				driver.switchTo().defaultContent();
				frameLevel--;
			}
			// We will get here if we have crawled through all of the frames without finding the object we are looking for.
			// So return false.
			return false;
		}
*/	
	private static Boolean frameCrawler (WebDriver driver, List<String> locatorList, Integer iteration) throws Exception {
		
		try {

			// We are being told to look for frames, so we will descend one layer into the frame stack and populate
			// an array with the number of frames we find.
			frameLevel++;			
			//System.out.println("I'm at frame level: " + frameLevel);
			
			// Look for frames.  If you find them, store them in a list.
			List <WebElement> framesList = driver.findElements(By.xpath("//iframe"));
			
			if (framesList.isEmpty()) {
				// Not all frames are iFrames, so look for those too.
				framesList = driver.findElements(By.xpath("//frame"));
			}
			//System.out.println("I see " + framesList.size() + " frames.");
			
			// Start switching into frames and look for the web element we desire within each one.
			for(WebElement myFrame:framesList) {
				
				// Add the current frame to the frameMap, so we can switch to it.
				frameMap.put(frameLevel, myFrame);
				
				// Switch to the default content prior to iteratively switching into the frames.  This is based on the
				// WebDriver limitation of not being able to switch up 1 frame level.
				driver.switchTo().defaultContent();
				//System.out.println("I'm switching to default content.");
				
				for(int i=1; i <= frameLevel; i++) {
					// For each level of sub-frames, 
					// switch into the current frame
					//System.out.println("Attempting to switch to a frame at level " + i);
					driver.switchTo().frame(frameMap.get(i));
				}
				
				// This will return true if it finds an enabled object, and false if it doesn't.
				// If we don't find the object, look for sub-frames and return true if the object is found within them.
				if (objectLocator(driver, locatorList, iteration)) {
					return true;
				} else {
					//System.out.println("Calling frameCrawler recursively.");
					if(frameCrawler(driver, locatorList, iteration)) {
						return true;
					}
				}
				
				// There are no sub-frames, so we need to navigate back up to the parent frame before going down to the next frame.			
				// This command takes us to the root frame, instead of just up one-level, 
				driver.switchTo().defaultContent();
				frameLevel--;
				//System.out.println("Switching down a level.");
				// Remove the current frame from the map, as our element was not found there.
				frameMap.remove(frameLevel);
			}

			// We will get here if we have crawled through all of the frames without finding the object we are looking for.
			// So return false.
			return false;
			
			// We will get here if there are no frames, that means it will throw an exception for the iframes and never get
			// to the other frames.  Will this ever happen?
			/*} catch (Exception e) {
				
				if (e.getMessage().contains("NoSuchElementException")) {
					return false;
				} else {
					return false;
				}
			*/
			// This will occur if the frameCrawler cannot switch to a particular frame.
			} catch (NoSuchElementException e) {
				return false;
			}
		}	
	
/*	private static Boolean frameLister (WebDriver driver, List<String> locatorList, Integer iteration) throws Exception {
		
		try {
		
			// framesList is a variable local to this method, so it should create a new 
			// instantiation of itself if it is called recursively.  This should eliminate
			// problems with nested frames.
			List <WebElement> framesList = driver.findElements(By.xpath("//iframe"));
			
			if (framesList.isEmpty()) {
				framesList = driver.findElements(By.xpath("//frame"));
			}

			// Switch to one of the frames so that objectLocator can look within it
			for(WebElement myFrame:framesList) {
				driver.switchTo().frame(myFrame);

				// This will return true if it finds an enabled object, and false if it doesn't.
				// If we don't find the object, look for sub-frames and return true if the object is found within them.
				if (objectLocator(driver, locatorList, iteration)) {
					return true;
				} else {
					if(frameLister(driver, locatorList, iteration)) {
						return true;
					}
				}
			}

			// There are no sub-frames, so we need to navigate back up to the parent frame before going down to the next frame.
			// This command takes us to the root frame, instead of just up one-level
			driver.switchTo().defaultContent();
			
			// We will get here if we have crawled through all of the frames without finding the object we are looking for.
			// So return false.
			return false;
			
			// We will get here if there are no frames, that means it will throw an exception for the iframes and never get
			// to the other frames.  Will this ever happen?
			} catch (NoSuchElementException e) {
				
				return false;
				// continue;
			}
			
		}	
*/				
	private static Boolean objectLocator (WebDriver driver, List<String> locatorList, Integer iteration) throws Exception {

		try {
			
			ListIterator<String> iterator = locatorList.listIterator();
			
			while(iterator.hasNext()) {	
			
				String iteratedPath = "(" + iterator.next() + ")[" + iteration + "]";

				// Wrap this in a separate try because webDriver throws exceptions when an element isn't found.
				try {
					//	This will throw a NoSuchElementException if the element is not found
					foundObject = driver.findElement(By.xpath(iteratedPath));

					// We will only get here if the proceeding call found an object.  Check if the object is displayed.
					// If it isn't, continue looking through the various xpaths.
					if (foundObject.isDisplayed()) {
						
						// occassionally an element will have the aria-disabled property, which the webDriver 'isEnabled'
						// method ignores, so we have to check for this by hand.
						if (foundObject.getAttribute("aria-disabled") == null) {
							// This property is not in place, so the element should be clickable.
							return true;
						} else if (foundObject.getAttribute("aria-disabled").equalsIgnoreCase("true")) {
							// The property is set to true, so continue searching for the next element
							continue;
						}

					} else {
						continue;
					}
					
				} catch (NoSuchElementException ex) {
					//System.out.println("Didn't find: " + iteratedPath);
					continue;
				}
			}
			return false;

		} catch (Exception e) {
			throw new Exception();
		}
	}

// Class-level bracket.
}
