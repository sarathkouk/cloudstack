package org.apache.cloudstack.api.solidfire;

public class SfApiConstants {
    public static final String MVIP = "mvip";
    public static final String SVIP = "svip";
    public static final String CLUSTER_NAME = "clustername";
    public static final String NAME = "name";
    public static final String TAG = "tag";
    public static final String START_IP = "startip";
    public static final String SIZE = "size";
    public static final String NETMASK = "netmask";
    public static final String TOTAL_CAPACITY = "totalcapacity";
    public static final String TOTAL_MIN_IOPS = "totalminiops";
    public static final String TOTAL_MAX_IOPS = "totalmaxiops";
    public static final String TOTAL_BURST_IOPS = "totalburstiops";
    public static final String BURST_IOPS = "burstiops";
    public static final String SF_VIRTUAL_NETWORK_ID = "sfvirtualnetworkid";

    // descriptions
    public static final String SOLIDFIRE_CLUSTER_NAME_DESC = "SolidFire cluster name";
    public static final String SOLIDFIRE_MVIP_DESC = "SolidFire management virtual IP address";
    public static final String SOLIDFIRE_SVIP_DESC = "SolidFire storage virtual IP address for VLAN";
    public static final String SOLIDFIRE_USERNAME_DESC = "SolidFire cluster admin username";
    public static final String SOLIDFIRE_PASSWORD_DESC = "SolidFire cluster admin password";
    public static final String TOTAL_CAPACITY_DESC = "Total capacity (in GBs)";
    public static final String TOTAL_MIN_IOPS_DESC = "Total minimum IOPS";
    public static final String TOTAL_MAX_IOPS_DESC = "Total maximum IOPS";
    public static final String TOTAL_BURST_IOPS_DESC = "Total burst IOPS";
    public static final String IQN_DESC = "Volume IQN";
    public static final String SIZE_DESC = "Size (in GBs)";
    public static final String MIN_IOPS_DESC = "Min IOPS";
    public static final String MAX_IOPS_DESC = "Max IOPS";
    public static final String BURST_IOPS_DESC = "Burst IOPS";
    public static final String VIRTUAL_NETWORK_NAME_DESC = "VLAN name";
    public static final String VIRTUAL_NETWORK_TAG_DESC = "VLAN tag";
    public static final String START_IP_ADDRESS_DESC = "Start IP address";
    public static final String NUMBER_OF_IP_ADDRESSES_DESC = "Number of contiguous IP addresses starting at '" + SfApiConstants.START_IP + "'";
    public static final String NETMASK_DESC = "Netmask of VLAN";
    public static final String ACCOUNT_ID_DESC = "Account ID";
    public static final String VIRTUAL_NETWORK_ID_DESC = "Virtual network ID";
    public static final String VOLUME_ID_DESC = "Volume ID";
    public static final String VOLUME_NAME_DESC = "Volume name";
    public static final String ZONE_ID_DESC = "Zone ID";
}
