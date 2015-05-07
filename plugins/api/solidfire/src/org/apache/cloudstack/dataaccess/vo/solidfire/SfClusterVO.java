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
package org.apache.cloudstack.dataaccess.vo.solidfire;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.solidfire.SfCluster;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "sf_cluster")
public class SfClusterVO implements SfCluster {
    private static final long serialVersionUID = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "mvip")
    private String mvip;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "total_capacity")
    private long totalCapacity;

    @Column(name = "total_min_iops")
    private long totalMinIops;

    @Column(name = "total_max_iops")
    private long totalMaxIops;

    @Column(name = "total_burst_iops")
    private long totalBurstIops;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public SfClusterVO() {
        uuid = UUID.randomUUID().toString();
    }

    public SfClusterVO(String name, String mvip, String username, String password, long totalCapacity,
            long totalMinIops, long totalMaxIops, long totalBurstIops,long zoneId) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.mvip = mvip;
        this.username = username;
        this.password = password;
        this.totalCapacity = totalCapacity;
        this.totalMinIops = totalMinIops;
        this.totalMaxIops = totalMaxIops;
        this.totalBurstIops = totalBurstIops;
        this.zoneId = zoneId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMvip() {
        return mvip;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    @Override
    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalMinIops(long totalMinIops) {
        this.totalMinIops = totalMinIops;
    }

    @Override
    public long getTotalMinIops() {
        return totalMinIops;
    }

    public void setTotalMaxIops(long totalMaxIops) {
        this.totalMaxIops = totalMaxIops;
    }

    @Override
    public long getTotalMaxIops() {
        return totalMaxIops;
    }

    public void setTotalBurstIops(long totalBurstIops) {
        this.totalBurstIops = totalBurstIops;
    }

    @Override
    public long getTotalBurstIops() {
        return totalBurstIops;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }
}
