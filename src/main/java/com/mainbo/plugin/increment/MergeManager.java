/**
 * Mainbo.com Inc.
 * Copyright (c) 2015-2017 All Rights Reserved.
 */

package com.mainbo.plugin.increment;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseManagerListener;
import org.apache.maven.shared.release.ReleasePrepareRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * <pre>
 *
 * </pre>
 *
 * @author tmser
 * @version $Id: MergeManager.java, v 1.0 2018年1月2日 下午3:46:12 tmser Exp $
 */
public interface MergeManager {

  void increment(ReleasePrepareRequest performRequest) throws ReleaseExecutionException, ReleaseFailureException;

  /**
   * Clean a release.
   *
   * @param releaseDescriptor
   *          the configuration to use for release
   * @param reactorProjects
   *          the reactor projects
   */
  void clean(ReleaseDescriptor releaseDescriptor, ReleaseManagerListener listener, List<MavenProject> reactorProjects);

}
