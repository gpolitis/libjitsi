/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.impl.neomedia.transform;

import org.jitsi.impl.neomedia.*;
import org.jitsi.util.*;

/**
 * Extends the <tt>PacketTransformer</tt> interface with methods which allow
 * the transformation of a single packet into a single packet.
 *
 * Eases the implementation of <tt>PacketTransformer<tt>-s which transform each
 * packet into a single transformed packet (as opposed to an array of possibly
 * more than one packet).
 *
 * @author Boris Grozev
 */
public abstract class SinglePacketTransformer
    implements PacketTransformer
{
    /**
     * The number of <tt>Throwable</tt>s to log with a single call to
     * <tt>logger</tt>. If every <tt>Throwable</tt> is logged in either of
     * {@link #reverseTransform(RawPacket)} and {@link #transform(RawPacket)},
     * the logging may be overwhelming.
     */
    private static final int EXCEPTIONS_TO_LOG = 1000;

    /**
     * The <tt>Logger</tt> used by the <tt>SinglePacketTransformer</tt> class
     * and its instances to print debug information.
     */
    private static final Logger logger
        = Logger.getLogger(SinglePacketTransformer.class);

    /**
     * The number of exceptions caught in {@link #reverseTransform(RawPacket)}.
     */
    private long exceptionsInReverseTransform;

    /**
     * The number of exceptions caught in {@link #transform(RawPacket)}.
     */
    private long exceptionsInTransform;

    /**
     * Transforms a specific packet.
     *
     * @param pkt the packet to be transformed.
     * @return the transformed packet.
     */
    public abstract RawPacket transform(RawPacket pkt);

    /**
     * Reverse-transforms a specific packet.
     *
     * @param pkt the transformed packet to be restored.
     * @return the restored packet.
     */
    public abstract RawPacket reverseTransform(RawPacket pkt);

    /**
     * {@inheritDoc}
     *
     * Transforms an array of packets by calling <tt>transform(RawPacket)</tt>
     * on each one.
     */
    @Override
    public RawPacket[] transform(RawPacket[] pkts)
    {
        if (pkts != null)
        {
            for (int i = 0; i < pkts.length; i++)
            {
                RawPacket pkt = pkts[i];

                if (pkt != null)
                {
                    try
                    {
                        pkts[i] = transform(pkt);
                    }
                    catch (Throwable t)
                    {
                        exceptionsInTransform++;
                        if (((exceptionsInTransform % EXCEPTIONS_TO_LOG) == 0)
                                || (exceptionsInTransform == 1))
                        {
                            logger.error(
                                    "Failed to transform RawPacket(s)!",
                                    t);
                        }
                        if (t instanceof Error)
                            throw (Error) t;
                        else if (t instanceof RuntimeException)
                            throw (RuntimeException) t;
                        else
                            throw new RuntimeException(t);
                    }
                }
            }
        }

        return pkts;
    }

    /**
     * {@inheritDoc}
     * Reverse-transforms an array of packets by calling
     * <tt>reverseTransform(RawPacket)</tt> on each one.
     */
    @Override
    public RawPacket[] reverseTransform(RawPacket[] pkts)
    {
        if (pkts != null)
        {
            for (int i = 0; i < pkts.length; i++)
            {
                RawPacket pkt = pkts[i];

                if (pkt != null)
                {
                    try
                    {
                        pkts[i] = reverseTransform(pkt);
                    }
                    catch (Throwable t)
                    {
                        exceptionsInReverseTransform++;
                        if (((exceptionsInReverseTransform % EXCEPTIONS_TO_LOG)
                                    == 0)
                                || (exceptionsInReverseTransform == 1))
                        {
                            logger.error(
                                    "Failed to reverse-transform RawPacket(s)!",
                                    t);
                        }
                        if (t instanceof Error)
                            throw (Error) t;
                        else if (t instanceof RuntimeException)
                            throw (RuntimeException) t;
                        else
                            throw new RuntimeException(t);
                    }
                }
            }
        }

        return pkts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
    }
}
