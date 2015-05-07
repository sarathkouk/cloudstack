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

import java.util.Date;

import com.cloud.serializer.Param;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.solidfire.SfApiConstants;
import org.apache.cloudstack.solidfire.SfVolume;

@EntityReference(value = SfVolume.class)
public class ApiSolidFireVolumeResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "CloudStack ID")
    private long _id;

    @SerializedName("uuid")
    @Param(description = "CloudStack UUID")
    private String _uuid;

    @SerializedName("name")
    @Param(description = SfApiConstants.VOLUME_NAME_DESC)
    private String _name;

    @SerializedName("iqn")
    @Param(description = SfApiConstants.IQN_DESC)
    private String _iqn;

    @SerializedName("size")
    @Param(description = SfApiConstants.SIZE_DESC)
    private long _size;

    @SerializedName("miniops")
    @Param(description = SfApiConstants.MIN_IOPS_DESC)
    private long _minIops;

    @SerializedName("maxiops")
    @Param(description = SfApiConstants.MAX_IOPS_DESC)
    private long _maxIops;

    @SerializedName("burstiops")
    @Param(description = SfApiConstants.BURST_IOPS_DESC)
    private long _burstIops;

    @SerializedName("accountid")
    @Param(description = "ID of the account the volume is associated with")
    private long _accountId;

    @SerializedName("accountuuid")
    @Param(description = "UUID of the account the volume is associated with")
    private String _accountUuid;

    @SerializedName("accountname")
    @Param(description = "Name of the account the volume is associated with")
    private String _accountName;

    @SerializedName("vlanid")
    @Param(description = "ID of the VLAN the volume is associated with")
    private long _vlanId;

    @SerializedName("vlanuuid")
    @Param(description = "UUID of the VLAN the volume is associated with")
    private String _vlanUuid;

    @SerializedName("vlanname")
    @Param(description = "Name of the VLAN the volume is associated with")
    private String _vlanName;

    @SerializedName("zoneid")
    @Param(description = "ID of the zone the volume is associated with")
    private long _zoneId;

    @SerializedName("zoneuuid")
    @Param(description = "UUID of the zone the volume is associated with")
    private String _zoneUuid;

    @SerializedName("zonename")
    @Param(description = "Name of the zone the volume is associated with")
    private String _zoneName;

    @SerializedName("clustername")
    @Param(description = "Name of cluster the volume belongs to")
    private String _clusterName;

    @SerializedName("targetportal")
    @Param(description = "Target portal")
    private String _targetPortal;

    @SerializedName("chapinitiatorusername")
    @Param(description = "CHAP initiator username")
    private String _chapInitiatorUsername;

    @SerializedName("chapinitiatorsecret")
    @Param(description = "CHAP initiator secret")
    private String _chapInitiatorSecret;

    @SerializedName("chaptargetusername")
    @Param(description = "CHAP target username")
    private String _chapTargetUsername;

    @SerializedName("chaptargetsecret")
    @Param(description = "CHAP target secret")
    private String _chapTargetSecret;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Date volume was created")
    private Date _created;

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

    public void setIqn(String iqn) {
        _iqn = iqn;
    }

    public String getIqn() {
        return _iqn;
    }

    public void setSize(long size) {
        _size = size;
    }

    public long getSize() {
        return _size;
    }

    public void setMinIops(long minIops) {
        _minIops = minIops;
    }

    public long getMinIops() {
        return _minIops;
    }

    public void setMaxIops(long maxIops) {
        _maxIops = maxIops;
    }

    public long getMaxIops() {
        return _maxIops;
    }

    public void setBurstIops(long burstIops) {
        _burstIops = burstIops;
    }

    public long getBurstIops() {
        return _burstIops;
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

    public void setVlanId(long vlanId) {
        _vlanId = vlanId;
    }

    public long getVlanId() {
        return _vlanId;
    }

    public void setVlanUuid(String vlanUuid) {
        _vlanUuid = vlanUuid;
    }

    public String getVlanUuid() {
        return _vlanUuid;
    }

    public void setVlanName(String vlanName) {
        _vlanName = vlanName;
    }

    public String getVlanName() {
        return _vlanName;
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

    public void setTargetPortal(String targetPortal) {
        _targetPortal = targetPortal;
    }

    public String getTargetPortal() {
        return _targetPortal;
    }

    public void setChapInitiatorUsername(String chapInitiatorUsername) {
        _chapInitiatorUsername = chapInitiatorUsername;
    }

    public String getChapInitiatorUsername() {
        return _chapInitiatorUsername;
    }

    public void setChapInitiatorSecret(String chapInitiatorSecret) {
        _chapInitiatorSecret = chapInitiatorSecret;
    }

    public String getChapInitiatorSecret() {
        return _chapInitiatorSecret;
    }

    public void setChapTargetUsername(String chapTargetUsername) {
        _chapTargetUsername = chapTargetUsername;
    }

    public String getTargetInitiatorUsername() {
        return _chapTargetUsername;
    }

    public void setChapTargetSecret(String chapTargetSecret) {
        _chapTargetSecret = chapTargetSecret;
    }

    public String getTargetInitiatorSecret() {
        return _chapTargetSecret;
    }

    public void setCreated(Date created) {
        _created = created;
    }

    public Date getCreated() {
        return _created;
    }
}
