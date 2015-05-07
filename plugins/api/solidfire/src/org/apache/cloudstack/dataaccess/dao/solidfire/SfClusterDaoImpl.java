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

import org.apache.cloudstack.dataaccess.vo.solidfire.SfClusterVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = SfClusterVO.class)
public class SfClusterDaoImpl extends GenericDaoBase<SfClusterVO, Long> implements SfClusterDao {
    @SuppressWarnings("deprecation")
    @Override
    @DB()
    public SfClusterVO findByName(final String name) {
        SearchCriteria<SfClusterVO> sc = createSearchCriteria();

        sc.addAnd("name", SearchCriteria.Op.EQ, name);

        return findOneBy(sc);
    }

    @Override
    public List<SfClusterVO> findByZoneId(long zoneId) {
        String columnName = "zoneId";

        SearchBuilder<SfClusterVO> searchBuilder = createSearchBuilder();

        searchBuilder.and(columnName, searchBuilder.entity().getZoneId(), Op.EQ);

        searchBuilder.done();

        SearchCriteria<SfClusterVO> sc = searchBuilder.create();

        sc.setParameters(columnName, zoneId);

        return listBy(sc);
    }
}
