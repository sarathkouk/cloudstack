// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.admin.solidfire;

import com.cloud.user.Account;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireClusterResponse;
import org.apache.cloudstack.api.solidfire.SfApiConstants;
import org.apache.cloudstack.solidfire.SfCluster;
import org.apache.cloudstack.solidfire.SolidFireManager;
import org.apache.cloudstack.util.solidfire.SfUtil;

@APICommand(name = "createReferenceToSolidFireCluster", responseObject = ApiSolidFireClusterResponse.class, description = "Create Reference to SolidFire Cluster",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateReferenceToSolidFireClusterCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(CreateReferenceToSolidFireClusterCmd.class.getName());
    private static final String s_name = "createreferencetosolidfireclusterresponse";

    @Parameter(name = SfApiConstants.MVIP, type = CommandType.STRING, description = SfApiConstants.SOLIDFIRE_MVIP_DESC, required = true)
    private String _mvip;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = SfApiConstants.SOLIDFIRE_USERNAME_DESC, required = true)
    private String _username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, description = SfApiConstants.SOLIDFIRE_PASSWORD_DESC, required = true)
    private String _password;

    @Parameter(name = SfApiConstants.TOTAL_CAPACITY, type = CommandType.LONG, description = SfApiConstants.TOTAL_CAPACITY_DESC, required = true)
    private long _totalCapacity;

    @Parameter(name = SfApiConstants.TOTAL_MIN_IOPS, type = CommandType.LONG, description = SfApiConstants.TOTAL_MIN_IOPS_DESC, required = true)
    private long _totalMinIops;

    @Parameter(name = SfApiConstants.TOTAL_MAX_IOPS, type = CommandType.LONG, description = SfApiConstants.TOTAL_MAX_IOPS_DESC, required = true)
    private long _totalMaxIops;

    @Parameter(name = SfApiConstants.TOTAL_BURST_IOPS, type = CommandType.LONG, description = SfApiConstants.TOTAL_BURST_IOPS_DESC, required = true)
    private long _totalBurstIops;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = SfApiConstants.ZONE_ID_DESC, required = true)
    private long _zoneId;

    @Inject private SolidFireManager _solidFireManager;
    @Inject private SfUtil _sfUtil;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        try {
            s_logger.info(CreateReferenceToSolidFireClusterCmd.class.getName() + ".execute invoked");

            SfCluster sfCluster = _solidFireManager.createReferenceToSolidFireCluster(_mvip, _username, _password, _totalCapacity,
                    _totalMinIops, _totalMaxIops, _totalBurstIops, _zoneId);

            ApiSolidFireClusterResponse response = _sfUtil.getApiSolidFireClusterResponse(sfCluster);

            response.setResponseName(getCommandName());
            response.setObjectName("apicreatereferencetosolidfirecluster");

            setResponseObject(response);
        }
        catch (Throwable t) {
            s_logger.error(t.getMessage());

            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, t.getMessage());
        }
    }
}
