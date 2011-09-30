/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.sdk.SimpleValue;
import com.unboundid.util.ByteString;



/**
 * A transformation for LDAP PostalAddress syntax. Any newlines in the SCIM
 * values are replaced with '$' separator characters in the LDAP
 * values and vice-versa.
 */
public class PostalAddressTransformation extends Transformation
{
  /**
   * {@inheritDoc}
   */
  @Override
  public SimpleValue toSCIMValue(final AttributeDescriptor descriptor,
                                 final ByteString byteString)
  {
    switch (descriptor.getDataType())
    {
      case STRING:
        final String value = transformToSCIM(byteString.stringValue());
        return new SimpleValue(value);

      case DATETIME:
      case BOOLEAN:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The postal address transformation can not be used on " +
            descriptor.getDataType() + " data");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ASN1OctetString toLDAPValue(final AttributeDescriptor descriptor,
                                     final SimpleValue simpleValue)
  {
    switch (descriptor.getDataType())
    {
      case STRING:
        final String value = transformToLDAP(simpleValue.getStringValue());
        return new ASN1OctetString(value);

      case DATETIME:
      case BOOLEAN:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The postal address transformation can not be used on " +
            descriptor.getDataType() + " data");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toLDAPFilterValue(final String scimFilterValue)
  {
    return transformToLDAP(scimFilterValue);
  }



  /**
   * From LDAP RFC 4517:
   * Each character string (i.e., <line>) of a postal address value is
   * encoded as a UTF-8 [RFC3629] string, except that "\" and "$"
   * characters, if they occur in the string, are escaped by a "\"
   * character followed by the two hexadecimal digit code for the
   * character.
   */

  /**
   * Transform a SCIM postal address to an LDAP postal address.
   *
   * @param s  The value to be transformed.
   * @return  The LDAP value.
   */
  private String transformToLDAP(final String s)
  {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < s.length(); i++)
    {
      final char c = s.charAt(i);
      switch (c)
      {
        case '\n':
          builder.append('$');
          break;

        case '\\':
          builder.append("\\5C");
          break;

        case '$':
          builder.append("\\24");
          break;

        default:
          builder.append(c);
          break;
      }
    }

    return builder.toString();
  }



  /**
   * Transform an LDAP postal address to a SCIM postal address.
   *
   * @param s  The value to be transformed.
   * @return  The SCIM value.
   */
  private String transformToSCIM(final String s)
  {
    final StringBuilder builder = new StringBuilder();

    int i = 0;
    while (i < s.length())
    {
      final char c = s.charAt(i);
      switch (c)
      {
        case '\\':
          if (i + 3 > s.length())
          {
            // Not valid but let it pass untouched.
            builder.append(c);
            i++;
          }
          else
          {
            final String hex = s.substring(i+1, i+3).toUpperCase();
            if (hex.equals("5C"))
            {
              builder.append('\\');
            }
            else if (hex.equals("24"))
            {
              builder.append('$');
            }
            else
            {
              // Not valid but let it pass untouched.
              builder.append(c);
              builder.append(hex);
            }
            i += 3;
          }
          break;

        case '$':
          builder.append("\n");
          i++;
          break;

        default:
          builder.append(c);
          i++;
          break;
      }
    }

    return builder.toString();
  }
}
