/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.zookeeper;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.util.HashedWheelTimer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.client.RackawareEnsemblePlacementPolicy;
import org.apache.bookkeeper.client.RackawareEnsemblePlacementPolicyImpl;
import org.apache.bookkeeper.conf.ClientConfiguration;
import org.apache.bookkeeper.feature.FeatureProvider;
import org.apache.bookkeeper.net.DNSToSwitchMapping;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.bookkeeper.zookeeper.ZooKeeperClient;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.pulsar.common.policies.data.BookiesRackConfiguration;
import org.apache.pulsar.common.util.ObjectMapperFactory;
import org.apache.pulsar.zookeeper.ZooKeeperCache.Deserializer;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkIsolatedBookieEnsemblePlacementPolicy extends RackawareEnsemblePlacementPolicy
        implements Deserializer<BookiesRackConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(ZkIsolatedBookieEnsemblePlacementPolicy.class);

    public static final String ISOLATION_BOOKIE_GROUPS = "isolationBookieGroups";
    public static final String SECONDARY_ISOLATION_BOOKIE_GROUPS = "secondaryIsolationBookieGroups";

    private ZooKeeperCache bookieMappingCache = null;

    private final ObjectMapper jsonMapper = ObjectMapperFactory.create();
    // Using a pair to save the isolation groups, the left value is the primary group and the right value is
    // the secondary group.
    private ImmutablePair<Set<String>, Set<String>> defaultIsolationGroups;

    public ZkIsolatedBookieEnsemblePlacementPolicy() {
        super();
    }

    @Override
    public RackawareEnsemblePlacementPolicyImpl initialize(ClientConfiguration conf,
            Optional<DNSToSwitchMapping> optionalDnsResolver, HashedWheelTimer timer, FeatureProvider featureProvider,
            StatsLogger statsLogger) {
        Set<String> primaryIsolationGroups = new HashSet<>();
        Set<String> secondaryIsolationGroups = new HashSet<>();
        if (conf.getProperty(ISOLATION_BOOKIE_GROUPS) != null) {
            String isolationGroupsString = castToString(conf.getProperty(ISOLATION_BOOKIE_GROUPS));
            if (!isolationGroupsString.isEmpty()) {
                for (String isolationGroup : isolationGroupsString.split(",")) {
                    primaryIsolationGroups.add(isolationGroup);
                }
                bookieMappingCache = getAndSetZkCache(conf);
            }
        }
        if (conf.getProperty(SECONDARY_ISOLATION_BOOKIE_GROUPS) != null) {
            String secondaryIsolationGroupsString = castToString(conf.getProperty(SECONDARY_ISOLATION_BOOKIE_GROUPS));
            if (!secondaryIsolationGroupsString.isEmpty()) {
                for (String isolationGroup : secondaryIsolationGroupsString.split(",")) {
                    secondaryIsolationGroups.add(isolationGroup);
                }
            }
        }
        defaultIsolationGroups = ImmutablePair.of(primaryIsolationGroups, secondaryIsolationGroups);
        return super.initialize(conf, optionalDnsResolver, timer, featureProvider, statsLogger);
    }

    private static String castToString(Object obj) {
        if (null == obj) {
            return "";
        }
        if (obj instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object o : (List<?>) obj) {
                result.add((String) o);
            }
            return String.join(",", result);
        } else {
            return obj.toString();
        }
    }

    private ZooKeeperCache getAndSetZkCache(Configuration conf) {
        ZooKeeperCache zkCache = null;
        if (conf.getProperty(ZooKeeperCache.ZK_CACHE_INSTANCE) != null) {
            zkCache = (ZooKeeperCache) conf.getProperty(ZooKeeperCache.ZK_CACHE_INSTANCE);
        } else {
            int zkTimeout;
            String zkServers;
            if (conf instanceof ClientConfiguration) {
                zkTimeout = ((ClientConfiguration) conf).getZkTimeout();
                zkServers = ((ClientConfiguration) conf).getZkServers();
                try {
                    ZooKeeper zkClient = ZooKeeperClient.newBuilder().connectString(zkServers)
                            .sessionTimeoutMs(zkTimeout).build();
                    zkCache = new ZooKeeperCache("bookies-isolation", zkClient,
                            (int) TimeUnit.MILLISECONDS.toSeconds(zkTimeout)) {
                    };
                    conf.addProperty(ZooKeeperCache.ZK_CACHE_INSTANCE, zkCache);
                } catch (Exception e) {
                    LOG.error("Error creating zookeeper client", e);
                }
            } else {
                LOG.error("No zk configurations available");
            }
        }
        return zkCache;
    }

