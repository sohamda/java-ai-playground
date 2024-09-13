package org.vaadin.marcus.semantickernel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Ideally this information should come from a vector db or a knowledge mine,
 * But SK doesn't have a In_memory option so temporarily implemented as below
 *
 */
@Service
public class SKContentRetriever {

    @Value("classpath:terms-of-service.txt")
    Resource resourceFile;

    public String getTermsAndConditions(String query) throws IOException {
        return resourceFile.getContentAsString(StandardCharsets.UTF_8);
    }


}
