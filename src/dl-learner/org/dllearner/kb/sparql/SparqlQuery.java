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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.utilities.JamonMonitorLogger;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.engine.http.HttpQuery;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * Represents one SPARQL query. It includes support for stopping the SPARQL
 * query (which may be necessary if a timeout is reached).
 * 
 * @author Jens Lehmann
 * 
 */
public class SparqlQuery {

	private static Logger logger = Logger.getLogger(KnowledgeSource.class);

	private boolean isRunning = false;
	private String queryString;
	private QueryEngineHTTP queryExecution;
	private SparqlEndpoint endpoint;
	private ResultSet rs;
	private String json;
//	private SparqlQueryException sendException=null;

	/**
	 * Standard constructor.
	 * 
	 * @param queryString
	 * @param endpoint
	 */
	public SparqlQuery(String queryString, SparqlEndpoint endpoint) {
		this.queryString = queryString;
		this.endpoint = endpoint;
	}

	/**
	 * Sends a SPARQL query using the Jena library.
	 */
	public ResultSet send() {
		
		isRunning = true;
		logger.trace(queryString);
		
		String service = endpoint.getURL().toString();
		logger.trace(endpoint.getURL().toString());
		// Jena access to SPARQL endpoint
		queryExecution = new QueryEngineHTTP(service, queryString);
		for (String dgu : endpoint.getDefaultGraphURIs()) {
			queryExecution.addDefaultGraph(dgu);
		}
		for (String ngu : endpoint.getNamedGraphURIs()) {
			queryExecution.addNamedGraph(ngu);
		}
		
		//TODO remove after overnext Jena release
		HttpQuery.urlLimit = 3*1024 ;
			
		JamonMonitorLogger.getTimeMonitor(SparqlQuery.class, "httpTime").start();
		rs = queryExecution.execSelect();
		JamonMonitorLogger.getTimeMonitor(SparqlQuery.class, "httpTime").stop();
			
		logger.debug("query SPARQL server, retrieved: "+rs.getResultVars());
				
		logger.trace(rs.getResultVars().toString());
//		} catch (Exception e){
//			sendException=new SparqlQueryException(e.getMessage());
//			logger.debug(e.getMessage());
//			//e.printStackTrace();
//			logger.debug("Exception when querying Sparql Endpoint in " + this.getClass());
//			logger.debug(queryString);
//		}
		isRunning = false;
		return rs;
	}

	public void stop() {
		queryExecution.abort();
		isRunning = false;
	}

	public String getQueryString() {
		return queryString;
	}
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public QueryEngineHTTP getExecution(){
		return queryExecution;
	}
	
//	public SparqlQueryException getException(){
//		return sendException;
//	}

	/**
	 * sends a query and returns XML
	 * 
	 * @return String xml
	 */
	public static String getAsXMLString(ResultSet resultSet) {
		//if (rs == null)
		//	this.send();
		return ResultSetFormatter.asXMLString(resultSet);
	}

	/**
	 * Converts Jena result set to JSON.
	 * 
	 * @param resultSet The result set to transform.
	 * @return JSON representation of the result set.
	 */
	public static String getAsJSON(ResultSet resultSet) {
		// if (rs == null)
		//	this.send();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ResultSetFormatter.outputAsJSON(baos, resultSet);
		// possible Jena bug: Jena modifies the result set during
		// JSON transformation, so we need to get it back
		resultSet = JSONtoResultSet(baos.toString());
		try {
			return baos.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// should never happen as UTF-8 is supported
			throw new Error(e);
		}
	}

	/**
	 * Converts from JSON to internal Jena format.
	 * 
	 * @param json
	 *            A JSON representation if a SPARQL query result.
	 * @return A Jena ResultSet.
	 */
	public static ResultSet JSONtoResultSet(String json) {
		ByteArrayInputStream bais = new ByteArrayInputStream(json
				.getBytes(Charset.forName("UTF-8")));
		//System.out.println("JSON " + json);
		return ResultSetFactory.fromJSON(bais);
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}
	
	public void setRunning(boolean running){
		this.isRunning=running;
	}
	
	public ResultSet getResultSet(){
		return rs;
	}
}
