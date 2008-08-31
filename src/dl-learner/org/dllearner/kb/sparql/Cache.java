/**
 * Copyright (C) 2007-2008, Jens Lehmann
 *
 * This file is part of DL-Learner.
 * 
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.dllearner.kb.sparql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.dllearner.utilities.JamonMonitorLogger;

import com.jamonapi.Monitor;

/**
 * SPARQL query cache to avoid possibly expensive multiple queries. The queries
 * and their results are written to files. A cache has an associated cache
 * directory where all files are written.
 * 
 * Each SPARQL query and its result is written to one file. The name of this
 * file is a hash of the query. The result of the query is written as JSON
 * serialisation of the SPARQL XML result, see
 * http://www.w3.org/TR/rdf-sparql-json-res/.
 * 
 * Apart from the query and its result, a timestamp of the query is stored.
 * After a configurable amount of time, query results are considered outdated.
 * If a cached result of a SPARQL query exists, but is too old, the cache
 * behaves as if the cached result would not exist.
 * 
 * TODO: We are doing md5 hashing at the moment, so in rare cases different
 * SPARQL queries can be mapped to the same file. Support for such scenarios
 * needs to be included.
 * 
 * @author Sebastian Hellmann
 * @author Sebastian Knappe
 * @author Jens Lehmann
 */
public class Cache implements Serializable {

	private static Logger logger = Logger.getLogger(Cache.class);
	


	private static final long serialVersionUID = 843308736471742205L;

	// maps hash of a SPARQL queries to JSON representation
	// of its results; this
	// private HashMap<String, String> hm;

	private transient String cacheDir = "";
	private transient String fileEnding = ".cache";
	// private long timestamp;

	// specifies after how many seconds a cached result becomes invalid
	private long freshnessSeconds = 15 * 24 * 60 * 60;

	/**
	 *  same ad Cache(String) default is "cache"
	 */
	/*public Cache() {
		this("cache");
	} */
	
	/**
	 * A Persistant cache is stored in the folder cachePersistant.
	 * It has longer freshness 365 days and is mainly usefull for developing
	 * @return a Cache onject
	 */
	public static Cache getPersistentCache(){
		Cache c = new Cache("cachePersistant"); 
		c.setFreshnessInDays(365);
		return c;
	}
	
	/**
	 * @return the default cache object
	 */
	public static Cache getDefaultCache(){
		Cache c = new Cache("cache"); 
		return c;
	}
	
	/**
	 * the default cachedir normally is "cache".
	 * @return Default Cache Dir
	 */
	public static String getDefaultCacheDir(){
		return "cache";
	}
	
	/**
	 * Constructor for the cache itself.
	 * 
	 * @param cacheDir
	 *            Where the base path to the cache is .
	 */
	public Cache(String cacheDir) {
		
		this.cacheDir = cacheDir + File.separator;
		if (!new File(cacheDir).exists()) {
			logger
					.info("Created directory: " + cacheDir + " : " + new File(cacheDir).mkdir()
							+ ".");
		}
	}

