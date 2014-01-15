package com.motorola.mmsp.render.util;

import java.util.ArrayList;
import java.util.Random;

import android.util.Log;

public class MotoMathUtil {
	private static final String TAG = "MotoRenderPlayer";
	private static final Random mRandom = new Random(System.currentTimeMillis());
	private static final String UNIT_DS = "ds";
	private static final String UNIT_PX = "px";
	private static final String UNIT_DS_PER_SECOND = "ds/s";
	private static final String UNIT_DS_PER_SECOND_SQUARE = "ds/s2";
	private static final String UNIT_PX_PER_SECOND = "px/s";
	private static final String UNIT_PX_PER_SECOND_SQUARE = "px/s2";
	
	
	// static final int RADIX_INTEGER = 10;

	// static final float RADIX_DECIMAL = 0.1f;

	static final float[] EXPONENT_FLOAT_ARRAYS = { 0.1f, 0.01f, 0.001f,
			0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f,
			0.000000001f, 0.0000000001f };
	static final int[] EXPONENT_INTEGER_ARRAYS = { 1, 10, 100, 1000, 10000,
			100000, 1000000, 10000000, 100000000, 1000000000 };

	public final static int parseIntFast(String string) {
		int offset = 0;
		boolean negative = string.charAt(offset) == '-';
		int result = 0, length = string.length();
		if (negative && ++offset == length) {
			throw invalidInt(string);
		}
		result = parseInt(offset, length, string);
		if (result == -1) {
			throw invalidInt(string);
		}
		if (negative) {
			result = -result;
		}
		return result;
	}

	private final static int parseInt(int offset, int length, String string) {
		int result = 0;
		for (int i = 0; i < length - offset; i++) {
			int digit = -1;
			int codePoint = (int) string.charAt(length - i - 1);
			if ('0' <= codePoint && codePoint <= '9') {
				digit = codePoint - '0';
			}
			if (digit == -1) {
				return -1;
			}
			result += EXPONENT_INTEGER_ARRAYS[i] * digit;
		}
		return result;
	}

	public final static float parseFloatFast(String string) {
		int offset = 0;
		boolean negative = string.charAt(offset) == '-';
		float result = 0f;
		int length = string.length();
		if (negative && ++offset == length) {
			throw invalidFloat(string);
		}
		int decimal = string.indexOf('.');
		if (decimal < 0) {
			result = parseInt(offset, length, string);
			if (-1 == result) {
				throw invalidFloat(string);
			}
		} else if (decimal == 0) {
			result = decimal(string.substring(1, length));
			if (-1 == result) {
				throw invalidFloat(string);
			}
		} else {
			String intString = string.substring(0, decimal);
			int resultInt = parseInt(offset, intString.length(), intString);
			if (-1 == resultInt) {
				throw invalidFloat(string);
			}
			float resultDecimal = decimal(string.substring(decimal + 1, length));
			if (-1 == resultDecimal) {
				throw invalidFloat(string);
			}
			result = resultInt + resultDecimal;
		}

		if (negative) {
			result = -result;
		}

		return result;
	}

	private final static float decimal(String string) {
		float result = 0f;
		int length = string.length();
		for (int offset = 0; offset < length; offset++) {
			int digit = -1;
			int codePoint = (int) string.charAt(offset);
			if ('0' <= codePoint && codePoint <= '9') {
				digit = codePoint - '0';
			}
			if (digit == -1) {
				return -1f;
			}
			result += digit * EXPONENT_FLOAT_ARRAYS[offset];

		}
		return result;
	}

	private static NumberFormatException invalidInt(String s) {
		throw new NumberFormatException("Invalid int: \"" + s + "\"");
	}

	private static NumberFormatException invalidFloat(String s) {
		throw new NumberFormatException("Invalid float: \"" + s + "\"");
	}

	public static final float parseFloat(String value) {
        if (value == null || (value = value.trim()).length() == 0) {
            return 0;
        }
		try {
			if (value.startsWith("random")) {
				String first = value.substring((value.indexOf('(') + 1), value.indexOf(',')).trim();
				String end = value.substring(value.indexOf(',') + 1, value.indexOf(')')).trim();
				float firstFloat = parseFloatFast(first);
				float endFloat = parseFloatFast(end);
				return getRandomFloatBetween(firstFloat, endFloat);
				
			} else {
				float ret = parseFloatFast(value);
				return ret;
			}
			
		} catch (Exception e) {
			Log.e(TAG, String.format("%s is not a float!!", value != null ? value : "null"));
		}
		return 0;
	}
    
