package org.onap.aai.cacher.exceptions;

public class MissingTemplateException extends RuntimeException {
    public static final String DEFAULT_EXCEPTION_MESSAGE = "Failed in uriToTemplates for truncatedUri '{}'";

    public MissingTemplateException(String uri) {
        super(String.format(DEFAULT_EXCEPTION_MESSAGE, uri));
    }
}
