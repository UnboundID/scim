/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;



/**
 * This class provides a simple data structure with information that is
 * used to control the behavior of an {@link LDAPExternalServer} instance.
 */
public class LDAPExternalServerConfig
{
  /**
   * The host name of the LDAP Directory Server instance.
   */
  private String dsHost = "localhost";

  /**
   * The port number of the LDAP Directory Server instance.
   */
  private int dsPort = -1;

  /**
   * The base DN for the LDAP Directory Server.
   */
  private String dsBaseDN = "dc=example,dc=com";

  /**
   * The bind DN for the LDAP Directory Server.
   */
  private String dsBindDN = "cn=Directory Manager";

  /**
   * The bind password for the LDAP Directory Server.
   */
  private String dsBindPassword = "password";

  /**
   * The total number of connections that should be established to the LDAP
   * Directory Server.
   */
  private int numConnections = 1;



  /**
   * Create a new LDAP external server config with default information.
   */
  public LDAPExternalServerConfig()
  {
  }



  /**
   * Retrieve the the host name of the LDAP Directory Server instance.
   *
   * @return  The the host name of the LDAP Directory Server instance.
   */
  public String getDsHost()
  {
    return dsHost;
  }



  /**
   * Specifies the host name of the LDAP Directory Server instance.
   *
   * @param dsHost  The host name of the LDAP Directory Server instance.
   */
  public void setDsHost(final String dsHost)
  {
    this.dsHost = dsHost;
  }



  /**
   * Retrieve the port number of the LDAP Directory Server instance.
   *
   * @return  The port number of the LDAP Directory Server instance.
   */
  public int getDsPort()
  {
    return dsPort;
  }



  /**
   * Specifies the port number of the LDAP Directory Server instance.
   *
   * @param dsPort  The port number of the LDAP Directory Server instance.
   */
  public void setDsPort(final int dsPort)
  {
    this.dsPort = dsPort;
  }



  /**
   * Retrieve the base DN for the LDAP Directory Server.
   *
   * @return  The base DN for the LDAP Directory Server.
   */
  public String getDsBaseDN()
  {
    return dsBaseDN;
  }



  /**
   * Specifies the base DN for the LDAP Directory Server.
   *
   * @param dsBaseDN  The base DN for the LDAP Directory Server.
   */
  public void setDsBaseDN(final String dsBaseDN)
  {
    this.dsBaseDN = dsBaseDN;
  }



  /**
   * Retrieve the bind DN for the LDAP Directory Server.
   *
   * @return  The bind DN for the LDAP Directory Server.
   */
  public String getDsBindDN()
  {
    return dsBindDN;
  }



  /**
   * Specifies the bind DN for the LDAP Directory Server.
   *
   * @param dsBindDN  The bind DN for the LDAP Directory Server.
   */
  public void setDsBindDN(final String dsBindDN)
  {
    this.dsBindDN = dsBindDN;
  }



  /**
   * Retrieve the bind password for the LDAP Directory Server.
   *
   * @return  The bind password for the LDAP Directory Server.
   */
  public String getDsBindPassword()
  {
    return dsBindPassword;
  }



  /**
   * Specifies the bind password for the LDAP Directory Server.
   *
   * @param dsBindPassword  The bind password for the LDAP Directory Server.
   */
  public void setDsBindPassword(final String dsBindPassword)
  {
    this.dsBindPassword = dsBindPassword;
  }



  /**
   * Retrieve the total number of connections that should be established to the
   * LDAP Directory Server.
   *
   * @return  The total number of connections that should be established to the
   *          LDAP Directory Server.
   */
  public int getNumConnections()
  {
    return numConnections;
  }



  /**
   * Specifies the total number of connections that should be established to the
   * LDAP Directory Server.
   *
   * @param numConnections  The total number of connections that should be
   *                        established to the LDAP Directory Server.
   */
  public void setNumConnections(final int numConnections)
  {
    this.numConnections = numConnections;
  }
}
