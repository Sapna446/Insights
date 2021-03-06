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
package com.cognizant.devops.platformdal.dashboards;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.cognizant.devops.platformdal.core.BaseDAL;
import com.cognizant.devops.platformdal.user.UserPortfolioEnum;

public class CustomDashboardDAL extends BaseDAL{
	
	private static Logger log = LogManager.getLogger(CustomDashboardDAL.class);
	
	public List<CustomDashboard> getCustomDashboard(UserPortfolioEnum portfolio){
		try (Session session = getSessionObj()) {
			Query<CustomDashboard> createQuery = session
					.createQuery("FROM CustomDashboard C WHERE C.portfolio = :portfolio", CustomDashboard.class);
			createQuery.setParameter("portfolio", portfolio);
			return createQuery.getResultList();

		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;
		}
	}
	
	public boolean addCustomDashboard(String dashboardName, String dashboardJson, UserPortfolioEnum portfolio){
		try (Session session = getSessionObj()) {
		CustomDashboard customDashboard = new CustomDashboard();
		customDashboard.setDashboardName(dashboardName);
		customDashboard.setDashboardJson(dashboardJson);
		customDashboard.setPortfolio(portfolio);
		session.beginTransaction();
		session.save(customDashboard);
		session.getTransaction().commit();		
		return true;
		}catch (Exception e) {
			log.error(e.getMessage());
			throw e;
		}
	}
}
