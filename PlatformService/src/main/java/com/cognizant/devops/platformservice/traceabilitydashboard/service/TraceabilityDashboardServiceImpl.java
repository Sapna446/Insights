/*******************************************************************************
 * Copyright 2017 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.cognizant.devops.platformservice.traceabilitydashboard.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.springframework.stereotype.Service;

import com.cognizant.devops.platformcommons.constants.PlatformServiceConstants;
import com.cognizant.devops.platformcommons.core.enums.FileDetailsEnum;
import com.cognizant.devops.platformcommons.core.util.InsightsUtils;
import com.cognizant.devops.platformcommons.dal.neo4j.GraphDBHandler;
import com.cognizant.devops.platformcommons.exception.InsightsCustomException;
import com.cognizant.devops.platformdal.filemanagement.InsightsConfigFiles;
import com.cognizant.devops.platformdal.filemanagement.InsightsConfigFilesDAL;
import com.cognizant.devops.platformservice.config.PlatformServiceStatusProvider;
import com.cognizant.devops.platformservice.traceabilitydashboard.constants.TraceabilityConstants;
import com.cognizant.devops.platformservice.traceabilitydashboard.util.TraceabilitySummaryUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

@Service("TreceabilityDashboardService")
public class TraceabilityDashboardServiceImpl implements TraceabilityDashboardService {

	
	Cache<String, String> pipelineCache;
	Cache<String, String> masterdataCache;
	String toolName;
	String fieldName;
	String fieldValue;
	String cacheKey;
	static final String PATTERN = "[\\[\\](){}\"\\\"\"]";
	static final String CATEGORY="category";
	static final String DATE_PATTERN = "MM/dd/yyyy HH:mm:ss";
	JsonObject dataModel = null;
	static final String DATA_MODEL_FILE_RESOLVED_PATH = System.getenv().get(TraceabilityConstants.ENV_VAR_NAME)
			+ File.separator + TraceabilityConstants.DATAMODEL_FOLDER_NAME + File.separator
			+ TraceabilityConstants.DATAMODEL_FILE_NAME;
	InsightsConfigFilesDAL configFilesDAL = new InsightsConfigFilesDAL();

	HashMap<String, String> handOverTimeMap = new HashMap<>();
	GsonBuilder gsonBuilder = new GsonBuilder();

	private static final Logger LOG = LogManager.getLogger(TraceabilityDashboardServiceImpl.class.getName());
	{
		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.withCache("traceability",
						CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
								ResourcePoolsBuilder.newResourcePoolsBuilder().heap(30, EntryUnit.ENTRIES).offheap(10,
										MemoryUnit.MB)))
				.build();
		cacheManager.init();
		LOG.debug("Traceability===== Cache Manaher Initilized ");
		pipelineCache = cacheManager.createCache("pipeline",
				CacheConfigurationBuilder
						.newCacheConfigurationBuilder(String.class, String.class,
								ResourcePoolsBuilder.heap(TraceabilityConstants.PIPELINE_CACHE_HEAP_SIZE_BYTES))
						.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(
								Duration.ofSeconds(TraceabilityConstants.PIPELINE_CACHE_EXPIRY_IN_SEC))));
		masterdataCache = cacheManager.createCache("masterdata",
				CacheConfigurationBuilder
						.newCacheConfigurationBuilder(String.class, String.class,
								ResourcePoolsBuilder.heap(TraceabilityConstants.MASTER_CACHE_HEAP_SIZE_BYTES))
						.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(
								Duration.ofSeconds(TraceabilityConstants.MASTER_CACHE_EXPIRY_IN_SEC))));

	}

	private void loadTraceabilityJson() throws InsightsCustomException {
		try {
			List<InsightsConfigFiles> configFile = configFilesDAL
					.getAllConfigurationFilesForModule(FileDetailsEnum.FileModule.TRACEABILITY.name());
			String configFileData = new String(configFile.get(0).getFileData(), StandardCharsets.UTF_8);
			dataModel = (JsonObject) new JsonParser().parse(configFileData);
			LOG.debug("Traceability.json is present and loaded properly");
			PlatformServiceStatusProvider.getInstance().createPlatformServiceStatusNode(
					"Traceability.json is successfully loaded.", PlatformServiceConstants.SUCCESS);
		} catch (JsonSyntaxException e) {
			LOG.error("Traceability.json is not formatted");
			PlatformServiceStatusProvider.getInstance().createPlatformServiceStatusNode(
					"Traceability.json is not formatted", PlatformServiceConstants.FAILURE);
			throw new InsightsCustomException("Traceability.json is not formatted");
			
		} catch (Exception e) {
			PlatformServiceStatusProvider.getInstance().createPlatformServiceStatusNode(
					"Error while loading Traceability.json", PlatformServiceConstants.FAILURE);
			LOG.error("Error while loading Traceability.json");
			throw new InsightsCustomException("Could not load Traceability.json. Please check whether the file has been uploaded in Configuration File Management.");
		}
	}

	private JsonObject getPipeLineResponse(LinkedHashMap<String, List<JsonObject>> map, JsonObject dataModel)
			throws InsightsCustomException {
		JsonArray pipeLineArray = new JsonArray();
		JsonObject pipeLineObject = new JsonObject();
		LinkedHashMap<String, String> sortedHandoverTimeMap = new LinkedHashMap<>();

		Set<Entry<String, List<JsonObject>>> keyset = map.entrySet();
		for (Map.Entry<String, List<JsonObject>> keyvaluePair : keyset) {
			List<JsonObject> limitedList = keyvaluePair.getValue().stream().limit(4).collect(Collectors.toList());
			limitedList.forEach(pipeLineArray::add);
			// Handover time object extraction and sorting
			if (!handOverTimeMap.isEmpty()) {
				try {
					List<String> childNodes = getDownTool(keyvaluePair.getKey(), dataModel);
					for (String eachNode : childNodes) {
						String construct = keyvaluePair.getKey() + " To " + eachNode;
						sortedHandoverTimeMap.put(construct, handOverTimeMap.get(construct));
					}

				} catch (InsightsCustomException e) {
					LOG.debug(e.getMessage());
				}
			}

		}
		/* Prepare Summary */
		JsonObject summaryObj = prepareSummary(map, dataModel);
		JsonArray summaryArray = new JsonArray();
		summaryArray.add(summaryObj);
		/* Timelag Response */
		JsonObject handOverTime = new JsonObject();
		sortedHandoverTimeMap.forEach(handOverTime::addProperty);
		JsonArray handOverArray = new JsonArray();
		handOverArray.add(handOverTime);
		/* Pipeline Response */
		pipeLineObject.add("pipeline", pipeLineArray);
		pipeLineObject.add("summary", summaryArray);
		pipeLineObject.add("timelag", handOverArray);
		return pipeLineObject;
	}

	public static JsonArray stringify(List<String> val) {
		JsonArray jsArray = new JsonArray();
		for (String value : val) {
			jsArray.add(value);
		}
		return jsArray;
	}

	public String buildCypherQuery(String toolname, String toolField,String toolCategory, List<String> toolVal, List<String> excludeLabels,
			int hopCount) {
		StringBuilder queryBuilder = new StringBuilder();
		if (hopCount == 1) {
			return queryBuilder.append("match(n:").append(toolCategory).append(":"+toolname).append(":DATA{").append(toolField).append(":")
					.append("'").append(toolVal.get(0)).append("'")
					.append("}) return n.toolName as toolname ,n.uuid as uuid").toString();
		} else if (hopCount == 2) {
			queryBuilder.append("MATCH (a:").append(toolname).append(":DATA:").append(toolCategory).append(") -[r]- (b:DATA) WHERE ");
			if (!toolField.equals("")) {
				queryBuilder.append("a.").append(toolField).append(" IN ").append(stringify(toolVal)).append(" ")
				.append("and exists(b.toolName) ");
			}
			if (!excludeLabels.isEmpty()) {
				queryBuilder.append("AND NOT (");
				for (String excludeLabel : excludeLabels) {
					queryBuilder.append("b:").append(excludeLabel).append(" OR ");
				}
				queryBuilder.replace(0, queryBuilder.length(),
						queryBuilder.substring(0, queryBuilder.lastIndexOf("OR")));
				queryBuilder.append(")");
			}
			queryBuilder.append(
					"with case when exists(r.handovertime) then collect(r.handovertime) else [] end as val, a, b");
			queryBuilder
					.append(" unwind(case val when [] then [null] else val end) as list ");
			queryBuilder.append("return  b.toolName as toolName, collect(distinct b.uuid) as uuids , abs(avg(list))");
			return queryBuilder.toString();
		} else if (hopCount == 3) {
			return "MATCH (a:DATA) WHERE a.uuid IN " + stringify(toolVal)
					+ " WITH distinct a.toolName as toolName, collect(distinct a) as nodes Return toolName, nodes";
		} else if(hopCount == 4)
		{
			return queryBuilder.append("match(n:").append(toolCategory).append(":"+toolname).append(":DATA{").append(toolField).append(":")
			.append("'").append(toolVal.get(0)).append("'")
			.append("})").append(" return n").toString();
			
		}
		else if(hopCount == 5)
		{
			return queryBuilder.append("match(n:").append(toolCategory).append(":"+toolname).append(":DATA{").append("epicKey").append(":")
					.append("'").append(toolVal.get(0)).append("'")
					.append("})").append(" return n").toString();
		}
		
		else {
			return "MATCH (a:" + toolname + ":DATA) WHERE a.uuid IN " + stringify(toolVal)
					+ " WITH distinct a.toolName as toolName, collect(distinct a) as nodes Return toolName, nodes";
		}

	}

	public Map<String, List<String>> format(JsonObject resp, List<String> sourceTool) {
		Map<String, List<String>> mapOfToolAndUUIDS = new HashMap<>();
		JsonElement val = null;
		int dataArraySize = resp.getAsJsonArray(TraceabilityConstants.RESULTS).getAsJsonArray().get(0).getAsJsonObject().get("data")
				.getAsJsonArray().size();
		List<String> uuidList = new ArrayList<>();
		for (int i = 0; i < dataArraySize; i++) {
		
			JsonArray rowArray = resp.getAsJsonArray(TraceabilityConstants.RESULTS).getAsJsonArray().get(0).getAsJsonObject().get("data")
					.getAsJsonArray().get(i).getAsJsonObject().get("row").getAsJsonArray();

			if (rowArray.get(1).isJsonArray()) {
				JsonArray arrOfuuid = rowArray.get(1).getAsJsonArray();
				val = rowArray.get(2);

				for (int j = 0; j < arrOfuuid.size(); j++) {
					uuidList.add(arrOfuuid.get(j).getAsString());
				}
			} else {
				uuidList.add(rowArray.get(1).getAsString());
			}

			if (null !=val && !val.isJsonNull()) {
				handOverTimeMap.put(sourceTool.get(0) + " To " + rowArray.get(0).getAsString(), InsightsUtils.getDateTimeFromEpoch(val.getAsLong()));
			}		
			mapOfToolAndUUIDS.put(rowArray.get(0).getAsString(), uuidList);
		}
		
		return mapOfToolAndUUIDS;
	}

	public Map<String, String> formatNeo4jResponse(JsonObject resp, int hopCount) {
		Map<String, String> map = new HashMap<>();
		JsonArray jsonDataRespArray = resp.getAsJsonArray(TraceabilityConstants.RESULTS).getAsJsonArray().get(0).getAsJsonObject()
				.get("data").getAsJsonArray();
		if (jsonDataRespArray.size() > 0) {
			if (hopCount == 1) {
				// To Extract the UUID of the selected tool in UI for the first time
				String jsonResp = jsonDataRespArray.getAsJsonArray().get(0).getAsJsonObject().get("row").toString();
				String[] extractedToolsWithUUID = jsonResp.replaceAll(PATTERN, "").split(",");
				for (int j = 1; j < extractedToolsWithUUID.length; j++)
					map.put(extractedToolsWithUUID[j], extractedToolsWithUUID[0]);
			} else {
				for (int i = 0; i < jsonDataRespArray.size(); i++) {
					String rawOutput = jsonDataRespArray.get(i).getAsJsonObject().get("row").toString();
					String[] extractedToolsWithUUID = rawOutput.replaceAll(PATTERN, "").split(",");
					for (int j = 1; j < extractedToolsWithUUID.length; j++)
						map.put(extractedToolsWithUUID[j], extractedToolsWithUUID[0]);
				}
			}
		}
		return map;
	}

	public JsonObject executeCypherQuery(String query) throws  InsightsCustomException {
		LOG.debug("Traceability ===== Cypher Query To Execute {}",query);
		GraphDBHandler dbHandler = new GraphDBHandler();
		JsonObject neo4jResponse = dbHandler.executeCypherQueryForJsonResponse(query);
		LOG.debug("Response received from neo4j");
		LOG.debug("Traceability ===== response received from neo4j");
		return neo4jResponse;
	
	}

	private List<String> getUpTool(String toolName, JsonObject dataModel) throws InsightsCustomException {
		final String UPTOOL = "uptool";
		List<String> tools = new ArrayList<>();
		JsonObject toolObject = dataModel.getAsJsonObject(toolName);
		LOG.debug("Traceability ======= Inside Uptool Collection Method for Tool {}",toolName);
		if (toolObject.has(UPTOOL)) {
			if (toolObject.get(UPTOOL).isJsonArray()) {
				JsonArray toolArray = toolObject.get(UPTOOL).getAsJsonArray();
				for (int i = 0; i < toolArray.size(); i++) {
					tools.add(toolArray.get(i).getAsString());
				}
				return tools;
			} else {
				throw new InsightsCustomException("Traceability not configured properly in datamodel.");
			}

		} else {
			throw new InsightsCustomException("Traceability not configured properly in datamodel.");
		}

	}

	private List<JsonObject> sortToolsPayload(List<JsonObject> payload) {	
		payload.sort((JsonObject o1, JsonObject o2) -> {
			final String KEY_NAME = "inSightsTimeX";
			try {
				long d1 = InsightsUtils.getEpochTime(o1.get(KEY_NAME).getAsString());
				long d2 = InsightsUtils.getEpochTime(o2.get(KEY_NAME).getAsString());
				if (d1 > d2) {
					return 1;
				} else if (d1 == d2) {
					return 0;
				} else {
					return -1;
				}
			} catch (Exception e) {
				LOG.error("Traceability==== No date availble for sorting");
				return 0;
			}

		});
		return payload;
	}

	private Map<String, List<JsonObject>> getMasterResponse(JsonObject response, JsonObject dataModel) {

		JsonArray finaltoolsArray = new JsonArray();
		// Master Map contains toolname as string and list of toolpayload .
		HashMap<String, List<JsonObject>> masterMap = new HashMap<>();
		LinkedHashMap<String, List<JsonObject>> sortedmasterMap = new LinkedHashMap<>();	
		JsonArray responseArray = response.getAsJsonArray(TraceabilityConstants.RESULTS).getAsJsonArray().get(0).getAsJsonObject()
				.get("data").getAsJsonArray();
		int count = responseArray.size();
		for (int j = 0; j < count; j++) {
			List<JsonObject> toolsPayload = new ArrayList<>();
			// find the toolname in neo4j response
			LOG.debug("Data From Neo4J has been loaded properly");
			JsonArray toolsArray = responseArray.get(j).getAsJsonObject().get("row").getAsJsonArray();
			String toolNameFromNeo4j = toolsArray.get(0).getAsString();
			// Get the response template of the specific tool from data model
			if(dataModel.has(toolNameFromNeo4j))
			{
			JsonObject toolPayloadFromDatamodel = dataModel.get(toolNameFromNeo4j).getAsJsonObject();
			if (toolPayloadFromDatamodel != null) {
				Set<Entry<String, JsonElement>> keyset = toolPayloadFromDatamodel.entrySet();				
				int	numOfObjectsPerTool = toolsArray.get(1).getAsJsonArray().size();				 
				for (int i = 0; i < numOfObjectsPerTool; i++) {
					JsonObject formattedJsonObject = new JsonObject();
					for (Map.Entry<String, JsonElement> toolKeyValueSetFromDataModel : keyset) {
						// Every tool is JsonArray so loop it if it has more than one element get
						// extract the value of the each key
						if (toolKeyValueSetFromDataModel.getKey().equals(TraceabilityConstants.ORDER)) {
							formattedJsonObject.addProperty(toolKeyValueSetFromDataModel.getKey(),
									toolKeyValueSetFromDataModel.getValue().toString());
						} 
						if (!toolKeyValueSetFromDataModel.getValue().isJsonArray()) {

							JsonElement propertyValFromNeo4j = toolsArray.get(1).getAsJsonArray()
									.get(i).getAsJsonObject()
									.get(toolKeyValueSetFromDataModel.getValue().getAsString());

							if (propertyValFromNeo4j != null) {
								String neo4jValue = propertyValFromNeo4j.getAsString();
								formattedJsonObject.addProperty(toolKeyValueSetFromDataModel.getKey(), neo4jValue);
							}
						}
					}
					/* Add toolStatus property explicitly if the object does not have it already */
					if (formattedJsonObject.get("toolstatus") == null)
						formattedJsonObject.addProperty("toolstatus", "Success");
					formattedJsonObject.addProperty("count", Integer.toString(numOfObjectsPerTool));
					toolsPayload.add(formattedJsonObject);
				}

				/* sort the tools payload with timestamp before adding into the mastermap */
				List<JsonObject> sortedPayload = sortToolsPayload(toolsPayload);
				/* prepare the summary */
				/* Add each tool list payload to mastermap with toolname as key */
				masterMap.put(toolNameFromNeo4j, sortedPayload);

				LOG.debug("Traceability ===== Master Response Received Successfully");
			}
			}
		}
		List<Map.Entry<String, List<JsonObject>>> list = new LinkedList<>(masterMap.entrySet());
		Collections.sort(list, (Map.Entry<String, List<JsonObject>> o1, Map.Entry<String, List<JsonObject>> o2) -> {
			if (o1.getValue().get(0).has(TraceabilityConstants.ORDER)) {
				return (Integer.valueOf(o1.getValue().get(0).get(TraceabilityConstants.ORDER).getAsInt()).compareTo(
						(Integer.valueOf(o2.getValue().get(0).get(TraceabilityConstants.ORDER).getAsInt()))));
			} else {
				return -1;
			}
		});
		for (Map.Entry<String, List<JsonObject>> entry : list) {
			sortedmasterMap.put(entry.getKey(), entry.getValue());
		}
		/* prepare the summary */
		JsonObject finalObj = new JsonObject();
		//check this as it is not being used
		finalObj.add("data", finaltoolsArray);
		return sortedmasterMap;
	}

	private JsonObject prepareSummary(HashMap<String, List<JsonObject>> masterMap, JsonObject dataModel)
			throws InsightsCustomException {

		final String OPERAND_NAME = "OperandName";
		final String OPERAND_VALUE = "OperandValue";
		HashMap<String, List<JsonObject>> map = masterMap;
		JsonObject summaryObject = new JsonObject();
		for (Map.Entry<String, List<JsonObject>> entry : map.entrySet()) {
			try {
				String tool = entry.getKey();
				List<JsonObject> payload = entry.getValue();
				JsonObject summary = new JsonObject();
				JsonObject toolObjectFromDataModel = dataModel.get(tool).getAsJsonObject();
				if (toolObjectFromDataModel.has(TraceabilityConstants.MESSAGES)) // check the toolname has message
				{
					int messageSize = toolObjectFromDataModel.get(TraceabilityConstants.MESSAGES).getAsJsonArray().size();
					for (int i = 0; i < messageSize; i++) {
						JsonObject messageClause = toolObjectFromDataModel.get(TraceabilityConstants.MESSAGES).getAsJsonArray().get(i)
								.getAsJsonObject();
						String operationName = messageClause.get("Operation").getAsString();
						if (operationName.equals("SUM")) {
							String operandName = messageClause.get(OPERAND_NAME).getAsString();					
							JsonArray operandValue = messageClause.get(OPERAND_VALUE).getAsJsonArray();
							String message = messageClause.get(TraceabilityConstants.MESSAGE).getAsString();
							String resp = TraceabilitySummaryUtil.calSUM(operandName, operandValue, payload, message);
							if (!resp.equals("")) {
								summary.addProperty(String.valueOf(i), resp);
								summaryObject.add(tool, summary);
							}
						}
						if (operationName.equals("PERCENTAGE")) {
							String operandName = messageClause.get(OPERAND_NAME).getAsString();
							
							JsonArray operandValue = messageClause.get(OPERAND_VALUE).getAsJsonArray();
							String message = messageClause.get(TraceabilityConstants.MESSAGE).getAsString();
							String resp = TraceabilitySummaryUtil.calPercentage(operandName, operandValue, payload,
									message);
							if (!resp.equals("")) {
								summary.addProperty(String.valueOf(i), resp);
								summaryObject.add(tool, summary);
							}
						}
						if (operationName.equals("TIMEDIFF")) {
							String operandName = messageClause.get(OPERAND_NAME).getAsString();
							String message = messageClause.get(TraceabilityConstants.MESSAGE).getAsString();
							String resp;
							resp = TraceabilitySummaryUtil.calTimeDiffrence(operandName, payload, message);
							if (!resp.equals("")) {
								summary.addProperty(String.valueOf(i), resp);
								summaryObject.add(tool, summary);
							}

						}
					}
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
				throw new InsightsCustomException("Traceability matrix not configured properly...");
			}
		}
		return summaryObject;

	}

	private List<String> getDownTool(String toolName, JsonObject dataModel) throws InsightsCustomException {
		final String DOWNTOOL = "downtool";
		List<String> tools = new ArrayList<>();
		JsonObject toolObject = dataModel.getAsJsonObject(toolName);
		LOG.debug("Traceability======== inside downtool collection method for tool {}",toolName);
		if (toolObject.has(DOWNTOOL)) {
			if (toolObject.get(DOWNTOOL).isJsonArray()) {
				JsonArray toolArray = toolObject.get(DOWNTOOL).getAsJsonArray();
				for (int i = 0; i < toolArray.size(); i++) {
					tools.add(toolArray.get(i).getAsString());
				}
				return tools;
			} else {
				throw new InsightsCustomException("Traceability matrix not configured properly.");
			}
		} else {
			throw new InsightsCustomException("Traceability matrix not configured properly.");
		}

	}

	private HashMap<String, List<String>> resolveUpAndDownLinks(HashMap<String, List<String>> drilldownListMap,
			List<String> baseTool) throws  InsightsCustomException {

		HashMap<String, List<String>> mainToolList = new HashMap<>();
		List<String> excludeLabels = new ArrayList<>();
		excludeLabels.addAll(baseTool);
		while (drilldownListMap.size() > 0) {
			List<String> labelsTemp = new ArrayList<>();
			HashMap<String, List<String>> tempMap = new HashMap<>();
			for (Map.Entry<String, List<String>> entry : drilldownListMap.entrySet()) {
				String tool = entry.getKey();
				List<String> uuids = entry.getValue();
				mainToolList.put(tool, uuids);				
				if(!(dataModel.has(tool) && dataModel.get(tool).getAsJsonObject().has(CATEGORY)))
				{
					throw new InsightsCustomException("No category defined for tool :"+tool);
				}		
				String category=dataModel.get(tool).getAsJsonObject().get(CATEGORY).getAsString();
				String cypher = buildCypherQuery(tool, "uuid",category, uuids, excludeLabels, 2);
				/* collect all parent or child uuids for current basenode. */
				tempMap.putAll(format(executeCypherQuery(cypher), Arrays.asList(tool)));
				/* collect the tools as basetool for the next hop in drilldown */
				labelsTemp.addAll(Arrays.asList(tool));

			}
			excludeLabels = (labelsTemp.stream().distinct().collect(Collectors.toList()));
			drilldownListMap.clear();
			drilldownListMap.putAll(tempMap);

		}
		return mainToolList;
	}

	@Override
	public JsonObject getPipeline(String toolName, String fieldName, String fieldValue,boolean isEpic)  throws InsightsCustomException {
		loadTraceabilityJson();
		this.toolName = toolName;
		this.fieldName = fieldName;
		this.fieldValue = fieldValue;
		cacheKey = toolName + "." + fieldName + "." + fieldValue;
		if (pipelineCache.get(cacheKey) != null) {
			LOG.debug("Pipeline response loaded from cache");
			return new JsonParser().parse(pipelineCache.get(cacheKey)).getAsJsonObject();
		} else {
			try {
				/* Load JsonObject from DataModel.json for processing */
				/* check if it is already loaded */
				HashMap<String, List<String>> uplinkMap = new HashMap<>();
				HashMap<String, List<String>> downlinkMap = new HashMap<>();
				HashMap<String, List<String>> toolsListMap = new HashMap<>();
				/* Get the uuid of the tool selected in UI and store it in Main Map */
				if (!dataModel.get(toolName).getAsJsonObject().has(CATEGORY)) {
					throw new InsightsCustomException("No category defined for tool :" + this.toolName);
				}
			    String toolCategory = dataModel.get(toolName).getAsJsonObject().get(CATEGORY).getAsString();
				if (isEpic) {
					return processEpic(toolName, fieldName, toolCategory,
							new ArrayList<String>(Arrays.asList(fieldValue)));

				} else {
					String cypher = buildCypherQuery(toolName, fieldName, toolCategory,
							new ArrayList<String>(Arrays.asList(fieldValue)), Collections.emptyList(), 1);
					toolsListMap.putAll(format(executeCypherQuery(cypher), Collections.emptyList()));

					/* Get the upTool and DownTool of the selected tool in UI */
					List<String> upTools = getUpTool(toolName, dataModel);
					List<String> downTools = getDownTool(toolName, dataModel);
					/*
					 * Execute the first query to get the linked tools for the tool selected in UI
					 */
					HashMap<String, List<String>> temp = (HashMap<String, List<String>>) format(
							executeCypherQuery(buildCypherQuery(toolName, fieldName, toolCategory,
									new ArrayList<String>(Arrays.asList(fieldValue)), Collections.emptyList(), 2)),
							Arrays.asList(toolName));

					if (temp.size() > 0) {
						uplinkMap = (HashMap<String, List<String>>) temp.entrySet().stream()
								.filter(e -> upTools.stream().anyMatch(e.getKey()::equals))
								.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

						downlinkMap = (HashMap<String, List<String>>) temp.entrySet().stream()
								.filter(e -> downTools.stream().anyMatch(e.getKey()::equals))
								.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
					}
					if (uplinkMap.size() > 0) {
						toolsListMap.putAll(resolveUpAndDownLinks(uplinkMap, Arrays.asList(toolName)));
						LOG.debug("drilldown completed for uplink");
					}
					if (downlinkMap.size() > 0) {
						toolsListMap.putAll(resolveUpAndDownLinks(downlinkMap, Arrays.asList(toolName)));
						LOG.debug("drilldown completed for downlink");
					}
					List<String> uuids = new ArrayList<>();
					for (List<String> uuidList : toolsListMap.values()) {
						uuidList.forEach(uuids::add);
					}
					String pipelineCypher = buildCypherQuery(null, null, "", uuids, null, 3);
					JsonObject neo4jResponse = executeCypherQuery(pipelineCypher);
					LOG.debug("pipeline  response received from neo4j");
					/* Get the MasterResponse Map which will used for caching */
					LinkedHashMap<String, List<JsonObject>> masterMap = (LinkedHashMap<String, List<JsonObject>>) getMasterResponse(
							neo4jResponse, dataModel);
					masterdataCache.put(cacheKey, masterMap.toString());
					LOG.debug("Traceability data has been loaded successfully");
					/* Filter MaterMap and send only first 4 JsonObjects for the tool */
					JsonObject response = getPipeLineResponse(masterMap, dataModel);
					LOG.debug("Pipeline response prepared successfully");
					/* Get the MasterResponse Map which will used for caching */
					pipelineCache.put(cacheKey, response.toString());
					return response;
				}

			} catch (JsonSyntaxException | JsonIOException | InsightsCustomException ex1) {
				LOG.error(ex1.getMessage());
				throw new InsightsCustomException(ex1.getMessage());
			}
		}
	}

	private JsonObject processEpic(String toolName, String fieldName, String toolCategory, ArrayList<String> arrayList) throws InsightsCustomException {
		
		JsonObject responseObject = new JsonObject();		
		JsonArray issuesArray = new JsonArray();
		String fieldValue=arrayList.get(0);
		
		// To get epic node response
		String cypher = buildCypherQuery(toolName, fieldName, toolCategory,
				new ArrayList<String>(Arrays.asList(fieldValue)), Collections.emptyList(), 4);
		JsonObject response = executeCypherQuery(cypher);
		List<JsonObject> epic = formatResponse(response, toolName);		
		epic.forEach(eachObject->eachObject.addProperty("toolName", "Epic"));
		// to get epic issues
		cypher = buildCypherQuery(toolName, fieldName, toolCategory, new ArrayList<String>(Arrays.asList(fieldValue)),
				Collections.emptyList(), 5);
		response = executeCypherQuery(cypher);
		List<JsonObject> issues = formatResponse(response, toolName);		
		issues.forEach(eachIssue->eachIssue.addProperty("order", "2"));		
		LOG.debug(responseObject);	
		
		
		
		List<JsonObject>epicIssues=Stream.concat(epic.stream(), issues.stream()).collect(Collectors.toList());
		Map<String, List<JsonObject>> masterMap =epicIssues.stream().collect(Collectors.groupingBy(eachObject->eachObject.get("toolName").getAsString()));
		epicIssues.forEach(issuesArray::add);
		responseObject.add("pipeline", issuesArray);			
		responseObject.add("summary", new JsonArray());
		responseObject.add("timelag", new JsonArray());	
		pipelineCache.put(toolName+"."+fieldName+"."+fieldValue, responseObject.toString());	
		masterdataCache.put(toolName+"."+fieldName+"."+fieldValue, masterMap.toString());
		return responseObject;
	
		
		
		
	}
	public JsonObject getIssuePipeline(String issue) throws InsightsCustomException
	{
		JsonObject responsePipeline = new JsonObject();
		try {		
			JsonParser parser = new JsonParser();
			JsonObject obj = (JsonObject) parser.parse(issue);
			String epicKey = obj.get("epicKey").getAsString();
			LOG.debug("Tracebility=====response has epicKey {}", epicKey);
			JsonObject toolObj = dataModel.get(toolName).getAsJsonObject();
			Set<String> keys = toolObj.keySet();
			for (String key : keys) {
				if (toolObj.has(key) && !toolObj.get(key).isJsonArray()
						&& toolObj.get(key).getAsString().equals(fieldName)) {
					fieldValue = obj.get(key).getAsString();
				}

			}
			String cacheRetrivalKey = toolName + "." + fieldName + "." + epicKey;
			JsonObject cachedResponseObject = new JsonParser().parse(pipelineCache.get(cacheRetrivalKey))
					.getAsJsonObject();
			JsonArray cachedResponse = cachedResponseObject.get("pipeline").getAsJsonArray();
			List<JsonObject> cachedRepsonseList = new ArrayList<>();
			cachedResponse.forEach(element -> cachedRepsonseList.add(element.getAsJsonObject()));
			responsePipeline = getPipeline(toolName, fieldName, fieldValue, false);
			JsonArray pipelineArray = responsePipeline.get("pipeline").getAsJsonArray();
			List<JsonObject> filteredList = new ArrayList<>();

			for (JsonElement element : pipelineArray) {

				JsonObject eachObj = element.getAsJsonObject();
				if (!eachObj.get("toolName").getAsString().equals(toolName)) {
					eachObj.addProperty("order", eachObj.get("order").getAsInt() + 1);
					filteredList.add(eachObj);
				}
			}
			List<JsonObject> finalPipeline = Stream.concat(filteredList.stream(), cachedRepsonseList.stream())
					.collect(Collectors.toList());
			pipelineArray = new JsonArray();
			for (JsonObject finalObject : finalPipeline) {
				pipelineArray.add(finalObject);
			}
			responsePipeline.add("pipeline", pipelineArray);
		} catch (Exception e) {
			LOG.error("Traceability========mandatory fields are missing in traceability json {}", e.getMessage());
			throw new InsightsCustomException("Mandatory properties are missing in datamodel!");
		}

		return responsePipeline;
	}

	private List<JsonObject> formatResponse(JsonObject response,String toolName) {
		
		// Master Map contains toolname as string and list of toolpayload .	
		List<JsonObject> issues = new ArrayList<>();
		List<JsonObject> sortedList ;
		JsonArray responseArray = response.getAsJsonArray(TraceabilityConstants.RESULTS).getAsJsonArray().get(0)
				.getAsJsonObject().get("data").getAsJsonArray();
		int count = responseArray.size();
		for (int j = 0; j < count; j++) {
			// find the toolname in neo4j response
			LOG.debug("Data From Neo4J has been loaded properly");
			JsonArray toolsArray = responseArray.get(j).getAsJsonObject().get("row").getAsJsonArray();
			JsonObject toolPayloadFromDatamodel = dataModel.get(toolName).getAsJsonObject();
			if (toolPayloadFromDatamodel != null) {
				Set<Entry<String, JsonElement>> keyset = toolPayloadFromDatamodel.entrySet();
				JsonObject neo4jEacRowResponse = toolsArray.get(0).getAsJsonObject();
				JsonObject formattedJsonObject = new JsonObject();
				for (Map.Entry<String, JsonElement> toolKeyValueSetFromDataModel : keyset) {
					// Every tool is JsonArray so loop it if it has more than one element get
					// extract the value of the each key
					if (toolKeyValueSetFromDataModel.getKey().equals(TraceabilityConstants.ORDER)) {
						formattedJsonObject.addProperty(toolKeyValueSetFromDataModel.getKey(),
								toolKeyValueSetFromDataModel.getValue().toString());
					}
					if (!toolKeyValueSetFromDataModel.getValue().isJsonArray()) {
						JsonElement propertyValFromNeo4j = neo4jEacRowResponse
								.get(toolKeyValueSetFromDataModel.getValue().getAsString());
						if (propertyValFromNeo4j != null) {
							String neo4jValue = propertyValFromNeo4j.getAsString();
							formattedJsonObject.addProperty(toolKeyValueSetFromDataModel.getKey(), neo4jValue);
						}
					}
				}
				formattedJsonObject.addProperty("count", Integer.toString(count));
				issues.add(formattedJsonObject);
			}		
		}
		sortedList = sortToolsPayload(issues);
		
		return sortedList;
		}
		
	

	@Override
	public List<JsonObject> getToolSummary(String toolName, String cacheKey) throws InsightsCustomException {
		String toolSummary = masterdataCache.get(cacheKey);
		LOG.error(cacheKey);
		if (toolSummary != null) {
			Gson gson = new Gson();
			Map<String, List<JsonObject>> attributes = gson.fromJson(toolSummary, Map.class);
			List<JsonObject> toolPayload = attributes.get(toolName);
			LOG.debug("Summary data loaded from cache");
			return toolPayload;
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<String> getAvailableTools() throws InsightsCustomException {		
		loadTraceabilityJson();	
		List<String> availableTools = new ArrayList<>();
		try {
			availableTools = dataModel.keySet().stream().collect(Collectors.toList());
		} catch (Exception e) {
			LOG.error("Treceability ==== Unable to load  datamodel");
			throw new InsightsCustomException(e.getMessage());
		}
		return availableTools;
	}

	@Override
	public List<String> getToolKeyset(String toolName) throws InsightsCustomException {
		loadTraceabilityJson();		
		final String FILTER = "uifilter";
		List<String> tools = new ArrayList<>();
		JsonObject toolObject = dataModel.getAsJsonObject(toolName);
		if (toolObject.has(FILTER) && toolObject.get(FILTER).isJsonArray()) {
			JsonArray toolArray = toolObject.get(FILTER).getAsJsonArray();
			for (int i = 0; i < toolArray.size(); i++) {
				tools.add(toolArray.get(i).getAsString());
			}
			return tools;
		}
		return Collections.emptyList();
	}
	
}
