package com.jordansamhi.androspecter.printers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.text.SimpleDateFormat;

/*-
 * #%L
 * AndroSpecter
 *
 * %%
 * Copyright (C) 2023 Jordan Samhi
 * All rights reserved
 *
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

/**
 * Provides methods for printing messages to the console with different types of prefix indicators.
 *
 * @author Jordan Samhi
 */
public class Writer2 {

    // private static Writer2 instance;
    private String instrumentation_outputs;
    private String apk_name;
    private String unix_time;

    public Writer2(String instrumentation_outputs, String apk_name) {
        this.instrumentation_outputs = instrumentation_outputs;
        this.apk_name = apk_name;

        this.unix_time = String.valueOf(Instant.now().getEpochSecond());
    }

    // public static Writer2 v() {
    //     if (instance == null) {
    //         instance = new Writer2();
    //     }
    //     return instance;
    // }

    /**
     * Prints an error message to the console, with a prefix indicating an error.
     *
     * @param s the error message to print
     */
    public void perror(String s) {
        this.print('x', s);
    }


    /**
     * Prints a success message to the console, with a prefix indicating success.
     *
     * @param s the success message to print
     */
    public void psuccess(String s) {
        this.print('✓', s);
    }

    /**
     * Prints a warning message to the console, with a prefix indicating a warning.
     *
     * @param s the warning message to print
     */
    public void pwarning(String s) {
        this.print('!', s);
    }

    /**
     * Prints an informational message to the console, with a prefix indicating information.
     *
     * @param s the informational message to print
     */
    public void pinfo(String s) {
        this.print('*', s);
    }

    /**
     * Prints a message to the console, with the given prefix character.
     *
     * @param c the prefix character to print before the message
     * @param s the message to print
     */
    private void print(char c, String s) {

        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timeStamp = sdf.format(now);
        
        String to_print = String.format("[%c] %s %s%n", c, timeStamp, s);
        System.out.printf(to_print);

        try
        {

            String filename = Paths.get(
                this.instrumentation_outputs, 
                this.apk_name+"_"+this.unix_time+".txt"
            ).toString();

            if (!Files.exists(Paths.get(filename))) {
                File myObj = new File(filename);
                myObj.createNewFile();
            }

            FileWriter fw = new FileWriter(filename,true);
            fw.write(to_print); 
            fw.close();
        }
        catch(IOException ioe)
        {
            System.err.println("IOException: " + ioe.getMessage());
        }

    }
}
