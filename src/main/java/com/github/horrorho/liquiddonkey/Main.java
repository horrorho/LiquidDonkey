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
package com.github.horrorho.liquiddonkey;

import com.github.horrorho.liquiddonkey.util.DumpStackTraceHook;
import com.github.horrorho.liquiddonkey.cloud.Looter;
import com.github.horrorho.liquiddonkey.exception.FatalException;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.config.ConfigFactory;
import com.github.horrorho.liquiddonkey.settings.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main.
 *
 * @author ahseya
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Config config = ConfigFactory.getInstance().from(args);

        logger.debug("-- main() > options: {}", config);

        if (config == null) {
            return;
        } 
        
        if (config.print().toPrintStackTrace()) {
            DumpStackTraceHook.add();
        }

        Printer printer = Printer.instanceOf(config.print());

        try (Looter looter = Looter.newInstance(config, printer)) {
            looter.loot();
        } catch (FatalException ex) {
            logger.warn("-- main() > FatalException", ex);
            printer.println(Level.ERROR, ex);
        } catch (Exception ex) {
            logger.warn("-- main() > Exception", ex);
            printer.println(Level.ERROR, ex);
        }

        if (config.print().toPrintStackTrace()) {
            DumpStackTraceHook.remove();
        }
    }
}
