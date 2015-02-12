/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bigtop.itest.posix

class UGI {
  /**
   * Local cache for POSIX user information (as specified in /etc/passwd).
   * The key is a username and values are maps with the following information for
   * each user: passwd (password), uid (numeric user ID), gid (numeric group ID),
   * descr (textual user description), home (user home directory),
   * shell (default shell of the user).
   *
   * NOTE: all these bits of information are stored as Strings (even uid, gid).
   */
  Map users;
  /**
   * Local cache for POSIX group information (as specified in /etc/group).
   * The key is a groupname and values are maps with the following information for
   * each group: passwd (password), gid (numeric group ID), users (list of all users
   * belonging to this group as reported by id(1)).
   *
   * NOTE: all these bits of information are stored as Strings (even gid).
   */
  Map groups;

  /**
   * Synchronizes caches (users and groups) with /etc/passwd and /etc/group
   */
  public void refresh() {
    users = [:];
    groups = [:];
    Map tmp_groups = [:];
    (new File("/etc/passwd")).eachLine {
      // NOTE: the :x bit is a workaround for split to work on strings like +::::::
      def chunks = "${it}:x".split(':');
      users[chunks[0]] = [passwd: chunks[1],
        uid: chunks[2],
        gid: chunks[3],
        descr: chunks[4],
        home: chunks[5],
        shell: chunks[6],
      ];
      tmp_groups[chunks[3]] = chunks[0];
    }
    (new File("/etc/group")).eachLine {
      def chunks = it.split(':');

      groups[chunks[0]] = [passwd: chunks[1],
        gid: chunks[2],
        users: ((chunks.size() == 4) ? chunks[3].split(',').toList() : []),
      ];

      if (tmp_groups[chunks[2]] != null) {
        groups[chunks[0]].users.add(tmp_groups[chunks[2]]);
      }
    }
  }

  public UGI() {
    refresh();
  }
}
