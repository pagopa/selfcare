package it.pagopa.selfcare.dashboard.config;

import freemarker.cache.URLTemplateLoader;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

@Slf4j
class CloudTemplateLoader extends URLTemplateLoader {
    private URL root;

    public CloudTemplateLoader(URL root) {
        super();
        this.root = root;
    }

    @Override
    protected URL getURL(String template) {
        try {
            return root.toURI().resolve(template).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
