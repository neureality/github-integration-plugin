package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static org.kohsuke.github.GHIssueState.CLOSED;

/**
 * Trigger PR if changed files match include/exclude regex filters.
 */
public class GitHubPRFileChangeEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Pull Request File Change (include/exclude)";

    /** Regex of files to include (default: everything). */
    private String includeRegex = ".*";

    /** Regex of files to exclude (default: nothing). */
    private String excludeRegex = "";

    @DataBoundConstructor
    public GitHubPRFileChangeEvent() {}

    @DataBoundSetter
    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = (includeRegex == null || includeRegex.isEmpty()) ? ".*" : includeRegex;
    }

    @DataBoundSetter
    public void setExcludeRegex(String excludeRegex) {
        this.excludeRegex = (excludeRegex == null) ? "" : excludeRegex;
    }

    public String getIncludeRegex() { return includeRegex; }
    public String getExcludeRegex() { return excludeRegex; }

    @Override
    public GitHubPRCause check(@NonNull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        PrintStream log = listener.getLogger();

        GHPullRequest remotePR = prDecisionContext.getRemotePR();

        if (remotePR.getState() == CLOSED) {
            return null;
        }

        Pattern include = Pattern.compile(includeRegex);
        Pattern exclude = excludeRegex.isEmpty() ? null : Pattern.compile(excludeRegex);

        for (GHPullRequestFileDetail f : remotePR.listFiles()) {
            String name = f.getFilename();
            boolean inc = include.matcher(name).find();
            boolean exc = (exclude != null) && exclude.matcher(name).find();

            if (inc && !exc) {
                log.println(DISPLAY_NAME + ": matched file " + name);

                for (GHPullRequestFileDetail file : remotePR.listFiles()) {
                    log.printf("%s: %s (%s lines added, %s deleted, status=%s)%n",
                            DISPLAY_NAME,
                            file.getFilename(),
                            file.getAdditions(),
                            file.getDeletions(),
                            file.getStatus());
                }
                GitHubPRCause cause = prDecisionContext.newCause(
                        "PR contains matching file: " + name, false);
                return cause;
            }
        }

        log.println(DISPLAY_NAME + ": no matching files found.");
        return null;
    }

    @Symbol("filePattern")
    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}