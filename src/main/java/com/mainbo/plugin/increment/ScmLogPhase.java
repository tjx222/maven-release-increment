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
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.AbstractScmVersion;
import org.apache.maven.scm.ChangeFile;
import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.changelog.ChangeLogScmRequest;
import org.apache.maven.scm.command.changelog.ChangeLogScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.phase.AbstractReleasePhase;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.SelectorUtils;

/**
 * parse the SCM repository log.
 *
 * @author tmser
 */
public class ScmLogPhase extends AbstractReleasePhase {
  /**
   * Tool that gets a configured SCM repository from release configuration.
   *
   * @plexus.requirement
   */
  private ScmRepositoryConfigurator scmRepositoryConfigurator;

  /**
   * @plexus.requirement
   */
  private DependencyTreeBuilder dependencyTreeBuilder;

  /**
   * @plexus.requirement
   */
  private ArtifactMetadataSource artifactMetadataSource;

  /**
   * @plexus.requirement
   */
  private ArtifactCollector artifactCollector;

  private ArtifactRepository localRepository;

  /**
   * @plexus.requirement
   */
  private ArtifactFactory artifactFactory;

  private static final String WEBLIB = "WEB-INF" + File.separator + "lib" + File.separator;
  private static final String WEBCLASSES = "WEB-INF" + File.separator + "classes" + File.separator;

  private static final String WEBROOT = File.separator + "src" + File.separator + "main" + File.separator + "webapp"
      + File.separator;

  private Set<String> exclusionPatterns = new HashSet<String>(Arrays.asList("**" + File.separator + "pom.xml",
      "**" + File.separator + "lastrelease.log", "**" + File.separator + "lastreleaselib.log"));

  @Override
  public ReleaseResult execute(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
      List<MavenProject> reactorProjects) throws ReleaseExecutionException, ReleaseFailureException {
    ReleaseResult relResult = new ReleaseResult();

    List<String> additionalExcludes = releaseDescriptor.getCheckModificationExcludes();

    if (additionalExcludes != null) {
      // SelectorUtils expects OS-specific paths and patterns
      for (String additionalExclude : additionalExcludes) {
        exclusionPatterns.add(additionalExclude.replace("\\", File.separator).replace("/", File.separator));
      }
    }
    localRepository = ((MergeReleaseDescriptor) releaseDescriptor).getLocalRepository();
    logInfo(relResult, "Log release with the label ...");
    ChangeLogScmResult result = getLogResult(releaseDescriptor, releaseEnvironment, reactorProjects);

    if (!result.isSuccess()) {
      throw new ReleaseScmCommandException("Unable to log SCM", result);
    } else {
      logInfo(relResult, "start process increment pack!");
      processLogResult(relResult, result, reactorProjects);
    }

    writeLastReleaseLog(ReleaseUtil.getRootProject(reactorProjects), releaseDescriptor);
    relResult.setResultCode(ReleaseResult.SUCCESS);

    return relResult;
  }

  /**
   * @param project
   */
  private void writeLastReleaseLog(MavenProject project, ReleaseDescriptor releaseDescriptor) {
    File root = project.getFile();
    File releaseLog = new File(root.getParent(), "lastrelease.log");
    MergeReleaseDescriptor rd = (MergeReleaseDescriptor) releaseDescriptor;
    try {
      FileUtils.writeStringToFile(releaseLog, rd.getStartReversion() + "/" + rd.getEndReversion());
    } catch (IOException e) {
      getLogger().warn("failed write last releaseLog from " + releaseLog.getAbsolutePath());
    }
  }

  @Override
  public ReleaseResult clean(List<MavenProject> reactorProjects) {
    for (MavenProject mp : reactorProjects) {
      if ("war".equalsIgnoreCase(mp.getPackaging())) {
        String outputPath = mp.getBuild().getOutputDirectory();
        File targetFolder = new File(outputPath);
        File incrementFolder = new File(targetFolder.getParent(),
            mp.getArtifactId() + "-" + mp.getVersion() + "-increment");
        if (!incrementFolder.exists()) {
          getLogger().info(incrementFolder.getAbsolutePath() + " not exists!");
        } else {
          try {
            FileUtils.deleteDirectory(incrementFolder);
          } catch (IOException e) {
            getLogger()
                .warn("delete folder failed" + incrementFolder.getAbsolutePath() + " not exists!" + e.getMessage());
          }
        }

        File zipFile = new File(targetFolder.getParent(), incrementFolder.getName() + ".zip");

        if (!zipFile.exists()) {
          getLogger().info(zipFile.getAbsolutePath() + " not exists!");
        } else {
          FileUtils.deleteQuietly(zipFile);
        }

        File root = mp.getFile();
        File releaseLog = new File(root.getParent(), "lastreleaselib.log");
        if (!releaseLog.exists()) {
          getLogger().info(releaseLog.getAbsolutePath() + " not exists!");
        } else {
          FileUtils.deleteQuietly(releaseLog);
        }
      }

    }

    return super.getReleaseResultSuccess();
  }

