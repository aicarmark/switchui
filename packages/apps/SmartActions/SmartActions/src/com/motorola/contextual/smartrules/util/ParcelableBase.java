/*
 * @(#)ParcelableBase.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import android.os.Parcel;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/** This class is a base class used to allow other classes to become parcelable
 * without actually implementing a statement for every instance variable to be
 * backed up. That is, this class generically implements parcel such that any
 * primitives or String variables will be backed up to the parcel without any
 * additional code.
 *
 *
 *<code><pre>
 * CLASS:
 *   The advantage in using this class is that primitive variables or Strings are added
 *   automatically to a parcel, no code needs to be added to support the parcel
 *   operation for each String or primitive.
 *
 *   However, the drawback to using this class is that it is slower than implementing
 *   the statement-by-statement packing and unpacking the parcel manually. This is
 *   slower because it uses reflection to determine the field type.
 *
 * RESPONSIBILITIES:
 *   Handle writing and reading from parcel these types:
 *      int, float, double, long, and String.
 *
 * COLABORATORS:
 * 	 None
 *
 * USAGE:
 * 	FriendTuple extends ParcelableBase .....
 *
 *		public FriendTuple(Parcel parcel) {
 *			super(parcel);
 *		}
 *
 *		public void writeToParcel(Parcel parcel, int flags) {
 *	 		super.writeToParcel(parcel, flags);
 * 		}
 * 	.....
 *
 * </pre></code>
 **/
public class ParcelableBase implements Constants{

	private static final String TAG = ParcelableBase.class.getSimpleName();
    private final static int PUBLIC_STATIC_FINAL = (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);

    public static final int PARCEL_PRESENT 	= 1;
    public static final int PARCEL_ABSENT 	= 0;

    /** Basic constructor
     */
    public ParcelableBase() {

        super();
    }


    /** This is the constructor for re-inflating the instance from a parcel.
     *
     * This code is relatively slow compared to that of a specific paragraph for
     * instantiating from a parcel. But, if speed isn't an issue, then this code
     * should work fine.
     */
    public ParcelableBase(final Parcel parcel) {

        readFromParcel(parcel);
    }


    /** This method is required for using parcelables with AIDL files.
     * @see http://www.fdocs.cn/android/sdk1.5_r2/en/guide/developing/tools/aidl.html#parcelable
     */
    public void readFromParcel(Parcel in) {

    	readFromParcelRecursively(in, getClass());
    }
    
    @SuppressWarnings("rawtypes")
	public void readFromParcelRecursively(Parcel parcel, Class className) {
        
        if(className.equals(ParcelableBase.class)){
     	   return;
 	       
        } else{
        	readFromParcelRecursively(parcel, className.getSuperclass());
     	   	Field[] fields = className.getDeclaredFields();
 	        if (fields != null) {
 	        	readFromParcel(parcel, fields);
 	        }
        }
    }


    /** required method for classes implementing Parcelable.
     *
     * @return - zero, default for describing the contents.
     */
    public int describeContents() {

        return 0;
    }


    /** this methods reads the content from the parcel.
     *
     * @param parcel - parcel
     * @param fields - array of fields to pull from the parcel.
     */
    public void readFromParcel(final Parcel parcel, final Field[] fields)  {
    	int count_read=0;
        for (final Field field : fields) {

            final int modifiers = field.getModifiers();
            
            // don't read value from to parcel if field is static or public or final
            if ((modifiers & PUBLIC_STATIC_FINAL) == 0) {
            	count_read++;
                final Class<?> type = field.getType();
                field.setAccessible(true);
		if(LOG_VERBOSE) Log.v(TAG, "readFromParcel : "+field.toString());
                try {
                    // these types should represent ALL the database field types
                    // except Blob

                    if (type.equals(int.class)) {
                        field.setInt(this, parcel.readInt());
                    } else if (type.equals(String.class)) {
                        final String s = parcel.readString();
                        field.set(this, s == null || s.equals("null") ? null : s);
                    } else if (type.equals(long.class)) {
                        field.setLong(this, parcel.readLong());
                    } else if (type.equals(double.class)) {
                        field.setDouble(this, parcel.readDouble());
                    } else if (type.equals(float.class)) {
                        field.setFloat(this, parcel.readFloat());
                    }
                } catch (final IllegalArgumentException e) {
                    // this error should only occur in development
                    e.printStackTrace();

                } catch (final IllegalAccessException e) {
                    // this error should only occur in development
                    e.printStackTrace();

                }
            }
        }
        if(LOG_VERBOSE) Log.v(TAG, "readFromParcel field read count : "+count_read);
    }


    /**
     * this code writes the fields to the parcel.
     *
     * @param parcel
     *            - parcel
     * @param flags
     *            - flags
     * @param fields
     *            - fields to read from the parcel.
     */
    public void writeFieldsToParcel(final Parcel parcel, final int flags, final Field[] fields) {
    	int count_write=0;
        final int publicStaticFinal = (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        for (final Field field : fields) {
            final int modifiers = field.getModifiers();

            // don't write to parcel if field is static or public or final
            if ((modifiers & publicStaticFinal) == 0) {
            	count_write++;
                final Class<?> type = field.getType();
                field.setAccessible(true);
                if(LOG_VERBOSE) Log.v(TAG, "writeFieldsToParcel : "+field.toString());
                try {
                    // these types should represent ALL the database field types
                    // except Blob

                    if (type.equals(int.class))
                        parcel.writeInt(field.getInt(this));
                    else if (type.equals(String.class))
                        parcel.writeString(field.get(this) != null ? (String) field
                                           .get(this) : "null");
                    else if (type.equals(long.class))
                        parcel.writeLong(field.getLong(this));
                    else if (type.equals(double.class))
                        parcel.writeDouble(field.getDouble(this));
                    else if (type.equals(float.class))
                        parcel.writeFloat(field.getFloat(this));


                } catch (final IllegalArgumentException e) {
                    // this error should only occur during development
                    e.printStackTrace();
                } catch (final IllegalAccessException e) {
                    // this error should only occur during development
                    e.printStackTrace();
                }
            }
        }
        if(LOG_VERBOSE) Log.v(TAG, "writeFieldsToParcel field written count : "+count_write);
    }


    /** This code writes the primitives to the parcel plus String type.
     *
     * This code is relatively slow compared to that of a specific paragraph for
     * writing to a parcel directly. But, if speed isn't an issue, then this
     * code should work fine.
     */
    public void writeToParcel(final Parcel parcel, final int flags) {

    	writeToParcelRecursive(parcel, flags, getClass());
    }
    
    /** This code writes the primitives to the parcel plus String type.
    *
    * This code is relatively slow compared to that of a specific paragraph for
    * writing to a parcel directly. But, if speed isn't an issue, then this
    * code should work fine.
    */
   @SuppressWarnings("rawtypes")
   public void writeToParcelRecursive(final Parcel parcel, final int flags, Class className) {

       if(className.equals(ParcelableBase.class)){
    	   return;
	       
       } else{
    	   writeToParcelRecursive(parcel, flags, className.getSuperclass());
    	   Field[] fields = className.getDeclaredFields();
	       if (fields != null) {
	           writeFieldsToParcel(parcel, flags, fields);
	       }
       }      
   }
}