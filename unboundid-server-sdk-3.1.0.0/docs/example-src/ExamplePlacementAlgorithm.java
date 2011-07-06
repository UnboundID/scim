/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * docs/licenses/cddl.txt
 * or http://www.opensource.org/licenses/cddl1.php.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * docs/licenses/cddl.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2010-2011 UnboundID Corp.
 */
package com.unboundid.directory.sdk.examples;



import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.unboundid.directory.sdk.common.operation.AddRequest;
import com.unboundid.directory.sdk.common.types.OperationContext;
import com.unboundid.directory.sdk.proxy.api.PlacementAlgorithm;
import com.unboundid.directory.sdk.proxy.config.PlacementAlgorithmConfig;
import com.unboundid.directory.sdk.proxy.types.BackendSet;
import com.unboundid.directory.sdk.proxy.types.ProxyServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a placement algorithm which sends
 * entries to different backend sets based on a hash generated from the value of
 * a specified attribute.  If an entry has multiple values for the specified
 * attribute, then only the first value will be used.  If an entry does not
 * have the specified attribute, then a simple round-robin distribution will be
 * used.
 * <BR><BR>
 * It takes the following configuration arguments:
 * <UL>
 *   <LI>attribute-name -- The name of the attribute from which to generate the
 *       hash.</LI>
 *   <LI>max-characters -- The maximum number of characters to use from the
 *       value when generating the hash.  A value of zero indicates that all
 *       characters in the value should be used.</LI>
 * </UL>
 */
