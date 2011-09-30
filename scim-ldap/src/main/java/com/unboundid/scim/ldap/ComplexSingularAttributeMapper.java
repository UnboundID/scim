/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SimpleValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * This class provides an attribute mapper that maps a singular complex SCIM
 * attribute to several single-valued LDAP attributes, where each LDAP
 * attribute holds one of the SCIM sub-attributes.
 */
public class ComplexSingularAttributeMapper extends AttributeMapper
{
  /**
   * The set of sub-attribute transformations indexed by the name of the
   * sub-attribute.
   */
  private final Map<String, SubAttributeTransformation> map;

  /**
   * The set of LDAP attributes that are mapped by this attribute mapper.
   */
  private final Set<String> ldapAttributeTypes;



  /**
   * Create a new instance of a complex singular attribute mapper.
   *
   * @param scimAttributeType  The SCIM attribute type that is mapped by this
   *                           attribute mapper.
   * @param transformations    The set of sub-attribute transformations for
   *                           this attribute mapper.
   */
  public ComplexSingularAttributeMapper(
      final SCIMAttributeType scimAttributeType,
      final List<SubAttributeTransformation> transformations)
  {
    super(scimAttributeType);

    map = new HashMap<String, SubAttributeTransformation>();
    ldapAttributeTypes = new HashSet<String>();
    for (final SubAttributeTransformation t : transformations)
    {
      map.put(t.getSubAttribute(), t);
      ldapAttributeTypes.add(t.getAttributeTransformation().getLdapAttribute());
    }
  }



  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return ldapAttributeTypes;
  }



  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
  {
    final String subAttributeName =
        filter.getFilterAttribute().getSubAttributeName();
    if (subAttributeName == null)
    {
      return Filter.createORFilter();
    }

    final SubAttributeTransformation subAttributeTransformation =
        map.get(subAttributeName);
    if (subAttributeTransformation == null)
    {
      return Filter.createORFilter();
    }

    final AttributeTransformation attributeTransformation =
        subAttributeTransformation.getAttributeTransformation();
    final String ldapAttributeType = attributeTransformation.getLdapAttribute();

    final String ldapFilterValue;
    if (filter.getFilterValue() != null)
    {
      final Transformation t = attributeTransformation.getTransformation();
      ldapFilterValue = t.toLDAPFilterValue(filter.getFilterValue());
    }
    else
    {
      ldapFilterValue = null;
    }

    final SCIMFilterType filterType = filter.getFilterType();
    switch (filterType)
    {
      case EQUALITY:
      {
        return Filter.createEqualityFilter(ldapAttributeType,
                                           ldapFilterValue);
      }

      case CONTAINS:
      {
        return Filter.createSubstringFilter(ldapAttributeType,
                                            null,
                                            new String[] { ldapFilterValue },
                                            null);
      }

      case STARTS_WITH:
      {
        return Filter.createSubstringFilter(ldapAttributeType,
                                            ldapFilterValue,
                                            null,
                                            null);
      }

      case PRESENCE:
      {
        return Filter.createPresenceFilter(ldapAttributeType);
      }

      case GREATER_THAN:
      case GREATER_OR_EQUAL:
      {
        return Filter.createGreaterOrEqualFilter(ldapAttributeType,
                                                 ldapFilterValue);
      }

      case LESS_THAN:
      case LESS_OR_EQUAL:
      {
        return Filter.createLessOrEqualFilter(ldapAttributeType,
                                              ldapFilterValue);
      }

      default:
        throw new RuntimeException(
            "Filter type " + filterType + " is not supported");
    }
  }



  @Override
  public String toLDAPSortAttributeType()
  {
    return null;
  }



  @Override
  public void toLDAPAttributes(final SCIMObject scimObject,
                               final Collection<Attribute> attributes)
  {
    final AttributeDescriptor descriptor =
        getSchema().getAttribute(getSCIMAttributeType().getName());

    final SCIMAttribute scimAttribute =
        scimObject.getAttribute(getSCIMAttributeType().getSchema(),
                                getSCIMAttributeType().getName());
    if (scimAttribute != null)
    {
      final SCIMAttributeValue value = scimAttribute.getSingularValue();

      for (final SubAttributeTransformation sat : map.values())
      {
        final AttributeTransformation at = sat.getAttributeTransformation();
        final String scimType = sat.getSubAttribute();
        final String ldapType = at.getLdapAttribute();

        final SCIMAttribute subAttribute = value.getAttribute(scimType);
        if (subAttribute != null)
        {
          final AttributeDescriptor subDescriptor =
              descriptor.getAttribute(scimType);
          final ASN1OctetString v = at.getTransformation().toLDAPValue(
              subDescriptor, subAttribute.getSingularValue().getValue());
          attributes.add(new Attribute(ldapType, v));
        }
      }
    }
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry)
  {
    final AttributeDescriptor descriptor =
        getSchema().getAttribute(getSCIMAttributeType().getName());

    final List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

    for (final SubAttributeTransformation sat : map.values())
    {
      final String subAttributeName = sat.getSubAttribute();
      final AttributeTransformation at = sat.getAttributeTransformation();

      final AttributeDescriptor subDescriptor =
          descriptor.getAttribute(subAttributeName);
      final Attribute a = entry.getAttribute(at.getLdapAttribute());
      if (a != null)
      {
        final ASN1OctetString[] rawValues = a.getRawValues();
        if (rawValues.length > 0)
        {
          final SimpleValue simpleValue =
              at.getTransformation().toSCIMValue(subDescriptor, rawValues[0]);
          subAttributes.add(
              SCIMAttribute.createSingularAttribute(
                  subDescriptor, new SCIMAttributeValue(simpleValue)));
        }
      }
    }

    if (subAttributes.isEmpty())
    {
      return null;
    }

    final SCIMAttributeValue complexValue =
        SCIMAttributeValue.createComplexValue(subAttributes);
    return SCIMAttribute.createSingularAttribute(descriptor, complexValue);
  }
}
