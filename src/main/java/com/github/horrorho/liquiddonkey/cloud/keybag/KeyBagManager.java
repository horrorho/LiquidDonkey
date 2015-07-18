/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.liquiddonkey.cloud.keybag;

import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KeyBagManager.
 *
 * @author ahseya
 */
@Immutable
@ThreadSafe
public final class KeyBagManager {

    /**
     * Returns a new unlocked KeyBagManager instance.
     *
     * @param keySet the key set, not null
     * @return a new unlocked KeyBagManager instance, not null
     * @throws BadDataException if the KeyBagManager cannot be unlocked or a data handling error occurred
     */
    public static KeyBagManager from(ICloud.MBSKeySet keySet) throws BadDataException {
        logger.trace("<< from() < {}", keySet);

        if (keySet.getKeyCount() < 2) {
            throw new BadDataException("Bad keybag.");
        }

        ByteString passCode = keySet.getKey(0).getKeyData();
        Map<ByteString, KeyBag> uuidToKeyBag = new HashMap<>();

        for (int i = 1; i < keySet.getKeyCount(); i++) {
            KeyBag keyBag = KeyBagFactory.from(keySet.getKey(i), passCode);
            uuidToKeyBag.put(keyBag.uuid(), keyBag);
        }

        KeyBagManager instance = new KeyBagManager(uuidToKeyBag, FileKeyFactory.create());

        logger.trace(">> from() > {}", instance);
        return instance;
    }

    private static final Logger logger = LoggerFactory.getLogger(KeyBagManager.class);

    private final Map<ByteString, KeyBag> uuidToKeyBag;
    private final FileKeyFactory fileKeyFactory;

    KeyBagManager(Map<ByteString, KeyBag> uuidToKeyBag, FileKeyFactory fileKeyFactory) {
        this.uuidToKeyBag = uuidToKeyBag;
        this.fileKeyFactory = fileKeyFactory;
    }

    public ByteString fileKey(ICloud.MBSFile file) {
        ICloud.MBSFileAttributes fileAttributes = file.getAttributes();

        if (!fileAttributes.hasEncryptionKey()) {
            logger.warn("-- fileKey() > no encryption key: {}", file.getRelativePath());
            return null;
        }

        ByteString uuid = fileAttributes.getEncryptionKey().substring(0, 0x10);

        KeyBag keyBag = uuidToKeyBag.get(uuid);

        if (keyBag == null) {
            logger.warn("-- fileKey() > no key bag for uuid: {}", Bytes.hex(uuid));
            return null;
        } else {
            return fileKeyFactory.fileKey(keyBag, file);
        }
    }

    @Override
    public String toString() {
        return "KeyBagManager{" + "uuidToKeyBag=" + uuidToKeyBag + '}';
    }
}
