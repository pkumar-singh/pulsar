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
package org.apache.pulsar.broker.namespace;

import static org.testng.Assert.assertTrue;

import com.google.common.collect.Sets;

import org.apache.pulsar.broker.service.BrokerTestBase;
import org.apache.pulsar.client.api.Producer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@Ignore // PLSR-250, ignoring whole test as it was cleanup that failed
public class NamespaceUnloadingTest extends BrokerTestBase {

    @BeforeMethod
    @Override
    protected void setup() throws Exception {
        super.baseSetup();
    }

    @AfterMethod(alwaysRun = true)
    @Override
    protected void cleanup() throws Exception {
        super.internalCleanup();
    }

    @Test
    public void testUnloadNotLoadedNamespace() throws Exception {
        admin.namespaces().createNamespace("prop/ns-test-1");
        admin.namespaces().setNamespaceReplicationClusters("prop/ns-test-1", Sets.newHashSet("test"));

        assertTrue(admin.namespaces().getNamespaces("prop").contains("prop/ns-test-1"));

        admin.namespaces().unload("prop/ns-test-1");
    }

    @Test
    public void testUnloadPartiallyLoadedNamespace() throws Exception {
        admin.namespaces().createNamespace("prop/ns-test-2", 16);
        admin.namespaces().setNamespaceReplicationClusters("prop/ns-test-2", Sets.newHashSet("test"));

        Producer<byte[]> producer = pulsarClient.newProducer().topic("persistent://prop/ns-test-2/my-topic")
                .create();

        assertTrue(admin.namespaces().getNamespaces("prop").contains("prop/ns-test-2"));

        admin.namespaces().unload("prop/ns-test-2");

        producer.close();
    }

}
