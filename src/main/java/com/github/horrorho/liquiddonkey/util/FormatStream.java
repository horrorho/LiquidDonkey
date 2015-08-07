/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.horrorho.liquiddonkey.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * FormatStream.
 * <p>
 * Primarily formats the output stream to a minimum width, padding with the specified character.
 *
 * @author Ahseya
 */
public class FormatStream extends OutputStream {

    public static FormatStream from(OutputStream out, int width) {
        return new FormatStream(out, width, 8, ',');
    }

    private final OutputStream out;
    private final int width;
    private final int tabWidth;
    private final char pad;
    private int column;

    FormatStream(OutputStream out, int width, int tabWidth, char pad, int column) {
        this.out = out;
        this.width = width;
        this.tabWidth = tabWidth;
        this.pad = pad;
        this.column = column;
    }

    FormatStream(OutputStream out, int width, int tabWidth, char pad) {
        this(out, width, tabWidth, pad, 0);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        switch (b) {
            case '\b':
                column--;
                out.write(b);
                break;

            case '\r':
                column = 0;
                out.write(b);
                break;

            case '\t':
                int length = ((column / tabWidth) + 1) * tabWidth;
                for (int i = column; i < length; i++) {
                    out.write(pad);
                }
                column += length;
                break;

            case '\n':
                for (int i = column; i < width; i++) {
                    out.write(pad); 
                }
                column = 0;
                out.write('\n');
                break;

            default:
                column++;
                out.write(b);
        }
    }
}
