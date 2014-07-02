/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.service.neomedia;

import net.sf.fmj.media.rtp.*;

/**
 * Created by gp on 7/1/14.
 */
public interface RTCPTerminationStrategy
{
    /**
     *
     * @return
     */
    RTCPPacketTransformer getRTCPPacketTransformer();

    /**
     *
     * @return
     */
    RTCPReportBuilder getRTCPReportBuilder();
}
