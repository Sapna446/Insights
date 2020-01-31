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
package com.cognizant.devops.platformservice.correlationbuilder.service;

import java.util.List;

import com.cognizant.devops.platformcommons.exception.InsightsCustomException;
import com.google.gson.JsonObject;
import com.cognizant.devops.platformdal.correlationConfig.CorrelationConfiguration;
public interface CorrelationBuilderService {	
	public List<CorrelationConfiguration> getCorrelationJson() throws InsightsCustomException;
	public JsonObject getNeo4jJson() throws  InsightsCustomException;
	public Boolean saveConfig(String configDetails)throws InsightsCustomException;
	public Boolean deleteCorrelation(String relationName)throws InsightsCustomException;
	public Boolean updateCorrelationStatus(String flagDeatils )throws InsightsCustomException;
}
