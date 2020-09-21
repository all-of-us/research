package org.pmiops.workbench.utils.mappers;

import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.leonardo.model.LeonardoGceConfig;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoMachineConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeImage;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.DataprocConfig;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeStatus;

@Mapper(config = MapStructConfig.class)
public interface LeonardoMapper {

  DataprocConfig toDataprocConfig(LeonardoMachineConfig leonardoMachineConfig);

  @Mapping(target = "cloudService", ignore = true)
  @Mapping(target = "properties", ignore = true)
  LeonardoMachineConfig toLeonardoMachineConfig(DataprocConfig dataprocConfig);

  @AfterMapping
  default void addCloudServiceEnum(@MappingTarget LeonardoMachineConfig leonardoMachineConfig) {
    leonardoMachineConfig.setCloudService(LeonardoMachineConfig.CloudServiceEnum.DATAPROC);
  }

  GceConfig toGceConfig(LeonardoGceConfig leonardoGceConfig);

  @Mapping(target = "cloudService", ignore = true)
  LeonardoGceConfig toLeonardoGceConfig(GceConfig gceConfig);

  @AfterMapping
  default void addCloudServiceEnum(@MappingTarget LeonardoGceConfig leonardoGceConfig) {
    leonardoGceConfig.setCloudService(LeonardoGceConfig.CloudServiceEnum.GCE);
  }

  @Mapping(target = "patchInProgress", ignore = true)
  LeonardoListRuntimeResponse toListRuntimeResponse(LeonardoGetRuntimeResponse runtime);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  ListRuntimeResponse toApiListRuntimeResponse(
      LeonardoListRuntimeResponse leonardoListRuntimeResponse);

  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "toolDockerImage", source = "runtimeImages")
  @Mapping(target = "gceConfig", ignore = true)
  @Mapping(target = "dataprocConfig", ignore = true)
  Runtime toApiRuntime(LeonardoGetRuntimeResponse runtime);

  default Integer extractIntField(Map<String, Object> map, String field) {
    if (map.get(field) == null) {
      return null;
    }

    return ((Number) map.get(field)).intValue();
  }

  @AfterMapping
  default void mapRuntimeConfig(
      @MappingTarget Runtime runtime, LeonardoGetRuntimeResponse leonardoGetRuntimeResponse) {
    // There's a lot of unchecked casting happening here but I couldn't find a way around it
    // This is primarily due to the fact that Swagger generates the `runtimeConfig` object as a
    // generic Java object.
    // From manual testing, it seems like it is generated as a LinkedTreeMap.

    Map<String, Object> runtimeConfig =
        (Map<String, Object>) leonardoGetRuntimeResponse.getRuntimeConfig();

    if (runtimeConfig.get("cloudService").equals("DATAPROC")) {
      runtime.dataprocConfig(
          new DataprocConfig()
              .numberOfWorkers(extractIntField(runtimeConfig, "numberOfWorkers"))
              .masterMachineType((String) runtimeConfig.get("masterMachineType"))
              .masterDiskSize(extractIntField(runtimeConfig, "masterDiskSize"))
              .workerMachineType((String) runtimeConfig.get("workerMachineType"))
              .workerDiskSize(extractIntField(runtimeConfig, "workerDiskSize"))
              .numberOfWorkerLocalSSDs(extractIntField(runtimeConfig, "numberOfWorkerLocalSSDs"))
              .numberOfPreemptibleWorkers(
                  extractIntField(runtimeConfig, "numberOfPreemptibleWorkers")));
    } else if (runtimeConfig.get("cloudService").equals("GCE")) {
      runtime.gceConfig(
          new GceConfig()
              .diskSize(extractIntField(runtimeConfig, "diskSize"))
              .bootDiskSize(extractIntField(runtimeConfig, "bootDiskSize"))
              .machineType((String) runtimeConfig.get("machineType")));
    } else {
      throw new IllegalArgumentException(
          "Invalid LeonardoGetRuntimeResponse.RuntimeConfig.cloudService : "
              + runtimeConfig.get("cloudService"));
    }
  }

  default RuntimeStatus toApiRuntimeStatus(LeonardoRuntimeStatus leonardoRuntimeStatus) {
    if (leonardoRuntimeStatus == null) {
      return RuntimeStatus.UNKNOWN;
    }
    return RuntimeStatus.fromValue(leonardoRuntimeStatus.toString());
  }

  default String getJupyterImage(List<LeonardoRuntimeImage> images) {
    return images.stream()
        .filter(image -> image.getImageType().equals("Jupyter"))
        .findFirst()
        .get()
        .getImageUrl();
  }
}
