/*
 * Copyright (c) 2014 "Kaazing Corporation," (www.kaazing.com)
 *
 * This file is part of Robot.
 *
 * Robot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kaazing.specification.websocket.functions;

import java.util.logging.Logger;

/**
 * Internal class. This class manages the Base64 encoding and decoding
 */
final class Base64Util {

    private static final String CLASS_NAME = Base64Util.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final byte[] INDEXED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();
    private static final byte PADDING_BYTE = (byte) '=';

    @Deprecated
    private Base64Util() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    public static byte[] encode(byte[] decodedArray) {
        LOG.entering(CLASS_NAME, "encode", decodedArray);

        int decodedSize = decodedArray.length;
        int effectiveDecodedSize = ((decodedSize + 2) / 3) * 3;
        int decodedFragmentSize = decodedSize % 3;

        int encodedArraySize = effectiveDecodedSize / 3 * 4;
        byte[] encodedArray = new byte[encodedArraySize];
        int encodedArrayPosition = 0;

        int decodedArrayOffset = 0;
        int decodedArrayPosition = decodedArrayOffset + 0;
        int decodedArrayLimit = decodedArrayOffset + decodedArray.length - decodedFragmentSize;

        while (decodedArrayPosition < decodedArrayLimit) {
            int byte0 = decodedArray[decodedArrayPosition++] & 0xff;
            int byte1 = decodedArray[decodedArrayPosition++] & 0xff;
            int byte2 = decodedArray[decodedArrayPosition++] & 0xff;

            encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
            encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30) | ((byte1 >> 4) & 0x0f)];
            encodedArray[encodedArrayPosition++] = INDEXED[((byte1 << 2) & 0x3c) | ((byte2 >> 6) & 0x03)];
            encodedArray[encodedArrayPosition++] = INDEXED[byte2 & 0x3f];
        }

        if (decodedFragmentSize == 1) {
            int byte0 = decodedArray[decodedArrayPosition++] & 0xff;

            encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
            encodedArray[encodedArrayPosition++] = INDEXED[(byte0 << 4) & 0x30];
            encodedArray[encodedArrayPosition++] = PADDING_BYTE;
            encodedArray[encodedArrayPosition++] = PADDING_BYTE;
        }
        else if (decodedFragmentSize == 2) {
            int byte0 = decodedArray[decodedArrayPosition++] & 0xff;
            int byte1 = decodedArray[decodedArrayPosition++] & 0xff;

            encodedArray[encodedArrayPosition++] = INDEXED[(byte0 >> 2) & 0x3f];
            encodedArray[encodedArrayPosition++] = INDEXED[((byte0 << 4) & 0x30) | ((byte1 >> 4) & 0x0f)];
            encodedArray[encodedArrayPosition++] = INDEXED[(byte1 << 2) & 0x3c];
            encodedArray[encodedArrayPosition++] = PADDING_BYTE;
        }

        return encodedArray;
    }

    public static byte[] decode(byte[] encodedArray) {
        LOG.entering(CLASS_NAME, "decode", encodedArray);

        if (encodedArray == null) {
            return null;
        }

        int length = encodedArray.length;
        if (length % 4 != 0) {
            throw new IllegalArgumentException("Invalid Base64 Encoded String");
        }

        byte[] decodedArray = new byte[length / 4 * 3];
        int decodedArrayOffset = 0;
        int i = 0;
        while (i < length) {
            int char0 = encodedArray[i++];
            int char1 = encodedArray[i++];
            int char2 = encodedArray[i++];
            int char3 = encodedArray[i++];

            int byte0 = mapped(char0);
            int byte1 = mapped(char1);
            int byte2 = mapped(char2);
            int byte3 = mapped(char3);

            decodedArray[decodedArrayOffset++] = (byte) (((byte0 << 2) & 0xfc) | ((byte1 >> 4) & 0x03));
            if (char2 != PADDING_BYTE) {
                decodedArray[decodedArrayOffset++] = (byte) (((byte1 << 4) & 0xf0) | ((byte2 >> 2) & 0x0f));
                if (char3 != PADDING_BYTE) {
                    decodedArray[decodedArrayOffset++] = (byte) (((byte2 << 6) & 0xc0) | (byte3 & 0x3f));
                }
            }
        }

        assert decodedArrayOffset == decodedArray.length;

        return decodedArray;
    }

    private static int mapped(int ch) {
        if ((ch & 0x40) != 0) {
            if ((ch & 0x20) != 0) {
                // a(01100001)-z(01111010) -> 26-51
                assert ch >= 'a';
                assert ch <= 'z';
                return ch - 71;
            } else {
                // A(01000001)-Z(01011010) -> 0-25
                assert ch >= 'A';
                assert ch <= 'Z';
                return ch - 65;
            }
        } else if ((ch & 0x20) != 0) {
            if ((ch & 0x10) != 0) {
                if ((ch & 0x08) != 0 && (ch & 0x04) != 0) {
                    // =(00111101) -> 0
                    assert ch == '=';
                    return 0;
                } else {
                    // 0(00110000)-9(00111001) -> 52-61
                    assert ch >= '0';
                    assert ch <= '9';
                    return ch + 4;
                }
            } else {
                if ((ch & 0x04) != 0) {
                    // /(00101111) -> 63
                    assert ch == '/';
                    return 63;
                } else {
                    // +(00101011) -> 62
                    assert ch == '+';
                    return 62;
                }
            }
        } else {
            LOG.warning("Invalid BASE64 string");
            throw new IllegalArgumentException("Invalid BASE64 string");
        }
    }
}