  /**
   * @param result
   * @throws ReleaseFailureException
   */
  private void processLogResult(ReleaseResult relResult, ChangeLogScmResult result, List<MavenProject> reactorProjects)
      throws ReleaseFailureException {
    Map<String, List<ChangeFile>> projectChangeResult = new HashMap<>();
    Map<String, ChangeFile> jarChangeResult = new HashMap<>();
    for (ChangeSet cs : result.getChangeLog().getChangeSets()) {
      logInfo(relResult, "change log: " + cs.getRevision() + "  " + cs.getComment());
      file_iter: for (ChangeFile cf : cs.getFiles()) {// 处理变更文件
        String filename = cf.getName().replace("\\", File.separator).replace("/", File.separator);
        if (filename.startsWith(File.separator)) {
          filename = filename.substring(File.separator.length());
        }
        logInfo(relResult, "change file : " + cf.getRevision() + " " + cf.getName());
        for (String exclusionPattern : exclusionPatterns) {
          if (SelectorUtils.matchPath(exclusionPattern, filename)) {
            logDebug(relResult, "Ignoring changed file: " + filename);
            continue file_iter;
          }
        }

        for (MavenProject mp : reactorProjects) {
          if (filename.contains(mp.getBuild().getTestSourceDirectory().replace(mp.getBasedir().getAbsolutePath(), "")
              .replace("\\", File.separator).replace("/", File.separator))) {
            logDebug(relResult, "Ignoring changed file: " + filename);
            continue file_iter;
          }

          if (filename.contains(File.separator + mp.getBasedir().getName() + File.separator)) {
            if ("war".equalsIgnoreCase(mp.getPackaging())) { // 只处理war包和当前项目匹配的变更
              List<ChangeFile> changeFiles = projectChangeResult.get(mp.getBasedir().getName());
              if (changeFiles == null) {
                changeFiles = new ArrayList<>();
                projectChangeResult.put(mp.getBasedir().getName(), changeFiles);
              }
              if (findOrigin(cf, mp, changeFiles)) {
                changeFiles.add(cf);
              }
              break;
            } else if ("jar".equalsIgnoreCase(mp.getPackaging())) {
              String jarname = mp.getArtifactId() + "-" + mp.getVersion() + ".jar";
              ChangeFile jarpack = new ChangeFile(WEBLIB + jarname);
              jarpack.setAction(ScmFileStatus.ADDED);
              jarpack
                  .setOriginalName(new File(mp.getBuild().getOutputDirectory()).getParent() + File.separator + jarname);
              jarChangeResult.put(jarname, jarpack);

              String deljarname = mp.getArtifactId() + "-*.jar";
              ChangeFile deljarpack = new ChangeFile(WEBLIB + deljarname);
              deljarpack.setAction(ScmFileStatus.DELETED);
              deljarpack.setOriginalName(jarpack.getOriginalName() + "-deleted");
              jarChangeResult.put(jarname + "-deleted", deljarpack);
            }
          }

        }
      }
    }

    addDepenMoudle(reactorProjects, projectChangeResult, jarChangeResult);

    if (!projectChangeResult.isEmpty()) {
      for (MavenProject mp : reactorProjects) {
        if ("war".equalsIgnoreCase(mp.getPackaging())) {
          List<ChangeFile> changeFiles = projectChangeResult.get(mp.getBasedir().getName());
          if (changeFiles != null && changeFiles.size() > 0) {
            packageIncrement(relResult, mp, changeFiles);
          }
        }

      }
    }

  }

