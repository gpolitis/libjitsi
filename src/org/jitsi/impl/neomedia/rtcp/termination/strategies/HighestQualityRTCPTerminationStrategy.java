/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.neomedia.rtcp.termination.strategies;

import net.sf.fmj.media.rtp.*;
import org.jitsi.impl.neomedia.rtcp.*;
import org.jitsi.service.neomedia.*;

import java.util.*;

/**
 * Created by gp on 7/4/14.
 */
public class HighestQualityRTCPTerminationStrategy
        implements RTCPTerminationStrategy, RTCPPacketTransformer
{
    /**
     *
     */
    private final RTCPReportBuilder reportBuilder =
            new NullRTCPReportBuilderImpl();

    /**
     *
     */
    private final MyRTCPReportBlockComparator comparator
            = new MyRTCPReportBlockComparator();

    /**
     *
     */
    private final HashMap<Long, MySSRCStats> myStatsMap = new HashMap();

    /**
     *
     */
    private final HashMap<Long, MyREMBStats> myRembsMap = new HashMap();

    /**
     *
     */
    class MyRTCPReportBlockComparator implements
            Comparator<RTCPReportBlock>
    {
        @Override
        public int compare(RTCPReportBlock o1, RTCPReportBlock o2)
        {
            if (o1 == null)
                throw new IllegalArgumentException("o1");

            if (o2 == null)
                throw new IllegalArgumentException("o2");

            if (o1.getSSRC() != o2.getSSRC())
                throw new IllegalArgumentException("o1.ssrc != o2.ssrc");

            // The compare method compares its two arguments, returning a
            // negative integer, 0, or a positive integer depending on whether
            // the first argument is less than, equal to, or greater than the
            // second.
            //
            // here we ask, o1 < o2 iff o2 is of higher quality than o1

            // TODO(gp) need a more elaborate ranking algorithm.
            int fractionDiff = o2.getFractionLost() - o1.getFractionLost();
            long lostDiff = o2.getNumLost() - o1.getNumLost();
            long jitterDiff = o2.getJitter() - o1.getJitter();

            return fractionDiff;
        }
    }

    /**
     *
     */
    class MySSRCStats
    {
        Date received;
        RTCPReportBlock[] reports;
    }

    class MyREMBStats
    {
        Date received;
        RTCPREMBPacket packet;
    }

    @Override
    public RTCPPacketTransformer getRTCPPacketTransformer()
    {
        return this;
    }

    @Override
    public RTCPReportBuilder getRTCPReportBuilder()
    {
        return reportBuilder;
    }

    @Override
    public RTCPCompoundPacket transformRTCPPacket(
            RTCPCompoundPacket inPacket)
    {
        if (inPacket == null
                || inPacket.packets == null || inPacket.packets.length == 0)
        {
            return inPacket;
        }

        Vector<RTCPPacket> outPackets = new Vector<RTCPPacket>();

        for (RTCPPacket p : inPacket.packets)
        {
            switch (p.type)
            {
                case RTCPPacketType.RR:
                    // Don't add the RR in the outPacket as we mute RRs from the
                    // peers. But update the stats.

                    RTCPRRPacket rr = (RTCPRRPacket) p;
                    outPackets.add(rr);
                    if (rr.reports != null && rr.reports.length != 0)
                    {
                        MySSRCStats stats = new MySSRCStats();
                        stats.received = new Date();
                        stats.reports = rr.reports;

                        synchronized (myStatsMap)
                        {
                            myStatsMap.put((long) rr.ssrc, stats);

                            // see if we have better stats for all the report
                            // blocks in this RR
                            for (RTCPReportBlock block1 : rr.reports)
                            {
                                Iterator it = myStatsMap.entrySet().iterator();
                                while (it.hasNext())
                                {
                                    Map.Entry<Long, MySSRCStats> mapEntry =
                                            (Map.Entry<Long, MySSRCStats>) it.next();

                                    MySSRCStats value = mapEntry.getValue();
                                    if (value.reports != null && value.reports.length != 0)
                                    {
                                        // stats received from some other
                                        // endpoint
                                        for (RTCPReportBlock block2 : value.reports)
                                        {
                                            if (block1.getSSRC() == block2.getSSRC())
                                            {
                                                if (comparator.compare(block1, block2) < 0)
                                                {
                                                    block1.copyFrom(block2);
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }

                    break;
                case RTCPPacketType.SR:
                    RTCPSRPacket sr = (RTCPSRPacket) p;
                    outPackets.add(sr);

                    if (sr.reports != null && sr.reports.length != 0)
                    {
                        MySSRCStats stats = new MySSRCStats();
                        stats.received = new Date();
                        stats.reports = sr.reports;

                        synchronized (myStatsMap)
                        {
                            myStatsMap.put((long) sr.ssrc, stats);

                            // see if we have better stats for all the report
                            // blocks in this SR
                            for (RTCPReportBlock block1 : sr.reports)
                            {
                                Iterator it = myStatsMap.entrySet().iterator();
                                while (it.hasNext())
                                {
                                    Map.Entry<Long, MySSRCStats> mapEntry =
                                            (Map.Entry<Long, MySSRCStats>) it.next();

                                    MySSRCStats value = mapEntry.getValue();
                                    if (value.reports != null && value.reports.length != 0)
                                    {
                                        // stats received from some other
                                        // endpoint
                                        for (RTCPReportBlock block2 : value.reports)
                                        {
                                            if (block1.getSSRC() == block2.getSSRC())
                                            {
                                                if (comparator.compare(block1, block2) < 0)
                                                {
                                                    block1.copyFrom(block2);
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                    break;
                case RTCPPacketType.PSFB:
                    RTCPFBPacket psfb = (RTCPFBPacket) p;
                    switch (psfb.fmt)
                    {
                        case RTCPPSFBFormat.REMB:
                            RTCPREMBPacket remb = (RTCPREMBPacket) p;
                            outPackets.add(remb);

                            MyREMBStats stats = new MyREMBStats();
                            stats.received = new Date();
                            stats.packet = remb;
                            synchronized (myRembsMap)
                            {
                                myRembsMap.put(remb.senderSSRC, stats);

                                // see if we have better (mantissa, exp) combo
                                Iterator it = myRembsMap.entrySet().iterator();
                                while (it.hasNext())
                                {
                                    Map.Entry<Long, MyREMBStats> mapEntry =
                                            (Map.Entry<Long, MyREMBStats>) it.next();

                                    MyREMBStats value = mapEntry.getValue();
                                    double val = value.packet.mantissa * Math.pow(2, value.packet.exp);
                                    int mantissa = value.packet.mantissa;
                                    int exp = value.packet.exp;
                                    double newval = mantissa * Math.pow(2, exp);
                                    if (newval > val)
                                    {
                                        remb.mantissa = mantissa;
                                        remb.exp = exp;
                                    }
                                }
                            }
                            break;
                        default:
                            // Pass through everything else, like PLIs and NACKs
                            outPackets.add(psfb);
                            break;
                    }
                    break;
                default:
                    // Pass through everything else, like PLIs and NACKs
                    outPackets.add(p);
                    break;
            }
        }

        RTCPPacket[] outarr = new RTCPPacket[outPackets.size()];
        outPackets.copyInto(outarr);

        RTCPCompoundPacket outPacket = new RTCPCompoundPacket(outarr);

        return outPacket;
    }
}
