import {
  ConceptsApi,
  Criteria,
  CriteriaType,
  Domain,
  DomainCount,
  DomainCountsListResponse,
  DomainInfo,
  DomainInfoResponse,
  SearchConceptsRequest,
  SurveyModule, SurveyQuestions,
  SurveysResponse
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export class ConceptStubVariables {
  static STUB_CONCEPTS: Criteria[] = [
    {
      id: 1,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 1',
      domainId: Domain.CONDITION.toString(),
      type: CriteriaType.SNOMED.toString(),
      conceptId: 8107,
      isStandard: true,
      childCount: 1,
      parentCount: 0
    },
    {
      id: 2,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 2',
      domainId: Domain.CONDITION.toString(),
      type: CriteriaType.SNOMED.toString(),
      conceptId: 8107,
      isStandard: true,
      childCount: 2,
      parentCount: 0
    },
    {
      id: 3,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 3',
      domainId: Domain.MEASUREMENT.toString(),
      type: CriteriaType.LOINC.toString(),
      conceptId: 1234,
      isStandard: true,
      childCount: 1,
      parentCount: 0
    },
    {
      id: 4,
      parentId: 0,
      group: true,
      selectable: true,
      hasAttributes: false,
      name: 'Stub Concept 4',
      domainId: Domain.MEASUREMENT.toString(),
      type: CriteriaType.LOINC.toString(),
      conceptId: 2345,
      isStandard: true,
      childCount: 1,
      parentCount: 0
    },
  ];
}
export class SurveyStubVariables {
  static STUB_SURVEYS: SurveyModule[] = [
    {
      conceptId: 1,
      name: 'The Basics',
      description: 'Basis description',
      questionCount: 101,
      participantCount: 200,
      orderNumber: 1
    },
    {
      conceptId: 2,
      name: 'Overall Health',
      description: 'Overall Health description',
      questionCount: 102,
      participantCount: 300,
      orderNumber: 2
    },
    {
      conceptId: 3,
      name: 'LifeStyle',
      description: 'Lifestyle description',
      questionCount: 103,
      participantCount: 300,
      orderNumber: 3
    }
  ];
}

export class DomainStubVariables {
  static STUB_DOMAINS: DomainInfo[] = [
    {
      domain: Domain.CONDITION,
      name: 'Condition',
      description: 'The Conditions Stub',
      standardConceptCount: 1,
      allConceptCount: 2,
      participantCount: 30
    },
    {
      domain: Domain.MEASUREMENT,
      name: 'Measurement',
      description: 'The Measurements Stub',
      standardConceptCount: 50,
      allConceptCount: 65,
      participantCount: 200
    },
  ];
}

export class DomainCountStubVariables {
  static STUB_DOMAIN_COUNTS: DomainCount[] = [
    {
      domain: Domain.CONDITION,
      name: 'Condition',
      conceptCount: 2
    }, {
      domain: Domain.MEASUREMENT,
      name: 'Measurement',
      conceptCount: 1
    }, {
      domain: Domain.DRUG,
      name: 'Drug',
      conceptCount: 2
    }
  ];
}

export class SurveyQuestionStubVariables {
  static STUB_SURVEY_QUESTIONS: SurveyQuestions[] = [
    {
      question: 'Survey question 1',
      conceptId: 1
    }, {
      question: 'Survey question 2',
      conceptId: 2
    }
  ];
}

export class ConceptsApiStub extends ConceptsApi {
  public concepts?: Criteria[];
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });

    this.concepts = ConceptStubVariables.STUB_CONCEPTS;
  }

  public getDomainInfo(
    workspaceNamespace: string, workspaceId: string): Promise<DomainInfoResponse> {
    return Promise.resolve({items: DomainStubVariables.STUB_DOMAINS});
  }

  public getSurveyInfo(workspaceNamespace: string, workspaceId: string): Promise<SurveysResponse> {
    return Promise.resolve({items: SurveyStubVariables.STUB_SURVEYS});
  }

  public domainCounts(workspaceNamespace: string, workspaceId: string): Promise<DomainCountsListResponse> {
    return Promise.resolve({domainCounts: DomainCountStubVariables.STUB_DOMAIN_COUNTS});
  }

  // This just returns static values rather than doing a real search.
  // Real search functionality should be tested at the API level.
  // This creates more predictable responses.
  // Need to update with new api calls
  /*public searchConcepts(
    workspaceNamespace: string, workspaceId: string,
    request?: SearchConceptsRequest): Promise<ConceptListResponse> {
    return new Promise<ConceptListResponse>(resolve => {
      const response = {
        items: [],
        standardConcepts: [],
        vocabularyCounts: [],
      };
      const foundDomain =
        DomainStubVariables.STUB_DOMAINS.find(domain => domain.domain === request.domain);
      if (request.query === 'headerText') {
        this.extendConceptListForHeaderText();
      }
      this.concepts.forEach((concept) => {
        if (concept.domainId !== foundDomain.name) {
          return;
        }
        if (request.standardConceptFilter === StandardConceptFilter.ALLCONCEPTS) {
          response.items.push(concept);
          if (concept.standardConcept) {
            response.standardConcepts.push(concept);
          }
        } else if (
          request.standardConceptFilter === StandardConceptFilter.STANDARDCONCEPTS) {
          if (concept.standardConcept) {
            response.items.push(concept);
            response.standardConcepts.push(concept);
          }
        } else if (request.standardConceptFilter
          === StandardConceptFilter.NONSTANDARDCONCEPTS) {
          if (!concept.standardConcept) {
            response.items.push(concept);
          }
        }
      });
      resolve(response);
    });
  }*/

  public searchSurveys(workspaceNamespace: string, workspaceId: string, request?: SearchConceptsRequest): Promise<Array<SurveyQuestions>> {
    return Promise.resolve(SurveyQuestionStubVariables.STUB_SURVEY_QUESTIONS);
  }

}
