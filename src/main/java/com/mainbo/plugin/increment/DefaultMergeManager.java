package com.mainbo.plugin.increment;

/*
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseCleanRequest;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseManagerListener;
import org.apache.maven.shared.release.ReleasePrepareRequest;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;

/**
 * Implementation of the release manager.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DefaultMergeManager extends AbstractLogEnabled implements MergeManager {
  /**
   * The phases of release to run to perform.
   */
  private List<String> incrementPhases;

  /**
   * The available phases.
   */
  private Map<String, ReleasePhase> releasePhases;

  private static final int PHASE_SKIP = 0, PHASE_START = 1, PHASE_END = 2, GOAL_START = 11, GOAL_END = 12;

  /** {@inheritDoc} */
  @Override
  public void increment(ReleasePrepareRequest performRequest)
      throws ReleaseExecutionException, ReleaseFailureException {
    increment(performRequest, new ReleaseResult());
  }

  private void logInfo(ReleaseResult result, String message) {
    if (result != null) {
      result.appendInfo(message);
    }

    getLogger().info(message);
  }

  private void increment(ReleasePrepareRequest performRequest, ReleaseResult result)
      throws ReleaseExecutionException, ReleaseFailureException {
    updateListener(performRequest.getReleaseManagerListener(), "increment", GOAL_START);

    ReleaseDescriptor releaseDescriptor = performRequest.getReleaseDescriptor();

    logInfo(result, "Start release increment version.");

    for (String name : incrementPhases) {
      ReleasePhase phase = releasePhases.get(name);

      if (phase == null) {
        throw new ReleaseExecutionException("Unable to find phase '" + name + "' to execute");
      }

      updateListener(performRequest.getReleaseManagerListener(), name, PHASE_START);

      ReleaseResult phaseResult = null;
      try {
        if (BooleanUtils.isTrue(performRequest.getDryRun())) {
          phaseResult = phase.simulate(releaseDescriptor, performRequest.getReleaseEnvironment(),
              performRequest.getReactorProjects());
        } else {
          phaseResult = phase.execute(releaseDescriptor, performRequest.getReleaseEnvironment(),
              performRequest.getReactorProjects());
        }
      } finally {
        if (result != null && phaseResult != null) {
          result.appendOutput(phaseResult.getOutput());
        }
      }

      updateListener(performRequest.getReleaseManagerListener(), name, PHASE_END);
    }

    updateListener(performRequest.getReleaseManagerListener(), "increment", GOAL_END);
  }

  /**
   * Determines the path of the working directory. By default, this is the
   * checkout directory. For some SCMs, the project root directory is not the
   * checkout directory itself, but a SCM-specific subdirectory.
   *
   * @param checkoutDirectory
   *          The checkout directory as java.io.File
   * @param relativePathProjectDirectory
   *          The relative path of the project directory within the checkout
   *          directory or ""
   * @return The working directory
   */
  protected File determineWorkingDirectory(File checkoutDirectory, String relativePathProjectDirectory) {
    if (StringUtils.isNotEmpty(relativePathProjectDirectory)) {
      return new File(checkoutDirectory, relativePathProjectDirectory);
    } else {
      return checkoutDirectory;
    }
  }

  void updateListener(ReleaseManagerListener listener, String name, int state) {
    if (listener != null) {
      switch (state) {
      case GOAL_START:
        listener.goalStart(name, getGoalPhases(name));
        break;
      case GOAL_END:
        listener.goalEnd();
        break;
      case PHASE_SKIP:
        listener.phaseSkip(name);
        break;
      case PHASE_START:
        listener.phaseStart(name);
        break;
      case PHASE_END:
        listener.phaseEnd();
        break;
      default:
        listener.error(name);
      }
    }
  }

  private List<String> getGoalPhases(String name) {
    List<String> phases = new ArrayList<String>();

    if ("increment".equals(name)) {
      phases.addAll(incrementPhases);
    }

    return Collections.unmodifiableList(phases);
  }

  /** {@inheritDoc} */
  @Override
  public void clean(ReleaseDescriptor releaseDescriptor, ReleaseManagerListener listener,
      List<MavenProject> reactorProjects) {
    ReleaseCleanRequest cleanRequest = new ReleaseCleanRequest();
    cleanRequest.setReleaseDescriptor(releaseDescriptor);
    cleanRequest.setReleaseManagerListener(listener);
    cleanRequest.setReactorProjects(reactorProjects);

    clean(cleanRequest);
  }

  /** {@inheritDoc} */
  public void clean(ReleaseCleanRequest cleanRequest) {
    updateListener(cleanRequest.getReleaseManagerListener(), "cleanup", PHASE_START);

    getLogger().info("Cleaning up ...");

    for (String name : incrementPhases) {
      {
        ReleasePhase phase = releasePhases.get(name);

        phase.clean(cleanRequest.getReactorProjects());
      }

      updateListener(cleanRequest.getReleaseManagerListener(), "cleanup", PHASE_END);
    }
  }

}