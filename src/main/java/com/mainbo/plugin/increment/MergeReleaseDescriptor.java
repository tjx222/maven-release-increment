/**
 * Mainbo.com Inc.
 * Copyright (c) 2015-2017 All Rights Reserved.
 */

package com.mainbo.plugin.increment;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * <pre>
 * 合并配置项
 * </pre>
 *
 * @author tmser
 * @version $Id: Snippet.java, v 1.0 2018年1月2日 下午5:35:41 tmser Exp $
 */
public class MergeReleaseDescriptor extends ReleaseDescriptor {

  /**
   * <pre>
   *
   * </pre>
   */
  private static final long serialVersionUID = 5010069106692876966L;

  private String startReversion;

  private String endReversion;

  private String incrementGoals;

  private boolean ignoreUpdate;

  private ArtifactRepository localRepository;

  /**
   * Getter method for property <tt>startReversion</tt>.
   *
   * @return startReversion String
   */
  public String getStartReversion() {
    return startReversion;
  }

  /**
   * Setter method for property <tt>startReversion</tt>.
   *
   * @param startReversion
   *          String value to be assigned to property startReversion
   */
  public void setStartReversion(String startReversion) {
    this.startReversion = startReversion;
  }

  /**
   * Getter method for property <tt>endReversion</tt>.
   *
   * @return endReversion String
   */
  public String getEndReversion() {
    return endReversion;
  }

  /**
   * Setter method for property <tt>endReversion</tt>.
   *
   * @param endReversion
   *          String value to be assigned to property endReversion
   */
  public void setEndReversion(String endReversion) {
    this.endReversion = endReversion;
  }

  /**
   * Getter method for property <tt>incrementGoals</tt>.
   *
   * @return incrementGoals String
   */
  public String getIncrementGoals() {
    return incrementGoals;
  }

  /**
   * Setter method for property <tt>incrementGoals</tt>.
   *
   * @param incrementGoals
   *          String value to be assigned to property incrementGoals
   */
  public void setIncrementGoals(String incrementGoals) {
    this.incrementGoals = incrementGoals;
  }

  /**
   * Getter method for property <tt>localRepository</tt>.
   *
   * @return localRepository ArtifactRepository
   */
  public ArtifactRepository getLocalRepository() {
    return localRepository;
  }

  /**
   * Setter method for property <tt>localRepository</tt>.
   *
   * @param localRepository
   *          ArtifactRepository value to be assigned to property
   *          localRepository
   */
  public void setLocalRepository(ArtifactRepository localRepository) {
    this.localRepository = localRepository;
  }

  /**
   * Getter method for property <tt>igoreUpdate</tt>.
   *
   * @return igoreUpdate Boolean
   */
  public Boolean getIgnoreUpdate() {
    return ignoreUpdate;
  }

  /**
   * Setter method for property <tt>igoreUpdate</tt>.
   *
   * @param igoreUpdate
   *          Boolean value to be assigned to property igoreUpdate
   */
  public void setIgnoreUpdate(boolean ignoreUpdate) {
    this.ignoreUpdate = ignoreUpdate;
  }

}
