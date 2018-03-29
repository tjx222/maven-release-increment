package com.tmser.plugin.increment;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.phase.AbstractReleasePhase;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Input any variables that were not yet configured.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.phase.ReleasePhase"
 *                   role-hint="input-variables"
 */
public class CheckStartDatePhase extends AbstractReleasePhase {
  /**
   * Component used to prompt for input.
   *
   * @plexus.requirement
   */
  private Prompter prompter;

  void setPrompter(Prompter prompter) {
    this.prompter = prompter;
  }

  @Override
  public ReleaseResult execute(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
      List<MavenProject> reactorProjects) throws ReleaseExecutionException {
    ReleaseResult result = new ReleaseResult();

    // get the root project
    MavenProject project = ReleaseUtil.getRootProject(reactorProjects);

    String reversionRange = readLastReleaseLog(project);
    try {
      reversionRange = prompter.prompt("What is SCM release reversion or date range ?", reversionRange);
    } catch (PrompterException e) {
      throw new ReleaseExecutionException("Error reading version from input handler: " + e.getMessage(), e);
    }
    MergeReleaseDescriptor rd = (MergeReleaseDescriptor) releaseDescriptor;
    if (StringUtils.isNotBlank(reversionRange)) {
      String[] range = reversionRange.split("/");
      String endReversion = null;
      String startReversion = null;
      if (range.length >= 2) {
        startReversion = range[0].trim();
        endReversion = range[1].trim();
      } else {
        startReversion = reversionRange;
      }

      rd.setEndReversion(StringUtils.isBlank(endReversion) ? "HEAD" : endReversion);
      rd.setStartReversion(startReversion);
    }
    result.setResultCode(ReleaseResult.SUCCESS);
    return result;
  }

  protected String readLastReleaseLog(MavenProject project) {
    File root = project.getFile();
    File releaseLog = new File(root.getParent(), "lastrelease.log");
    String startReversion = null;
    if (releaseLog.exists() && releaseLog.canRead()) {
      try {
        startReversion = FileUtils.readFileToString(releaseLog);
      } catch (IOException e) {
        getLogger().warn("can read last releaseLog from " + releaseLog.getAbsolutePath());
      }
    }

    if (StringUtils.isNotBlank(startReversion)) {
      String[] range = startReversion.split("/");
      if (range.length > 1) {
        String end = range[1];
        if ("HEAD".equals(end)) {
          end = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        }
        startReversion = end + "/HEAD";
      }
    }

    if (StringUtils.isBlank(startReversion)) {
      // no config, use current date as default
      startReversion = DateFormatUtils.ISO_DATE_FORMAT.format(new Date()) + "/HEAD";
    }
    return startReversion;
  }

  @Override
  public ReleaseResult simulate(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
      List<MavenProject> reactorProjects) throws ReleaseExecutionException {
    // It makes no modifications, so simulate is the same as execute
    return execute(releaseDescriptor, releaseEnvironment, reactorProjects);
  }

  @Override
  public ReleaseResult clean(List<MavenProject> projects) {
    for (MavenProject project : projects) {
      File root = project.getFile();
      File releaseLog = new File(root.getParent(), "lastrelease.log");
      if (releaseLog.exists()) {
        FileUtils.deleteQuietly(releaseLog);
      }
    }
    return getReleaseResultSuccess();
  }

}