    /**
     * add this method for velocity attribute that support ds unit
     * 
     * @param value
     * @param ds2PxScale
     * @return
     */
    public static final float parseFloatVelocity(String value, float ds2PxScale) {

        if (value == null || (value = value.trim()).length() == 0) {
            return 0;
        }

        try {
            if (value.startsWith("random")) {
                String first = value.substring((value.indexOf('(') + 1),
                        value.indexOf(',')).trim();
                String end = value.substring(value.indexOf(',') + 1,
                        value.indexOf(')')).trim();
                float firstFloat = parseFloat2(first, UNIT_DS_PER_SECOND,
                        UNIT_PX_PER_SECOND, ds2PxScale);
                float endFloat = parseFloat2(end, UNIT_DS_PER_SECOND,
                        UNIT_PX_PER_SECOND, ds2PxScale);
                return getRandomFloatBetween(firstFloat, endFloat);

            } else {
                return parseFloat2(value, UNIT_DS_PER_SECOND,
                        UNIT_PX_PER_SECOND, ds2PxScale);
            }

        } catch (Exception e) {
            Log.e(TAG, String.format("%s is not a float!!",
                    value != null ? value : "null"));
        }

        return 0;
    }

	/**
	 * add this method for normal attribute that support ds unit
	 * 
	 * @param value
	 * @param ds2PxScale
	 * @return
	 */
	public static final float parseFloatNormal(String value, float ds2PxScale) {
		if (value == null || (value = value.trim()).length() == 0) {
			return 0;
		}

		try {
			if (value.startsWith("random")) {
				String first = value.substring((value.indexOf('(') + 1),
						value.indexOf(',')).trim();
				String end = value.substring(value.indexOf(',') + 1,
						value.indexOf(')')).trim();
				float firstFloat = parseFloat2(first, UNIT_DS,
						UNIT_PX, ds2PxScale);
				float endFloat = parseFloat2(end, UNIT_DS, UNIT_PX,
						ds2PxScale);
				return getRandomFloatBetween(firstFloat, endFloat);

			} else {
				return parseFloat2(value, UNIT_DS, UNIT_PX, ds2PxScale);
			}

		} catch (Exception e) {
			Log.e(TAG, String.format("%s is not a float!!",
					value != null ? value : "null"));
		}

		return 0;

	}
    
	/**
	 * add this method for acceleration attribute that support ds unit
	 * 
	 * @param value
	 * @param ds2PxScale
	 * @return
	 */
	public static final float parseFloatAcceleration(String value,
			float ds2PxScale) {
		if (value == null || (value = value.trim()).length() == 0) {
			return 0;
		}

		try {
			if (value.startsWith("random")) {
				String first = value.substring((value.indexOf('(') + 1),
						value.indexOf(',')).trim();
				String end = value.substring(value.indexOf(',') + 1,
						value.indexOf(')')).trim();
				float firstFloat = parseFloat2(first,
						UNIT_DS_PER_SECOND_SQUARE, UNIT_PX_PER_SECOND_SQUARE,
						ds2PxScale);
				float endFloat = parseFloat2(end, UNIT_DS_PER_SECOND_SQUARE,
						UNIT_PX_PER_SECOND_SQUARE, ds2PxScale);
				return getRandomFloatBetween(firstFloat, endFloat);

			} else {
				return parseFloat2(value, UNIT_DS_PER_SECOND_SQUARE,
						UNIT_PX_PER_SECOND_SQUARE, ds2PxScale);
			}

		} catch (Exception e) {
			Log.e(TAG, String.format("%s is not a float!!",
					value != null ? value : "null"));
		}

		return 0;

	}
    
	private static final float parseFloat2(String value, String suffixDS,
			String suffixPx, float ds2PxScale) {
		float ret = 0f;
		int lastIndexDs = value.lastIndexOf(suffixDS);
		if (-1 != lastIndexDs) {
			ret = parseFloatFast(value.substring(0, lastIndexDs))
					* ds2PxScale;
		} else {
			int lastIndexPx = value.lastIndexOf(suffixPx);
			if (-1 != lastIndexPx) {
				ret = parseFloatFast(value.substring(0, lastIndexPx));
			} else {
				ret = parseFloatFast(value);
			}
		}
		return ret;
	}
	
	public static final float getRandomFloatBetween(float first, float end) {
		return (mRandom.nextFloat() * (end - first)) + first;
	}
	
