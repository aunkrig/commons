
/*
 * de.unkrig.commons - A general-purpose Java class library
 *
 * Copyright (c) 2011, Arno Unkrig
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

package de.unkrig.commons.text.pattern;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * @see #matches
 */
public final
class IncludeExclude extends Glob {

    /**
     * File names that one would typically exclude from file searches.
     */
    public static final Glob DEFAULT_EXCLUDES;
    static {
        IncludeExclude ie = new IncludeExclude();
        ie.appendExclude(Glob.compile(".svn",     Pattern2.WILDCARD));
        ie.appendExclude(Glob.compile("***/.svn", Pattern2.WILDCARD));
        ie.appendExclude(Glob.compile("***!.svn", Pattern2.WILDCARD));
        ie.appendExclude(Glob.compile("CVS",      Pattern2.WILDCARD));
        ie.appendExclude(Glob.compile("***/CVS",  Pattern2.WILDCARD));
        ie.appendExclude(Glob.compile("***!CVS",  Pattern2.WILDCARD));
        DEFAULT_EXCLUDES = ie;
    }

    public IncludeExclude() {}

    /**
     * @deprecated Equivalent with {@code addInclude(include, false)}
     */
    @Deprecated public void
    appendInclude(Glob include) {
        this.addInclude(include, false);
    }

    /**
     * @deprecated Equivalent with {@code addInclude(include, true)}
     */
    @Deprecated public void
    prependInclude(Glob include) {
        this.addInclude(include, true);
    }

    /**
     * @param override If {@code true}, then the glob takes precedence over all previously added includes and excludes;
     *                 if {@code false}, then the glob is only applied if all previously added includes and exclude
     *                 do <i>not</i> match
     * @see #matches(String)
     * @see #replace(String)
     */
    public void
    addInclude(Glob include, boolean override) {
        ModeAndGlob mag = new ModeAndGlob(Mode.INCLUDE, include);
        if (override) {
            this.mags.add(0, mag);
        } else {
            this.mags.add(mag);
        }
    }

    /**
     * @deprecated Equivalent with {@code addExclude(exclude, false)}
     */
    @Deprecated public void
    appendExclude(Glob exclude) {
        this.addExclude(exclude, false);
    }

    /**
     * @deprecated Equivalent with {@code addExclude(exclude, true)}
     */
    @Deprecated public void
    prependExclude(Glob exclude) {
        this.addExclude(exclude, true);
    }

    /**
     * @param override If {@code true}, then the glob takes precedence over all previously added includes and excludes;
     *                 if {@code false}, then the glob is only applied if all previously added includes and exclude
     *                 do <i>not</i> match
     * @see #matches(String)
     * @see #replace(String)
     */
    public void
    addExclude(Glob exclude, boolean override) {
        ModeAndGlob mag = new ModeAndGlob(Mode.EXCLUDE, exclude);
        if (override) {
            this.mags.add(0, mag);
        } else {
            this.mags.add(mag);
        }
    }

    /**
     * All includes and excludes are matched against the {@code subject}. On the first successful match,
     * {@code true} or {@code false} is returned, depending on whether the match was an INCLUDE or an EXCLUDE.
     * If none of the includes and excludes match, {@code true} or {@code false} is returned, depending on whether the
     * lowest-priority rule is an EXCLUDE or an INCLUDE.
     */
    @Override public boolean
    matches(String subject) {

        if (this.mags.isEmpty()) return true;

        boolean isContainer = subject.endsWith("!") || subject.endsWith("/");

        for (ModeAndGlob mag : this.mags) {
            if (mag.mode == Mode.INCLUDE) {
                if (mag.glob.matches(subject)) return true;
            } else {
                if (!isContainer && mag.glob.matches(subject)) return false;
            }
        }
        return this.mags.get(this.mags.size() - 1).mode == Mode.EXCLUDE;
    }

    /**
     * The {@code subject} is transformed by all matching includes, until an exclude matches.
     *
     * @return The transformed subject, or {@code null} iff an exclude matches before the first include matches
     */
    @Override @Nullable public String
    replace(String subject) {
        boolean hadInclude = false;
        for (ModeAndGlob map : this.mags) {
            switch (map.mode) {
            case INCLUDE:
                String tmp = map.glob.replace(subject);
                if (tmp != null) {
                    subject    = tmp;
                    hadInclude = true;
                }
                break;
            case EXCLUDE:
                if (map.glob.matches(subject)) return hadInclude ? subject : null;
                break;
            default:
                throw new IllegalStateException();
            }
        }
        return hadInclude ? subject : null;
    }

    @Override public String
    toString() {
        Iterator<ModeAndGlob> it = this.mags.iterator();
        if (!it.hasNext()) return "(empty)";

        StringBuilder sb = new StringBuilder();

        {
            ModeAndGlob mag = it.next();
            if (mag.mode == Mode.EXCLUDE) sb.append('~');
            sb.append(mag.glob);
        }

        while (it.hasNext()) {
            ModeAndGlob mag = it.next();
            sb.append(mag.mode == Mode.INCLUDE ? ',' : '~');
            sb.append(mag.glob);
        }

        return sb.toString();
    }

    /**
     * @see ModeAndGlob
     */
    private enum Mode { INCLUDE, EXCLUDE }

    /**
     * Representation of a {@link Glob} which is either <em>included</em> or <em>excluded</em>.
     */
    private static
    class ModeAndGlob {
        final Mode mode;
        final Glob glob;

        ModeAndGlob(Mode mode, Glob glob) {
            this.mode = mode;
            this.glob = glob;
        }
    }

    /**
     * The {@link ModeAndGlob}s that comprise this object, in order of decreasing precendence.
     */
    private final List<ModeAndGlob> mags = new ArrayList<ModeAndGlob>();
}