  /**
   * @param reactorProjects
   * @param projectChangeResult
   * @param jarChangeResult
   */
  private void addDepenMoudle(List<MavenProject> reactorProjects, Map<String, List<ChangeFile>> projectChangeResult,
      Map<String, ChangeFile> jarChangeResult) {
    if (!jarChangeResult.isEmpty()) {
      for (MavenProject mp : reactorProjects) {
        if ("war".equalsIgnoreCase(mp.getPackaging())) {
          List<ChangeFile> changeFiles = projectChangeResult.get(mp.getBasedir().getName());
          Set<String> dependMoudlesSet = new HashSet<>();
          readMpDependMoudles(mp, dependMoudlesSet);
          for (String dms : dependMoudlesSet) {
            if (jarChangeResult.get(dms) != null) {
              if (changeFiles == null) {
                changeFiles = new ArrayList<>();
                projectChangeResult.put(mp.getArtifactId(), changeFiles);
              }
              changeFiles.add(jarChangeResult.get(dms));
              if (jarChangeResult.get(dms + "-deleted") != null) {
                changeFiles.add(jarChangeResult.get(dms + "-deleted"));
              }
            }
          }
        }
      }

    }

  }

  /**
   * @param mp
   */
  private void readMpDependMoudles(MavenProject mp, Set<String> dependMoudlesSet) {
    MavenProject parent = mp;
    MavenProject self = null;
    while (parent != null && !parent.equals(self)) {
      if (parent.getProjectReferences() != null) {
        for (Object obj : parent.getProjectReferences().values()) {
          MavenProject mvp = (MavenProject) obj;
          if ("jar".equalsIgnoreCase(mvp.getPackaging())) {
            dependMoudlesSet.add(mvp.getArtifactId() + "-" + mvp.getVersion() + ".jar");
            readMpDependMoudles(mvp, dependMoudlesSet);
          }
        }
      }
      self = parent;
      parent = mp.getParent();
    }
  }

  /**
   * 打包项目变更
   * 
   * @param mp
   * @param changeFiles
   * @throws ReleaseFailureException
   * @throws IOException
   */
  private void packageIncrement(ReleaseResult relResult, MavenProject mp, List<ChangeFile> changeFiles)
      throws ReleaseFailureException {
    String outputPath = mp.getBuild().getOutputDirectory();
    File targetFolder = new File(outputPath);
    if (!targetFolder.exists()) {
      logWarn(relResult, "please add package command before incement to compile this project!");
      return;
    }

    File incrementFolder = new File(targetFolder.getParent(),
        mp.getArtifactId() + "-" + mp.getVersion() + "-increment");
    try {
      FileUtils.forceMkdir(incrementFolder);
    } catch (IOException e) {
      throw new ReleaseFailureException("Create increment folder failed " + e.getMessage());
    }

    changeFiles.addAll(copyModifyLib(mp));// 获取需要更新依赖jar包

    Map<String, String> addFiles = new HashMap<>();
    Map<String, String> delFiles = new HashMap<>();
    Map<String, String> modifyFiles = new HashMap<>();
    for (ChangeFile changeFile : changeFiles) {
      if (ScmFileStatus.MODIFIED.equals(changeFile.getAction())) { // modify
        modifyFiles.put(changeFile.getOriginalName(), changeFile.getName());
      } else if (ScmFileStatus.ADDED.equals(changeFile.getAction())) {
        if (delFiles.remove(changeFile.getOriginalName()) == null) {
          addFiles.put(changeFile.getOriginalName(), changeFile.getName());
        }
      } else if (ScmFileStatus.DELETED.equals(changeFile.getAction())) {
        if (addFiles.remove(changeFile.getOriginalName()) == null) {
          delFiles.put(changeFile.getOriginalName(), changeFile.getName());
        }
      }
    }

    modifyFiles.putAll(addFiles);
    for (Map.Entry<String, String> changeFile : modifyFiles.entrySet()) {
      File origin = new File(changeFile.getKey());
      if (origin.exists() && origin.isFile()) {
        File destFile = new File(incrementFolder, changeFile.getValue());
        try {
          FileUtils.copyFile(origin, destFile);
        } catch (IOException e) {
          logWarn(relResult,
              "copy file failed , src : " + changeFile.getKey() + " , dest :" + destFile.getAbsolutePath());
        }
      } else {
        logWarn(relResult, "modify file not exists or not file , path : " + changeFile.getKey());
      }
    }

    if (delFiles.size() > 0) {
      File delLog = new File(incrementFolder, "delete_files.log");
      boolean noNeedClear = true;
      if (delLog.exists()) {
        noNeedClear = false;
      }
      for (Map.Entry<String, String> changeFile : delFiles.entrySet()) {
        try {
          FileUtils.writeStringToFile(delLog, changeFile.getValue() + "\n", noNeedClear);
          noNeedClear = true;
        } catch (IOException e) {
          logWarn(relResult, "write delete file log failed, path : " + changeFile.getValue());
        }
      }
    }

    zipIncrementFolder(incrementFolder);
  }

