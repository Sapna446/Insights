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

package com.cognizant.devops.platformdal.vsmReport;

import com.cognizant.devops.platformdal.workflow.InsightsWorkflowConfiguration;

import javax.persistence.*;

@Entity
@Table(name = "\"INSIGHTS_VSMREPORT_CONFIGURATION\"")
public class VsmReportConfig {
    @Id
    @Column(name = "ID", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "UUID")
    private String uuid;

    @Column(name = "FILENAME")
    private String fileName;

    @Column(name = "FILE")
    private byte[] file;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "CREATED_DATE")
    private Long createdDate;

    @Column(name = "UPDATED_DATE")
    private Long updatedDate = 0L;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "workflowId", referencedColumnName = "workflowId")
    private InsightsWorkflowConfiguration workflowConfig;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public Long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Long updatedDate) {
        this.updatedDate = updatedDate;
    }

    public InsightsWorkflowConfiguration getWorkflowConfig() {
        return workflowConfig;
    }

    public void setWorkflowConfig(InsightsWorkflowConfiguration workflowConfig) {
        this.workflowConfig = workflowConfig;
    }
}
