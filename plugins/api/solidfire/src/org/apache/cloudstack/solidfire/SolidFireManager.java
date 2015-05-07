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
package org.apache.cloudstack.solidfire;

import java.util.List;

import org.apache.cloudstack.framework.config.Configurable;

public interface SolidFireManager extends Configurable {
    // ********** Cluster-related commands **********

    SfCluster listSolidFireCluster(String clusterName);

    List<SfCluster> listSolidFireClusters();

    SfCluster createReferenceToSolidFireCluster(String mvip, String username, String password, long totalCapacity,
            long totalMinIops, long totalMaxIops, long totalBurstIops, long zoneId);

    SfCluster updateReferenceToSolidFireCluster(String clusterName, long totalCapacity,
            long totalMinIops, long totalMaxIops, long totalBurstIops);

    SfCluster deleteReferenceToSolidFireCluster(String clusterName);

    // ********** VLAN-related commands **********

    SfVirtualNetwork listSolidFireVirtualNetworkById(long id);

    List<SfVirtualNetwork> listSolidFireVirtualNetworkByClusterName(String clusterName);

    // Long (instead of long) for both zoneId and accountId because they're optional and null is used to indicate that they're not present
    // zoneId and accountId are not dependent upon one another (i.e. either one can be null, both can be null, or both can be not be null)
    List<SfVirtualNetwork> listSolidFireVirtualNetworks(Long zoneId, Long accountId);

    SfVirtualNetwork createSolidFireVirtualNetwork(String clusterName, String name, String tag, String startIp, int size,
            String netmask, String svip, long accountId);

    SfVirtualNetwork updateSolidFireVirtualNetwork(long id, String name, String startIp, int size, String netmask);

    SfVirtualNetwork deleteSolidFireVirtualNetwork(long id);

 // ********** Volume-related commands **********

    SfVolume listSolidFireVolume(long id);

    List<SfVolume> listSolidFireVolumes();

    SfVolume createSolidFireVolume(String name, long size, long minIops, long maxIops, long burstIops, long accountId, long sfVirtualNetworkId);

    SfVolume updateSolidFireVolume(long id, long size, long minIops, long maxIops, long burstIops);

    SfVolume deleteSolidFireVolume(long id);
}
