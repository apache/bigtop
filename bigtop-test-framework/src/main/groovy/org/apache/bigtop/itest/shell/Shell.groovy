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

package org.apache.bigtop.itest.shell

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class Shell {
  static private Log LOG = LogFactory.getLog(Shell.class)

  static private final String DEFAULT_SHELL = "/bin/bash -s";

  String shell;
  String user;

  String script;
  List<String> out;
  List<String> err;
  int ret;

  Shell(String sh) {
    shell = sh;
  }

  Shell(String sh, String u) {
    shell = sh;
    user = u;
  }

  Shell() {
    shell = DEFAULT_SHELL;
  }

  void setUser(String u) {
    user = u
  }

  /**
   * Execute shell script consisting of as many Strings as we have arguments,
   * possibly under an explicit username (requires sudoers privileges).
   * NOTE: individual strings are concatenated into a single script as though
   * they were delimited with new line character. All quoting rules are exactly
   * what one would expect in standalone shell script.
   *
   * After executing the script its return code can be accessed as getRet(),
   * stdout as getOut() and stderr as getErr(). The script itself can be accessed
   * as getScript()
   * WARNING: it isn't thread safe
   * @param timeout timeout in milliseconds to wait before killing the script.
   * If timeout < 0, then this method will wait until the script completes
   * and will not be killed.
   * @param args shell script split into multiple Strings
   * @return Shell object for chaining
   */
  Shell execWithTimeout(int timeout, Object... args) {
    def proc = user ? "sudo -u $user PATH=${System.getenv('PATH')} $shell".execute() :
                                    "$shell".execute()
    script = args.join("\n")
    if (LOG.isTraceEnabled()) {
        LOG.trace("${shell} << __EOT__\n${script}\n__EOT__");
    }

    Thread.start {
      def writer = new PrintWriter(new BufferedOutputStream(proc.out))
      writer.println(script)
      writer.close()
    }
    ByteArrayOutputStream outStream = new ByteArrayOutputStream(4096)
    ByteArrayOutputStream errStream = new ByteArrayOutputStream(4096)
    Thread.start {
      proc.consumeProcessOutput(outStream, errStream)
    }
    if (timeout >= 0) {
      proc.waitForOrKill(timeout)
    } else {
      proc.waitFor()
    }

    // Possibly a bug in String.split as it generates a 1-element array on an
    // empty String
    if (outStream.size() != 0) {
      out = outStream.toString().split('\n')
    } else {
      out = Collections.EMPTY_LIST
    }
    if (errStream.size() != 0) {
      err = errStream.toString().split('\n')
    }
    else {
      err = Collections.EMPTY_LIST
    }
    ret = proc.exitValue()

    if (LOG.isTraceEnabled()) {
        if (ret != 0) {
           LOG.trace("return: $ret");
        }
        if (out.size() != 0) {
           LOG.trace("\n<stdout>\n${out.join('\n')}\n</stdout>");
        }
        if (err.size() != 0) {
           LOG.trace("\n<stderr>\n${err.join('\n')}\n</stderr>");
        }
    }
    return this
  }

  /**
   * Execute shell script consisting of as many Strings as we have arguments,
   * possibly under an explicit username (requires sudoers privileges).
   * NOTE: individual strings are concatenated into a single script as though
   * they were delimited with new line character. All quoting rules are exactly
   * what one would expect in standalone shell script.
   *
   * After executing the script its return code can be accessed as getRet(),
   * stdout as getOut() and stderr as getErr(). The script itself can be accessed
   * as getScript()
   * WARNING: it isn't thread safe
   * @param timeout timeout in milliseconds to wait before killing the script
   * . If timeout < 0, then this method will wait until the script completes
   * and will not be killed.
   * @param args shell script split into multiple Strings
   * @return Shell object for chaining
   */
  Shell exec(Object... args) {
    return execWithTimeout(-1, args)
  }

  /**
   * Executes a shell script consisting of as many strings as we have args,
   * under an explicit user name. This method does the same job as
   * {@linkplain #exec(java.lang.Object[])}, but will return immediately,
   * with the process continuing execution in the background. If this method
   * is called, the output stream and error stream of this script  will be
   * available in the {@linkplain #out} and {@linkplain #err} lists.
   * WARNING: it isn't thread safe
   * <strong>CAUTION:</strong>
   * If this shell object is used to run other script while a script is
   * being executed in the background, then the output stream and error
   * stream of the script executed later will be what is available,
   * and the output and error streams of this script may be lost.
   * @param args
   * @return Shell object for chaining
   */
  Shell fork(Object... args) {
    forkWithTimeout(-1, args)
    return this
  }

  /**
   * Executes a shell script consisting of as many strings as we have args,
   * under an explicit user name. This method does the same job as
   * {@linkplain #execWithTimeout(int, java.lang.Object[])}, but will return immediately,
   * with the process continuing execution in the background for timeout
   * milliseconds (or until the script completes , whichever is earlier). If
   * this method
   * is called, the output stream and error stream of this script will be
   * available in the {@linkplain #out} and {@linkplain #err} lists.
   * WARNING: it isn't thread safe
   * <strong>CAUTION:</strong>
   * If this shell object is used to run other script while a script is
   * being executed in the background, then the output stream and error
   * stream of the script executed later will be what is available,
   * and the output and error streams of this script may be lost.
   * @param timeout The timoeut in milliseconds before the script is killed
   * @param args
   * @return Shell object for chaining
   */
  Shell forkWithTimeout(int timeout, Object... args) {
    Thread.start {
      execWithTimeout(timeout, args)
    }
    return this
  }
}