	public static final int getRandomIntBetween(int firstInt, int endInt) {
		if (endInt > firstInt) {
			return firstInt + mRandom.nextInt(endInt - firstInt);
		} else if (endInt < firstInt){
			return firstInt - mRandom.nextInt(firstInt - endInt);
		} else {
			return firstInt;
		}
	}
	
	public static final int parseInt(String value) {
		if (value == null) {
			return 0;
		}
		try {
			value = value.trim();
			if (value.startsWith("random")) {
				String first = value.substring((value.indexOf('(') + 1), value.indexOf(',')).trim();
				String end = value.substring(value.indexOf(',') + 1, value.indexOf(')')).trim();
				int firstInt = parseIntFast(first);
				int endInt = parseIntFast(end);
				return getRandomIntBetween(firstInt, endInt);
			} else {
				if (value.charAt(0) == '@') {
					value = value.substring(1);
				}
				return parseIntFast(value);
			}
			
		} catch (Exception e) {
			Log.e(TAG, String.format("%s is not a integer!!", value));
		}
		return 0;
	} 
	
	public static final boolean parseBoolean(String value) {
		try {
			value = value.trim();
			if (value.startsWith("random")) {
				int valueInt = parseInt(value);
				return (0 != valueInt);
			}
			return ("1".equals(value) || "true".equalsIgnoreCase(value));
			
		} catch (Exception e) {
			Log.e(TAG, String.format("%s is not a boolean!!", value));
		}
		return false;
	} 
	
	public static final int[] parseIntArray(String value) {
		int[] ret = null;
		ArrayList<Integer> retArrayList = new ArrayList<Integer>();
		int start = value.indexOf('(') + 1;
		int end = value.indexOf(',', start);
		int len = value.length();
		while (end != -1 && start < len) {
			try {
				String number = value.substring(start, end).trim();
				int numberRet = 0;
				
				if (number.startsWith("random")) {
					int endTemp = value.indexOf(',', end+1);
					if (endTemp == -1) {
						endTemp = value.indexOf(')', end+1) + 1;
					}
					end = endTemp;
					number = value.substring(start, end).trim();
					
					String numberFirst = number.substring((number.indexOf('(') + 1), number.indexOf(',')).trim();
					String numberEnd = number.substring(number.indexOf(',') + 1, number.indexOf(')')).trim();
					int firstInt = parseIntFast(numberFirst);
					int endInt = parseIntFast(numberEnd);
					numberRet = getRandomIntBetween(firstInt, endInt);
				} else {
					numberRet = parseIntFast(number);
				}
				retArrayList.add(numberRet);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			start = end + 1;
			end = value.indexOf(',', start);
			if (end == -1) {
				end = value.indexOf(')', start);
			}
		}
		
		int size = retArrayList.size();
		ret = new int[size];
		for (int i=0; i<size; i++) {
			ret[i] = retArrayList.get(i);
		}
		return ret;
	}
	
	
	public static final float[] parseFloatArray(String value) {
		float[] ret = null;
		ArrayList<Float> retArrayList = new ArrayList<Float>();
		int start = value.indexOf('(') + 1;
		int end = value.indexOf(',', start);
		if (end == -1) {
			end = value.indexOf(')');
		}
		int len = value.length();
		while (end != -1 && start < len) {
			try {
				String number = value.substring(start, end).trim();
				float numberRet = 0;
				
				if (number.startsWith("random")) {
					int endTemp = value.indexOf(',', end+1);
					if (endTemp == -1) {
						endTemp = value.indexOf(')', end+1) + 1;
					}
					end = endTemp;
					number = value.substring(start, end).trim();
					
					String numberFirst = number.substring((number.indexOf('(') + 1), number.indexOf(',')).trim();
					String numberEnd = number.substring(number.indexOf(',') + 1, number.indexOf(')')).trim();
					float firstFloat = parseFloatFast(numberFirst);
					float endFloat = parseFloatFast(numberEnd);
					numberRet = getRandomFloatBetween(firstFloat, endFloat);
				} else {
					numberRet = parseFloatFast(number);
				}
				retArrayList.add(numberRet);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			start = end + 1;
			end = value.indexOf(',', start);
			if (end == -1) {
				end = value.indexOf(')', start);
			}
		}
		
		int size = retArrayList.size();
		ret = new float[size];
		for (int i=0; i<size; i++) {
			ret[i] = retArrayList.get(i);
		}
		return ret;
	}
	
}
