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
package org.apache.cloudstack.dataaccess.dao.solidfire;

import java.util.List;

import javax.ejb.Local;

import org.apache.cloudstack.dataaccess.vo.solidfire.SfVirtualNetworkVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = SfVirtualNetworkVO.class)
public class SfVirtualNetworkDaoImpl extends GenericDaoBase<SfVirtualNetworkVO, Long> implements SfVirtualNetworkDao {
    @Override
    public List<SfVirtualNetworkVO> findByClusterId(long clusterId) {
        String columnName = "sf_cluster_id";

        SearchBuilder<SfVirtualNetworkVO> searchBuilder = createSearchBuilder();

        searchBuilder.and(columnName, searchBuilder.entity().getSfClusterId(), Op.EQ);

        searchBuilder.done();

        SearchCriteria<SfVirtualNetworkVO> sc = searchBuilder.create();

        sc.setParameters(columnName, clusterId);

        return listBy(sc);
    }

    @Override
    public List<SfVirtualNetworkVO> findByAccountId(long accountId) {
        String columnName = "account_id";

        SearchBuilder<SfVirtualNetworkVO> searchBuilder = createSearchBuilder();

        searchBuilder.and(columnName, searchBuilder.entity().getAccountId(), Op.EQ);

        searchBuilder.done();

        SearchCriteria<SfVirtualNetworkVO> sc = searchBuilder.create();

        sc.setParameters(columnName, accountId);

        return listBy(sc);
    }
}
