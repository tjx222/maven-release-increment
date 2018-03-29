package com.tmser.plugin.increment;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.shared.release.config.ReleaseDescriptor;

/**
 * Abstract Mojo containing SCM parameters
 * 
 * @author Robert Scholte
 */
// Extra layer since 2.4. Don't use @since doclet, these would be inherited by
// the subclasses
public abstract class AbstractScmReleaseMojo extends AbstractReleaseMojo {
  /**
   * The SCM username to use.
   */
  @Parameter(property = "username")
  private String username;

  /**
   * The SCM password to use.
   */
  @Parameter(property = "password")
  private String password;

  /**
   * Add a new or overwrite the default implementation per provider.
   * The key is the scm prefix and the value is the role hint of the
   * {@link org.apache.maven.scm.provider.ScmProvider}.
   *
   * @since 2.0-beta-6
   * @see ScmManager#setScmProviderImplementation(String, String)
   */
  @Parameter
  private Map<String, String> providerImplementations;

  /**
   * The SCM manager.
   */
  @Component
  private ScmManager scmManager;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (providerImplementations != null) {
      for (Map.Entry<String, String> providerEntry : providerImplementations.entrySet()) {
        getLog().info("Change the default '" + providerEntry.getKey() + "' provider implementation to '"
            + providerEntry.getValue() + "'.");
        scmManager.setScmProviderImplementation(providerEntry.getKey(), providerEntry.getValue());
      }
    }
  }

  @Override
  protected ReleaseDescriptor createReleaseDescriptor() {
    ReleaseDescriptor descriptor = super.createReleaseDescriptor();

    descriptor.setScmPassword(password);
    descriptor.setScmUsername(username);
    return descriptor;
  }
}