	// compute md5-hash
	private String getHash(String string) {
		// calculate md5 hash of the string (code is somewhat
		// difficult to read, but there doesn't seem to be a
		// single function call in Java for md5 hashing)
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md5.reset();
		md5.update(string.getBytes());
		byte[] result = md5.digest();

		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			hexString.append(Integer.toHexString(0xFF & result[i]));
		}
		return hexString.toString();
	}

	// return filename where the query result should be saved
	private String getFilename(String sparqlQuery) {
		return cacheDir + getHash(sparqlQuery) + fileEnding;
	}

	/**
	 * Gets a result for a query if it is in the cache.
	 * 
	 * @param sparqlQuery
	 *            SPARQL query to check.
	 * @return Query result as JSON or null if no result has been found or it is
	 *         outdated.
	 */
	@SuppressWarnings({"unchecked"})
	private String getCacheEntry(String sparqlQuery) {
		
		String filename = getFilename(sparqlQuery);
		File file = new File(filename);
		
		// return null (indicating no result) if file does not exist
		if(!file.exists()) {
			return null;
		}
			
		
		LinkedList<Object> entry = null;
		try {
			FileInputStream fos = new FileInputStream(filename);
			ObjectInputStream o = new ObjectInputStream(fos);
			entry = (LinkedList<Object>) o.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		// TODO: we need to check whether the query is correct
		// (may not always be the case due to md5 hashing)
		
		// determine whether query is outdated
		long timestamp = (Long) entry.get(0);
		boolean fresh = checkFreshness(timestamp);
		
		if(!fresh) {
			// delete file
			file.delete();
			// return null indicating no result
			return null;
		}
		
		return (String) entry.get(2);
	}

	/**
	 * Adds an entry to the cache.
	 * 
	 * @param sparqlQuery
	 *            The SPARQL query.
	 * @param result
	 *            Result of the SPARQL query.
	 */
	private void addToCache(String sparqlQuery, String result) {
		String filename = getFilename(sparqlQuery);
		long timestamp = System.currentTimeMillis();

		// create the object which will be serialised
		LinkedList<Object> list = new LinkedList<Object>();
		list.add(timestamp);
		list.add(sparqlQuery);
		list.add(result);

		// create the file we want to use
		File file = new File(filename);

		try {
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(filename, false);
			ObjectOutputStream o = new ObjectOutputStream(fos);
			o.writeObject(list);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// check whether the given timestamp is fresh
	private boolean checkFreshness(long timestamp) {
		return ((System.currentTimeMillis() - timestamp) <= (freshnessSeconds * 1000));
	}

	/**
	 * Takes a SPARQL query (which has not been evaluated yet) as argument and
	 * returns a JSON result set. The result set is taken from this cache if the
	 * query is stored here. Otherwise the query is send and its result added to
	 * the cache and returned. Convenience method.
	 * 
	 * @param query
	 *            The SPARQL query.
	 * @return Jena result set in JSON format
	 */
	public String executeSparqlQuery(SparqlQuery query) {
		Monitor totaltime =JamonMonitorLogger.getTimeMonitor(Cache.class, "TotalTimeExecuteSparqlQuery").start();
		JamonMonitorLogger.increaseCount(Cache.class, "TotalQueries");
	
		Monitor readTime = JamonMonitorLogger.getTimeMonitor(Cache.class, "ReadTime").start();
		String result = getCacheEntry(query.getSparqlQueryString());
		readTime.stop();
		
		if (result != null) {
			query.setJson(result);
			
		    query.setRunning(false);
			SparqlQuery.writeToSparqlLog("***********\nJSON retrieved from cache");
			SparqlQuery.writeToSparqlLog("wget -S -O - '\n"+query.getSparqlEndpoint().getHTTPRequest());
			SparqlQuery.writeToSparqlLog(query.getSparqlQueryString());
			
			//SparqlQuery.writeToSparqlLog("JSON: "+result);
			JamonMonitorLogger.increaseCount(Cache.class, "SuccessfulHits");
			
		} else {
			
			//ResultSet rs= query.send();
		    	query.send();
			String json = query.getJson();
			if (json!=null){
				addToCache(query.getSparqlQueryString(), json);
				SparqlQuery.writeToSparqlLog("result added to cache: "+json);
				result=json;
				//query.setJson(result);
			} else {
				json="";
				result="";
				logger.warn(Cache.class.getSimpleName()+"empty result: "+query.getSparqlQueryString());
				
			}
			
			//return json;
		}
		totaltime.stop();
		return result;
	}
	
	/**
	 * deletes all Files in the cacheDir, does not delete the cacheDir itself, 
	 * and can thus still be used without creating a new Cache Object
	 */
	public void clearCache() {
		try{
			File f = new File(cacheDir);
		    String[] files = f.list();
		    for (int i = 0; i < files.length; i++) {
				new File(cacheDir+"/"+files[i]).delete();
			}    
		}catch (Exception e) {
			logger.warn("deleting cache failed");
			e.printStackTrace();
		}
	    
	}
	
	/**
	 * Changes how long cached results will stay fresh (default 15 days).
	 * @param days number of days
	 */
	public void setFreshnessInDays(int days){
		freshnessSeconds = days * 24 * 60 * 60;
	}

}
