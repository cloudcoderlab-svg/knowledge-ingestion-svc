package com.kengine.ingestion.config;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Hibernate UserType for PostgreSQL pgvector type. Handles conversion between String (Java) and
 * vector (PostgreSQL).
 *
 * <p>Usage:
 *
 * <pre>
 * &#64;Column(name = "embedding")
 * &#64;Type(VectorType.class)
 * private String embedding;
 * </pre>
 */
public class VectorType implements UserType<String> {

  @Override
  public int getSqlType() {
    return Types.OTHER; // pgvector is a custom PostgreSQL type
  }

  @Override
  public Class<String> returnedClass() {
    return String.class;
  }

  @Override
  public boolean equals(String x, String y) {
    return (x == null) ? (y == null) : x.equals(y);
  }

  @Override
  public int hashCode(String x) {
    return x == null ? 0 : x.hashCode();
  }

  @Override
  public String nullSafeGet(
      ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
      throws SQLException {
    String value = rs.getString(position);
    return rs.wasNull() ? null : value;
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
      throws SQLException {
    if (value == null) {
      st.setNull(index, Types.OTHER);
    } else {
      // Use PGobject to properly handle pgvector type
      try {
        org.postgresql.util.PGobject pgObject = new org.postgresql.util.PGobject();
        pgObject.setType("vector");
        pgObject.setValue(value);
        st.setObject(index, pgObject);
      } catch (Exception e) {
        throw new SQLException("Failed to set vector value", e);
      }
    }
  }

  @Override
  public String deepCopy(String value) {
    return value; // String is immutable
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Serializable disassemble(String value) {
    return value;
  }

  @Override
  public String assemble(Serializable cached, Object owner) {
    return (String) cached;
  }

  @Override
  public String replace(String detached, String managed, Object owner) {
    return detached;
  }
}
