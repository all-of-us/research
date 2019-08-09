package org.pmiops.workbench.cohortreview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;

@Mapper(componentModel = "spring")
public interface CohorReviewMapper {
  @Mapping(target = "participantId", source = "db.participantKey.participantId")
  @Mapping(target = "status", source = "db.statusEnum")
  @Mapping(target = "birthDate", source = "db.birthDate", dateFormat = "yyyy-MM-dd")
  org.pmiops.workbench.model.ParticipantCohortStatus toApiParticipant(ParticipantCohortStatus db);
}
