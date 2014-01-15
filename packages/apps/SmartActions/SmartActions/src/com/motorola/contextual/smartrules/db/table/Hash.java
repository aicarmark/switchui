/*
 * @(#)Hash.java
 *
 * (c) COPYRIGHT 2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/07/12 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;

import java.lang.reflect.Array;

/** This class generates a Hash based on type
 *
 *<code><pre>
 * CLASS:
 * 	Extends nothing, 
 *  Implements nothing
 *
 * RESPONSIBILITIES:
 * 	Hash any primitive type or Object or Array type
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public final class Hash {

  public static final int SEED = 41;
  private static final int PRIME_NUMBER = 57;

  /** generate seed value using a prime number
   * 
   * @param seed 
   * @return seed x prime
   */
  private static int seed( int seed ){
	  return PRIME_NUMBER * seed;
  }
  
  /** Hash boolean type
   * 
   * @param seed - seed value
   * @param b - boolean to hash
   */
  public static int hash( int seed, boolean b ) {
    return seed( seed ) + ( b ? 1 : 0 );
  }

  /** Hash int type
   * 
   * @param seed - seed value
   * @param i - integer to hash
   * @return hash
   */
  public static int hash( int seed, int i ) {
    return seed( seed ) + i;
  }

  /** Hash long type
   * @param seed - seed value
   * @param l - long to hash
   * @return hash
   */
  public static int hash( int seed, long l ) {
    return seed(seed) + (int)( l ^ (l >>> 32) );
  }

  /**
   * Hash float type
   * @param seed - seed value
   * @param f - float to hash
   * @return hash
   */
  public static int hash( int seed, float f ) {
    return hash( seed, Float.floatToIntBits(f) );
  }

  /** Hash double type
   * @param seed - seed value
   * @param d - double to hash
   * @return hash
   */
  public static int hash( int seed, double d ) {
    return hash( seed, Double.doubleToLongBits(d) );
  }

  /** Hash char type
   * @param seed - seed value
   * @param c - char to hash
   * @return hash
   */
  public static int hash( int seed, char c ) {
    return seed( seed ) + (int)c;
  }

  /** Hash object type
   * @param seed - seed value
   * @param o - object type to hash
   * @return hash
   */
  public static int hash( int seed, Object o ) {
    int result = seed;
    result = ( o == null? hash(result, 0) : hash(result, o.hashCode()));
    return result;
  }

  /** Hash array type
   * @param seed - seed value
   * @param a - array to hash
   * @return hash
   */
  public static int hash( int seed, Array a ) {
	int result = seed;
	for ( int i = 0; i < Array.getLength(a); ++i ) {
		result = hash(result, Array.get(a, i));
	}
	return result;
  }
} 

