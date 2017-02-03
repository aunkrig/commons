
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2014, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.commons.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for processing command line arguments.
 */
public final
class CommandLineUtil {

    private CommandLineUtil() {}

    /**
     * Replaces 'combined' single-letter options (as in {@code "ls -lart"}) with 'long' options (as {@code "ls -long
     * -all -reverse -time"}).
     *
     * @param args    The command line arguments, e.g. {@code "-foo", "-abc", "arg1", "arg2", "-bar"}
     * @param idx     The index of the command line argument to expand, e.g. 1 (designating {@code "-abc"})
     * @param mapping Triplets of {@link Character}s, {@link String}s and {@link Integer}s representing unexpanded
     *                option, expanded option and option argument count, e.g. {@code 'a', "-alpha", 1, 'b', "-beta", 0,
     *                'c', "-gamma", 1 }
     * @return        The expanded <var>args</var>, e.g. {@code "-foo", "-alpha", "arg1", "-beta", "-gamma", "arg2",
     *                "-bar"}
     */
    public static String[]
    expandSingleLetterOptions(String[] args, int idx, Object... mapping) {

        List<String> nargs = new ArrayList<String>(Arrays.asList(args).subList(0, idx));

        String arg       = args[idx++];
        int    argLength = arg.length();
        LETTERS: for (int i = 1; i < argLength; i++) {
            char c = arg.charAt(i);
            for (int j = 0; j < mapping.length; j += 3) {
                if ((Character) mapping[j] == c) {
                    nargs.add((String) mapping[j + 1]);
                    if (idx + (Integer) mapping[j + 2] > args.length) {
                        System.err.println("Too few arguments for command line option '-" + c + "' - try '-help'");
                        System.exit(1);
                    }
                    for (int k = 0; k < (Integer) mapping[j + 2]; k++) nargs.add(args[idx++]);
                    continue LETTERS;
                }
            }
            System.err.println("Invalid command line option '-" + c + "' - try '-help'.");
            System.exit(1);
        }
        nargs.addAll(Arrays.asList(args).subList(idx, args.length));
        return nargs.toArray(new String[nargs.size()]);
    }
}
