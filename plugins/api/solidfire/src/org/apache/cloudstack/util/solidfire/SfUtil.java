package org.apache.cloudstack.util.solidfire;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireClusterResponse;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireVirtualNetworkResponse;
import org.apache.cloudstack.api.response.solidfire.ApiSolidFireVolumeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dataaccess.dao.solidfire.SfClusterDao;
import org.apache.cloudstack.dataaccess.dao.solidfire.SfVirtualNetworkDao;
import org.apache.cloudstack.solidfire.SfCluster;
import org.apache.cloudstack.solidfire.SfVirtualNetwork;
import org.apache.cloudstack.solidfire.SfVolume;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SfUtil {
    @Inject private AccountDao _accountDao;
    @Inject private AccountDetailsDao _accountDetailsDao;
    @Inject private AccountManager _accountMgr;
    @Inject private SfClusterDao _sfClusterDao;
    @Inject private DataCenterDao _zoneDao;
    @Inject private SfVirtualNetworkDao _sfVirtualNetworkDao;

    private SfUtil() {
    }

    public ApiSolidFireClusterResponse getApiSolidFireClusterResponse(SfCluster sfCluster) {
        ApiSolidFireClusterResponse sfResponse = new ApiSolidFireClusterResponse();

        sfResponse.setId(sfCluster.getId());
        sfResponse.setUuid(sfCluster.getUuid());
        sfResponse.setName(sfCluster.getName());
        sfResponse.setMvip(sfCluster.getMvip());
        sfResponse.setUsername(sfCluster.getUsername());
        sfResponse.setTotalCapacity(sfCluster.getTotalCapacity());
        sfResponse.setTotalMinIops(sfCluster.getTotalMinIops());
        sfResponse.setTotalMaxIops(sfCluster.getTotalMaxIops());
        sfResponse.setTotalBurstIops(sfCluster.getTotalBurstIops());
        sfResponse.setZoneId(sfCluster.getZoneId());

        DataCenterVO dataCenterVO = _zoneDao.findById(sfCluster.getZoneId());

        sfResponse.setZoneName(dataCenterVO.getName());

        sfResponse.setObjectName("sfcluster");

        return sfResponse;
    }

    public List<ApiSolidFireClusterResponse> getApiSolidFireClusterResponse(List<SfCluster> sfClusters) {
        List<ApiSolidFireClusterResponse> sfResponse = new ArrayList<>();

        if (sfClusters != null) {
            for (SfCluster sfCluster : sfClusters) {
                ApiSolidFireClusterResponse response = getApiSolidFireClusterResponse(sfCluster);

                sfResponse.add(response);
            }
        }

        return sfResponse;
    }

    public ApiSolidFireVirtualNetworkResponse getApiSolidFireVirtualNetworkResponse(SfVirtualNetwork sfVirtualNetwork, ResponseView responseView) {
        ApiSolidFireVirtualNetworkResponse sfResponse = new ApiSolidFireVirtualNetworkResponse();

        sfResponse.setId(sfVirtualNetwork.getId());
        sfResponse.setUuid(sfVirtualNetwork.getUuid());
        sfResponse.setName(sfVirtualNetwork.getName());
        sfResponse.setSvip(sfVirtualNetwork.getSvip());
        sfResponse.setAccountId(sfVirtualNetwork.getAccountId());

        Account account = _accountDao.findById(sfVirtualNetwork.getAccountId());

        sfResponse.setAccountUuid(account.getUuid());
        sfResponse.setAccountName(account.getAccountName());

        SfCluster sfCluster = _sfClusterDao.findById(sfVirtualNetwork.getSfClusterId());

        sfResponse.setZoneId(sfCluster.getZoneId());

        DataCenterVO dataCenterVO = _zoneDao.findById(sfCluster.getZoneId());

        sfResponse.setZoneUuid(dataCenterVO.getUuid());
        sfResponse.setZoneName(dataCenterVO.getName());

        if (ResponseView.Full.equals(responseView)) {
            sfResponse.setTag(sfVirtualNetwork.getTag());
            sfResponse.setStartIp(sfVirtualNetwork.getStartIp());
            sfResponse.setSize(sfVirtualNetwork.getSize());
            sfResponse.setNetmask(sfVirtualNetwork.getNetmask());
            sfResponse.setClusterName(sfCluster.getName());
        }

        sfResponse.setObjectName("sfvirtualnetwork");

        return sfResponse;
    }

    public List<ApiSolidFireVirtualNetworkResponse> getApiSolidFireVirtualNetworkResponse(List<SfVirtualNetwork> sfVirtualNetworks, ResponseView responseView) {
        List<ApiSolidFireVirtualNetworkResponse> sfResponse = new ArrayList<>();

        if (sfVirtualNetworks != null) {
            for (SfVirtualNetwork sfVirtualNetwork : sfVirtualNetworks) {
                ApiSolidFireVirtualNetworkResponse response = getApiSolidFireVirtualNetworkResponse(sfVirtualNetwork, responseView);

                sfResponse.add(response);
            }
        }

        return sfResponse;
    }

    public ApiSolidFireVolumeResponse getApiSolidFireVolumeResponse(SfVolume sfVolume, ResponseView responseView) {
        ApiSolidFireVolumeResponse sfResponse = new ApiSolidFireVolumeResponse();

        sfResponse.setId(sfVolume.getId());
        sfResponse.setUuid(sfVolume.getUuid());
        sfResponse.setName(sfVolume.getName());
        sfResponse.setIqn(sfVolume.getIqn());
        sfResponse.setSize(sfVolume.getSize());
        sfResponse.setMinIops(sfVolume.getMinIops());
        sfResponse.setMaxIops(sfVolume.getMaxIops());
        sfResponse.setBurstIops(sfVolume.getBurstIops());
        sfResponse.setCreated(sfVolume.getCreated());

        SfVirtualNetwork sfVirtualNetwork = _sfVirtualNetworkDao.findById(sfVolume.getSfVirtualNetworkId());

        sfResponse.setAccountId(sfVirtualNetwork.getAccountId());

        Account account = _accountDao.findById(sfVirtualNetwork.getAccountId());

        sfResponse.setAccountUuid(account.getUuid());
        sfResponse.setAccountName(account.getAccountName());

        SfCluster sfCluster = _sfClusterDao.findById(sfVirtualNetwork.getSfClusterId());

        sfResponse.setZoneId(sfCluster.getZoneId());

        DataCenterVO dataCenterVO = _zoneDao.findById(sfCluster.getZoneId());

        sfResponse.setZoneUuid(dataCenterVO.getUuid());
        sfResponse.setZoneName(dataCenterVO.getName());

        if (ResponseView.Full.equals(responseView)) {
            sfResponse.setClusterName(sfCluster.getName());
        }

        sfResponse.setTargetPortal(sfVirtualNetwork.getSvip());
        sfResponse.setVlanId(sfVirtualNetwork.getId());
        sfResponse.setVlanUuid(sfVirtualNetwork.getUuid());
        sfResponse.setVlanName(sfVirtualNetwork.getName());

        AccountDetailVO accountDetail = _accountDetailsDao.findDetail(sfVirtualNetwork.getAccountId(), SolidFireUtil.CHAP_INITIATOR_USERNAME);

        sfResponse.setChapInitiatorUsername(accountDetail.getValue());

        accountDetail = _accountDetailsDao.findDetail(sfVirtualNetwork.getAccountId(), SolidFireUtil.CHAP_INITIATOR_SECRET);

        sfResponse.setChapInitiatorSecret(accountDetail.getValue());

        accountDetail = _accountDetailsDao.findDetail(sfVirtualNetwork.getAccountId(), SolidFireUtil.CHAP_TARGET_USERNAME);

        sfResponse.setChapTargetUsername(accountDetail.getValue());

        accountDetail = _accountDetailsDao.findDetail(sfVirtualNetwork.getAccountId(), SolidFireUtil.CHAP_TARGET_SECRET);

        sfResponse.setChapTargetSecret(accountDetail.getValue());

        sfResponse.setObjectName("sfvolume");

        return sfResponse;
    }

    public List<ApiSolidFireVolumeResponse> getApiSolidFireVolumeResponse(List<SfVolume> sfVolumes, ResponseView responseView) {
        List<ApiSolidFireVolumeResponse> sfResponse = new ArrayList<>();

        if (sfVolumes != null) {
            for (SfVolume sfVolume : sfVolumes) {
                ApiSolidFireVolumeResponse response = getApiSolidFireVolumeResponse(sfVolume, responseView);

                sfResponse.add(response);
            }
        }

        return sfResponse;
    }

    public boolean isRootAdmin() {
        Account account = getCallingAccount();

        return isRootAdmin(account.getId());
    }

    public boolean isRootAdmin(long accountId) {
        return _accountMgr.isRootAdmin(accountId);
    }

    public Account getCallingAccount() {
        Account account = CallContext.current().getCallingAccount();

        if (account == null) {
            throw new CloudRuntimeException("The user's account cannot be determined.");
        }

        return account;
    }
}