  /**
   * 压缩增量文件夹
   * 
   * @param incrementFolder
   */
  private void zipIncrementFolder(File incrementFolder) {
    ZipArchiver zipArch = new ZipArchiver();
    File zipFile = new File(incrementFolder.getParent(), incrementFolder.getName() + ".zip");
    zipArch.setDestFile(zipFile);
    try {
      zipArch.addDirectory(incrementFolder);
      zipArch.createArchive();
    } catch (ArchiverException e) {
      getLogger().warn(" add zip folder failed, " + e.getMessage());
    } catch (IOException e) {
      getLogger().warn("create zip file failed, " + e.getMessage());
    }
  }

  /**
   * 拷贝修改过的war 包
   * 
   * @param mp
   */
  private List<ChangeFile> copyModifyLib(MavenProject mp) {

    Set<Artifact> allDeps = new HashSet<>();
    findDeps(mp, allDeps);
    Map<String, Artifact> depsMap = new HashMap<>();
    for (Artifact dependency : allDeps) {
      if ("compile".equalsIgnoreCase(dependency.getScope()) || "system".equalsIgnoreCase(dependency.getScope())) {
        depsMap.put(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion(),
            dependency);
      }
    }

    Set<String> lastDepKey = new HashSet<>();
    Set<String> newDepKey = new HashSet<>();
    // load last dependencies
    lastDepKey.addAll(readLastReleaseLibLog(mp));
    List<String> deleteDepKey = new ArrayList<>();

    for (String depkey : lastDepKey) {// 对比两次版本间差异
      if (depsMap.remove(depkey) == null) {
        deleteDepKey.add(depkey);
      } else {
        // write log
        newDepKey.add(depkey);
      }
    }

    List<ChangeFile> changedLib = new ArrayList<>();
    for (String key : deleteDepKey) {
      String[] depinfos = key.split(":");
      String cfname = WEBLIB + depinfos[1] + "-" + depinfos[2] + ".jar";
      ChangeFile cf = new ChangeFile(cfname);
      cf.setAction(ScmFileStatus.DELETED);
      changedLib.add(cf);
    }

    for (Artifact dependency : depsMap.values()) {
      File depFile = dependency.getFile();
      if (depFile == null) {
        depFile = new File(localRepository.getBasedir(), localRepository.pathOf(dependency));
      }

      if (depFile == null || !depFile.exists()) {
        getLogger().warn("artifact not exist ! " + dependency.getArtifactId() + "：" + dependency.getVersion());
        continue;
      }
      String dpname = depFile.getName();
      String cfname = WEBLIB + dpname;
      ChangeFile cf = new ChangeFile(cfname);
      cf.setAction(ScmFileStatus.ADDED);
      cf.setOriginalName(depFile.getAbsolutePath());
      changedLib.add(cf);

      String delcfname = WEBLIB + dependency.getArtifactId() + "-*.jar";
      ChangeFile delcf = new ChangeFile(delcfname);
      delcf.setAction(ScmFileStatus.DELETED);
      delcf.setOriginalName(depFile.getAbsolutePath() + "-deleted");
      changedLib.add(delcf);
    }

    newDepKey.addAll(depsMap.keySet());
    writeNewreleaseLibLog(mp, newDepKey);

    return changedLib;

  }

  /**
   * @param mp
   * @param newDepKey
   */
  private void writeNewreleaseLibLog(MavenProject project, Set<String> newDepKey) {
    File root = project.getFile();
    File releaseLog = new File(root.getParent(), "lastreleaselib.log");
    try {
      FileUtils.writeLines(releaseLog, "utf-8", newDepKey);
    } catch (IOException e) {
      getLogger().warn("can write last release lib Log from " + releaseLog.getAbsolutePath());
    }

  }

  protected List<String> readLastReleaseLibLog(MavenProject project) {
    File root = project.getFile();
    File releaseLog = new File(root.getParent(), "lastreleaselib.log");
    List<String> lastLibs = null;
    if (releaseLog.exists() && releaseLog.canRead()) {
      try {
        lastLibs = FileUtils.readLines(releaseLog, "utf-8");
      } catch (IOException e) {
        getLogger().warn("can read last release lib Log from " + releaseLog.getAbsolutePath());
      }
    }

    return lastLibs == null ? Collections.<String>emptyList() : lastLibs;
  }