//    @Override
//    public PlacementResult<List<BookieId>> newEnsemble(int ensembleSize, int writeQuorumSize, int ackQuorumSize,
//            Map<String, byte[]> customMetadata, Set<BookieId> excludeBookies)
//            throws BKNotEnoughBookiesException {
//        Set<BookieId> blacklistedBookies = getBlacklistedBookies(ensembleSize);
//        if (excludeBookies == null) {
//            excludeBookies = new HashSet<BookieId>();
//        }
//        excludeBookies.addAll(blacklistedBookies);
//        return super.newEnsemble(ensembleSize, writeQuorumSize, ackQuorumSize, customMetadata, excludeBookies);
//    }
//
//    @Override
//    public PlacementResult<BookieId> replaceBookie(int ensembleSize, int writeQuorumSize, int ackQuorumSize,
//            Map<String, byte[]> customMetadata, List<BookieId> currentEnsemble,
//            BookieId bookieToReplace, Set<BookieId> excludeBookies)
//            throws BKNotEnoughBookiesException {
//        Set<BookieId> blacklistedBookies = getBlacklistedBookies(ensembleSize);
//        if (excludeBookies == null) {
//            excludeBookies = new HashSet<BookieId>();
//        }
//        excludeBookies.addAll(blacklistedBookies);
//        return super.replaceBookie(ensembleSize, writeQuorumSize, ackQuorumSize, customMetadata, currentEnsemble,
//                bookieToReplace, excludeBookies);
//    }
//
//    private Set<BookieId> getBlacklistedBookies(int ensembleSize) {
//        Set<BookieId> blacklistedBookies = new HashSet<BookieId>();
//        try {
//            if (bookieMappingCache != null) {
//                BookiesRackConfiguration allGroupsBookieMapping = bookieMappingCache
//                        .getData(ZkBookieRackAffinityMapping.BOOKIE_INFO_ROOT_PATH, this)
//                        .orElseThrow(() -> new KeeperException.NoNodeException(
//                                ZkBookieRackAffinityMapping.BOOKIE_INFO_ROOT_PATH));
//                Set<String> allBookies = allGroupsBookieMapping.keySet();
//                int totalAvailableBookiesInPrimaryGroup = 0;
//                for (String group : allBookies) {
//                    Set<String> bookiesInGroup = allGroupsBookieMapping.get(group).keySet();
//                    if (!primaryIsolationGroups.contains(group)) {
//                        for (String bookieAddress : bookiesInGroup) {
//                            blacklistedBookies.add(BookieId.parse(bookieAddress));
//                        }
//                    } else {
//                        for (String groupBookie : bookiesInGroup) {
//                            totalAvailableBookiesInPrimaryGroup += knownBookies
//                                    .containsKey(BookieId.parse(groupBookie)) ? 1 : 0;
//                        }
//                    }
//                }
//                // sometime while doing isolation, user might not want to remove isolated bookies from other default
//                // groups. so, same set of bookies could be overlapped into isolated-group and other default groups. so,
//                // try to remove those overlapped bookies from excluded-bookie list because they are also part of
//                // isolated-group bookies.
//                for (String group : primaryIsolationGroups) {
//                    Map<String, BookieInfo> bookieGroup = allGroupsBookieMapping.get(group);
//                    if (bookieGroup != null && !bookieGroup.isEmpty()) {
//                        for (String bookieAddress : bookieGroup.keySet()) {
//                            blacklistedBookies.remove(BookieId.parse(bookieAddress));
//                        }
//                    }
//                }
//                // if primary-isolated-bookies are not enough then add consider secondary isolated bookie group as well.
//                if (totalAvailableBookiesInPrimaryGroup < ensembleSize) {
//                    LOG.info(
//                            "Not found enough available-bookies from primary isolation group {} , checking secondary group",
//                            primaryIsolationGroups, secondaryIsolationGroups);
//                    for (String group : secondaryIsolationGroups) {
//                        Map<String, BookieInfo> bookieGroup = allGroupsBookieMapping.get(group);
//                        if (bookieGroup != null && !bookieGroup.isEmpty()) {
//                            for (String bookieAddress : bookieGroup.keySet()) {
//                                blacklistedBookies.remove(BookieId.parse(bookieAddress));
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            LOG.warn("Error getting bookie isolation info from zk: {}", e.getMessage());
//        }
//        return blacklistedBookies;
//    }

    @Override
    public BookiesRackConfiguration deserialize(String key, byte[] content) throws Exception {
        LOG.info("Reloading the bookie isolation groups mapping cache.");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading the bookie mappings with bookie info data: {}", new String(content));
        }
        return jsonMapper.readValue(content, BookiesRackConfiguration.class);
    }
}
