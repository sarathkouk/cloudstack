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
package org.apache.cloudstack.api.command.user.solidfire;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireVolumeResponse;
import org.apache.cloudstack.api.solidfire.SfApiConstants;
import org.apache.cloudstack.solidfire.SfVolume;
import org.apache.cloudstack.solidfire.SolidFireManager;
import org.apache.cloudstack.util.solidfire.SfUtil;

@APICommand(name = "listSolidFireVolumes", responseObject = ApiSolidFireVolumeResponse.class, description = "List SolidFire Volumes",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSolidFireVolumesCmd extends BaseListCmd {
    private static final Logger s_logger = Logger.getLogger(ListSolidFireVolumesCmd.class.getName());
    private static final String s_name = "listsolidfirevolumesresponse";

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ApiSolidFireVolumeResponse.class, description = SfApiConstants.VOLUME_ID_DESC)
    private Long _id;

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
    public void execute() {
        try {
            s_logger.info(ListSolidFireVolumesCmd.class.getName() + ".execute invoked");

            final List<SfVolume> sfVolumes;

            if (_id != null) {
                sfVolumes = new ArrayList<>();

                SfVolume sfVolume = _solidFireManager.listSolidFireVolume(_id);

                if (sfVolume != null) {
                    sfVolumes.add(sfVolume);
                }
            }
            else {
                sfVolumes = _solidFireManager.listSolidFireVolumes();
            }

            ResponseView responseView = _sfUtil.isRootAdmin() ? ResponseView.Full : ResponseView.Restricted;

            List<ApiSolidFireVolumeResponse> responses = _sfUtil.getApiSolidFireVolumeResponse(sfVolumes, responseView);

            ListResponse<ApiSolidFireVolumeResponse> listReponse = new ListResponse<>();

            listReponse.setResponses(responses);
            listReponse.setResponseName(getCommandName());
            listReponse.setObjectName("apilistsolidfirevolumes");

            setResponseObject(listReponse);
        }
        catch (Throwable t) {
            s_logger.error(t.getMessage());

            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, t.getMessage());
        }
    }
}
