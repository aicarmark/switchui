/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 * w04917 (Brian Lee)        2011/05/11   IKCTXTAW-272    Test for error handling of non-responsive peripheral and
 *                                                        cleanup klocwork error
 *
 * To execute test, follow the steps below on Eclipse:
 * 1. Import Meter project as Android Library Project to workspace
 * 2. Import CollectionService project as Android Library Project to workspace
 * 3. Make CollectionService project use the Meter project library.
 *    This can be done by right cilcking on CollectionService project->Android->Library->Add
 * 4. Import DataCollection project. Make it use CollectionService project as library.
 * 5. In Java Build Path/Libraries, add gson1.6 library from Meter
 * 6. Remove android:sharedUserID line from manifest tag and remove android:process line from application tag, in the AndroidManifest.xml file
 * 7. Right click on MeterReaderTest.java in Eclipse and select Run As -> Android JUnit Test
 *
 * We have to recompile and reinstall because the target app is set to use the com.motorola.process.system.
 * Android Instrumentation tests cannot force the test to run in a process that already has applications running.
 */

package com.motorola.datacollection.meter.reader.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.test.mock.MockContext;
import android.util.Log;

import com.motorola.collectionservice.Callback_Client;
import com.motorola.collectionservice.CollectionServiceAPI;
import com.motorola.collectionservice.ICollectionService;
import com.motorola.datacollection.meter.reader.MeterReader;
import com.motorola.meter.Meter;
import com.motorola.meter.PowerMeter;
import com.motorola.meter.reading.MeterReading;

public class MeterReaderTest extends ServiceTestCase<MeterReader> {
    private static final String TAG = "MeterReaderTest";

    private MockPowerMeter1 mockPowerMeter1;
    private MockPowerMeter2 mockPowerMeter2;
    private SleepyPowerMeter sleepyPowerMeter;

    private MeterReaderContext mContextMeter;
    private MeterReaderContext mContextApp1;
    private boolean mRegistrationSuccessful;

    private static final String TEST_PACKAGE_NAME = "com.motorola.datacollection.meter.reader.test";
    private static final String TEST_CLASS_NAME = "MeterReaderTest";

    /* SERVICE_TIMEOUT_MS should be greater than MeterReader.READ_METER_EXPIRE_TIME_MS */
    private static final int SERVICE_TIMEOUT_MS = 2000;

    /* latch to make sure the meters are connected/bound to the service before we move on */
    private CountDownLatch meter1RegisterLatch, meter2RegisterLatch, sleepyRegisterLatch;

    private String phantomMeter = "Phantom Meter";
    private static final int mockIntVal = 1337;
    private static final String mockStringVal = "Mock String";
    private static final double mockDoubleVal = 82.82;
    private static final boolean mockBooleanVal = true;
    private static final char mockCharVal = 'B';
    private static final float mockFloatVal = 10.04f;
    private static final long mockLongVal = 8282045;


    /* client related variables */
    private Callback_Client clientCallback1, clientCallback2;
    private ICollectionService collectionService1;
    private CountDownLatch latch1, latch2;

    private ArrayList<Bundle> callbackBundleList1, callbackBundleList2;
    private ArrayList<String> callbackStringList1;
    private Bundle callbackBundle1, callbackBundle2;
    private boolean callbackBoolean1, callbackBoolean2;
    private String callbackString1;

    public MeterReaderTest() {
        super(MeterReader.class);
    }

    @Override
    protected void setUp() throws Exception {
        Log.d(TAG,"setUp()");
        super.setUp();

        meter1RegisterLatch = null;
        meter2RegisterLatch = null;
        sleepyRegisterLatch = null;

        clientCallback1 = null;
        collectionService1 = null;
        latch1 = null;

        clientCallback2 = null;
        latch2 = null;

        callbackBundleList1 = null;
        callbackBundleList2 = null;

        callbackBundle1 = null;
        callbackBoolean1 = false;
        callbackStringList1 = null;

        callbackBundle2 = null;
        callbackBoolean2 = false;

        mContextMeter = new MeterReaderContext();
        mContextApp1 = new MeterReaderContext();

        mockPowerMeter1 = new MockPowerMeter1(mContextMeter);
        meter1RegisterLatch = new CountDownLatch(1);
        mRegistrationSuccessful = mockPowerMeter1.registerMeter(MockPowerReading1.class);
        try {
            meter1RegisterLatch.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        mockPowerMeter2 = null;
        sleepyPowerMeter = null;
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d(TAG,"tearDown()");

        mockPowerMeter1.unregisterMeter();

        if (mockPowerMeter2 != null) {
            mockPowerMeter2.unregisterMeter();
            mockPowerMeter2 = null;
        }

        if (sleepyPowerMeter != null) {
            sleepyPowerMeter.unregisterMeter();
            sleepyPowerMeter = null;
        }

        mContextMeter = null;
        mContextApp1 = null;
        mockPowerMeter1 = null;
        super.tearDown();
    }

    public void testPreconditions() {
        assertNotNull(mContextMeter);
        assertNotNull(mContextApp1);
        assertNotNull(mockPowerMeter1);
        assertTrue(mRegistrationSuccessful);
    }

    private void bindCollectionService() {
        latch1 = new CountDownLatch(1);

        ServiceConnection connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                collectionService1 = ICollectionService.Stub.asInterface(service);
                latch1.countDown();
            }
            public void onServiceDisconnected(ComponentName componentName) {}
        };

        callbackBoolean1 = false;
        mContextApp1.bindService(new Intent(CollectionServiceAPI.INTENT_BIND_FRAMEWORK),
                                 connection, Context.BIND_AUTO_CREATE);

        boolean bindSuccess = false;
        try {
            bindSuccess = latch1.await(SERVICE_TIMEOUT_MS,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("Binding to CollectionService interrupted.");
        }

        assertTrue("Binding to CollectionService timed out in " + SERVICE_TIMEOUT_MS + " ms.", bindSuccess);
        assertNotNull("Null Ibinder returned after connecting to CollectionService.",collectionService1);
    }

