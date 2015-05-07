package org.apache.cloudstack.api.solidfire;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.command.admin.solidfire.CreateReferenceToSolidFireClusterCmd;
import org.apache.cloudstack.api.command.admin.solidfire.CreateSolidFireVirtualNetworkCmd;
import org.apache.cloudstack.api.command.admin.solidfire.DeleteReferenceToSolidFireClusterCmd;
import org.apache.cloudstack.api.command.admin.solidfire.DeleteSolidFireVirtualNetworkCmd;
import org.apache.cloudstack.api.command.admin.solidfire.ListSolidFireClustersCmd;
import org.apache.cloudstack.api.command.admin.solidfire.UpdateReferenceToSolidFireClusterCmd;
import org.apache.cloudstack.api.command.admin.solidfire.UpdateSolidFireVirtualNetworkCmd;
import org.apache.cloudstack.api.command.user.solidfire.CreateSolidFireVolumeCmd;
import org.apache.cloudstack.api.command.user.solidfire.DeleteSolidFireVolumeCmd;
import org.apache.cloudstack.api.command.user.solidfire.ListSolidFireVirtualNetworksCmd;
import org.apache.cloudstack.api.command.user.solidfire.ListSolidFireVolumesCmd;
import org.apache.cloudstack.api.command.user.solidfire.UpdateSolidFireVolumeCmd;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.AdapterBase;

@Component
public class ApiSolidFireServiceImpl extends AdapterBase implements ApiSolidFireService {
    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();

        cmdList.add(ListSolidFireClustersCmd.class);
        cmdList.add(CreateReferenceToSolidFireClusterCmd.class);
        cmdList.add(UpdateReferenceToSolidFireClusterCmd.class);
        cmdList.add(DeleteReferenceToSolidFireClusterCmd.class);

        cmdList.add(ListSolidFireVirtualNetworksCmd.class);
        cmdList.add(CreateSolidFireVirtualNetworkCmd.class);
        cmdList.add(UpdateSolidFireVirtualNetworkCmd.class);
        cmdList.add(DeleteSolidFireVirtualNetworkCmd.class);

        cmdList.add(ListSolidFireVolumesCmd.class);
        cmdList.add(CreateSolidFireVolumeCmd.class);
        cmdList.add(UpdateSolidFireVolumeCmd.class);
        cmdList.add(DeleteSolidFireVolumeCmd.class);

        return cmdList;
    }
}
