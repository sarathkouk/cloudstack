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
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireVirtualNetworkResponse;
import org.apache.cloudstack.api.solidfire.SfApiConstants;
import org.apache.cloudstack.solidfire.SfVirtualNetwork;
import org.apache.cloudstack.solidfire.SolidFireManager;
import org.apache.cloudstack.util.solidfire.SfUtil;

@APICommand(name = "deleteSolidFireVirtualNetwork", responseObject = ApiSolidFireVirtualNetworkResponse.class, description = "Delete SolidFire Virtual Network",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteSolidFireVirtualNetworkCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(DeleteSolidFireVirtualNetworkCmd.class.getName());
    private static final String s_name = "deletesolidfirevirtualnetworkresponse";

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ApiSolidFireVirtualNetworkResponse.class, description = SfApiConstants.VIRTUAL_NETWORK_ID_DESC, required = true)
    private long _id;

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
        SfVirtualNetwork sfVirtualNetwork = _entityMgr.findById(SfVirtualNetwork.class, _id);

        if (sfVirtualNetwork != null) {
            sfVirtualNetwork.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        try {
            s_logger.info(DeleteSolidFireVirtualNetworkCmd.class.getName() + ".execute invoked");

            SfVirtualNetwork sfVirtualNetwork = _solidFireManager.deleteSolidFireVirtualNetwork(_id);

            ApiSolidFireVirtualNetworkResponse response = _sfUtil.getApiSolidFireVirtualNetworkResponse(sfVirtualNetwork, ResponseView.Full);

            response.setResponseName(getCommandName());
            response.setObjectName("apideletesolidfirevirtualnetwork");

            setResponseObject(response);
        }
        catch (Throwable t) {
            s_logger.error(t.getMessage());

            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, t.getMessage());
        }
    }
}
