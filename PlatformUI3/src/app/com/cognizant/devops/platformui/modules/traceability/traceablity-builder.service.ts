import { Injectable } from '@angular/core';
import { RestCallHandlerService } from '@insights/common/rest-call-handler.service';
import { HttpClient } from '@angular/common/http';
import { DataSharedService } from '@insights/common/data-shared-service';


export interface ITraceablityService {

}

@Injectable()
export class TraceabiltyService implements ITraceablityService {

    constructor(private restCallHandlerService: RestCallHandlerService, private httpClient: HttpClient,
        private dataShare: DataSharedService) {
    }

    getAssetHistory(toolName: string, toolField: string, toolValue: string): Promise<any> {
        var restHandler = this.restCallHandlerService;
        return restHandler.get("GET_DETAILS", {'toolName': toolName, 'fieldName': toolField, 'fieldValue': toolValue });
    }

    getEpicIssues(toolName: string, toolField: string, toolValue: string,isEpic:boolean): Promise<any> {
        var restHandler = this.restCallHandlerService;
        return restHandler.get("GET_EPIC_ISSUES", {'toolName': toolName, 'fieldName': toolField, 'fieldValue': toolValue,'isEpic':isEpic });
    }

    getIssuesPipeline(issue:string): Promise<any>{
        var restHandler = this.restCallHandlerService;
       // return restHandler.get("GET_ISSUES_PIPELINE", {'issue': issue});
       return this.restCallHandlerService.postWithData("GET_ISSUES_PIPELINE", issue, { 'Content-Type': 'application/x-www-form-urlencoded' }).toPromise();
    }

    

    getAsssetDetails(toolName: string, cachestring: string) {
        var restHandler = this.restCallHandlerService;
        return restHandler.get("GET_TOOL_DETAILS", {'toolName': toolName, 'cacheKey': cachestring });
    }
    getAvailableTools()
    {
        var restHandler = this.restCallHandlerService;
        return restHandler.get("GET_TOOL_LIST", { });
    }
    getToolKeyset(toolName: string)
    {
        var restHandler = this.restCallHandlerService;
        return restHandler.get("GET_TOOL_KEYSET", {'toolName': toolName });   
    }
}