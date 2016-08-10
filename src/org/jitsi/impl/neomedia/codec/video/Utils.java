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

package org.jitsi.impl.neomedia.codec.video;

import org.jitsi.impl.neomedia.codec.*;
import org.jitsi.impl.neomedia.*;
import org.jitsi.service.neomedia.codec.Constants;
import org.jitsi.util.*;

/**
 * This class contains utility methods for video codecs.
 *
 * @author George Politis
 */
public class Utils
{
  /**
   * The {@link Logger} used by the {@link Utils} class for logging output.
   */
  private static final Logger logger = Logger.getLogger(Utils.class);


  private static org.jitsi.impl.neomedia.codec.video.h264.DePacketizer
      h264DePacketizer =
          new org.jitsi.impl.neomedia.codec.video.h264.DePacketizer();

  private static org.jitsi.impl.neomedia.codec.video.vp8.DePacketizer
      vp8DePacketizer =
          new org.jitsi.impl.neomedia.codec.video.vp8.DePacketizer();

  /**
   * Utility method that determines whether or not a packet is a key frame.
   *
   * @param pkt The <tt>RawPacket</tt> to determine whether it is a key frame
   * or not.
   * @param codecPT The codec payload type
   * @param redPT The RED payload type.
   * @return true if the packet is a key frame, false otherwise.
   */
  public static boolean isKeyFrame(RawPacket pkt, Byte redPT, Byte codecPT,
      String targetCodec)
  {
      if (pkt == null)
      {
          return false;
      }
      if(isKeyFrame(pkt.getBuffer(), pkt.getOffset(), pkt.getLength(),
            redPT, codecPT, targetCodec))
      {
          return true;
      }
      return false;
  }

  /**
   * Utility method that determines whether or not a packet is a key frame.
   *
   * @param buf the buffer that holds the RTP payload.
   * @param off the offset in the buff where the RTP payload is found.
   * @param len then length of the RTP payload in the buffer.
   * @param redPT The RED payload type.
   * @param codecPT The codec payload type.
   * @param targetCodec The targetCodec used
   * @return true if the packet is a key frame, false otherwise.
   */
  public static boolean isKeyFrame(
      byte[] buf, int off, int len, Byte redPT, Byte codecPT, String targetCodec)
  {
    if (buf == null || buf.length < off + Math.max(len, 1) || codecPT == null)
    {
        return false;
    }

    boolean isKeyFrame = false;
    try {
        // XXX this will not work correctly when RTX gets enabled!
        if (redPT != null && redPT == RawPacket.getPayloadType(buf, off, len))
        {
            REDBlock block = REDBlockIterator
                .getPrimaryBlock(buf,
                    RawPacket.getPayloadOffset(buf, off, len),
                    RawPacket.getPayloadLength(buf, off, len));

            if (block != null && codecPT == block.getPayloadType())
            {
                if (targetCodec.equalsIgnoreCase(Constants.VP8))
                {
                    isKeyFrame
                        = vp8DePacketizer.isKeyFrame(
                        buf,
                        block.getOffset(),
                        block.getLength());
                }
                else if (targetCodec.equalsIgnoreCase(Constants.H264))
                {
                    isKeyFrame
                        = h264DePacketizer.isKeyFrame(
                        buf,
                        block.getOffset(),
                        block.getLength());
                }
                else
                {
                    logger.error("Unsupported Codec type: " + codecPT);
                }
            }
        }
        else if (codecPT == RawPacket.getPayloadType(buf, off, len))
        {
            // XXX There's RawPacket#getPayloadLength() but the implementation
            // includes pkt.paddingSize at the time of this writing and
            // we do
            // not know whether that's going to stay that way.
            if (targetCodec.equalsIgnoreCase(Constants.VP8))
            {
                isKeyFrame
                    = vp8DePacketizer.isKeyFrame(
                    buf,
                    RawPacket.getPayloadOffset(buf, off, len),
                    len
                        - RawPacket.getHeaderLength(buf, off, len)
                        - RawPacket.getPaddingSize(buf, off, len));
            }
            else if (targetCodec.equalsIgnoreCase(Constants.H264))
            {
                isKeyFrame
                    = h264DePacketizer.isKeyFrame(
                    buf,
                    RawPacket.getPayloadOffset(buf, off, len),
                    len);
            }
            else
            {
                logger.error("Unsupported Codec type: " + codecPT);
            }
        }
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
        // While ideally we should check the bounds everywhere and not
        // attempt to access the packet's buffer at invalid indexes, there
        // are too many places where it could inadvertently happen. It's
        // safer to return a default value of 'false' from this utility
        // method than to risk killing a thread which doesn't expect this.
        logger.warn("Caught an exception while checking for keyframe:", e);
        isKeyFrame = false;
    }

    return isKeyFrame;
  }
}