  private void findDeps(MavenProject mp, Set<Artifact> allDeps) {
    try {
      ArtifactFilter artifactFilter = new ScopeArtifactFilter(null);

      DependencyNode rootNode = dependencyTreeBuilder.buildDependencyTree(mp, localRepository, artifactFactory,
          artifactMetadataSource, artifactFilter, artifactCollector);

      CollectingDependencyNodeVisitor visitor = new CollectingDependencyNodeVisitor();
      rootNode.accept(visitor);
      List<DependencyNode> nodes = visitor.getNodes();
      for (DependencyNode dependencyNode : nodes) {
        int state = dependencyNode.getState();
        Artifact artifact = dependencyNode.getArtifact();
        if (state == DependencyNode.INCLUDED) {
          allDeps.add(artifact);
        }
      }
    } catch (DependencyTreeBuilderException e) {
      getLogger().warn("cann't parse tree dependencies of  " + mp.toString() + ", " + e.getMessage());
    }
  }

  /**
   * @param cf
   * @param parent
   */
  private boolean findOrigin(ChangeFile cf, MavenProject mp, List<ChangeFile> changeFiles) {
    String svnFileName = cf.getName().replace("\\", File.separator).replace("/", File.separator);
    String baseDir = mp.getBasedir().getName().replace("\\", File.separator).replace("/", File.separator);
    File originFile = null;
    String fileName = svnFileName.substring(svnFileName.lastIndexOf(baseDir));
    String soursePath = mp.getBuild().getSourceDirectory().replace("\\", File.separator).replace("/", File.separator);
    String outputPath = mp.getBuild().getOutputDirectory().replace("\\", File.separator).replace("/", File.separator);
    String sourceRelativePath = soursePath.substring(soursePath.lastIndexOf(baseDir));

    String pfileName = fileName;
    if (pfileName.startsWith(sourceRelativePath)) {
      // source file
      pfileName = pfileName.replace(sourceRelativePath, "").replace(".java", ".class");
      originFile = new File(outputPath, pfileName);
      pfileName = WEBCLASSES + pfileName;

      final String innerClassName = originFile.getName().replace(".class", "") + "$";
      // find inner class
      if (originFile.exists()) {
        for (File innerClass : originFile.getParentFile().listFiles(new FileFilter() {
          @Override
          public boolean accept(File pathname) {
            return pathname.isFile() && pathname.getName().startsWith(innerClassName);
          }
        })) {
          ChangeFile innercf = new ChangeFile(new File(pfileName).getParent() + File.separator + innerClass.getName());
          innercf.setAction(ScmFileStatus.ADDED);
          innercf.setOriginalName(innerClass.getAbsolutePath());
          changeFiles.add(innercf);
        }
      }

    } else {
      @SuppressWarnings("unchecked")
      List<Resource> resoursePaths = mp.getResources();
      for (Resource resource : resoursePaths) {
        String resourcePath = resource.getDirectory().replace("\\", File.separator).replace("/", File.separator);
        String resourceRelativePath = resourcePath.substring(resourcePath.lastIndexOf(baseDir));
        if (pfileName.startsWith(resourceRelativePath)) {
          pfileName = pfileName.replace(resourceRelativePath, "");
          originFile = new File(outputPath, pfileName);
          pfileName = WEBCLASSES + pfileName;
          break;
        }
      }
    }

    if (originFile == null) {
      String webSourcePath = mp.getBasedir().getName() + WEBROOT;// add
      if (pfileName.contains(webSourcePath)) {
        // setting
        pfileName = pfileName.replace(webSourcePath, "");
        originFile = new File(mp.getBasedir().getParentFile(), fileName);
      } else {
        getLogger().info("igone not web integrant file " + fileName);
        return false;
      }
    }
    cf.setName(pfileName);
    cf.setOriginalName(originFile.getAbsolutePath());
    return true;
  }

  private Date parseVersion(ScmVersion version, String versionStr) {
    Date date = null;
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    try {
      date = df.parse(versionStr);
    } catch (ParseException e) {
      df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      try {
        date = df.parse(versionStr);
      } catch (ParseException e1) {
        version.setName(versionStr);
      }
    }
    return date;
  }

