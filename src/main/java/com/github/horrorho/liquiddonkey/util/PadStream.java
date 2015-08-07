/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.horrorho.liquiddonkey.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Lightweight stream padding.
 * <p>
 * Pads the output stream to a minimum width with the specified character.
 *
 * @author Ahseya
 */
public class PadStream extends OutputStream {

    public static PadStream from(OutputStream out, int width) {
        return from(out, width, ' ');
    }

    public static PadStream from(OutputStream out, int width, char pad) {
        return new PadStream(out, width, 8, pad);
    }

    private final OutputStream out;
    private final int width;
    private final int tabWidth;
    private final char pad;
    private int column;

    PadStream(OutputStream out, int width, int tabWidth, char pad, int column) {
        this.out = out;
        this.width = width;
        this.tabWidth = tabWidth;
        this.pad = pad;
        this.column = column;
    }

    PadStream(OutputStream out, int width, int tabWidth, char pad) {
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
                for (int i = column; i < column + length; i++) {
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

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
