package edu.umich.keyword;

import java.io.*;
import java.net.URL;
import java.util.Set;

import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FileDownloader {

    public static String downloader(WebDriver driver, WebElement element, String downloadPath, Integer timeout) throws Exception {
    	
    	String result = "";
 	
    	// This will grab the URL to download from the web element in question.
    	// Web element comes from the object locator.
        String urlHREF = element.getAttribute("href");
        if (urlHREF.trim().equals("")) {
            throw new Exception("The element you have specified does not link to anything!");
        }
        URL downloadURL = new URL(urlHREF); 
        
        DefaultHttpClient client = new DefaultHttpClient();        
        
        client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2965);
        
        CookieStore cookieStore = new BasicCookieStore();
        Set<org.openqa.selenium.Cookie> driverCookieSet = driver.manage().getCookies(); 
        
        // Somewhere in here, 4 of the 6 cookies are getting lost.  Their values are also getting mangled.
        for (org.openqa.selenium.Cookie myCookie : driverCookieSet) {
        	BasicClientCookie newCookie = new BasicClientCookie(myCookie.getName(), myCookie.getValue());
        	newCookie.setDomain(myCookie.getDomain());
        	newCookie.setPath(myCookie.getPath());
        	newCookie.setSecure(myCookie.isSecure());         
            cookieStore.addCookie(newCookie);
            }
        
        client.setCookieStore(cookieStore);
        
        HttpGet getRequest = new HttpGet(downloadURL.toURI());    
             
        try {
            HttpResponse myResponse = client.execute(getRequest);
            
            int status = myResponse.getStatusLine().getStatusCode();

            // Check to make sure that the get request has succeeded,
            // fail if we get a status other than 200. 
            if (status != 200) {
            	return "fail: " + myResponse.getStatusLine();
            }
                  
            result = fileWriter(downloadPath, myResponse);

        } catch (Exception Ex) {
            throw new Exception(Ex.getCause());
        } finally {
            getRequest.abort();
        }       
        return result;
    }
    
    
    private static String fileWriter(String downloadPath, HttpResponse myResponse) {

    	String fileName = "";
    	
    	try {
   	
	        // Read in the content and write it out one line at a time.
	        //BufferedReader rd = new BufferedReader(new InputStreamReader(myResponse.getEntity().getContent()));
    		InputStream rd = myResponse.getEntity().getContent();
	    	
	        // Obtain the name of the file from the header.
	        // Use this name for writing the file.
	        for (Header header : myResponse.getAllHeaders()) {
	        	HeaderElement[] helelms = header.getElements();
	        	if (helelms.length > 0) {
	        	    HeaderElement helem = helelms[0];
	        	    if (helem.getName().equalsIgnoreCase("inline")) {
	        	        NameValuePair nmv = helem.getParameterByName("filename");
	        	        if (nmv != null) {
	        	            fileName = nmv.getValue();
	        	        }
	        	   }           	
	        	}
	        }
	        
	        /*
	        BufferedWriter downloadedFile = new BufferedWriter(new FileWriter(downloadPath + fileName)); 
	        
	        String chunk;
	        while ((chunk = rd.readLine()) != null) {
	        	//downloadedFile.write(chunk + System.lineSeparator());
	        	downloadedFile.write(chunk);
	        }
	        */
	        //
	        FileOutputStream fout = new FileOutputStream(new File(downloadPath + fileName));

	        byte[] b = new byte[8192];
	        int bytesRead;
	           while (true) {
	            bytesRead = rd.read(b);
	               // System.out.println("bytesRead = "+bytesRead );
	               if (bytesRead==-1) { 
	            	   break;
	               }
	               fout.write(b, 0, bytesRead);
	           }        
	        
	        fout.flush();
	        fout.close();
	        //
	        
	        
	        // Close down our reader and writer.
	        rd.close();
	        //downloadedFile.close();
	       
    	} catch (Exception ex) {
    		return "fail: " + ex;
    	}
        
        return "pass";
    	
    }
}
