/*******************************************************************************
 * Copyright 2020 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package com.cognizant.devops.platformservice.vsmreport.controller;

import com.cognizant.devops.platformcommons.exception.InsightsCustomException;
import com.cognizant.devops.platformservice.rest.util.PlatformServiceUtil;
import com.cognizant.devops.platformservice.vsmreport.service.VsmReportService;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/externalApi")
public class ExternalApiController {
    static Logger log = LogManager.getLogger(ExternalApiController.class);

    @Autowired
    VsmReportService vsmReportServiceImpl;

    @PostMapping(value = "/importVsmReport", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    JsonObject importVsmReport(@RequestParam String uuid, @RequestParam String fileName, @RequestParam("file") MultipartFile file, @RequestParam String email) {
        try {
            boolean checkValidFile = PlatformServiceUtil.validateFile(file.getOriginalFilename());
            log.debug("checkValidFile: {} ", checkValidFile);
            if (checkValidFile) {
                vsmReportServiceImpl.saveVsmReport(uuid, fileName, file, email);
                return PlatformServiceUtil.buildSuccessResponse();
            } else {
                return PlatformServiceUtil.buildFailureResponse("Error while parsing file , Please try again !");
            }
        } catch (InsightsCustomException e) {
            log.error(e.getStackTrace());
            return PlatformServiceUtil.buildFailureResponse(e.getMessage());
        }
    }
}
