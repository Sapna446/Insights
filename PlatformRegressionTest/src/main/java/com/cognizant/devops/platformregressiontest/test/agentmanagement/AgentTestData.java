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
package com.cognizant.devops.platformregressiontest.test.agentmanagement;

import java.io.File;
import java.io.IOException;
import org.testng.annotations.DataProvider;
import com.cognizant.devops.platformregressiontest.test.common.ConfigOptionsTest;
import com.cognizant.devops.platformregressiontest.test.common.XLUtils;

public class AgentTestData {
	public static final String AGENT_REGISTRATION = "AgentRegistration";
	public static final String  DELETE_AGENT= "DeleteAgent";
	public static final String UPDATE_AGENT = "UpdateAgent";
	@DataProvider(name = "agentdataprovider")
	String[][] getAgentData() throws IOException {

		String path = System.getenv().get(ConfigOptionsTest.INSIGHTS_HOME) + File.separator
				+ ConfigOptionsTest.AUTO_DIR + File.separator + ConfigOptionsTest.TESTDATA_FILE;

		int rowNum = XLUtils.getRowCount(path,  AGENT_REGISTRATION);
		int colNum = XLUtils.getCellCount(path, AGENT_REGISTRATION, 1);

		String agentData[][] = new String[rowNum][colNum];

		for (int i = 1; i <= rowNum; i++) {

			for (int j = 0; j < colNum; j++) {

				agentData[i - 1][j] = XLUtils.getCellData(path, AGENT_REGISTRATION, i, j);

			}
		}

		return (agentData);

	}

	@DataProvider(name = "agentupdateprovider")
	String[][] getUpdateAgentData() throws IOException {

		String path = System.getenv().get(ConfigOptionsTest.INSIGHTS_HOME) + File.separator
				+ ConfigOptionsTest.AUTO_DIR + File.separator + ConfigOptionsTest.TESTDATA_FILE;

		int rowNum = XLUtils.getRowCount(path, UPDATE_AGENT);
		int colNum = XLUtils.getCellCount(path, UPDATE_AGENT, 1);

		String updateAgentData[][] = new String[rowNum][colNum];

		for (int i = 1; i <= rowNum; i++) {

			for (int j = 0; j < colNum; j++) {

				updateAgentData[i - 1][j] = XLUtils.getCellData(path, UPDATE_AGENT, i, j);

			}
		}

		return (updateAgentData);

	}

	@DataProvider(name = "agentdeletedataprovider")
	String[][] getAgentDeleteData() throws IOException {

		String path = System.getenv().get(ConfigOptionsTest.INSIGHTS_HOME) + File.separator
				+ ConfigOptionsTest.AUTO_DIR + File.separator + ConfigOptionsTest.TESTDATA_FILE;

		int rowNum = XLUtils.getRowCount(path, DELETE_AGENT);
		int colNum = XLUtils.getCellCount(path, DELETE_AGENT, 1);

		String agentDeleteData[][] = new String[rowNum][colNum];

		for (int i = 1; i <= rowNum; i++) {

			for (int j = 0; j < colNum; j++) {

				agentDeleteData[i - 1][j] = XLUtils.getCellData(path, DELETE_AGENT, i, j);

			}
		}

		return (agentDeleteData);

	}

}
