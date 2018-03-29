package com.tmser.plugin.increment;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * Clean up after a release preparation. This is done automatically after a
 * successful <tt>release:perform</tt>,
 * so is best served for cleaning up a failed or abandoned release, or a dry
 * run. Note that only the working copy
 * is cleaned up, no previous steps are rolled back.
 * For more info see <a href=
 * "http://maven.apache.org/plugins/maven-release-plugin/examples/clean-release.html"
 * >http://maven.apache.org/plugins/maven-release-plugin/examples/clean-release.
 * html</a>.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
@Mojo(name = "clean", aggregator = true)
public class CleanReleaseMojo extends AbstractReleaseMojo {

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
    releaseDescriptor.setWorkingDirectory(getBasedir().getAbsolutePath());
    mergeManager.clean(releaseDescriptor, null, getReactorProjects());
  }

}