    public void testCollectionServiceAPI_getTimestamp() {
        Bundle bundle = new Bundle();
        bundle.putLong(Meter.METER_TIMESTAMP, mockLongVal);

        long actualLong = CollectionServiceAPI.getTimestamp(bundle);
        assertEquals(mockLongVal, actualLong);

        bundle = new Bundle();
        actualLong = CollectionServiceAPI.getTimestamp(bundle);
        assertEquals("getTimestamp with no timestamp value did not return -1.",-1, actualLong);
    }

    public void testCollectionServiceAPI_getTimestamp_missingValue() {
        Bundle bundle = new Bundle();

        long actualLong = CollectionServiceAPI.getTimestamp(bundle);
        assertEquals(-1, actualLong);
    }

    public void testCollectionServiceAPI_getTimestamp_nullBundle() {
        long actualLong = CollectionServiceAPI.getTimestamp(null);
        assertEquals(-1, actualLong);
    }

    public void testCollectionServiceAPI_getDataType() {
        Bundle bundle = new Bundle();
        bundle.putString(Meter.METER_DATA_TYPE, mockStringVal);

        String actualString = CollectionServiceAPI.getDataType(bundle);
        assertEquals(mockStringVal, actualString);
    }

    public void testCollectionServiceAPI_getDataType_nullString() {
        Bundle bundle = new Bundle();
        bundle.putString(Meter.METER_DATA_TYPE, null);

        String actualString = CollectionServiceAPI.getDataType(bundle);
        assertNull(actualString);
    }

    public void testCollectionServiceAPI_getDataType_missingValue() {
        Bundle bundle = new Bundle();

        String actualString = CollectionServiceAPI.getDataType(bundle);
        assertNull(actualString);
    }

    public void testCollectionServiceAPI_getDataType_nullBundle() {
        String actualString = CollectionServiceAPI.getDataType(null);
        assertNull(actualString);
    }

    public void testCollectionServiceAPI_getRawData() {
        Bundle bundle = new Bundle();
        bundle.putString(Meter.METER_READING_JSON, mockStringVal);

        String actualString = CollectionServiceAPI.getRawData(bundle);
        assertEquals(mockStringVal, actualString);
    }

    public void testCollectionServiceAPI_getRawData_nullString() {
        Bundle bundle = new Bundle();
        bundle.putString(Meter.METER_READING_JSON, null);

        String actualString = CollectionServiceAPI.getRawData(bundle);
        assertNull(actualString);
    }

    public void testCollectionServiceAPI_getRawData_missingValue() {
        Bundle bundle = new Bundle();

        String actualString = CollectionServiceAPI.getRawData(bundle);
        assertNull(actualString);
    }

    public void testCollectionServiceAPI_getRawData_nullBundle() {
        String actualString = CollectionServiceAPI.getRawData(null);
        assertNull(actualString);
    }

    /* Tests if appilcations can successfully bind to the CollectionService */
    public void testBindService_CollectionService() {
        bindCollectionService();
    }

