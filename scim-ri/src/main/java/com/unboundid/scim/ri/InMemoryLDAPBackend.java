/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.UpdatableLDAPRequest;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMRequest;


/**
 * This class provides an implementation of the SCIM server backend API that
 * uses an in-memory LDAP server as the resource storage repository.
 */
public class InMemoryLDAPBackend
  extends LDAPBackend
{
  /**
   * An in-memory LDAP server providing the resource storage repository.
   */
  private InMemoryDirectoryServer ldapServer;


  /**
   * Create a new in-memory LDAP backend.
   *
   * @param ldapServer  An in-memory LDAP server. The server will be shut down
   *                    by the backend when the backend is taken out of service.
   */
  public InMemoryLDAPBackend(final InMemoryDirectoryServer ldapServer)
  {
    super();
    this.ldapServer = ldapServer;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeBackend()
  {
    if (ldapServer != null)
    {
      ldapServer.shutDown(true);
      ldapServer = null;
    }
  }



  /**
   * Perform basic authentication using the provided information.
   *
   * @param userID   The user ID to be authenticated.
   * @param password The user password to be verified.
   *
   * @return {@code true} if the provided user ID and password are valid.
   */
  @Override
  public boolean authenticate(final String userID, final String password)
  {
    try
    {
      final BindRequest bindRequest =
          new PLAINBindRequest(getSASLAuthenticationID(userID), password);
      final BindResult bindResult = ldapServer.bind(bindRequest);
      return bindResult.getResultCode().equals(ResultCode.SUCCESS);
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      return false;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected LDAPInterface getLDAPInterface(final String userID)
      throws LDAPException
  {
    return ldapServer;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void addCommonControls(final SCIMRequest scimRequest,
                                   final UpdatableLDAPRequest ldapRequest)
  {
    ldapRequest.addControl(
        new ProxiedAuthorizationV2RequestControl(
            getSASLAuthenticationID(scimRequest.getAuthenticatedUserID())));
  }



}
