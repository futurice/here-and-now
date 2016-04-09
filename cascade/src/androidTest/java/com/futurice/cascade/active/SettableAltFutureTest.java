package com.futurice.cascade.active;

import android.support.annotation.CallSuper;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.futurice.cascade.Async;
import com.futurice.cascade.AsyncAndroidTestCase;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.*;

@SmallTest
public class SettableAltFutureTest extends AsyncAndroidTestCase {

    @Before
    @CallSuper
    public void setUp() throws Exception {
        super.setUp();
    }

    @Ignore
    @Test
    public void testCancel() throws Exception {
        SettableAltFuture<?, Integer> settableAltFuture = new SettableAltFuture<>(Async.WORKER);
        assertTrue(settableAltFuture.cancel("Just because"));
        assertTrue(settableAltFuture.isCancelled());
        assertEquals(null, settableAltFuture.safeGet());
        try {
            settableAltFuture.get();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Just because"));
        }
    }

    @Test
    public void testCancel1() throws Exception {

    }

    @Test
    public void testIsCancelled() throws Exception {

    }

    @Test
    public void testIsCancelled1() throws Exception {

    }

    @Test
    public void testIsDone() throws Exception {

    }

    @Test
    public void testIsDone1() throws Exception {

    }

    @Test
    public void testIsConsumed() throws Exception {

    }

    @Test
    public void testIsConsumed1() throws Exception {

    }

    @Test
    public void testIsForked() throws Exception {

    }

    @Test
    public void testIsForked1() throws Exception {

    }

    @Test
    public void testFork() throws Exception {

    }

    @Test
    public void testDoFork() throws Exception {

    }

    @Test
    public void testSetPreviousAltFuture() throws Exception {

    }

    @Test
    public void testClearPreviousAltFuture() throws Exception {

    }

    @Test
    public void testGetPreviousAltFuture() throws Exception {

    }

    @Test
    public void testAssertNotDone() throws Exception {

    }

    @Test
    public void testGet() throws Exception {

    }

    @Test
    public void testSafeGet() throws Exception {

    }

    @Test
    public void testGetThreadType() throws Exception {

    }

    @Test
    public void testSet() throws Exception {

    }

    @Test
    public void testCompareAndSet() throws Exception {

    }

    @Test
    public void testDoThenOnCancelled() throws Exception {

    }

    @Test
    public void testDoThenOnError() throws Exception {

    }

    @Test
    public void testOnError() throws Exception {

    }

    @Test
    public void testDoThenActions() throws Exception {

    }

    @Test
    public void testSplit() throws Exception {

    }

    @Test
    public void testThen() throws Exception {

    }

    @Test
    public void testThen1() throws Exception {

    }

    @Test
    public void testThen2() throws Exception {

    }

    @Test
    public void testThen3() throws Exception {

    }

    @Test
    public void testThen4() throws Exception {

    }

    @Test
    public void testThen5() throws Exception {

    }

    @Test
    public void testThen6() throws Exception {

    }

    @Test
    public void testThen7() throws Exception {

    }

    @Test
    public void testThen8() throws Exception {

    }

    @Test
    public void testMap() throws Exception {

    }

    @Test
    public void testMap1() throws Exception {

    }

    @Test
    public void testFilter() throws Exception {

    }

    @Test
    public void testFilter1() throws Exception {

    }

    @Test
    public void testSet1() throws Exception {

    }

    @Test
    public void testSet2() throws Exception {

    }
}