/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.security;



import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import junit.framework.Assert;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.SecretManager.InvalidToken;
import org.apache.hadoop.hdfs.security.token.DelegationTokenIdentifier;
import org.apache.hadoop.hdfs.security.token.DelegationTokenSecretManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.log.Log;

public class TestDelegationToken {
  private MiniDFSCluster cluster;
  Configuration config;
  
  @Before
  public void setUp() throws Exception {
    config = new HdfsConfiguration();
    config.setLong(DFSConfigKeys.DFS_NAMENODE_DELEGATION_TOKEN_MAX_LIFETIME_KEY, 10000);
    config.setLong(DFSConfigKeys.DFS_NAMENODE_DELEGATION_TOKEN_RENEW_INTERVAL_KEY, 5000);
    FileSystem.setDefaultUri(config, "hdfs://localhost:" + "0");
    cluster = new MiniDFSCluster(0, config, 1, true, true, true,  null, null, null, null);
    cluster.waitActive();
  }

  @After
  public void tearDown() throws Exception {
    if(cluster!=null) {
      cluster.shutdown();
    }
  }

  private Token<DelegationTokenIdentifier> generateDelegationToken(
      String owner, String renewer) {
    DelegationTokenSecretManager dtSecretManager = cluster.getNamesystem()
        .getDelegationTokenSecretManager();
    DelegationTokenIdentifier dtId = new DelegationTokenIdentifier(new Text(
        owner), new Text(renewer));
    return new Token<DelegationTokenIdentifier>(dtId, dtSecretManager);
  }
  
  @Test
  public void testDelegationTokenSecretManager() throws Exception {
    DelegationTokenSecretManager dtSecretManager = cluster.getNamesystem()
        .getDelegationTokenSecretManager();
    Token<DelegationTokenIdentifier> token = generateDelegationToken(
        "SomeUser", "JobTracker");
    // Fake renewer should not be able to renew
	  Assert.assertFalse(dtSecretManager.renewToken(token, "FakeRenewer"));
	  Assert.assertTrue(dtSecretManager.renewToken(token, "JobTracker"));
    DelegationTokenIdentifier identifier = new DelegationTokenIdentifier();
    byte[] tokenId = token.getIdentifier();
    identifier.readFields(new DataInputStream(
             new ByteArrayInputStream(tokenId)));
    Assert.assertTrue(null != dtSecretManager.retrievePassword(identifier));
    Log.info("Sleep to expire the token");
	  Thread.sleep(6000);
	  //Token should be expired
	  try {
	    dtSecretManager.retrievePassword(identifier);
	    //Should not come here
	    Assert.fail("Token should have expired");
	  } catch (InvalidToken e) {
	    //Success
	  }
	  Assert.assertTrue(dtSecretManager.renewToken(token, "JobTracker"));
	  Log.info("Sleep beyond the max lifetime");
	  Thread.sleep(5000);
	  Assert.assertFalse(dtSecretManager.renewToken(token, "JobTracker"));
  }
  
  @Test 
  public void testCancelDelegationToken() throws Exception {
    DelegationTokenSecretManager dtSecretManager = cluster.getNamesystem()
        .getDelegationTokenSecretManager();
    Token<DelegationTokenIdentifier> token = generateDelegationToken(
        "SomeUser", "JobTracker");
    //Fake renewer should not be able to renew
    Assert.assertFalse(dtSecretManager.cancelToken(token, "FakeCanceller"));
    Assert.assertTrue(dtSecretManager.cancelToken(token, "JobTracker"));
    Assert.assertFalse(dtSecretManager.renewToken(token, "JobTracker"));
  }
  
  @Test
  public void testDelegationTokenDFSApi() throws Exception {
    DelegationTokenSecretManager dtSecretManager = cluster.getNamesystem().getDelegationTokenSecretManager();
    DistributedFileSystem dfs = (DistributedFileSystem) cluster.getFileSystem();
    Token<DelegationTokenIdentifier> token = dfs.getDelegationToken(new Text("JobTracker"));
    DelegationTokenIdentifier identifier = new DelegationTokenIdentifier();
    byte[] tokenId = token.getIdentifier();
    identifier.readFields(new DataInputStream(
             new ByteArrayInputStream(tokenId)));
    Log.info("A valid token should have non-null password, and should be renewed successfully");
    Assert.assertTrue(null != dtSecretManager.retrievePassword(identifier));
    Assert.assertTrue(dtSecretManager.renewToken(token, "JobTracker"));
  }
  
}