  private ChangeLogScmResult getLogResult(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
      List<MavenProject> reactorProjects) throws ReleaseExecutionException, ReleaseScmRepositoryException {
    ReleaseDescriptor basedirAlignedReleaseDescriptor = ReleaseUtil
        .createBasedirAlignedReleaseDescriptor(releaseDescriptor, reactorProjects);

    ScmRepository repository;
    ScmProvider provider;
    try {
      repository = scmRepositoryConfigurator.getConfiguredRepository(basedirAlignedReleaseDescriptor.getScmSourceUrl(),
          releaseDescriptor, releaseEnvironment.getSettings());

      provider = scmRepositoryConfigurator.getRepositoryProvider(repository);
    } catch (ScmRepositoryException e) {
      throw new ReleaseScmRepositoryException(e.getMessage(), e.getValidationMessages());
    } catch (NoSuchScmProviderException e) {
      throw new ReleaseExecutionException("Unable to configure SCM repository: " + e.getMessage(), e);
    }

    ChangeLogScmResult result;
    try {
      ScmFileSet fileSet = new ScmFileSet(new File(basedirAlignedReleaseDescriptor.getWorkingDirectory()));
      ScmVersion startVersion = new AbstractScmVersion("HEAD") {
        private static final long serialVersionUID = 6513522962534864765L;

        @Override
        public String getType() {
          return "HEAD";
        }
      };

      MergeReleaseDescriptor rd = (MergeReleaseDescriptor) releaseDescriptor;
      startVersion.setName(rd.getStartReversion());

      ChangeLogScmRequest scmRequest = new ChangeLogScmRequest(repository, fileSet);
      Date startDate = parseVersion(startVersion, rd.getStartReversion());
      if (startDate != null) {
        scmRequest.setStartDate(startDate);
      } else {
        scmRequest.setStartRevision(startVersion);
      }

      ScmVersion endReversion = new AbstractScmVersion("HEAD") {
        private static final long serialVersionUID = 6513522962534864765L;

        @Override
        public String getType() {
          return "HEAD";
        }
      };

      Date endDate = parseVersion(endReversion, rd.getEndReversion());
      if (endDate != null) {
        scmRequest.setEndDate(endDate);
      } else if (!"HEAD".equals(endReversion.getName())) {
        scmRequest.setEndRevision(endReversion);
      }

      if (getLogger().isDebugEnabled()) {
        getLogger().debug("ScmLogPhase :: scmLogParameters scmStartRevision" + rd.getStartReversion());
        getLogger().debug("ScmLogPhase :: scmLogParameters scmEndRevision HEAD");
        getLogger().debug("ScmTagPhase :: fileSet  " + fileSet);
      }

      result = provider.changeLog(scmRequest);
    } catch (ScmException e) {
      throw new ReleaseExecutionException("An error is occurred in the tag process: " + e.getMessage(), e);
    }

    return result;
  }

  @Override
  public ReleaseResult simulate(ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment,
      List<MavenProject> reactorProjects) throws ReleaseExecutionException, ReleaseFailureException {
    ReleaseResult result = new ReleaseResult();

    ReleaseDescriptor basedirAlignedReleaseDescriptor = ReleaseUtil
        .createBasedirAlignedReleaseDescriptor(releaseDescriptor, reactorProjects);

    logInfo(result, "Full run would be log " + basedirAlignedReleaseDescriptor.getScmSourceUrl());

    ChangeLogScmResult logResult = getLogResult(releaseDescriptor, releaseEnvironment, reactorProjects);
    if (!logResult.isSuccess()) {
      throw new ReleaseScmCommandException("Unable to log SCM", logResult);
    } else {
      logInfo(result, "finished log ...");
    }

    result.setResultCode(ReleaseResult.SUCCESS);

    return result;
  }

  public static Artifact getArtifact(MavenProject project, Dependency dependency) {
    for (Object o : project.getArtifacts()) {
      Artifact artifact = (Artifact) o;
      if (artifact.getGroupId().equals(dependency.getGroupId())
          && artifact.getArtifactId().equals(dependency.getArtifactId())
          && artifact.getType().equals(dependency.getType())) {
        if (artifact.getClassifier() == null && dependency.getClassifier() == null) {
          return artifact;
        } else if (dependency.getClassifier() != null && dependency.getClassifier().equals(artifact.getClassifier())) {
          return artifact;
        }
      }
    }
    return null;
  }

}
