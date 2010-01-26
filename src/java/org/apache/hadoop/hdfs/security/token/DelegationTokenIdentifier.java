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

package org.apache.hadoop.hdfs.security.token;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableFactories;
import org.apache.hadoop.io.WritableFactory;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.security.token.TokenIdentifier;

public class DelegationTokenIdentifier extends TokenIdentifier {
  static final Text KIND_NAME = new Text("HDFS_DELEGATION_TOKEN");

  private Text owner;
  private Text renewer;
  private long issueDate;
  private long maxDate;
  private int sequenceNumber;
  private int masterKeyId = 0;
  
  public DelegationTokenIdentifier() {
    this(new Text(), new Text());
  }
  
  public DelegationTokenIdentifier(Text owner, Text renewer) {
    this.owner = owner;
    this.renewer = renewer;
    issueDate = 0;
    maxDate = 0;
  }

  @Override
  public Text getKind() {
    return KIND_NAME;
  }
  
  /**
   * Get the username encoded in the token identifier
   * 
   * @return the username or owner
   */
  public Text getUsername() {
    return owner;
  }
  
  public Text getRenewer() {
    return renewer;
  }
  
  public void setIssueDate(long issueDate) {
    this.issueDate = issueDate;
  }
  
  public long getIssueDate() {
    return issueDate;
  }
  
  public void setMaxDate(long maxDate) {
    this.maxDate = maxDate;
  }
  
  public long getMaxDate() {
    return maxDate;
  }

  public void setSequenceNumber(int seqNum) {
    this.sequenceNumber = seqNum;
  }
  
  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public void setMasterKeyId(int newId) {
    masterKeyId = newId;
  }

  public int getMasterKeyId() {
    return masterKeyId;
  }

  static boolean isEqual(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }
  
  /** {@inheritDoc} */
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof DelegationTokenIdentifier) {
      DelegationTokenIdentifier that = (DelegationTokenIdentifier) obj;
      return this.sequenceNumber == that.sequenceNumber 
          && this.issueDate == that.issueDate 
          && this.maxDate == that.maxDate
          && this.masterKeyId == that.masterKeyId
          && isEqual(this.owner, that.owner) 
          && isEqual(this.renewer, that.renewer);
    }
    return false;
  }

  /** {@inheritDoc} */
  public int hashCode() {
    return this.sequenceNumber;
  }
  
  public void readFields(DataInput in) throws IOException {
    owner.readFields(in);
    renewer.readFields(in);
    issueDate = WritableUtils.readVLong(in);
    maxDate = WritableUtils.readVLong(in);
    sequenceNumber = WritableUtils.readVInt(in);
    masterKeyId = WritableUtils.readVInt(in);
  }

  public void write(DataOutput out) throws IOException {
    owner.write(out);
    renewer.write(out);
    WritableUtils.writeVLong(out, issueDate);
    WritableUtils.writeVLong(out, maxDate);
    WritableUtils.writeVInt(out, sequenceNumber);
    WritableUtils.writeVInt(out, masterKeyId);
  }
}
