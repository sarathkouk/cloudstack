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
package org.apache.cloudstack.api.response.solidfire;

import com.cloud.serializer.Param;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.solidfire.SfApiConstants;
import org.apache.cloudstack.solidfire.SfVirtualNetwork;

@EntityReference(value = SfVirtualNetwork.class)
public class ApiSolidFireVirtualNetworkResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "CloudStack ID")
    private long _id;

    @SerializedName("uuid")
    @Param(description = "CloudStack UUID")
    private String _uuid;

    @SerializedName("name")
    @Param(description = SfApiConstants.VIRTUAL_NETWORK_NAME_DESC)
    private String _name;

    @SerializedName("tag")
    @Param(description = SfApiConstants.VIRTUAL_NETWORK_TAG_DESC)
    private String _tag;

    @SerializedName("startip")
    @Param(description = SfApiConstants.START_IP_ADDRESS_DESC)
    private String _startIp;

    @SerializedName("size")
    @Param(description = SfApiConstants.SIZE_DESC)
    private int _size;

    @SerializedName("netmask")
    @Param(description = SfApiConstants.NETMASK_DESC)
    private String _netmask;

    @SerializedName("svip")
    @Param(description = SfApiConstants.SOLIDFIRE_SVIP_DESC)
    private String _svip;

    @SerializedName("accountid")
    @Param(description = "ID of the account the VLAN is associated with")
    private long _accountId;

    @SerializedName("accountuuid")
    @Param(description = "UUID of the account the VLAN is associated with")
    private String _accountUuid;

    @SerializedName("accountname")
    @Param(description = "Name of the account the volume is associated with")
    private String _accountName;

    @SerializedName("zoneid")
    @Param(description = "ID of the zone the VLAN is associated with")
    private long _zoneId;

    @SerializedName("zoneuuid")
    @Param(description = "UUID of the zone the VLAN is associated with")
    private String _zoneUuid;

    @SerializedName("zonename")
    @Param(description = "Name of the zone the volume is associated with")
    private String _zoneName;

    @SerializedName("clustername")
    @Param(description = "Name of cluster the VLAN belongs to")
    private String _clusterName;

    public void setId(long id) {
        _id = id;
    }

    public long getId() {
        return _id;
    }

    public void setUuid(String uuid) {
        _uuid = uuid;
    }

    public String getUuid() {
        return _uuid;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public void setTag(String tag) {
        _tag = tag;
    }

    public String getTag() {
        return _tag;
    }

    public void setStartIp(String startIp) {
        _startIp = startIp;
    }

    public String getStartIp() {
        return _startIp;
    }

    public void setSize(int size) {
        _size = size;
    }

    public int getSize() {
        return _size;
    }

    public void setNetmask(String netmask) {
        _netmask = netmask;
    }

    public String getNetmask() {
        return _netmask;
    }

    public void setSvip(String svip) {
        _svip = svip;
    }

    public String getSvip() {
        return _svip;
    }

    public void setAccountId(long accountId) {
        _accountId = accountId;
    }

    public long getAccountId() {
        return _accountId;
    }

    public void setAccountUuid(String accountUuid) {
        _accountUuid = accountUuid;
    }

    public String getAccountUuid() {
        return _accountUuid;
    }

    public void setAccountName(String accountName) {
        _accountName = accountName;
    }

    public String getAccountName() {
        return _accountName;
    }

    public void setZoneId(long zoneId) {
        _zoneId = zoneId;
    }

    public long getZoneId() {
        return _zoneId;
    }

    public void setZoneUuid(String zoneUuid) {
        _zoneUuid = zoneUuid;
    }

    public String getZoneUuid() {
        return _zoneUuid;
    }

    public void setZoneName(String zoneName) {
        _zoneName = zoneName;
    }

    public String getZoneName() {
        return _zoneName;
    }

    public void setClusterName(String clusterName) {
        _clusterName = clusterName;
    }

    public String getClusterName() {
        return _clusterName;
    }
}
