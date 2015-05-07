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

import org.apache.cloudstack.solidfire.SfVirtualNetwork;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "sf_virtual_network")
public class SfVirtualNetworkVO implements SfVirtualNetwork {
    private static final long serialVersionUID = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "sf_id")
    private long sfId;

    @Column(name = "name")
    private String name;

    @Column(name = "tag")
    private String tag;

    @Column(name = "start_ip")
    private String startIp;

    @Column(name = "size")
    private int size;

    @Column(name = "netmask")
    private String netmask;

    @Column(name = "svip")
    private String svip;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "sf_cluster_id")
    private long sfClusterId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public SfVirtualNetworkVO() {
        uuid = UUID.randomUUID().toString();
    }

    public SfVirtualNetworkVO(long sfId, String name, String tag, String startIp, int size, String netmask, String svip, long accountId, long sfClusterId) {
        this.uuid = UUID.randomUUID().toString();
        this.sfId = sfId;
        this.name = name;
        this.tag = tag;
        this.startIp = startIp;
        this.size = size;
        this.netmask = netmask;
        this.svip = svip;
        this.accountId = accountId;
        this.sfClusterId = sfClusterId;
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
    public long getSfId() {
        return sfId;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String getTag() {
        return tag;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    @Override
    public String getStartIp() {
        return startIp;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int getSize() {
        return size;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    @Override
    public String getNetmask() {
        return netmask;
    }

    public void setSvip(String svip) {
        this.svip = svip;
    }

    @Override
    public String getSvip() {
        return svip;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getSfClusterId() {
        return sfClusterId;
    }
}
