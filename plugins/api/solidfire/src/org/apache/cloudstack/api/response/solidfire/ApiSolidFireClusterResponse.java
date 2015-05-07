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
import org.apache.cloudstack.solidfire.SfCluster;

@EntityReference(value = SfCluster.class)
public class ApiSolidFireClusterResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "CloudStack ID")
    private long _id;

    @SerializedName("uuid")
    @Param(description = "CloudStack UUID")
    private String _uuid;

    @SerializedName("name")
    @Param(description = SfApiConstants.SOLIDFIRE_CLUSTER_NAME_DESC)
    private String _name;

    @SerializedName("mvip")
    @Param(description = SfApiConstants.SOLIDFIRE_MVIP_DESC)
    private String _mvip;

    @SerializedName("username")
    @Param(description = SfApiConstants.SOLIDFIRE_USERNAME_DESC)
    private String _username;

    @SerializedName("totalcapacity")
    @Param(description = "SolidFire cluster total capacity for CloudStack (in GBs)")
    private long _totalCapacity;

    @SerializedName("totalminiops")
    @Param(description = "SolidFire cluster total minimum IOPS for CloudStack")
    private long _totalMinIops;

    @SerializedName("totalmaxiops")
    @Param(description = "SolidFire cluster total maximum IOPS for CloudStack")
    private long _totalMaxIops;

    @SerializedName("totalburstiops")
    @Param(description = "SolidFire cluster total burst IOPS for CloudStack")
    private long _totalBurstIops;

    @SerializedName("zoneid")
    @Param(description = "Zone ID that the SolidFire cluster is associated with")
    private long _zoneId;

    @SerializedName("zonename")
    @Param(description = "Zone name that the SolidFire cluster is associated with")
    private String _zoneName;

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

    public void setMvip(String mvip) {
        _mvip = mvip;
    }

    public String getMvip() {
        return _mvip;
    }

    public void setUsername(String username) {
        _username = username;
    }

    public String getUsername() {
        return _username;
    }

    public void setTotalCapacity(long totalCapacity) {
        _totalCapacity = totalCapacity;
    }

    public long getTotalCapacity() {
        return _totalCapacity;
    }

    public void setTotalMinIops(long totalMinIops) {
        _totalMinIops = totalMinIops;
    }

    public long getTotalMinIops() {
        return _totalMinIops;
    }

    public void setTotalMaxIops(long totalMaxIops) {
        _totalMaxIops = totalMaxIops;
    }

    public long getTotalMaxIops() {
        return _totalMaxIops;
    }

    public void setTotalBurstIops(long totalBurstIops) {
        _totalBurstIops = totalBurstIops;
    }

    public long getTotalBurstIops() {
        return _totalBurstIops;
    }

    public void setZoneId(long zoneId) {
        _zoneId = zoneId;
    }

    public long getZoneId() {
        return _zoneId;
    }

    public void setZoneName(String zoneName) {
        _zoneName = zoneName;
    }

    public String getZoneName() {
        return _zoneName;
    }
}
