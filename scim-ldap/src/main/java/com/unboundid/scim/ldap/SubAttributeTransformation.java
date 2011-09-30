/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

/**
 * A class representing a transformation of a SCIM sub-attribute to a
 * specified LDAP attribute.
 */
public class SubAttributeTransformation
{
  private final String subAttribute;
  private final AttributeTransformation attributeTransformation;



  /**
   * Create a new sub-attribute transformation.
   *
   * @param subAttribute  The name of the SCIM sub-attribute that is the
   *                      subject of the transformation.
   * @param at            The attribute transformation to be applied.
   */
  public SubAttributeTransformation(final String subAttribute,
                                    final AttributeTransformation at)
  {
    this.subAttribute = subAttribute;
    this.attributeTransformation = at;
  }



  /**
   * Create a new sub-attribute transformation from the JAXB type that defines
   * a sub-attribute.
   *
   * @param definition  The JAXB type that defines a sub-attribute.
   *
   * @return  The new attribute transformation.
   */
  public static SubAttributeTransformation create(
      final SubAttributeDefinition definition)
  {
    final String subAttribute = definition.getName();
    final AttributeTransformation at =
        AttributeTransformation.create(definition.getMapping());

    return new SubAttributeTransformation(subAttribute, at);
  }



  /**
   * Create a new sub-attribute transformation from the JAXB type that
   * represents a sub-attribute mapping.
   *
   * @param mapping  The JAXB type that represents a sub-attribute mapping.
   *
   * @return  The new attribute transformation.
   */
  public static SubAttributeTransformation create(
      final SubAttributeMapping mapping)
  {
    final String subAttribute = mapping.getName();
    final String ldapAttribute = mapping.getLdapAttribute();
    final Transformation transformation =
        Transformation.create(mapping.getTransform());

    final AttributeTransformation at =
        new AttributeTransformation(ldapAttribute, transformation);
    return new SubAttributeTransformation(subAttribute, at);
  }



  /**
   * Retrieve the name of the SCIM sub-attribute that is the subject of the
   * transformation.
   *
   * @return  The name of the SCIM sub-attribute that is the subject of the
   *          transformation.
   */
  public String getSubAttribute()
  {
    return subAttribute;
  }



  /**
   * Retrieve the attribute transformation to be applied.
   *
   * @return  The attribute transformation to be applied.
   */
  public AttributeTransformation getAttributeTransformation()
  {
    return attributeTransformation;
  }
}
