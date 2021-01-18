package com.rdns.rohost;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RestController
public class MyApp {
	private static final Logger logger = LoggerFactory.getLogger(MyApp.class);
	
	/**
	 * 
	 * @param data
	 * @param user
	 * @return
	 */
	@PostMapping(path="/add", produces=MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> addPtrRecord(@Valid @RequestBody(required=true) MyReqData data,
			@AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
		String zone = user.getUsername();
		//first I must verify if the record <num> does not exists in the zone;
		//whmapi1 dumpzone domain=example.com
		//if exists, remove that line;
		//whmapi1 removezonerecord zone=example.com line=4
		//finally add the new reverse;
		//whmapi1 addzonerecord zone=0.168.192.in-addr.arpa name=1 ptrdname=hostname.example.com type=PTR}
		logger.info("POST --- User zone: " + zone + ". Request body: " + data.toString());
		
		int hasPtr = 0;
		try {
			hasPtr = hasPtr(zone, data.getRecordName()); //will return the line number from the zone
			if(hasPtr != 0) {
				logger.info("addPtrRecord: zone( " + zone + " ) has PTR, deleteing...");
				deleteZone(zone, String.valueOf(hasPtr));
				logger.info("addPtrRecord: done");
			}
		}
		catch(Exception e) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), null);
		}
		
		logger.info("addPtrRecord: trying to add " + zone + ", " + data.getRecordName() + ", " + data.getPtrDomainName());
		int ret = addRecordZone(zone, data.getRecordName(), data.getPtrDomainName());
		if(ret != 0) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record was not added in the requested zone", null);
		}
		
		return new ResponseEntity<Object>("{}", HttpStatus.OK);
	}

	public int deleteZone(String zone, String lineNo) {
		List<String> removeZone = Arrays.asList("removezonerecord", "zone="+zone, "line="+lineNo);
		String result = "";
		JsonObject jo;

		try {
			logger.info("deleteZone: cmd = " + String.join(" ", removeZone));
			result = runCmd(removeZone);

			jo = JsonParser.parseString(result).getAsJsonObject();
			jo = jo.get("metadata").getAsJsonObject();
			if(jo.get("result").getAsInt() != 1)
				return 1;
		}
		catch(Exception e) {
	        return 1;
		}
		
		return 0;
	}
	
	public int addRecordZone(String zone, String startNum, String domainptr) {
		List<String> addPtrZone = Arrays.asList("addzonerecord", "zone="+zone, "name="+startNum, "ptrdname="+domainptr, "type=PTR");
		String result = "";
		JsonObject jo;

		try {
			result = runCmd(addPtrZone);

			jo = JsonParser.parseString(result).getAsJsonObject();
			jo = jo.get("metadata").getAsJsonObject();
			if(jo.get("result").getAsInt() != 1)
				return 1;
		}
		catch(Exception e) {
	        return 1;
		}
		
		return 0;
	}

	/**
	 * 
	 * @param params: list of cmd parameters to run
	 * @return json output for the whapi1 cmd
	 * @throws Exception
	 */
	private String runCmd(List<String> params) throws Exception {
		List<String> internalP = new ArrayList<String>();
		internalP.add(0, "--output=json");
		internalP.add(0, "/usr/local/cpanel/bin/whmapi1");
		for(String s: params) {
			internalP.add(s);
		}
		
		ProcessBuilder pb = new ProcessBuilder(internalP);
		BufferedReader buffer;
		StringBuilder stringBuilder;
		String lineSeparator = System.getProperty("line.separator");
		try {
			Process p = pb.start();
			buffer = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
			stringBuilder = new StringBuilder();
			String line;
			while ((line = buffer.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(lineSeparator);
			}
			p.waitFor();
			p.destroy();
		}
		catch(Exception e) {
			throw new Exception("Error executing cmd");
		}
		
		return stringBuilder.toString();
	}
	
	/**
	 * 
	 * @param zone
	 * @param recordName
	 * @return lineNumber if the zone has recordName or 0 if not found
	 * @throws Exception
	 */
	private int hasPtr(String zone, String recordName) throws Exception {
		String toMatch = recordName + "." + zone + ".";
		JsonObject jo, dataread;
		logger.info("hasPtr: toMatch = " + toMatch);
		
		try {
			String s = dumpZone(zone);
			jo = JsonParser.parseString(s).getAsJsonObject();
			dataread = jo.get("data").getAsJsonObject();
		}
		catch(Exception e) {
			throw e;
		}
		
		if(!dataread.isJsonNull()) {
			JsonArray jj;
			try {
				jj = dataread.get("zone").getAsJsonArray();
				if(jj.isJsonArray() && jj.size() == 1) {
					jo = jj.get(0).getAsJsonObject();
				}
				jj = jo.get("record").getAsJsonArray();

				for(JsonElement je: jj) {
					JsonObject j;
					j = je.getAsJsonObject();
					if(j.get("type").getAsString().equals("PTR")) {
						if(j.get("name").getAsString().equals(toMatch)) {
							return j.get("Line").getAsInt(); //return match line number
						}
					}
				}
			}
			catch(Exception e) {
		        throw new Exception("JSON search error for zone");
			}
		}
		else {
	        throw new Exception("NULL JSON response");
		}
		
		return 0;
	}
	
	private String dumpZone(String zone) throws Exception {
		List<String> dumpZone = Arrays.asList("dumpzone", "domain="+zone);
		String result = "";

		try {
			result = runCmd(dumpZone);
		}
		catch(Exception e) {
			throw new Exception("Can't get records for zone, " + zone + ". " + dumpZone.toString());
		}
		
		return result;
	}
		
	@GetMapping(path={"/get", "/get/{recordName}"}, produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getPtrRecord(@PathVariable("recordName") Optional<String> recordName,
			@AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
		String zone = user.getUsername();
		
		ObjectMapper mapper = new ObjectMapper(); 
		String toMatch = "";
		List<MyReqData> ptrEntities = new ArrayList<MyReqData>();

		if(recordName.isPresent()) { 
			toMatch = recordName.get() + "." + zone + ".";
			logger.info("GET --- User zone: " + zone + ". recordName: " + recordName);
		}
		else {
			logger.info("GET --- User zone: " + zone + ".");
		}
		JsonObject jo, dataread;
		
		try {
			String s = dumpZone(zone);
			
			jo = JsonParser.parseString(s).getAsJsonObject();
			dataread = jo.get("data").getAsJsonObject();
		}
		catch(Exception e) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), null);
		}
		
		if(!dataread.isJsonNull()) {
			JsonArray jj;
			try {
				jj = dataread.get("zone").getAsJsonArray();
				if(jj.isJsonArray() && jj.size() == 1) {
					jo = jj.get(0).getAsJsonObject();
				}
				jj = jo.get("record").getAsJsonArray();

				for(JsonElement je: jj) {
					JsonObject j;
					j = je.getAsJsonObject();
					if(j.get("type").getAsString().equals("PTR")) {
						if(toMatch != "") {
							if(j.get("name").getAsString().equals(toMatch)) {
								MyReqData data = new MyReqData();
								data.setPtrDomainName(j.get("ptrdname").getAsString());
								data.setRecordName(toMatch);
								return new ResponseEntity<Object>(mapper.writeValueAsString(data), HttpStatus.OK);
							}
						}
						else {
							if(j.get("type").getAsString().equals("PTR")) {
								MyReqData data = new MyReqData();
								data.setPtrDomainName(j.get("ptrdname").getAsString());
								data.setRecordName(j.get("name").getAsString());
								ptrEntities.add(data);
							}
						}
					}
				}
			}
			catch(Exception e) {
		        throw new ResponseStatusException(HttpStatus.NOT_MODIFIED, "JSON search error for zone", null);
			}
		}
		else {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "NULL JSON response", null);
		}
		
		
		try {
			return new ResponseEntity<Object>(mapper.writeValueAsString(ptrEntities), HttpStatus.OK);
		} catch (JsonProcessingException e) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Malformed JSON response: " + e.getMessage(), null);
		}
	}
	
	/**
	 * Will delete recordName from zone
	 * @param recordName
	 * @param user
	 * @return
	 */
	@DeleteMapping(path="/del/{recordName}", produces=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> delPtrRecord(@PathVariable String recordName, 
			@AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
		String zone = user.getUsername();
		
		logger.info("DELETE --- User zone: " + zone + ". recordName: " + recordName);

		int hasPtr = 0;
		try {
			hasPtr = hasPtr(zone, recordName); //will return the line number from the zone
			if(hasPtr != 0) {
				deleteZone(zone, String.valueOf(hasPtr));
			}
		}
		catch(Exception e) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), null);
		}
		
		return new ResponseEntity<Object>("{}", HttpStatus.OK);
	}
}
