package com.higherfrequencytrading.chronology.slf4j;

import net.openhft.chronicle.Chronicle;
import net.openhft.chronicle.ExcerptTailer;
import net.openhft.chronicle.VanillaChronicle;
import com.higherfrequencytrading.chronology.slf4j.impl.ChronicleLogWriters;
import net.openhft.lang.io.IOTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * TODO: add test case for text-logegrs
 */
public class VanillaChronicleBinaryLoggerTest extends ChronicleTestBase {

    // *************************************************************************
    //
    // *************************************************************************

    @Before
    public void setUp() {
        System.setProperty(
                "slf4j.chronicle.properties",
                System.getProperty("slf4j.chronicle.vanilla.binary.properties"));

        getChronicleLoggerFactory().relaod();
        getChronicleLoggerFactory().warmup();
    }

    @After
    public void tearDown() {
        getChronicleLoggerFactory().shutdown();

        IOTools.deleteDir(basePath(ChronicleLoggingConfig.TYPE_VANILLA));
    }

    // *************************************************************************
    //
    // *************************************************************************

    @Test
    public void testLoggerFactory() {
        assertEquals(
                StaticLoggerBinder.getSingleton().getLoggerFactory().getClass(),
                ChronicleLoggerFactory.class);
    }

    @Test
    public void testLogger() {
        Logger logger = LoggerFactory.getLogger(VanillaChronicleBinaryLoggerTest.class);

        assertNotNull(logger);
        assertEquals(logger.getClass(), ChronicleLogger.class);

        ChronicleLogger cl = (ChronicleLogger) logger;

        assertEquals(cl.getLevel(), ChronicleLoggingHelper.LOG_LEVEL_DEBUG);
        assertEquals(cl.getName(), VanillaChronicleBinaryLoggerTest.class.getName());
        assertTrue(cl.getWriter() instanceof ChronicleLogWriters.BinaryWriter);
        assertTrue(cl.getWriter().getChronicle() instanceof VanillaChronicle);
    }

    // *************************************************************************
    //
    // *************************************************************************

    @Test
    public void testLogging1() throws IOException {
        String theradName = "th-test-binary-logging";
        String loggerName = VanillaChronicleBinaryLoggerTest.class.getName();

        Thread.currentThread().setName(theradName);

        Logger l = LoggerFactory.getLogger(loggerName);
        l.debug("data {}, {}",
                new MySerializableData("a Serializable object"),
                new MyMarshallableData("a Marshallable object")
        );

        Chronicle reader = getVanillaChronicle(ChronicleLoggingConfig.TYPE_VANILLA, "root-binary");
        ExcerptTailer tailer = reader.createTailer();

        assertTrue(tailer.nextIndex());
        tailer.readLong();
        assertEquals(ChronicleLoggingHelper.LOG_LEVEL_DEBUG, tailer.readByte());
        assertEquals(Thread.currentThread().getId(), tailer.readLong());
        assertEquals(theradName, tailer.readEnum(String.class));
        assertEquals(loggerName, tailer.readEnum(String.class));
        assertEquals("data {}, {}", tailer.readEnum(String.class));

        int nbObjects = tailer.readInt();
        assertEquals(nbObjects, 2);

        Object serializableObject = tailer.readObject();
        assertNotNull(serializableObject);
        assertTrue(serializableObject instanceof MySerializableData);
        assertEquals(serializableObject.toString(), "a Serializable object");

        Object marshallableObject = tailer.readObject();
        assertNotNull(marshallableObject);
        assertTrue(marshallableObject instanceof MyMarshallableData);
        assertEquals(marshallableObject.toString(), "a Marshallable object");

        tailer.close();
        reader.close();
    }
}
