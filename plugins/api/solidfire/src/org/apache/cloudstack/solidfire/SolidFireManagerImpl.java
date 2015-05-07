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
import java.util.ArrayList;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.dataaccess.dao.solidfire.SfClusterDao;
import org.apache.cloudstack.dataaccess.dao.solidfire.SfVirtualNetworkDao;
import org.apache.cloudstack.dataaccess.dao.solidfire.SfVolumeDao;
import org.apache.cloudstack.dataaccess.vo.solidfire.SfClusterVO;
import org.apache.cloudstack.dataaccess.vo.solidfire.SfVirtualNetworkVO;
import org.apache.cloudstack.dataaccess.vo.solidfire.SfVolumeVO;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.cloudstack.util.solidfire.SfUtil;
import org.apache.cloudstack.util.solidfire.SolidFireConnection;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class SolidFireManagerImpl implements SolidFireManager {
    private static final Logger s_logger = Logger.getLogger(SolidFireManagerImpl.class);
    private static final int s_lockTimeInSeconds = 180;

    private static final ConfigKey<Long> s_sfTotalAccountCapacity =
            new ConfigKey<>(
                    "Advanced",
                    Long.class,
                    "sf.total.capacity",
                    "0",
                    "Total capacity the account can draw from any and all SolidFire clusters (in GBs)",
                    true, ConfigKey.Scope.Account);

    private static final ConfigKey<Long> s_sfTotalAccountMinIops =
            new ConfigKey<>(
                    "Advanced",
                    Long.class,
                    "sf.total.min.iops",
                    "0",
                    "Total minimum IOPS the account can draw from any and all SolidFire clusters",
                    true, ConfigKey.Scope.Account);

    private static final ConfigKey<Long> s_sfTotalAccountMaxIops =
            new ConfigKey<>(
                    "Advanced",
                    Long.class,
                    "sf.total.max.iops",
                    "0",
                    "Total maximum IOPS the account can draw from any and all SolidFire clusters",
                    true, ConfigKey.Scope.Account);

    private static final ConfigKey<Long> s_sfTotalAccountBurstIops =
            new ConfigKey<>(
                    "Advanced",
                    Long.class,
                    "sf.total.burst.iops",
                    "0",
                    "Total burst IOPS the account can draw from any and all SolidFire clusters",
                    true, ConfigKey.Scope.Account);

    @Inject private AccountDao _accountDao;
    @Inject private AccountDetailsDao _accountDetailsDao;
    @Inject private DataCenterDao _zoneDao;
    @Inject private SfClusterDao _sfClusterDao;
    @Inject private SfVirtualNetworkDao _sfVirtualNetworkDao;
    @Inject private SfVolumeDao _sfVolumeDao;
    @Inject private SfUtil _sfUtil;

    private SolidFireManagerImpl() {
    }

    @Override
    public SfCluster listSolidFireCluster(String clusterName) {
        s_logger.info("listSolidFireCluster invoked");

        verifyRootAdmin();

        return getSfCluster(clusterName);
    }

    @Override
    public List<SfCluster> listSolidFireClusters() {
        s_logger.info("listSolidFireClusters invoked");

        verifyRootAdmin();

        List<SfCluster> sfClusters = new ArrayList<>();

        List<SfClusterVO> sfClusterVOs = _sfClusterDao.listAll();

        if (sfClusterVOs != null) {
            sfClusters.addAll(sfClusterVOs);
        }

        return sfClusters;
    }

    @Override
    public SfCluster createReferenceToSolidFireCluster(String mvip, String username, String password, long totalCapacity,
            long totalMinIops, long totalMaxIops, long totalBurstIops, long zoneId) {
        s_logger.info("createReferenceToSolidFireCluster invoked");

        verifyRootAdmin();

        verifyClusterQuotas(totalCapacity, totalMinIops, totalMaxIops, totalBurstIops);

        verifyZone(zoneId);

        SolidFireConnection sfConnection = new SolidFireConnection(mvip, username, password);

        String clusterName = sfConnection.getClusterName();

        List<SfClusterVO> sfClusterVOs = _sfClusterDao.listAll();

        for (SfCluster sfCluster : sfClusterVOs) {
            if (sfCluster.getName().equals(clusterName)) {
                throw new CloudRuntimeException("Unable to add a reference to cluster '" + clusterName + "' as a reference to a cluster by this name already exists");
            }
        }

        SfClusterVO sfClusterVO = new SfClusterVO(clusterName, mvip, username, password, totalCapacity, totalMinIops, totalMaxIops, totalBurstIops, zoneId);

        return _sfClusterDao.persist(sfClusterVO);
    }

    @Override
    public SfCluster updateReferenceToSolidFireCluster(String clusterName, long newTotalCapacity,
            long newTotalMinIops, long newTotalMaxIops, long newTotalBurstIops) {
        s_logger.info("updateReferenceToSolidFireCluster invoked");

        verifyRootAdmin();

        verifyClusterQuotas(newTotalCapacity, newTotalMinIops, newTotalMaxIops, newTotalBurstIops);

        SfClusterVO sfClusterVO = getSfCluster(clusterName);

        GlobalLock sfClusterLock = GlobalLock.getInternLock(sfClusterVO.getUuid());

        if (!sfClusterLock.lock(s_lockTimeInSeconds)) {
            String errMsg = "Couldn't lock the DB on the following string (Storage cluster UUID): " + sfClusterVO.getUuid();

            s_logger.debug(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            TotalRemaining totalRemainingInCluster = getTotalRemainingInCluster(sfClusterVO);

            long totalUsedCapacityInCluster = sfClusterVO.getTotalCapacity() - totalRemainingInCluster.getTotalRemainingCapacity();
            long totalUsedMinIopsInCluster = sfClusterVO.getTotalMinIops() - totalRemainingInCluster.getTotalRemainingMinIops();
            long totalUsedMaxIopsInCluster = sfClusterVO.getTotalMaxIops() - totalRemainingInCluster.getTotalRemainingMaxIops();
            long totalUsedBurstIopsInCluster = sfClusterVO.getTotalBurstIops() - totalRemainingInCluster.getTotalRemainingBurstIops();

            if (totalUsedCapacityInCluster <= newTotalCapacity && totalUsedMinIopsInCluster <= newTotalMinIops &&
                    totalUsedMaxIopsInCluster <= newTotalMaxIops && totalUsedBurstIopsInCluster <= newTotalBurstIops) {
                sfClusterVO.setTotalCapacity(newTotalCapacity);
                sfClusterVO.setTotalMinIops(newTotalMinIops);
                sfClusterVO.setTotalMaxIops(newTotalMaxIops);
                sfClusterVO.setTotalBurstIops(newTotalBurstIops);

                if (_sfClusterDao.update(sfClusterVO.getId(), sfClusterVO)) {
                    return sfClusterVO;
                }

                throw new CloudRuntimeException("Unable to update the cluster table");
            }
            else {
                throw new CloudRuntimeException("Unable to update the cluster table as more capacity and/or performance is in use " +
                        "in the storage cluster than one or more of the values passed in");
            }
        }
        finally {
            sfClusterLock.unlock();
            sfClusterLock.releaseRef();
        }
    }

    @Override
    public SfCluster deleteReferenceToSolidFireCluster(String clusterName) {
        s_logger.info("deleteReferenceToSolidFireCluster invoked");

        verifyRootAdmin();

        SfCluster sfCluster = getSfCluster(clusterName);

        List<SfVirtualNetworkVO> sfVirtualNetworks = _sfVirtualNetworkDao.findByClusterId(sfCluster.getId());

        if (sfVirtualNetworks != null && sfVirtualNetworks.size() > 0) {
            throw new CloudRuntimeException("Unable to delete a reference to a cluster that has one or more virtual networks");
        }

        if (!_sfClusterDao.remove(sfCluster.getId())) {
            throw new CloudRuntimeException("Unable to remove the following cluster: " + clusterName);
        }

        return sfCluster;
    }

    @Override
    public SfVirtualNetwork listSolidFireVirtualNetworkById(long id) {
        s_logger.info("listSolidFireVirtualNetworkById invoked");

        SfVirtualNetwork sfVirtualNetwork = getSfVirtualNetwork(id);

        verifyPermissionsForAccount(sfVirtualNetwork.getAccountId());

        return getSfVirtualNetwork(id);
    }

    @Override
    public List<SfVirtualNetwork> listSolidFireVirtualNetworkByClusterName(String clusterName) {
        s_logger.info("listSolidFireVirtualNetworkByClusterName invoked");

        verifyRootAdmin();

        SfCluster sfCluster = getSfCluster(clusterName);

        return filterVirtualNetworksByCluster(_sfVirtualNetworkDao.listAll(), sfCluster.getId());
    }

    @Override
    public List<SfVirtualNetwork> listSolidFireVirtualNetworks(Long zoneId, Long accountId) {
        s_logger.info("listSolidFireVirtualNetworks invoked");

        final List<SfVirtualNetworkVO> sfVirtualNetworkVOs;

        if (_sfUtil.isRootAdmin()) {
            if (zoneId != null) {
                if (accountId != null) {
                    sfVirtualNetworkVOs = filterVirtualNetworksByZone(_sfVirtualNetworkDao.findByAccountId(accountId), zoneId);
                }
                else {
                    sfVirtualNetworkVOs = filterVirtualNetworksByZone(_sfVirtualNetworkDao.listAll(), zoneId);
                }
            }
            else {
                if (accountId != null) {
                    sfVirtualNetworkVOs = _sfVirtualNetworkDao.findByAccountId(accountId);
                }
                else {
                    sfVirtualNetworkVOs = _sfVirtualNetworkDao.listAll();
                }
            }
        }
        else {
            if (accountId != null && accountId != _sfUtil.getCallingAccount().getId()) {
                throw new CloudRuntimeException("Only a root admin can specify an account other than his own.");
            }

            if (zoneId != null) {
                sfVirtualNetworkVOs = filterVirtualNetworksByZone(_sfVirtualNetworkDao.findByAccountId(_sfUtil.getCallingAccount().getId()), zoneId);
            }
            else {
                sfVirtualNetworkVOs = _sfVirtualNetworkDao.findByAccountId(_sfUtil.getCallingAccount().getId());
            }
        }

        List<SfVirtualNetwork> sfVirtualNetworks = new ArrayList<>();

        if (sfVirtualNetworkVOs != null) {
            sfVirtualNetworks.addAll(sfVirtualNetworkVOs);
        }

        return sfVirtualNetworks;
    }

    @Override
    public SfVirtualNetwork createSolidFireVirtualNetwork(String clusterName, String name, String tag, String startIp, int size,
            String netmask, String svip, long accountId) {
        s_logger.info("createSolidFireVirtualNetwork invoked");

        verifyRootAdmin();

        verifyAccount(accountId);

        SfCluster sfCluster = getSfCluster(clusterName);

        SolidFireConnection sfConnection = new SolidFireConnection(sfCluster.getMvip(), sfCluster.getUsername(), sfCluster.getPassword());

        long sfVirtualNetworkId = sfConnection.createVirtualNetwork(name, tag, startIp, size, netmask, svip);

        SfVirtualNetworkVO sfVirtualNetworkVO = new SfVirtualNetworkVO(sfVirtualNetworkId, name, tag, startIp, size, netmask, svip, accountId, sfCluster.getId());

        return _sfVirtualNetworkDao.persist(sfVirtualNetworkVO);
    }

    @Override
    public SfVirtualNetwork updateSolidFireVirtualNetwork(long id, String name, String startIp, int size, String netmask) {
        s_logger.info("updateSolidFireVirtualNetwork invoked");

        verifyRootAdmin();

        SfVirtualNetworkVO sfVirtualNetworkVO = getSfVirtualNetwork(id);

        long sfClusterId = sfVirtualNetworkVO.getSfClusterId();

        SfClusterVO sfClusterVO = getSfCluster(sfClusterId);

        SolidFireConnection sfConnection = new SolidFireConnection(sfClusterVO.getMvip(), sfClusterVO.getUsername(), sfClusterVO.getPassword());

        sfConnection.modifyVirtualNetwork(sfVirtualNetworkVO.getSfId(), name, startIp, size, netmask);

        sfVirtualNetworkVO.setName(name);
        sfVirtualNetworkVO.setStartIp(startIp);
        sfVirtualNetworkVO.setSize(size);
        sfVirtualNetworkVO.setNetmask(netmask);

        if (_sfVirtualNetworkDao.update(sfVirtualNetworkVO.getId(), sfVirtualNetworkVO)) {
            return sfVirtualNetworkVO;
        }

        throw new CloudRuntimeException("Unable to update the virtual network table");
    }

    @Override
    public SfVirtualNetwork deleteSolidFireVirtualNetwork(long id) {
        s_logger.info("deleteSolidFireVirtualNetwork invoked");

        verifyRootAdmin();

        SfVirtualNetwork sfVirtualNetwork = getSfVirtualNetwork(id);

        List<SfVolumeVO> sfVolumes = _sfVolumeDao.findBySfVirtualNetworkId(sfVirtualNetwork.getId());

        if (sfVolumes != null && sfVolumes.size() > 0) {
            throw new CloudRuntimeException("Unable to delete a virtual network that has one or more volumes");
        }

        if (!_sfVirtualNetworkDao.remove(id)) {
            throw new CloudRuntimeException("Unable to remove the following virtual network: " + id);
        }

        SfCluster sfCluster = getSfCluster(sfVirtualNetwork.getSfClusterId());

        SolidFireConnection sfConnection = new SolidFireConnection(sfCluster.getMvip(), sfCluster.getUsername(), sfCluster.getPassword());

        sfConnection.deleteVirtualNetwork(sfVirtualNetwork.getSfId());

        return sfVirtualNetwork;
    }

    @Override
    public SfVolume listSolidFireVolume(long id) {
        s_logger.info("listSolidFireVolume invoked");

        SfVolume sfVolume = getSfVolume(id);

        SfVirtualNetwork sfVirtualNetwork = getSfVirtualNetwork(sfVolume.getSfVirtualNetworkId());

        verifyPermissionsForAccount(sfVirtualNetwork.getAccountId());

        return sfVolume;
    }

    @Override
    public List<SfVolume> listSolidFireVolumes() {
        s_logger.info("listSolidFireVolumes invoked");

        final List<SfVolumeVO> sfVolumeVOs;

        if (_sfUtil.isRootAdmin()) {
            sfVolumeVOs = _sfVolumeDao.listAll();
        }
        else {
            sfVolumeVOs = new ArrayList<>();

            List<SfVirtualNetworkVO> sfVirtualNetworkVOs = _sfVirtualNetworkDao.findByAccountId(_sfUtil.getCallingAccount().getId());

            if (sfVirtualNetworkVOs != null) {
                for (SfVirtualNetwork sfVirtualNetwork : sfVirtualNetworkVOs) {
                    List<SfVolumeVO> sfVolumeVOsForVirtualNetwork = _sfVolumeDao.findBySfVirtualNetworkId(sfVirtualNetwork.getId());

                    sfVolumeVOs.addAll(sfVolumeVOsForVirtualNetwork);
                }
            }
        }

        List<SfVolume> sfVolumes = new ArrayList<>();

        if (sfVolumeVOs != null) {
            sfVolumes.addAll(sfVolumeVOs);
        }

        return sfVolumes;
    }

    @Override
    public SfVolume createSolidFireVolume(String name, long size, long minIops, long maxIops, long burstIops, long accountId, long sfVirtualNetworkId) {
        s_logger.info("createSolidFireVolume invoked");

        verifyPermissionsForAccount(accountId);

        verifySfVirtualNetwork(sfVirtualNetworkId);

        SfVolume sfVolume = createVolume(name, size, minIops, maxIops, burstIops, accountId, sfVirtualNetworkId);

        if (sfVolume != null) {
            return sfVolume;
        }

        throw new CloudRuntimeException("Unable to create the volume");
    }

    @Override
    public SfVolume updateSolidFireVolume(long id, long size, long minIops, long maxIops, long burstIops) {
        s_logger.info("updateSolidFireVolume invoked");

        SfVolumeVO sfVolumeVO = getSfVolume(id);

        SfVirtualNetwork sfVirtualNetwork = getSfVirtualNetwork(sfVolumeVO.getSfVirtualNetworkId());

        verifyPermissionsForAccount(sfVirtualNetwork.getAccountId());

        if ((sfVolumeVO = updateVolume(sfVolumeVO, size, minIops, maxIops, burstIops)) != null) {
            return sfVolumeVO;
        }

        throw new CloudRuntimeException("Unable to update the volume with the following id: " + id);
    }

    @Override
    public SfVolume deleteSolidFireVolume(long id) {
        s_logger.info("deleteSolidFireVolume invoked");

        SfVolume sfVolume = getSfVolume(id);

        SfVirtualNetwork sfVirtualNetwork = getSfVirtualNetwork(sfVolume.getSfVirtualNetworkId());

        verifyPermissionsForAccount(sfVirtualNetwork.getAccountId());

        if (!_sfVolumeDao.remove(id)) {
            throw new CloudRuntimeException("Unable to remove the following volume: " + id);
        }

        SfCluster sfCluster = getSfCluster(sfVirtualNetwork.getSfClusterId());

        SolidFireConnection sfConnection = new SolidFireConnection(sfCluster.getMvip(), sfCluster.getUsername(), sfCluster.getPassword());

        sfConnection.deleteVolume(sfVolume.getSfId());

        return sfVolume;
    }

    @Override
    public String getConfigComponentName() {
        return SolidFireManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { s_sfTotalAccountCapacity, s_sfTotalAccountMinIops, s_sfTotalAccountMaxIops, s_sfTotalAccountBurstIops };
    }

    private SfClusterVO getSfCluster(String clusterName) {
        SfClusterVO sfClusterVO = _sfClusterDao.findByName(clusterName);

        if (sfClusterVO == null) {
            throw new CloudRuntimeException("The following SolidFire cluster name cannot be located in the database: '" + clusterName + "'.");
        }

        return sfClusterVO;
    }

    private SfClusterVO getSfCluster(long sfClusterId) {
        SfClusterVO sfClusterVO = _sfClusterDao.findById(sfClusterId);

        if (sfClusterVO == null) {
            throw new CloudRuntimeException("The SolidFire cluster with the following ID cannot be located in the database: '" + sfClusterId + "'.");
        }

        return sfClusterVO;
    }

    private SfVirtualNetworkVO getSfVirtualNetwork(long id) {
        SfVirtualNetworkVO sfVirtualNetworkVO = _sfVirtualNetworkDao.findById(id);

        if (sfVirtualNetworkVO == null) {
            throw new CloudRuntimeException("The SolidFire VLAN with the following ID cannot be located in the database: '" + id + "'.");
        }

        return sfVirtualNetworkVO;
    }

    private SfVolumeVO getSfVolume(long id) {
        SfVolumeVO sfVolumeVO = _sfVolumeDao.findById(id);

        if (sfVolumeVO == null) {
            throw new CloudRuntimeException("The SolidFire volume with the following ID cannot be located in the database: '" + id + "'.");
        }

        return sfVolumeVO;
    }

    private void verifyRootAdmin() {
        if (!_sfUtil.isRootAdmin()) {
            throw new PermissionDeniedException("Only a root admin can perform this operation.");
        }
    }

    private void verifyPermissionsForAccount(long accountId) {
        Account account = _sfUtil.getCallingAccount();

        if (_sfUtil.isRootAdmin(account.getId())) {
            return; // permissions OK
        }

        if (account.getId() == accountId) {
            return; // permissions OK
        }

        throw new PermissionDeniedException("Only a root admin or a user of the owning account can perform this operation.");
    }

    private void verifyClusterQuotas(long totalCapacity, long totalMinIops, long totalMaxIops, long totalBurstIops) {
        if (totalCapacity < 0) {
            throw new CloudRuntimeException("The total capacity of the cluster must be a positive whole number.");
        }

        if (totalMinIops < 0) {
            throw new CloudRuntimeException("The total minimum IOPS of the cluster must be a positive whole number.");
        }

        if (totalMaxIops < 0) {
            throw new CloudRuntimeException("The total maximum IOPS of the cluster must be a positive whole number.");
        }

        if (totalBurstIops < 0) {
            throw new CloudRuntimeException("The total burst IOPS of the cluster must be a positive whole number.");
        }

        if (totalMinIops > totalMaxIops) {
            throw new CloudRuntimeException("The total minimum IOPS of the cluster must be less than or equal to the total maximum IOPS of the cluster.");
        }

        if (totalMaxIops > totalBurstIops) {
            throw new CloudRuntimeException("The total maximum IOPS of the cluster must be less than or equal to the total burst IOPS of the cluster.");
        }
    }

    private void verifyAccount(long accountId) {
        Account account = _accountDao.findById(accountId);

        if (account == null) {
            throw new CloudRuntimeException("Unable to locate the following account: " + accountId);
        }
     }

    private void verifySfVirtualNetwork(long sfVirtualNetworkId) {
        SfVirtualNetwork sfVirtualNetwork = _sfVirtualNetworkDao.findById(sfVirtualNetworkId);

        if (sfVirtualNetwork == null) {
            throw new CloudRuntimeException("Unable to locate the following virtual network: " + sfVirtualNetworkId);
        }
     }

    private void verifyZone(long zoneId) {
       DataCenterVO dataCenterVO = _zoneDao.findById(zoneId);

       if (dataCenterVO == null) {
           throw new CloudRuntimeException("Unable to locate the following zone: " + zoneId);
       }
    }

    private SfVolume createVolume(String name, long size, long minIops, long maxIops, long burstIops, long accountId, long sfVirtualNetworkId) {
        verifyIops(minIops, maxIops, burstIops);

        Account account = _accountDao.findById(accountId);

        SfVirtualNetwork sfVirtualNetwork = getSfVirtualNetwork(sfVirtualNetworkId);
        SfCluster sfCluster = getSfCluster(sfVirtualNetwork.getSfClusterId());

        GlobalLock accountLock = GlobalLock.getInternLock(account.getUuid());

        if (!accountLock.lock(s_lockTimeInSeconds)) {
            String errMsg = "Couldn't lock the DB on the following string (Account UUID): " + sfCluster.getUuid();

            s_logger.debug(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        GlobalLock sfClusterLock = GlobalLock.getInternLock(sfCluster.getUuid());

        boolean clusterLockSuccess = false;

        try {
            clusterLockSuccess = sfClusterLock.lock(s_lockTimeInSeconds);
        }
        catch (Throwable t) {
            // clusterLockSuccess should still be false
        }

        if (!clusterLockSuccess) {
            accountLock.unlock();
            accountLock.releaseRef();

            String errMsg = "Couldn't lock the DB on the following string (Storage cluster UUID): " + sfCluster.getUuid();

            s_logger.debug(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            TotalRemaining totalRemainingInAccount = getTotalRemainingInAccount(accountId);

            long totalRemainingCapacityInAccount = totalRemainingInAccount.getTotalRemainingCapacity() - size;
            long totalRemainingMinIopsInAccount = totalRemainingInAccount.getTotalRemainingMinIops() - minIops;
            long totalRemainingMaxIopsInAccount = totalRemainingInAccount.getTotalRemainingMaxIops() - maxIops;
            long totalRemainingBurstIopsInAccount = totalRemainingInAccount.getTotalRemainingBurstIops() - burstIops;

            if (totalRemainingCapacityInAccount >= 0 && totalRemainingMinIopsInAccount >= 0 && totalRemainingMaxIopsInAccount >= 0 && totalRemainingBurstIopsInAccount >= 0) {
                TotalRemaining totalRemainingInCluster = getTotalRemainingInCluster(sfCluster);

                long totalRemainingCapacityInCluster = totalRemainingInCluster.getTotalRemainingCapacity() - size;
                long totalRemainingMinIopsInCluster = totalRemainingInCluster.getTotalRemainingMinIops() - minIops;
                long totalRemainingMaxIopsInCluster = totalRemainingInCluster.getTotalRemainingMaxIops() - maxIops;
                long totalRemainingBurstIopsInCluster = totalRemainingInCluster.getTotalRemainingBurstIops() - burstIops;

                if (totalRemainingCapacityInCluster >= 0 && totalRemainingMinIopsInCluster >= 0 && totalRemainingMaxIopsInCluster >= 0 && totalRemainingBurstIopsInCluster >= 0) {
                    SolidFireConnection sfConnection = new SolidFireConnection(sfCluster.getMvip(), sfCluster.getUsername(), sfCluster.getPassword());

                    long sfClusterId = sfCluster.getId();

                    AccountDetailVO accountDetail = getAccountDetail(accountId, sfClusterId);

                    if (accountDetail == null || accountDetail.getValue() == null) {
                        String sfAccountName = getSolidFireAccountName(accountId, account.getUuid());
                        SolidFireConnection.SolidFireAccount sfAccount = sfConnection.getSolidFireAccount(sfAccountName);

                        if (sfAccount == null) {
                            sfAccount = createSolidFireAccount(sfConnection, sfAccountName);
                        }

                        updateCsDbWithSolidFireAccountInfo(account.getId(), sfAccount, sfClusterId);

                        accountDetail = getAccountDetail(accountId, sfClusterId);
                    }

                    long sfAccountId = Long.parseLong(accountDetail.getValue());

                    long sfVolumeId = sfConnection.createVolume(name, sfAccountId, size, minIops, maxIops, burstIops);

                    SolidFireConnection.SolidFireVolume sfVolume = sfConnection.getVolume(sfVolumeId);

                    SfVolumeVO sfVolumeVO = new SfVolumeVO(sfVolumeId, name, sfVolume.getIqn(), size, minIops, maxIops, burstIops, sfVirtualNetworkId);

                    return _sfVolumeDao.persist(sfVolumeVO);
                }
                else {
                    throw new CloudRuntimeException("Unable to create the volume due to insufficient capacity or performance remaining in the storage cluster");
                }
            }
            else {
                throw new CloudRuntimeException("Unable to create the volume due to insufficient capacity or performance remaining in the account");
            }
        }
        finally {
            sfClusterLock.unlock();
            sfClusterLock.releaseRef();

            accountLock.unlock();
            accountLock.releaseRef();
        }
    }

    private SfVolumeVO updateVolume(SfVolumeVO sfVolumeVO, long size, long minIops, long maxIops, long burstIops) {
        verifyIops(minIops, maxIops, burstIops);

        SfVirtualNetwork sfVirtualNetwork = getSfVirtualNetwork(sfVolumeVO.getSfVirtualNetworkId());

        Account account = _accountDao.findById(sfVirtualNetwork.getAccountId());
        SfCluster sfCluster = getSfCluster(sfVirtualNetwork.getSfClusterId());

        GlobalLock accountLock = GlobalLock.getInternLock(account.getUuid());

        if (!accountLock.lock(s_lockTimeInSeconds)) {
            String errMsg = "Couldn't lock the DB on the following string (Account UUID): " + sfCluster.getUuid();

            s_logger.debug(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        GlobalLock sfClusterLock = GlobalLock.getInternLock(sfCluster.getUuid());

        boolean clusterLockSuccess = false;

        try {
            clusterLockSuccess = sfClusterLock.lock(s_lockTimeInSeconds);
        }
        catch (Throwable t) {
            // clusterLockSuccess should still be false
        }

        if (!clusterLockSuccess) {
            accountLock.unlock();
            accountLock.releaseRef();

            String errMsg = "Couldn't lock the DB on the following string (Storage cluster UUID): " + sfCluster.getUuid();

            s_logger.debug(errMsg);

            throw new CloudRuntimeException(errMsg);
        }

        try {
            TotalRemaining totalRemainingInAccount = getTotalRemainingInAccount(account.getId());

            long totalRemainingCapacityInAccount = totalRemainingInAccount.getTotalRemainingCapacity() - size;
            long totalRemainingMinIopsInAccount = totalRemainingInAccount.getTotalRemainingMinIops() - minIops;
            long totalRemainingMaxIopsInAccount = totalRemainingInAccount.getTotalRemainingMaxIops() - maxIops;
            long totalRemainingBurstIopsInAccount = totalRemainingInAccount.getTotalRemainingBurstIops() - burstIops;

            if (totalRemainingCapacityInAccount >= 0 && totalRemainingMinIopsInAccount >= 0 && totalRemainingMaxIopsInAccount >= 0 && totalRemainingBurstIopsInAccount >= 0) {
                TotalRemaining totalRemainingInCluster = getTotalRemainingInCluster(sfCluster, sfVolumeVO);

                long totalRemainingCapacityInCluster = totalRemainingInCluster.getTotalRemainingCapacity() - size;
                long totalRemainingMinIopsInCluster = totalRemainingInCluster.getTotalRemainingMinIops() - minIops;
                long totalRemainingMaxIopsInCluster = totalRemainingInCluster.getTotalRemainingMaxIops() - maxIops;
                long totalRemainingBurstIopsInCluster = totalRemainingInCluster.getTotalRemainingBurstIops() - burstIops;

                if (totalRemainingCapacityInCluster >= 0 && totalRemainingMinIopsInCluster >= 0 && totalRemainingMaxIopsInCluster >= 0 && totalRemainingBurstIopsInCluster >= 0) {
                    SolidFireConnection sfConnection = new SolidFireConnection(sfCluster.getMvip(), sfCluster.getUsername(), sfCluster.getPassword());

                    sfConnection.modifyVolume(sfVolumeVO.getSfId(), size, minIops, maxIops, burstIops);

                    sfVolumeVO.setSize(size);
                    sfVolumeVO.setMinIops(minIops);
                    sfVolumeVO.setMaxIops(maxIops);
                    sfVolumeVO.setBurstIops(burstIops);

                    if (!_sfVolumeDao.update(sfVolumeVO.getId(), sfVolumeVO)) {
                        throw new CloudRuntimeException("Unable to update the following volume:" + sfVolumeVO.getId());
                    }

                    return sfVolumeVO;
                }
                else {
                    throw new CloudRuntimeException("Unable to update the volume due to insufficient capacity or performance remaining in the storage cluster");
                }
            }
            else {
                throw new CloudRuntimeException("Unable to update the volume due to insufficient capacity or performance remaining in the account");
            }
        }
        finally {
            sfClusterLock.unlock();
            sfClusterLock.releaseRef();

            accountLock.unlock();
            accountLock.releaseRef();
        }
    }

    private TotalRemaining getTotalRemainingInAccount(long accountId) {
        return getTotalRemainingInAccount(accountId, null);
    }

    private TotalRemaining getTotalRemainingInAccount(long accountId, SfVolume volumeToExclude) {
        Long totalRemainingCapacity = s_sfTotalAccountCapacity.valueIn(accountId);
        Long totalRemainingMinIops = s_sfTotalAccountMinIops.valueIn(accountId);
        Long totalRemainingMaxIops = s_sfTotalAccountMaxIops.valueIn(accountId);
        Long totalRemainingBurstIops = s_sfTotalAccountBurstIops.valueIn(accountId);

        List<SfVolume> sfVolumesInAccount = new ArrayList<>();

        List<SfVirtualNetworkVO> sfVirtualNetworkVOs = _sfVirtualNetworkDao.findByAccountId(accountId);

        if (sfVirtualNetworkVOs != null) {
            for (SfVirtualNetwork sfVirtualNetwork : sfVirtualNetworkVOs) {
                List<SfVolumeVO> sfVolumeVOs = _sfVolumeDao.findBySfVirtualNetworkId(sfVirtualNetwork.getId());

                sfVolumesInAccount.addAll(sfVolumeVOs);
            }
        }

        for (SfVolume sfVolumeInAccount : sfVolumesInAccount) {
            if (volumeToExclude == null || sfVolumeInAccount.getId() != volumeToExclude.getId()) {
                totalRemainingCapacity -= sfVolumeInAccount.getSize();
                totalRemainingMinIops -= sfVolumeInAccount.getMinIops();
                totalRemainingMaxIops -= sfVolumeInAccount.getMaxIops();
                totalRemainingBurstIops -= sfVolumeInAccount.getBurstIops();
            }
        }

        return new TotalRemaining(totalRemainingCapacity, totalRemainingMinIops, totalRemainingMaxIops, totalRemainingBurstIops);
    }

    private TotalRemaining getTotalRemainingInCluster(SfCluster sfCluster) {
        return getTotalRemainingInCluster(sfCluster, null);
    }

    private TotalRemaining getTotalRemainingInCluster(SfCluster sfCluster, SfVolume volumeToExclude) {
        long totalRemainingCapacity = sfCluster.getTotalCapacity();
        long totalRemainingMinIops = sfCluster.getTotalMinIops();
        long totalRemainingMaxIops = sfCluster.getTotalMaxIops();
        long totalRemainingBurstIops = sfCluster.getTotalBurstIops();

        List<SfVolume> sfVolumesInCluster = new ArrayList<>();

        List<SfVirtualNetworkVO> sfVirtualNetworkVOs = _sfVirtualNetworkDao.findByClusterId(sfCluster.getId());

        if (sfVirtualNetworkVOs != null) {
            for (SfVirtualNetwork sfVirtualNetwork : sfVirtualNetworkVOs) {
                List<SfVolumeVO> sfVolumeVOs = _sfVolumeDao.findBySfVirtualNetworkId(sfVirtualNetwork.getId());

                sfVolumesInCluster.addAll(sfVolumeVOs);
            }
        }

        for (SfVolume sfVolumeInCluster : sfVolumesInCluster) {
            if (volumeToExclude == null || sfVolumeInCluster.getId() != volumeToExclude.getId()) {
                totalRemainingCapacity -= sfVolumeInCluster.getSize();
                totalRemainingMinIops -= sfVolumeInCluster.getMinIops();
                totalRemainingMaxIops -= sfVolumeInCluster.getMaxIops();
                totalRemainingBurstIops -= sfVolumeInCluster.getBurstIops();
            }
        }

        return new TotalRemaining(totalRemainingCapacity, totalRemainingMinIops, totalRemainingMaxIops, totalRemainingBurstIops);
    }

    private static class TotalRemaining {
        private final long _totalRemainingCapacity;
        private final long _totalRemainingMinIops;
        private final long _totalRemainingMaxIops;
        private final long _totalRemainingBurstIops;

        public TotalRemaining(long totalRemainingCapacity, long totalRemainingMinIops, long totalRemainingMaxIops, long totalRemainingBurstIops) {
            _totalRemainingCapacity = totalRemainingCapacity;
            _totalRemainingMinIops = totalRemainingMinIops;
            _totalRemainingMaxIops = totalRemainingMaxIops;
            _totalRemainingBurstIops = totalRemainingBurstIops;
        }

        public long getTotalRemainingCapacity() {
            return _totalRemainingCapacity;
        }

        public long getTotalRemainingMinIops() {
            return _totalRemainingMinIops;
        }

        public long getTotalRemainingMaxIops() {
            return _totalRemainingMaxIops;
        }

        public long getTotalRemainingBurstIops() {
            return _totalRemainingBurstIops;
        }
    }

    private static String getSolidFireAccountName(long accountId, String accountUuid) {
        return "CloudStack_" + accountId + "_" + accountUuid;
    }

    private static String getAccountKey(long sfClusterId) {
        return "sfAccountIdForClusterId_" + sfClusterId;
    }

    private AccountDetailVO getAccountDetail(long accountId, long sfClusterId) {
        return _accountDetailsDao.findDetail(accountId, getAccountKey(sfClusterId));
    }

    private SolidFireConnection.SolidFireAccount createSolidFireAccount(SolidFireConnection sfConnection, String sfAccountName) {
        long accountNumber = sfConnection.createSolidFireAccount(sfAccountName);

        return sfConnection.getSolidFireAccountById(accountNumber);
    }

    private void updateCsDbWithSolidFireAccountInfo(long accountId, SolidFireConnection.SolidFireAccount sfAccount, long sfClusterId) {
        AccountDetailVO accountDetail = new AccountDetailVO(accountId,
                getAccountKey(sfClusterId),
                String.valueOf(sfAccount.getId()));

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(accountId,
                SolidFireUtil.CHAP_INITIATOR_USERNAME,
                sfAccount.getName());

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(accountId,
                SolidFireUtil.CHAP_INITIATOR_SECRET,
                sfAccount.getInitiatorSecret());

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(accountId,
                SolidFireUtil.CHAP_TARGET_USERNAME,
                sfAccount.getName());

        _accountDetailsDao.persist(accountDetail);

        accountDetail = new AccountDetailVO(accountId,
                SolidFireUtil.CHAP_TARGET_SECRET,
                sfAccount.getTargetSecret());

        _accountDetailsDao.persist(accountDetail);
    }

    private List<SfVirtualNetwork> filterVirtualNetworksByCluster(List<SfVirtualNetworkVO> sfVirtualNetworkVOs, long clusterId) {
        List<SfVirtualNetwork> sfVirtualNetworkVOsToReturn = new ArrayList<>();

        if (sfVirtualNetworkVOs != null) {
            for (SfVirtualNetworkVO sfVirtualNetworkVO : sfVirtualNetworkVOs) {
                long sfClusterId = sfVirtualNetworkVO.getSfClusterId();

                if (sfClusterId == clusterId) {
                    sfVirtualNetworkVOsToReturn.add(sfVirtualNetworkVO);
                }
            }
        }

        return sfVirtualNetworkVOsToReturn;
    }

    private List<SfVirtualNetworkVO> filterVirtualNetworksByZone(List<SfVirtualNetworkVO> sfVirtualNetworkVOs, long zoneId) {
        List<SfVirtualNetworkVO> sfVirtualNetworkVOsToReturn = new ArrayList<>();

        if (sfVirtualNetworkVOs != null) {
            for (SfVirtualNetworkVO sfVirtualNetworkVO : sfVirtualNetworkVOs) {
                SfCluster sfCluster = getSfCluster(sfVirtualNetworkVO.getSfClusterId());

                if (sfCluster.getZoneId() == zoneId) {
                    sfVirtualNetworkVOsToReturn.add(sfVirtualNetworkVO);
                }
            }
        }

        return sfVirtualNetworkVOsToReturn;
    }

    private static void verifyIops(long minIops, long maxIops, long burstIops) {
        if (minIops <= 0 || maxIops <= 0 || burstIops <= 0) {
            throw new IllegalArgumentException("The 'Min IOPS', 'Max IOPS', and 'Burst IOPS' values must be greater than 0.");
        }

        if (minIops > maxIops) {
            throw new IllegalArgumentException("The 'Min IOPS' value cannot exceed the 'Max IOPS' value.");
        }

        if (maxIops > burstIops) {
            throw new IllegalArgumentException("The 'Max IOPS' value cannot exceed the 'Burst IOPS' value.");
        }
    }
}
