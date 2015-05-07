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

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireVirtualNetworkResponse;
import org.apache.cloudstack.api.solidfire.SfApiConstants;
import org.apache.cloudstack.solidfire.SfVirtualNetwork;
import org.apache.cloudstack.solidfire.SolidFireManager;
import org.apache.cloudstack.util.solidfire.SfUtil;

@APICommand(name = "createSolidFireVirtualNetwork", responseObject = ApiSolidFireVirtualNetworkResponse.class, description = "Create SolidFire Virtual Network",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateSolidFireVirtualNetworkCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(CreateSolidFireVirtualNetworkCmd.class.getName());
    private static final String s_name = "createsolidfirevirtualnetworkresponse";

    @Parameter(name = SfApiConstants.CLUSTER_NAME, type = CommandType.STRING, description = SfApiConstants.SOLIDFIRE_CLUSTER_NAME_DESC, required = true)
    private String _clusterName;

    @Parameter(name = SfApiConstants.NAME, type = CommandType.STRING, description = SfApiConstants.VIRTUAL_NETWORK_NAME_DESC, required = true)
    private String _name;

    @Parameter(name = SfApiConstants.TAG, type = CommandType.STRING, description = SfApiConstants.VIRTUAL_NETWORK_TAG_DESC, required = true)
    private String _tag;

    @Parameter(name = SfApiConstants.START_IP, type = CommandType.STRING, description = SfApiConstants.START_IP_ADDRESS_DESC, required = true)
    private String _startIp;

    @Parameter(name = SfApiConstants.SIZE, type = CommandType.INTEGER, description = SfApiConstants.NUMBER_OF_IP_ADDRESSES_DESC, required = true)
    private int _size;

    @Parameter(name = SfApiConstants.NETMASK, type = CommandType.STRING, description = SfApiConstants.NETMASK_DESC, required = true)
    private String _netmask;

    @Parameter(name = SfApiConstants.SVIP, type = CommandType.STRING, description = SfApiConstants.SOLIDFIRE_SVIP_DESC, required = true)
    private String _svip;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = SfApiConstants.ACCOUNT_ID_DESC, required = true)
    private long _accountId;

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
        return _accountId;
    }

    @Override
    public void execute() {
        try {
            s_logger.info(CreateSolidFireVirtualNetworkCmd.class.getName() + ".execute invoked");

            SfVirtualNetwork sfVirtualNetwork = _solidFireManager.createSolidFireVirtualNetwork(_clusterName, _name, _tag, _startIp, _size, _netmask, _svip, _accountId);

            ApiSolidFireVirtualNetworkResponse response = _sfUtil.getApiSolidFireVirtualNetworkResponse(sfVirtualNetwork, ResponseView.Full);

            response.setResponseName(getCommandName());
            response.setObjectName("apicreatesolidfirevirtualnetwork");

            setResponseObject(response);
        }
        catch (Throwable t) {
            s_logger.error(t.getMessage());

            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, t.getMessage());
        }
    }
}