    /* Tests CollectionService.requestAvailableDataTypes() */
    public void testRequestAvailableDataTypes() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {}
            public void readingErrorCallback(List<String> meterTypes) {}
            public void availableMetersCallback(List<String> meterTypes) {
                if (meterTypes != null) {
                    callbackStringList1 = new ArrayList<String>(meterTypes);
                }
                latch1.countDown();
            }
        };

        boolean success = false;
        try {
            success = collectionService1.requestAvailableMeters(clientCallback1);
            assertTrue("requestAvailableDataTypes failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestAvailableDataTypes failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestAvailableDataTypes interrupted.");
        }

        assertTrue("requestAvailableDataTypes timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertNotNull("requestAvailableDataTypes returned null.", callbackStringList1);
        /* There should be only one registered meter */
        assertTrue("dataTypeArray length is not equal to 1.", callbackStringList1.size() == 1);

        String dataType = callbackStringList1.get(0);
        String originalMeter = MockPowerReading1.class.getCanonicalName();

        assertNotNull("dataType is null.", dataType);
        assertTrue("dataType length is 0", dataType.length() > 0);
        assertEquals(originalMeter, dataType);
    }

    /* tests CollectionService.requestReading() called with registered meter type*/
    public void testRequestReading_registeredMeter() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;
        ArrayList<String> meter = new ArrayList<String>();
        meter.add(MockPowerReading1.class.getCanonicalName());
        try {
            success = collectionService1.requestReading(meter, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading returned an error.", callbackBoolean1);
        assertNotNull("requestReading returned null bundle list.", callbackBundleList1);
        assertEquals("requestReading returned wrong size bundle.", 1, callbackBundleList1.size());
        callbackBundle1 = callbackBundleList1.get(0);
        assertNotNull("requestReading returned null bundle.", callbackBundle1);

        /* unpack
         * Unfortnuately, packing is done privately in Meter parent class,
         * so the only way to get and test for packed data is after we request it
         * from the service through requestReading
         */
        MockPowerReading1 unpackedReading = (MockPowerReading1) CollectionServiceAPI.unpack(callbackBundle1);
        assertNotNull("Unpacking bundle resulted in a null object.", unpackedReading);
        if (unpackedReading != null) {
            assertEquals("MockPowerReading1 version is wrong", MockPowerReading1.MOCK_POWER_READING_1_VERSION, unpackedReading.getVersion());
            assertEquals("unpackedReading.mockDouble is corrupt.", mockDoubleVal, unpackedReading.mockDouble);
            assertEquals("unpackedReading.mockInt is corrupt.", mockIntVal, unpackedReading.mockInt);
            assertEquals("unpackedReading.mockString is corrupt.", mockStringVal, unpackedReading.mockString);
        }
    }

    /* tests CollectionService.requestReading() called with unregistered meter type*/
    public void testRequestReading_unregisteredMeter() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = false;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = true;
                if (meterTypes != null) {
                    callbackString1 = meterTypes.get(0);
                }
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;
        ArrayList<String> meter = new ArrayList<String>();
        meter.add(phantomMeter);
        try {
            success = collectionService1.requestReading(meter, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading did not return an error.", callbackBoolean1);
        assertEquals(phantomMeter, callbackString1);
        assertNull("requestReading returned a non-null bundle.", callbackBundleList1);
    }

    /* tests CollectionService.requestReading() called with null meter list*/
    public void testRequestReading_nullMeterList() {
        bindCollectionService();

        /* requestReading */
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {}
            public void readingErrorCallback(List<String> meterTypes) {}
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        boolean success = true;
        try {
            success = collectionService1.requestReading(null, clientCallback1);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        }
        assertFalse("requestReading request did not fail.", success);
    }

    /* tests CollectionService.requestReading() called with null callback*/
    public void testRequestReading_nullCallback() {
        bindCollectionService();

        boolean success = true;
        ArrayList<String> meter = new ArrayList<String>();
        meter.add(MockPowerReading1.class.getCanonicalName());
        try {
            success = collectionService1.requestReading(meter, null);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        }
        assertFalse("requestReading request did not fail.", success);
    }

    /* tests CollectionService.requestReading() called with null meter list and null callback*/
    public void testRequestReading_nullParams() {
        bindCollectionService();

        boolean success = true;
        try {
            success = collectionService1.requestReading(null, null);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        }
        assertFalse("requestReading request did not fail.", success);
    }

    /* tests CollectionService.requestReading() called with emptyString meter item */
    public void testRequestReading_emptyStringMeterItem() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = true;
                if (meterTypes != null) {
                    callbackStringList1 = new ArrayList<String>(meterTypes);
                }
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;

        ArrayList<String> meter = new ArrayList<String>();
        meter.add("");

        try {
            success = collectionService1.requestReading(meter, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading did not return an error.", callbackBoolean1);
        assertNotNull("requestReading returned null String list on error callback.", callbackStringList1);
        assertEquals("requestReading returned a different length list on error callback.", meter.size(), callbackStringList1.size());

        for (int index = 0; index < callbackStringList1.size(); index++) {
            assertEquals("dataTypes["+index+"] is not empty!", "", callbackStringList1.get(index));
        }
    }

    /* tests CollectionService.requestReading() called with null meter item */
    public void testRequestReading_nullMeterItem() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = true;
                if (meterTypes != null) {
                    callbackStringList1 = new ArrayList<String>(meterTypes);
                }
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;

        ArrayList<String> meter = new ArrayList<String>();
        meter.add(null);

        try {
            success = collectionService1.requestReading(meter, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading did not return an error.", callbackBoolean1);
        assertNotNull("requestReading returned null String list on error callback.", callbackStringList1);
        assertEquals("requestReading returned a different length list on error callback.", meter.size(), callbackStringList1.size());

        for (int index = 0; index < callbackStringList1.size(); index++) {
            assertNull("dataTypes["+index+"] is not null!", callbackStringList1.get(index));
        }
    }

    /* tests CollectionService.requestReading() called sequentially for two different registered meters */
    public void testRequestReading_twoRegisteredMeter() {
        mockPowerMeter2 = new MockPowerMeter2(mContextMeter);
        assertNotNull("Failed to create MockPowerMeter2", mockPowerMeter2);
        /* make sure the meter is actually connected/bound before we move on */
        meter2RegisterLatch = new CountDownLatch(1);
        boolean meter2RegisterSuccess = false;
        assertTrue("Failed to register MockPowerMeter2", mockPowerMeter2.registerMeter(MockPowerReading2.class));
        try {
            meter2RegisterSuccess = meter2RegisterLatch.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        assertTrue("MockPowerMeter2 registration timed out in " + SERVICE_TIMEOUT_MS + " ms.", meter2RegisterSuccess);

        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        latch2 = new CountDownLatch(1);

        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        clientCallback2 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean2 = true;
                if (readings != null) {
                    callbackBundleList2 = new ArrayList<Bundle>(readings);
                }
                latch2.countDown();
            }
            public void readingErrorCallback(List<String> dataType) {
                callbackBoolean2 = false;
                latch2.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        callbackBoolean2 = false;
        boolean success1 = false;
        boolean success2 = false;

        ArrayList<String> meter1 = new ArrayList<String>();
        meter1.add(MockPowerReading1.class.getCanonicalName());

        ArrayList<String> meter2 = new ArrayList<String>();
        meter2.add(MockPowerReading2.class.getCanonicalName());

        try {
            success1 = collectionService1.requestReading(meter1, clientCallback1);
        } catch (RemoteException e1) {
            fail("requestReading failed for MockPowerMeter1 due to RemoteException: " + e1.toString());
        }
        assertTrue("requestReading request for MockPowerMeter1 failed.", success1);

        try {
            success2 = collectionService1.requestReading(meter2, clientCallback2);
        } catch (RemoteException e1) {
            fail("requestReading failed for MockPowerMeter2 due to RemoteException: " + e1.toString());
        }
        assertTrue("requestReading request for MockPowerMeter2 failed.", success2);

        try {
            success1 = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("requestReading for MockPowerMeter1 interrupted.");
        }

        try {
            success2 = latch2.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("requestReading for MockPowerMeter2 interrupted.");
        }
        mockPowerMeter2.unregisterMeter();
        mockPowerMeter2 = null;

        assertTrue("requestReading for MockPowerMeter1 timed out in " + SERVICE_TIMEOUT_MS + " ms.", success1);
        assertTrue("requestReading for MockPowerMeter2 timed out in " + SERVICE_TIMEOUT_MS + " ms.", success2);

        assertTrue("requestReading for MockPowerMeter1 returned an error.", callbackBoolean1);
        assertTrue("requestReading for MockPowerMeter2 returned an error.", callbackBoolean2);

        assertNotNull("requestReading for MockPowerMeter1 returned null bundle list.", callbackBundleList1);
        assertEquals("requestReading for MockPowerMeter1 returned wrong size bundle.", 1, callbackBundleList1.size());
        callbackBundle1 = callbackBundleList1.get(0);
        assertNotNull("requestReading for MockPowerMeter1 returned null bundle.", callbackBundle1);

        assertNotNull("requestReading for MockPowerMeter2 returned null bundle list.", callbackBundleList2);
        assertEquals("requestReading for MockPowerMeter2 returned wrong size bundle.", 1, callbackBundleList2.size());
        callbackBundle2 = callbackBundleList2.get(0);
        assertNotNull("requestReading for MockPowerMeter2 returned null bundle.", callbackBundle2);

        /* unpack
         * Unfortnuately, packing is done privately in Meter parent class,
         * so the only way to get and test for packed data is after we request it
         * from the service through requestReading
         */
        MockPowerReading1 unpackedReading1 = (MockPowerReading1) CollectionServiceAPI.unpack(callbackBundle1);
        assertNotNull("Unpacking MockPowerReading1 resulted in a null object.", unpackedReading1);
        if (unpackedReading1 != null) {
            assertEquals("MockPowerReading1 version is wrong", MockPowerReading1.MOCK_POWER_READING_1_VERSION, unpackedReading1.getVersion());
            assertEquals("unpackedReading1.mockDouble is corrupt.", mockDoubleVal, unpackedReading1.mockDouble);
            assertEquals("unpackedReading1.mockInt is corrupt.", mockIntVal, unpackedReading1.mockInt);
            assertEquals("unpackedReading1.mockString is corrupt.", mockStringVal, unpackedReading1.mockString);
        }

        MockPowerReading2 unpackedReading2 = (MockPowerReading2) CollectionServiceAPI.unpack(callbackBundle2);
        assertNotNull("Unpacking MockPowerReading2 resulted in a null object.", unpackedReading2);
        if (unpackedReading2 != null) {
            assertEquals("MockPowerReading2 version is wrong", MockPowerReading2.MOCK_POWER_READING_2_VERSION, unpackedReading2.getVersion());
            assertEquals("unpackedReading2.mockCharVal is corrupt.", mockCharVal, unpackedReading2.mockChar);
            assertEquals("unpackedReading2.mockFloatVal is corrupt.", mockFloatVal, unpackedReading2.mockFloat);
            assertEquals("unpackedReading2.mockBooleanVal is corrupt.", mockBooleanVal, unpackedReading2.mockBoolean);
        }
    }

    /* tests CollectionService.requestReading() called an array of two registered meter types */
    public void testRequestReadingMultiple_twoRegisteredMeters() {
        mockPowerMeter2 = new MockPowerMeter2(mContextMeter);
        assertNotNull("Failed to create MockPowerMeter2", mockPowerMeter2);
        /* make sure the meter is actually connected/bound before we move on */
        meter2RegisterLatch = new CountDownLatch(1);
        boolean meter2RegisterSuccess = false;
        assertTrue("Failed to register MockPowerMeter2", mockPowerMeter2.registerMeter(MockPowerReading2.class));
        try {
            meter2RegisterSuccess = meter2RegisterLatch.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        assertTrue("MockPowerMeter2 registration timed out in " + SERVICE_TIMEOUT_MS + " ms.", meter2RegisterSuccess);

        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;

        ArrayList<String> meters = new ArrayList<String>();
        meters.add(MockPowerReading1.class.getCanonicalName());
        meters.add(MockPowerReading2.class.getCanonicalName());

        try {
            success = collectionService1.requestReading(meters, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        mockPowerMeter2.unregisterMeter();
        mockPowerMeter2 = null;

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading returned an error.", callbackBoolean1);
        assertNotNull("requestReading returned null bundle list.", callbackBundleList1);
        assertEquals("requestReading returned a different length list.", meters.size(), callbackBundleList1.size());

        /* unpack
         * Unfortnuately, packing is done privately in Meter parent class,
         * so the only way to get and test for packed data is after we request it
         * from the service through requestReading
         */
        String dataType = null;
        boolean firstMeterFound = false;
        boolean secondMeterFound = false;

        for (Bundle bundle : callbackBundleList1) {
            dataType = CollectionServiceAPI.getDataType(bundle);
            assertNotNull("requestReading had a null bundle item", dataType);
            if (dataType != null && dataType.equals(MockPowerReading1.class.getCanonicalName())) {
                MockPowerReading1 unpackedReading = (MockPowerReading1) CollectionServiceAPI.unpack(bundle);
                assertNotNull("Unpacking MockPowerReading1 bundle resulted in a null object.", unpackedReading);
                if (unpackedReading != null) {
                    assertEquals("MockPowerReading1 version is wrong", MockPowerReading1.MOCK_POWER_READING_1_VERSION, unpackedReading.getVersion());
                    assertEquals("unpackedReading.mockDouble is corrupt.", mockDoubleVal, unpackedReading.mockDouble);
                    assertEquals("unpackedReading.mockInt is corrupt.", mockIntVal, unpackedReading.mockInt);
                    assertEquals("unpackedReading.mockString is corrupt.", mockStringVal, unpackedReading.mockString);
                }
                firstMeterFound = true;
            } else if (dataType != null && dataType.equals(MockPowerReading2.class.getCanonicalName())) {
                MockPowerReading2 unpackedReading2 = (MockPowerReading2) CollectionServiceAPI.unpack(bundle);
                assertNotNull("Unpacking MockPowerReading2 resulted in a null object.", unpackedReading2);
                if (unpackedReading2 != null) {
                    assertEquals("MockPowerReading2 version is wrong", MockPowerReading2.MOCK_POWER_READING_2_VERSION, unpackedReading2.getVersion());
                    assertEquals("unpackedReading2.mockCharVal is corrupt.", mockCharVal, unpackedReading2.mockChar);
                    assertEquals("unpackedReading2.mockFloatVal is corrupt.", mockFloatVal, unpackedReading2.mockFloat);
                    assertEquals("unpackedReading2.mockBooleanVal is corrupt.", mockBooleanVal, unpackedReading2.mockBoolean);
                }
                secondMeterFound = true;
            } else {
                fail("Unknown dataType in bundle list! - " + dataType);
            }
        }
        assertTrue("MockPowerReading1 not found!", firstMeterFound);
        assertTrue("MockPowerReading2 not found!", secondMeterFound);
    }

    /* tests CollectionService.requestReading() called an array of one registered meter type and one unregistered meter type */
    public void testRequestReadingMultiple_oneRegisteredMeter_oneUnregisteredMeter() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;

        ArrayList<String> meters = new ArrayList<String>();
        meters.add(MockPowerReading1.class.getCanonicalName());
        meters.add(phantomMeter);

        try {
            success = collectionService1.requestReading(meters, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading returned an error.", callbackBoolean1);
        assertNotNull("requestReading returned null bundle list.", callbackBundleList1);
        assertEquals("requestReading returned a different length list.", 1, callbackBundleList1.size());

        /* unpack
         * Unfortnuately, packing is done privately in Meter parent class,
         * so the only way to get and test for packed data is after we request it
         * from the service through requestReading
         */
        String dataType = null;
        for (Bundle bundle : callbackBundleList1) {
            dataType = CollectionServiceAPI.getDataType(bundle);
            assertNotNull("requestReading had a null bundle item", dataType);
            if (dataType != null && dataType.equals(MockPowerReading1.class.getCanonicalName())) {
                MockPowerReading1 unpackedReading = (MockPowerReading1) CollectionServiceAPI.unpack(bundle);
                assertNotNull("Unpacking MockPowerReading1 bundle resulted in a null object.", unpackedReading);
                if (unpackedReading != null) {
                    assertEquals("unpackedReading.mockDouble is corrupt.", mockDoubleVal, unpackedReading.mockDouble);
                    assertEquals("unpackedReading.mockInt is corrupt.", mockIntVal, unpackedReading.mockInt);
                    assertEquals("unpackedReading.mockString is corrupt.", mockStringVal, unpackedReading.mockString);
                }
            } else {
                fail("Unknown dataType in bundle array! - " + dataType);
            }
        }
    }

    /* tests CollectionService.requestReading() called an array of two unregistered meter type */
    public void testRequestReadingMultiple_twoUnregisteredMeters() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = true;
                if (meterTypes != null) {
                    callbackStringList1 = new ArrayList<String>(meterTypes);
                }
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;

        ArrayList<String> meters = new ArrayList<String>();
        meters.add(phantomMeter);
        meters.add(phantomMeter + "2");

        try {
            success = collectionService1.requestReading(meters, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading did not return an error.", callbackBoolean1);
        assertNotNull("requestReading returned null String list on error callback.", callbackStringList1);
        assertEquals("requestReading returned a different length list on error callback.", meters.size(), callbackStringList1.size());

        boolean firstMeterFound = false;
        boolean secondMeterFound = false;

        for (String dataType : callbackStringList1) {
            if (meters.get(0).equals(dataType)) {
                firstMeterFound = true;
            } else if (meters.get(1).equals(dataType)) {
                secondMeterFound = true;
            }
        }
        assertTrue("First unregistered meter did not get returned on error callback.", firstMeterFound);
        assertTrue("Second unregistered meter did not get returned on error callback.", secondMeterFound);
    }

    /* tests CollectionService.requestReading() called with null meter items */
    public void testRequestReadingMultiple_twoNullMeters() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = true;
                if (meterTypes != null) {
                    callbackStringList1 = new ArrayList<String>(meterTypes);
                }
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;

        ArrayList<String> meters = new ArrayList<String>();
        meters.add(null);
        meters.add(null);

        try {
            success = collectionService1.requestReading(meters, clientCallback1);
            assertTrue("requestReading request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestReading failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestReading interrupted.");
        }

        assertTrue("requestReading timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestReading did not return an error.", callbackBoolean1);
        assertNotNull("requestReading returned null String list on error callback.", callbackStringList1);
        assertEquals("requestReading returned a different length list on error callback.", meters.size(), callbackStringList1.size());

        for (int index = 0; index < callbackStringList1.size(); index++) {
            assertNull("dataTypes["+index+"] is not null!", callbackStringList1.get(index));
        }
    }

    /* tests CollectionService.requestAllReadings() called with one registered meter type*/
    public void testRequestAllReadings_oneRegisteredMeter() {
        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;
        try {
            success = collectionService1.requestAllReadings(clientCallback1);
            assertTrue("requestAllReadings request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestAllReadings failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestAllReadings interrupted.");
        }

        assertTrue("requestAllReadings timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestAllReadings returned an error.", callbackBoolean1);
        assertNotNull("requestAllReadings returned null bundle list.", callbackBundleList1);
        assertEquals("requestAllReadings returned wrong size bundle.", 1, callbackBundleList1.size());
        callbackBundle1 = callbackBundleList1.get(0);
        assertNotNull("requestAllReadings returned null bundle.", callbackBundle1);

        /* unpack
         * Unfortnuately, packing is done privately in Meter parent class,
         * so the only way to get and test for packed data is after we request it
         * from the service through requestAllReadings
         */
        MockPowerReading1 unpackedReading = (MockPowerReading1) CollectionServiceAPI.unpack(callbackBundle1);
        assertNotNull("Unpacking bundle resulted in a null object.", unpackedReading);
        if (unpackedReading != null) {
            assertEquals("MockPowerReading1 version is wrong", MockPowerReading1.MOCK_POWER_READING_1_VERSION, unpackedReading.getVersion());
            assertEquals("unpackedReading.mockDouble is corrupt.", mockDoubleVal, unpackedReading.mockDouble);
            assertEquals("unpackedReading.mockInt is corrupt.", mockIntVal, unpackedReading.mockInt);
            assertEquals("unpackedReading.mockString is corrupt.", mockStringVal, unpackedReading.mockString);
        }
    }

    /* tests CollectionService.requestAllReadings() called with one registered meter type*/
    public void testRequestAllReadings_twoRegisteredMeter() {
        mockPowerMeter2 = new MockPowerMeter2(mContextMeter);
        assertNotNull("Failed to create MockPowerMeter2", mockPowerMeter2);
        /* make sure the meter is actually connected/bound before we move on */
        meter2RegisterLatch = new CountDownLatch(1);
        boolean meter2RegisterSuccess = false;
        assertTrue("Failed to register MockPowerMeter2", mockPowerMeter2.registerMeter(MockPowerReading2.class));
        try {
            meter2RegisterSuccess = meter2RegisterLatch.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        assertTrue("MockPowerMeter2 registration timed out in " + SERVICE_TIMEOUT_MS + " ms.", meter2RegisterSuccess);

        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;
        try {
            success = collectionService1.requestAllReadings(clientCallback1);
            assertTrue("requestAllReadings request failed.", success);
            success = latch1.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestAllReadings failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestAllReadings interrupted.");
        }

        mockPowerMeter2.unregisterMeter();
        mockPowerMeter2 = null;

        assertTrue("requestAllReadings timed out in " + SERVICE_TIMEOUT_MS + " ms.", success);
        assertTrue("requestAllReadings returned an error.", callbackBoolean1);
        assertNotNull("requestAllReadings returned null bundle list.", callbackBundleList1);
        assertEquals("requestAllReadings returned wrong size bundle.", 2, callbackBundleList1.size());

        /* unpack
         * Unfortnuately, packing is done privately in Meter parent class,
         * so the only way to get and test for packed data is after we request it
         * from the service through requestReading
         */
        String dataType = null;
        boolean firstMeterFound = false;
        boolean secondMeterFound = false;
        for (Bundle bundle : callbackBundleList1) {
            dataType = CollectionServiceAPI.getDataType(bundle);
            assertNotNull("requestReading had a null bundle item", dataType);
            if (dataType != null && dataType.equals(MockPowerReading1.class.getCanonicalName())) {
                MockPowerReading1 unpackedReading = (MockPowerReading1) CollectionServiceAPI.unpack(bundle);
                assertNotNull("Unpacking MockPowerReading1 bundle resulted in a null object.", unpackedReading);
                if (unpackedReading != null) {
                    assertEquals("MockPowerReading1 version is wrong", MockPowerReading1.MOCK_POWER_READING_1_VERSION, unpackedReading.getVersion());
                    assertEquals("unpackedReading.mockDouble is corrupt.", mockDoubleVal, unpackedReading.mockDouble);
                    assertEquals("unpackedReading.mockInt is corrupt.", mockIntVal, unpackedReading.mockInt);
                    assertEquals("unpackedReading.mockString is corrupt.", mockStringVal, unpackedReading.mockString);
                }
                firstMeterFound = true;
            } else if (dataType != null && dataType.equals(MockPowerReading2.class.getCanonicalName())) {
                MockPowerReading2 unpackedReading2 = (MockPowerReading2) CollectionServiceAPI.unpack(bundle);
                assertNotNull("Unpacking MockPowerReading2 resulted in a null object.", unpackedReading2);
                if (unpackedReading2 != null) {
                    assertEquals("MockPowerReading2 version is wrong", MockPowerReading2.MOCK_POWER_READING_2_VERSION, unpackedReading2.getVersion());
                    assertEquals("unpackedReading2.mockCharVal is corrupt.", mockCharVal, unpackedReading2.mockChar);
                    assertEquals("unpackedReading2.mockFloatVal is corrupt.", mockFloatVal, unpackedReading2.mockFloat);
                    assertEquals("unpackedReading2.mockBooleanVal is corrupt.", mockBooleanVal, unpackedReading2.mockBoolean);
                }
                secondMeterFound = true;
            } else {
                fail("Unknown dataType in bundle list! - " + dataType);
            }
        }
        assertTrue("MockPowerReading1 not found!", firstMeterFound);
        assertTrue("MockPowerReading2 not found!", secondMeterFound);
    }

    /* tests CollectionService.requestReading() on a non-responsive meter */
    public void testRequestReading_sleepyMeter() {
        sleepyPowerMeter = new SleepyPowerMeter(mContextMeter);
        assertNotNull("Failed to create SleepyPowerMeter", sleepyPowerMeter);
        /* make sure the meter is actually connected/bound before we move on */
        sleepyRegisterLatch = new CountDownLatch(1);
        boolean sleepyRegisterSuccess = false;
        assertTrue("Failed to register SleepyPowerMeter", sleepyPowerMeter.registerMeter(SleepyPowerReading.class));
        try {
            sleepyRegisterSuccess = sleepyRegisterLatch.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        assertTrue("SleepyPowerMeter registration timed out in " + SERVICE_TIMEOUT_MS + " ms.", sleepyRegisterSuccess);

        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);

        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success1 = false;

        ArrayList<String> meter1 = new ArrayList<String>();
        meter1.add(SleepyPowerReading.class.getCanonicalName());

        try {
            success1 = collectionService1.requestReading(meter1, clientCallback1);
        } catch (RemoteException e1) {
            fail("requestReading failed for SleepyPowerMeter due to RemoteException: " + e1.toString());
        }
        assertTrue("requestReading request for SleepyPowerMeter failed.", success1);

        try {
            /* wait for at least twice the READ_METER timeout value */
            success1 = latch1.await(MeterReader.getReadMeterExpireTimeMs() * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("requestReading for SleepyPowerMeter interrupted.");
        }

        sleepyPowerMeter.unregisterMeter();
        sleepyPowerMeter = null;

        assertTrue("requestReading for SleepyMeter timed out.", success1);

        assertTrue("requestReading for SleepyMeter returned an error.", callbackBoolean1);

        assertNotNull("requestReading for SleepyMeter returned null bundle list.", callbackBundleList1);
        assertEquals("requestReading for SleepyMeter returned wrong size bundle.", 1, callbackBundleList1.size());
        callbackBundle1 = callbackBundleList1.get(0);
        assertNotNull("requestReading for SleepyMeter returned null bundle.", callbackBundle1);

        /* we're expected to get a bundle with the original data type specified, but the rest of the value is -1/null */
        assertEquals(SleepyPowerReading.class.getCanonicalName(), CollectionServiceAPI.getDataType(callbackBundle1));
        assertEquals(-1,CollectionServiceAPI.getTotalUwh(callbackBundle1));
        assertEquals(-1,CollectionServiceAPI.getTimestamp(callbackBundle1));
        assertNull(CollectionServiceAPI.getRawData(callbackBundle1));
        SleepyPowerReading sleepyReading = (SleepyPowerReading) CollectionServiceAPI.unpack(callbackBundle1);
        assertNull(sleepyReading);
    }

    /* tests CollectionService.requestAllReadings() with a non-responsive meter mixed in with regular meter */
    public void testRequestAllReadings_withSleepyMeter() {
        sleepyPowerMeter = new SleepyPowerMeter(mContextMeter);
        assertNotNull("Failed to create SleepyPowerMeter", sleepyPowerMeter);
        /* make sure the meter is actually connected/bound before we move on */
        sleepyRegisterLatch = new CountDownLatch(1);
        boolean sleepyRegisterSuccess = false;
        assertTrue("Failed to register SleepyPowerMeter", sleepyPowerMeter.registerMeter(SleepyPowerReading.class));
        try {
            sleepyRegisterSuccess = sleepyRegisterLatch.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        assertTrue("SleepyPowerMeter registration timed out in " + SERVICE_TIMEOUT_MS + " ms.", sleepyRegisterSuccess);

        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);
        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success = false;
        try {
            success = collectionService1.requestAllReadings(clientCallback1);
            assertTrue("requestAllReadings request failed.", success);
            /* wait for at least twice the READ_METER timeout value */
            success = latch1.await(MeterReader.getReadMeterExpireTimeMs() * 2, TimeUnit.MILLISECONDS);
        } catch (RemoteException e1) {
            fail("requestAllReadings failed due to RemoteException: " + e1.toString());
        } catch (InterruptedException e) {
            fail("requestAllReadings interrupted.");
        }

        sleepyPowerMeter.unregisterMeter();
        sleepyPowerMeter = null;

        assertTrue("requestAllReadings timed out in.", success);
        assertTrue("requestAllReadings returned an error.", callbackBoolean1);
        assertNotNull("requestAllReadings returned null bundle list.", callbackBundleList1);
        assertEquals("requestAllReadings returned wrong size bundle.", 2, callbackBundleList1.size());

        /* unpack
         * Unfortnuately, packing is done privately in Meter parent class,
         * so the only way to get and test for packed data is after we request it
         * from the service through requestReading
         */
        String dataType = null;
        boolean firstMeterFound = false;
        boolean sleepyMeterFound = false;
        for (Bundle bundle : callbackBundleList1) {
            dataType = CollectionServiceAPI.getDataType(bundle);
            assertNotNull("requestReading had a null bundle item", dataType);
            if (dataType != null && dataType.equals(MockPowerReading1.class.getCanonicalName())) {
                MockPowerReading1 unpackedReading = (MockPowerReading1) CollectionServiceAPI.unpack(bundle);
                assertNotNull("Unpacking MockPowerReading1 bundle resulted in a null object.", unpackedReading);
                if (unpackedReading != null) {
                    assertEquals("MockPowerReading1 version is wrong", MockPowerReading1.MOCK_POWER_READING_1_VERSION, unpackedReading.getVersion());
                    assertEquals("unpackedReading.mockDouble is corrupt.", mockDoubleVal, unpackedReading.mockDouble);
                    assertEquals("unpackedReading.mockInt is corrupt.", mockIntVal, unpackedReading.mockInt);
                    assertEquals("unpackedReading.mockString is corrupt.", mockStringVal, unpackedReading.mockString);
                }
                firstMeterFound = true;
            } else if (dataType != null && dataType.equals(SleepyPowerReading.class.getCanonicalName())) {
                /* we're expected to get a bundle with the original data type specified, but the rest of the value is -1/null */
                assertEquals(-1,CollectionServiceAPI.getTotalUwh(bundle));
                assertEquals(-1,CollectionServiceAPI.getTimestamp(bundle));
                assertNull(CollectionServiceAPI.getRawData(bundle));
                SleepyPowerReading sleepyReading = (SleepyPowerReading) CollectionServiceAPI.unpack(bundle);
                assertNull(sleepyReading);
                sleepyMeterFound = true;
            } else {
                fail("Unknown dataType in bundle list! - " + dataType);
            }
        }
        assertTrue("MockPowerReading1 not found!", firstMeterFound);
        assertTrue("SleepyPowerReading not found!", sleepyMeterFound);
    }

    /* tests consecutive read of CollectionService.requestReading() on a non-responsive meter */
    public void testRequestReading_sleepyMeterConsecutive() {
        sleepyPowerMeter = new SleepyPowerMeter(mContextMeter);
        assertNotNull("Failed to create SleepyPowerMeter", sleepyPowerMeter);
        /* make sure the meter is actually connected/bound before we move on */
        sleepyRegisterLatch = new CountDownLatch(1);
        boolean sleepyRegisterSuccess = false;
        assertTrue("Failed to register SleepyPowerMeter", sleepyPowerMeter.registerMeter(SleepyPowerReading.class));
        try {
            sleepyRegisterSuccess = sleepyRegisterLatch.await(SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        assertTrue("SleepyPowerMeter registration timed out in " + SERVICE_TIMEOUT_MS + " ms.", sleepyRegisterSuccess);

        bindCollectionService();

        /* requestReading */
        latch1 = new CountDownLatch(1);

        clientCallback1 = new Callback_Client.Stub() {
            public void readingCallback(List<Bundle> readings) {
                callbackBoolean1 = true;
                if (readings != null) {
                    callbackBundleList1 = new ArrayList<Bundle>(readings);
                }
                latch1.countDown();
            }
            public void readingErrorCallback(List<String> meterTypes) {
                callbackBoolean1 = false;
                latch1.countDown();
            }
            public void availableMetersCallback(List<String> meterTypes) {}
        };

        callbackBoolean1 = false;
        boolean success1 = false;

        ArrayList<String> meter1 = new ArrayList<String>();
        meter1.add(SleepyPowerReading.class.getCanonicalName());

        try {
            success1 = collectionService1.requestReading(meter1, clientCallback1);
        } catch (RemoteException e1) {
            fail("requestReading failed for SleepyPowerMeter due to RemoteException: " + e1.toString());
        }
        assertTrue("requestReading request for SleepyPowerMeter failed.", success1);

        try {
            /* wait for at least twice the READ_METER timeout value */
            success1 = latch1.await(MeterReader.getReadMeterExpireTimeMs() * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("requestReading for SleepyPowerMeter interrupted.");
        }

        assertTrue("requestReading for SleepyMeter timed out.", success1);

        assertTrue("requestReading for SleepyMeter returned an error.", callbackBoolean1);

        assertNotNull("requestReading for SleepyMeter returned null bundle list.", callbackBundleList1);
        assertEquals("requestReading for SleepyMeter returned wrong size bundle.", 1, callbackBundleList1.size());
        callbackBundle1 = callbackBundleList1.get(0);
        assertNotNull("requestReading for SleepyMeter returned null bundle.", callbackBundle1);

        /* we're expected to get a bundle with the original data type specified, but the rest of the value is -1/null */
        assertEquals(SleepyPowerReading.class.getCanonicalName(), CollectionServiceAPI.getDataType(callbackBundle1));
        assertEquals(-1,CollectionServiceAPI.getTotalUwh(callbackBundle1));
        assertEquals(-1,CollectionServiceAPI.getTimestamp(callbackBundle1));
        assertNull(CollectionServiceAPI.getRawData(callbackBundle1));
        SleepyPowerReading sleepyReading = (SleepyPowerReading) CollectionServiceAPI.unpack(callbackBundle1);
        assertNull(sleepyReading);

        /* Round 2 of read */
        latch1 = new CountDownLatch(1);
        try {
            success1 = collectionService1.requestReading(meter1, clientCallback1);
        } catch (RemoteException e1) {
            fail("requestReading 2nd failed for SleepyPowerMeter due to RemoteException: " + e1.toString());
        }
        assertTrue("requestReading 2nd request for SleepyPowerMeter failed.", success1);

        try {
            /* wait for at least twice the READ_METER timeout value */
            success1 = latch1.await(MeterReader.getReadMeterExpireTimeMs() * 2, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("requestReading 2nd for SleepyPowerMeter interrupted.");
        }

        sleepyPowerMeter.unregisterMeter();
        sleepyPowerMeter = null;

        assertTrue("requestReading 2nd for SleepyMeter timed out.", success1);

        assertTrue("requestReading 2nd for SleepyMeter returned an error.", callbackBoolean1);

        assertNotNull("requestReading 2nd for SleepyMeter returned null bundle list.", callbackBundleList1);
        assertEquals("requestReading 2nd for SleepyMeter returned wrong size bundle.", 1, callbackBundleList1.size());
        callbackBundle1 = callbackBundleList1.get(0);
        assertNotNull("requestReading 2nd for SleepyMeter returned null bundle.", callbackBundle1);

        /* the meter from the previous round should return here, while the 2nd one is asleep.
         * we'll gladly take the result from the previous round, since it's meant for the same client (defined by the callback param)
         * and since it has a timestamp it won't be a problem. the 2nd one will just be lost, which is expected
         * since the meter is unreponsive (or very slow)
         */
        assertEquals(SleepyPowerReading.class.getCanonicalName(), CollectionServiceAPI.getDataType(callbackBundle1));
        assertEquals(mockIntVal,CollectionServiceAPI.getTotalUwh(callbackBundle1));
        assertTrue(CollectionServiceAPI.getTimestamp(callbackBundle1) > 0);
        assertNotNull(CollectionServiceAPI.getRawData(callbackBundle1));
        sleepyReading = (SleepyPowerReading) CollectionServiceAPI.unpack(callbackBundle1);
        assertNotNull(sleepyReading);
        if (sleepyReading != null) {
            assertEquals(sleepyReading.sleepyInt,mockIntVal);
        }
    }

    private class MeterReaderContext extends MockContext {

        /* we can't use handler here, since handler will be created in the same thread
         * as this test. Handlers are attached to the thread that created it.
         */
        private class BindTask implements Runnable {
            String intentAction;
            ServiceConnection serviceConnection;
            IBinder binder;

            public BindTask(String newAction, ServiceConnection conn, IBinder newBinder) {
                intentAction = newAction;
                serviceConnection = conn;
                binder = newBinder;
            }

            public void run() {
                Log.d(TAG,"MockContext.onServiceConnected - " + intentAction);
                serviceConnection.onServiceConnected(new ComponentName(TEST_PACKAGE_NAME, TEST_CLASS_NAME), binder);
            }
        }

        @Override
        public boolean bindService(Intent intent, ServiceConnection conn, int flags) {
            String action = intent.getAction();
            /* to simulate async binding to service, spawn a new thread and call onServiceConnected from there
             */
            BindTask bindTask = new BindTask(action, conn, MeterReaderTest.this.bindService(intent));
            Log.d(TAG,"MockContext.bindService(" + action + ")");
            (new Thread(bindTask)).start();
            return true;
        }

        @Override
        public void unbindService(ServiceConnection conn) {
            //nothing to do
        }

    }

    private class MockPowerMeter1 extends PowerMeter {

        public MockPowerMeter1(Context context) {
            super(context);
        }

        @Override
        protected void onMeterReaderConnected() {
            if (meter1RegisterLatch != null) {
                meter1RegisterLatch.countDown();
            }
        }

        @Override
        protected MeterReading getMeterReading() {
            MockPowerReading1 mockReading = new MockPowerReading1();

            mockReading.mockInt = mockIntVal;
            mockReading.mockString = mockStringVal;
            mockReading.mockDouble = mockDoubleVal;

            return mockReading;
        }
    }

    private class MockPowerMeter2 extends PowerMeter {

        public MockPowerMeter2(Context context) {
            super(context);
        }

        @Override
        protected void onMeterReaderConnected() {
            if (meter2RegisterLatch != null) {
                meter2RegisterLatch.countDown();
            }
        }

        @Override
        protected MeterReading getMeterReading() {
            MockPowerReading2 mockReading = new MockPowerReading2();

            mockReading.mockBoolean = mockBooleanVal;
            mockReading.mockChar = mockCharVal;
            mockReading.mockFloat = mockFloatVal;

            return mockReading;
        }
    }

    private class SleepyPowerMeter extends PowerMeter {

        public SleepyPowerMeter(Context context) {
            super(context);
        }

        @Override
        protected void onMeterReaderConnected() {
            if (sleepyRegisterLatch != null) {
                sleepyRegisterLatch.countDown();
            }
        }

        @Override
        protected MeterReading getMeterReading() {
            SleepyPowerReading sleepyReading = new SleepyPowerReading();

            /* sleep enough to trigger the READ_METER_TIME_EXPIRED event on MeterReader */
            long sleepTime = MeterReader.getReadMeterExpireTimeMs() * 3 / 2;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sleepyReading.totalUwh = mockIntVal;
            sleepyReading.sleepyInt = mockIntVal;

            return sleepyReading;
        }
    }
}
