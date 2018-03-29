package com.tmser.plugin.increment;

import java.util.Arrays;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleasePrepareRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseUtils;

/**
 * maven 增量发版插件
 * 通过分析 svn 提交日志，提取增量修改文件
 */
@Mojo(name = "increment", aggregator = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class IncrementMojo extends AbstractReleaseMojo {

  /**
   * Dry run: don't checkin or tag anything in the scm repository, or modify the
   * checkout. Running
   * <code>mvn -DdryRun=true release:prepare</code> is useful in order to check
   * that modifications to poms and scm
   * operations (only listed on the console) are working as expected. Modified
   * POMs are written alongside the
   * originals without modifying them.
   */
  @Parameter(defaultValue = "false", property = "dryRun")
  private boolean dryRun;

  /**
   * is ignore excute scm update before package ?
   */
  @Parameter(defaultValue = "false", property = "ignoreUpdate")
  private boolean ignoreUpdate;

  /**
   * A list of additional exclude filters that will be skipped when checking for
   * modifications on the working copy. Is
   * ignored, when checkModificationExcludes is set.
   *
   * @since 2.1
   */
  @Parameter
  private String[] checkModificationExcludes;

  /**
   * Command-line version of checkModificationExcludes.
   *
   * @since 2.1
   */
  @Parameter(property = "checkModificationExcludeList")
  private String checkModificationExcludeList;

  /**
   * Default version to use for new local working copy.
   *
   * @since 2.0-beta-8
   */
  @Parameter(property = "developmentVersion")
  private String developmentVersion;

  /**
   * excute maven goals before packaging incremtent.
   */
  @Parameter(defaultValue = " clean package", property = "incrementGoals")
  private String incrementGoals;

  @Parameter(defaultValue = "${localRepository}", readonly = true)
  private ArtifactRepository localRepository;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    incrementRelease();
  }

  protected void incrementRelease() throws MojoExecutionException, MojoFailureException {
    // this is here so the subclass can call it without getting the extra
    // generateReleasePoms check in execute()
    // above

    ReleaseDescriptor config = createReleaseDescriptor();
    if (checkModificationExcludeList != null) {
      checkModificationExcludes = checkModificationExcludeList.replaceAll("\\s", "").split(",");
    }

    if (checkModificationExcludes != null) {
      config.setCheckModificationExcludes(Arrays.asList(checkModificationExcludes));
    }

    // Create a config containing values from the session properties (ie command
    // line properties with cli).
    ReleaseDescriptor sysPropertiesConfig = ReleaseUtils
        .copyPropertiesToReleaseDescriptor(session.getExecutionProperties());
    mergeCommandLineConfig(config, sysPropertiesConfig);

    MergeReleaseDescriptor mconfig = (MergeReleaseDescriptor) config;
    mconfig.setIncrementGoals(incrementGoals);
    mconfig.setLocalRepository(localRepository);
    mconfig.setIgnoreUpdate(ignoreUpdate);

    ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
    prepareRequest.setReleaseDescriptor(mconfig);
    prepareRequest.setReleaseEnvironment(getReleaseEnvironment());
    prepareRequest.setReactorProjects(getReactorProjects());
    prepareRequest.setDryRun(dryRun);
    prepareRequest.setReleaseManagerListener(null);

    try {
      mergeManager.increment(prepareRequest);
    } catch (ReleaseExecutionException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (ReleaseFailureException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

}