public final class ExamplePlacementAlgorithm
       extends PlacementAlgorithm
{
  /**
   * The name of the argument that will be used to specify the name of the
   * attribute for which to generate the hash.
   */
  private static final String ARG_NAME_ATTR = "attribute-name";



  /**
   * The name of the argument that will be used to specify the maximum number of
   * characters to use to generate the hash.
   */
  private static final String ARG_NAME_MAX_CHARS = "max-characters";



  // A counter that will be used to select backend sets for entries that don't
  // contain the target attribute.
  private final AtomicLong roundRobinCounter;

  // The maximum number of characters to use in the hash.
  private volatile int maxChars;

  // The list of backend sets currently in use.
  private volatile List<BackendSet> backendSets;

  // The server context for the server in which this extension is running.
  private ProxyServerContext serverContext;

  // The name of the attribute to use to generate the hash.
  private volatile String attributeName;



  /**
   * Creates a new instance of this placement algorithm.  All placement
   * algorithm implementations must include a default constructor, but any
   * initialization should generally be done in the
   * {@code initializePlacementAlgorithm} method.
   */
  public ExamplePlacementAlgorithm()
  {
    roundRobinCounter = new AtomicLong(0L);
  }



  /**
   * Retrieves a human-readable name for this extension.
   *
   * @return  A human-readable name for this extension.
   */
  @Override()
  public String getExtensionName()
  {
    return "Example Placement Algorithm";
  }



  /**
   * Retrieves a human-readable description for this extension.  Each element
   * of the array that is returned will be considered a separate paragraph in
   * generated documentation.
   *
   * @return  A human-readable description for this extension, or {@code null}
   *          or an empty array if no description should be available.
   */
  @Override()
  public String[] getExtensionDescription()
  {
    return new String[]
    {
      "This placement algorithm serves an example that may be used to " +
           "demonstrate the process for creating a third-party placement " +
           "algorithm.  It will generate a hash from the first value of a " +
           "specified attribute and will use that to select which backend " +
           "set should be used to hold the associated entry."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this placement algorithm.  The argument parser may
   * also be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this placement algorithm.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the attribute name.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_ATTR;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{attr}";
    String    description     = "The name of the attribute whose value " +
         "be used to generate a hash for use when selecting the backend set.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));


    // Add an argument that allows you to specify the maximum number of
    // characters to use when generating the hash.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_MAX_CHARS;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{value}";
    description     = "The maximum number of characters to use when " +
         "generating the hash.  A value of zero indicates that the entire " +
         "value should be used.";

    int lowerBound   = 0;
    int upperBound   = Integer.MAX_VALUE;
    int defaultValue = 0;

    parser.addArgument(new IntegerArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, lowerBound,
         upperBound, defaultValue));
  }



  /**
   * Initializes this placement algorithm.
   *
   * @param  serverContext    A handle to the server context for the server in
   *                          which this extension is running.
   * @param  config           The general configuration for this placement
   *                          algorithm.
   * @param  parser           The argument parser which has been initialized
   *                          from the configuration for this placement
   *                          algorithm.
   * @param  balancingBaseDN  The balancing base DN for the associated
   *                          entry-balancing request processor.
   * @param  backendSets      The list of backend sets that will be used with
   *                          the entry-balancing request processor.
   *
   * @throws  LDAPException  If a problem occurs while initializing this
   *                         placement algorithm.
   */
  @Override()
  public void initializePlacementAlgorithm(
                   final ProxyServerContext serverContext,
                   final PlacementAlgorithmConfig config,
                   final ArgumentParser parser,
                   final String balancingBaseDN,
                   final List<BackendSet> backendSets)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning placement algorithm initialization");

    this.serverContext = serverContext;
    this.backendSets   = backendSets;


    // Get the target attribute name.
    final StringArgument attrArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_ATTR);
    attributeName = attrArg.getValue();


    // Get the maximum number of characters to use from the value.
    final IntegerArgument maxCharsArg =
         (IntegerArgument) parser.getNamedArgument(ARG_NAME_MAX_CHARS);
    maxChars = maxCharsArg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this placement
   *                              algorithm.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(
                      final PlacementAlgorithmConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    // No special validation is required.
    return true;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this placement
   *                               algorithm.
   * @param  parser                The argument parser which has been
   *                               initialized with the new configuration.
   * @param  adminActionsRequired  A list that can be updated with information
   *                               about any administrative actions that may be
   *                               required before one or more of the
   *                               configuration changes will be applied.
   * @param  messages              A list that can be updated with information
   *                               about the result of applying the new
   *                               configuration.
   *
   * @return  A result code that provides information about the result of
   *          attempting to apply the configuration change.
   */
  @Override()
  public ResultCode applyConfiguration(final PlacementAlgorithmConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the new target attribute name.
    final StringArgument attrArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_ATTR);
    final String newAttr = attrArg.getValue();


    // Get the new maximum number of characters to use from the value.
    final IntegerArgument maxCharsArg =
         (IntegerArgument) parser.getNamedArgument(ARG_NAME_MAX_CHARS);
    final int newMax = maxCharsArg.getValue();


    attributeName = newAttr;
    maxChars      = newMax;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this placement algorithm
   * is to be taken out of service.
   */
  @Override()
  public void finalizePlacementAlgorithm()
  {
    // No finalization is required.
  }



  /**
   * Adapts to a change in the backend sets configured for use with the
   * associated entry-balancing request processor.
   *
   * @param  balancingBaseDN  The updated balancing base DN for the associated
   *                          entry-balancing request processor.
   * @param  backendSets      The updated list of backend sets for the
   *                          associated entry-balancing request processor.
   */
  @Override()
  public void applyBalancingConfigurationChange(final String balancingBaseDN,
                   final List<BackendSet> backendSets)
  {
    this.backendSets = backendSets;
  }



  /**
   * Determines the backend set that should be used to process the specified
   * add operation.
   *
   * @param  operationContext  The operation context for the add operation
   *                           to be processed.
   * @param  addRequest        The add request being processed.
   *
   * @return  The backend set in which the add should be processed, or
   *          {@code null} if there is no appropriate backend set.
   */
  @Override()
  public BackendSet selectBackendSet(final OperationContext operationContext,
                                     final AddRequest addRequest)
  {
    // Create a local copy of the configuration in case it changes while this
    // method is running.
    final int              max  = maxChars;
    final List<BackendSet> sets = backendSets;
    final String           attr = attributeName;


    // Get the target attribute from the entry to be added.  Find the first
    // value and use it to make the determination.
    final List<Attribute> attrList = addRequest.getEntry().getAttribute(attr);
    if (attrList != null)
    {
      for (final Attribute a : attrList)
      {
        final String value = a.getValue();
        if (value != null)
        {
          final String s;
          if ((max == 0) || (max <= value.length()))
          {
            s = value;
          }
          else
          {
            s = value.substring(0, max);
          }

          final int slot = (s.hashCode() % sets.size());
          return sets.get(slot);
        }
      }
    }


    // If we've gotten here, then the entry doesn't have a value for the
    // specified attribute, so we should use the round-robin counter.
    final int slot =
         (int) (roundRobinCounter.getAndIncrement() % sets.size());
    return sets.get(slot);
  }



  /**
   * Retrieves a map containing examples of configurations that may be used for
   * this extension.  The map key should be a list of sample arguments, and the
   * corresponding value should be a description of the behavior that will be
   * exhibited by the extension when used with that configuration.
   *
   * @return  A map containing examples of configurations that may be used for
   *          this extension.  It may be {@code null} or empty if there should
   *          not be any example argument sets.
   */
  @Override()
  public Map<List<String>,String> getExamplesArgumentSets()
  {
    final LinkedHashMap<List<String>,String> exampleMap =
         new LinkedHashMap<List<String>,String>(1);

    exampleMap.put(
         Arrays.asList(
              ARG_NAME_ATTR + "=cn",
              ARG_NAME_MAX_CHARS + "=10"),
         "Select the backend set to use for add operations based on a hash " +
              "of up to the first 10 characters of the first value of the " +
              "cn attribute.");

    return exampleMap;
  }



  /**
   * Appends a string representation of this LDAP health check to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which the string representation should be
   *                 appended.
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("ExamplePlacementAlgorithm(attributeName='");
    buffer.append(attributeName);
    buffer.append("', maxChars=");
    buffer.append(maxChars);
    buffer.append(')');
  }
}