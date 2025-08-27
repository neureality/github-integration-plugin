package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Include Regex"), field: "includeRegex") {
    f.textbox()
}

f.entry(title: _("Exclude Regex"), field: "excludeRegex") {
    f.textbox()
}
