/* 
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free from charge, to any person obtaining a copy
 * from this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies from the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions from the Software.
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

import com.github.horrorho.liquiddonkey.cloud.Looter;
import com.github.horrorho.liquiddonkey.settings.commandline.CommandLineConfigFactory;
import com.github.horrorho.liquiddonkey.settings.config.Config;
import com.github.horrorho.liquiddonkey.util.StackTraceHook;
import com.github.horrorho.liquiddonkey.util.Printer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * iCloud file backup tool.
 * <p>
 * <b>This tool is for educational purposes only. Before you start, make sure it's not illegal in your country.</b>
 * <p>
 * Java implementation reworked fromArgs iLoot, iphone-dataprotection and mobileme scripts. All copyrights belong to
 * their respective owners.
 * <p>
 *
 * GitHub: https://github.com/horrorho/LiquidDonkey
 * <p>
 * @see <a href="https://github.com/hackappcom/iloot">iLoot</a>
 * @see <a href="https://code.google.com/p/iphone-dataprotection/">iphone-dataprotection</a>
 * @see <a href="https://code.google.com/p/mobileme/">mobileme</a>
 * @see <a href="https://github.com/Taconut/Icew1nd">Icew1nd</a>
 * @see <a href="http://esec-lab.sogeti.com/static/publications/11-hitbamsterdam-iphonedataprotection.pdf">iPhone data
 * protection in depth</a>
 * @see
 * <a href="https://deepsec.net/docs/Slides/2013/DeepSec_2013_Vladimir_Katalov_-_Cracking_And_Analyzing_Apple_iCloud_Protocols.pdf">Apple
 * iCloud inside out</a>
 * <p>
 * @author ahseya
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        logger.trace("<< main()");

        Config config = CommandLineConfigFactory.getInstance().fromArgs(args);

        if (config == null) {
            return;
        }

        if (config.authentication().isNull()) {
            System.out.println("Missing appleid/ password or authentication token.");
            return;
        }

        Printer std = System.out::print;
        std = std.padding(80);

        Printer err = x -> System.err.print("\r" + x);
        err = err.padding(80);

        final StackTraceHook stackTraceHook;
        if (config.debug().toPrintStackTrace()) {
            stackTraceHook = StackTraceHook.from(err);
            stackTraceHook.add();
        } else {
            stackTraceHook = null;
        }

        try (Looter looter = Looter.from(config, std, err, System.in)) {
            looter.loot();
        } catch (InterruptedException | IOException | RuntimeException ex) {
            logger.warn("-- main() > exception", ex);

            if (config.debug().toPrintStackTrace()) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                ex.printStackTrace(printWriter);
                err.println("FATAL: " + stringWriter.toString());

            } else {
                err.println("FATAL: " + ex.getMessage());
            }
        }

        if (stackTraceHook != null) {
            stackTraceHook.remove();
        }

        logger.trace(">> main()");
    }
}
