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
package org.apache.hadoop.oncrpc;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

/**
 * Simple UDP server implemented based on netty.
 */
public class SimpleUdpServer {
  public static final Log LOG = LogFactory.getLog(SimpleUdpServer.class);
  private final int SEND_BUFFER_SIZE = 65536;
  private final int RECEIVE_BUFFER_SIZE = 65536;

  protected final int port;
  protected final ChannelPipelineFactory pipelineFactory;
  protected final RpcProgram rpcProgram;
  protected final int workerCount;

  public SimpleUdpServer(int port, RpcProgram program, int workerCount) {
    this.port = port;
    this.rpcProgram = program;
    this.workerCount = workerCount;
    this.pipelineFactory = new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() {
        return Channels.pipeline(new SimpleUdpServerHandler(rpcProgram));
      }
    };
  }

  public void run() {
    // Configure the client.
    DatagramChannelFactory f = new NioDatagramChannelFactory(
        Executors.newCachedThreadPool(), workerCount);

    ConnectionlessBootstrap b = new ConnectionlessBootstrap(f);
    ChannelPipeline p = b.getPipeline();
    p.addLast("handler", new SimpleUdpServerHandler(rpcProgram));

    b.setOption("broadcast", "false");
    b.setOption("sendBufferSize", SEND_BUFFER_SIZE);
    b.setOption("receiveBufferSize", RECEIVE_BUFFER_SIZE);
    
    // Listen to the UDP port
    try {
      b.bind(new InetSocketAddress(port));
    } catch (ChannelException e) {
      LOG.error("Can't bind UDP port " + port + " for " + rpcProgram
          + ", error: " + e);
      System.exit(-1);
    }
    LOG.info("Started listening to UDP requests at port " + port
        + " for " + rpcProgram + " with workerCount " + workerCount);
  }
}
