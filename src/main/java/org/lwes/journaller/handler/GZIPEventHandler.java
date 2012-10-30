package org.lwes.journaller.handler;

/**
 * User: fmaritato
 * Date: 9/22/2010
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lwes.journaller.JournallerConstants;
import org.lwes.journaller.util.EventHandlerUtil;
import org.lwes.listener.DatagramQueueElement;

/**
 * Write events to GzipOuputStream.
 */
public class GZIPEventHandler extends AbstractFileEventHandler {

    private transient Log log = LogFactory.getLog(GZIPEventHandler.class);

    private GZIPOutputStream out = null;
    private GZIPOutputStream tmp = null;
    private GZIPOutputStream old = null;

    /**
     * Create the Event handler and open the output stream to the file.
     *
     * @throws IOException if there is a problem opening the file for writing.
     */
    public GZIPEventHandler(String filename, String filePattern) throws IOException {
        setFilename(filename);
        setFilenamePattern(filePattern);
        createOutputStream(filename);
        swapOutputStream();
    }

    /**
     * Convenience method to open a GZIPOutputStream.
     *
     * @throws IOException if there is a problem opening a handle to the file.
     */
    public void createOutputStream(String filename) throws IOException {
        tmp = new GZIPOutputStream(new FileOutputStream(filename, false));
        if (log.isDebugEnabled()) {
            log.debug("Created a new log file: " + getFilename());
        }
    }

    /**
     * @return The gzip file extension: ".gz"
     */
    public String getFileExtension() {
        return ".gz";
    }

    /**
     * This version of handleEvent is the one actually called by the journaller.
     *
     * @param element DatagramQueueElement containing the serialized bytes of the event.
     * @throws IOException
     */
    public void handleEvent(DatagramQueueElement element) throws IOException {
        DatagramPacket packet = element.getPacket();
        if (!isJournallerEvent(packet.getData())) {
            ByteBuffer b = ByteBuffer.allocate(JournallerConstants.MAX_HEADER_SIZE);
            EventHandlerUtil.writeHeader(packet.getLength(),
                                         element.getTimestamp(),
                                         packet.getAddress(),
                                         packet.getPort(),
                                         getSiteId(),
                                         b);
            synchronized (lock) {
                if (out != null) {
                    incrNumEvents();
                    out.write(b.array(), 0, JournallerConstants.MAX_HEADER_SIZE);
                    out.write(packet.getData());
                    out.flush();
                }
            }
        }
    }

    public void swapOutputStream() {
        old = out;
        out = tmp;
        tmp = null;
    }

    public void closeOutputStream() throws IOException {
        if (old != null) {
            old.flush();
            old.close();
            old = null;
        }
    }

    public ObjectName getObjectName() throws MalformedObjectNameException {
        return new ObjectName("org.lwes:name=GZIPEventHandler");
    }
}
