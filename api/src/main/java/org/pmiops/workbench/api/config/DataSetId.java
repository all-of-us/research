package org.pmiops.workbench.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("dataSetId")
public class DataSetId {

    @Value("${bigQuery.dataSetId:#{null}}")
    private String dataSetId;

    public String getDataSetId() {
        return this.dataSetId == null ? UUID.randomUUID().toString() : this.dataSetId;
    }
}
