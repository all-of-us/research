import * as fp from 'lodash/fp';

import {
  AgeType,
  CriteriaSubType,
  CriteriaType,
  Domain, GenderOrSexType,
  SearchGroup,
  SearchGroupItem,
  SearchParameter,
  SearchRequest,
  TemporalMention,
  TemporalTime
} from 'generated/fetch';
import {idsInUse} from './search-state.service';

export function typeDisplay(parameter): string {
  if ([Domain.CONDITION, Domain.PROCEDURE, Domain.MEASUREMENT].includes(parameter.domainId)) {
    return parameter.code;
  }
}

export function nameDisplay(parameter): string {
  if (parameter.type === CriteriaType.DECEASED) {
    return '';
  } else {
    let name = stripHtml(parameter.name);
    if (parameter.type === CriteriaType.ETHNICITY || parameter.type === CriteriaType.RACE) {
      name = toTitleCase(name);
    }
    return name;
  }
}

export function toTitleCase(str: string): string {
  return fp.startCase(fp.toLower(str));
}

export function attributeDisplay(parameter): string {
  if (parameter.type === CriteriaType.AGE) {
    const attrs = parameter.attributes;
    const display = [];
    attrs.forEach(attr => {
      const op = {
        'BETWEEN': 'In Range',
        'EQUAL': 'Equal To',
        'GREATER_THAN': 'Greater Than',
        'LESS_THAN': 'Less Than',
        'GREATER_THAN_OR_EQUAL_TO': 'Greater Than or Equal To',
        'LESS_THAN_OR_EQUAL_TO': 'Less Than or Equal To',
      }[attr.operator];
      const args = attr.operands.join(', ');
      display.push(`${op} ${args}`);
    });
    return display.join(' ');
  } else {
    return '';
  }
}

export function domainToTitle(domain: any): string {
  switch (domain) {
    case Domain.PERSON:
      domain = 'Demographics';
      break;
    case Domain.MEASUREMENT:
      domain = 'Labs and Measurements';
      break;
    case Domain.PHYSICALMEASUREMENT:
      domain = 'Physical Measurements';
      break;
    case Domain.PHYSICALMEASUREMENTCSS:
      domain = 'Physical Measurements';
      break;
    case Domain.VISIT:
      domain = 'Visits';
      break;
    case Domain.DRUG:
      domain = 'Drugs';
      break;
    case Domain.CONDITION:
      domain = 'Conditions';
      break;
    case Domain.PROCEDURE:
      domain = 'Procedures';
      break;
    case Domain.OBSERVATION:
      domain = 'Observations';
      break;
    case Domain.LAB:
      domain = 'Labs';
      break;
    case Domain.VITAL:
      domain = 'Vitals';
      break;
    case Domain.SURVEY:
      domain = 'Surveys';
      break;
    case Domain.ALLEVENTS:
      domain = 'All Events';
      break;
    case Domain.FITBIT:
      domain = 'Fitbit';
      break;
  }
  return domain;
}

export function typeToTitle(_type: string): string {
  switch (_type) {
    case CriteriaType[CriteriaType.AGE]:
      _type = 'Age';
      break;
    case CriteriaType[CriteriaType.DECEASED]:
      _type = 'Deceased';
      break;
    case CriteriaType[CriteriaType.ETHNICITY]:
      _type = 'Ethnicity';
      break;
    case CriteriaType[CriteriaType.GENDER]:
      _type = 'Gender Identity';
      break;
    case CriteriaType[CriteriaType.RACE]:
      _type = 'Race';
      break;
    case CriteriaType[CriteriaType.SEX]:
      _type = 'Sex Assigned at Birth';
      break;
  }
  return _type;
}

export function subTypeToTitle(subtype: string): string {
  switch (subtype) {
    case CriteriaSubType[CriteriaSubType.BP]:
      subtype = 'Blood Pressure';
      break;
    case CriteriaSubType[CriteriaSubType.BMI]:
      subtype = 'BMI';
      break;
    case CriteriaSubType[CriteriaSubType.HR]:
      subtype = 'Heart Rate';
      break;
    case CriteriaSubType[CriteriaSubType.HEIGHT]:
      subtype = 'Height';
      break;
    case CriteriaSubType[CriteriaSubType.HC]:
      subtype = 'Hip Circumference';
      break;
    case CriteriaSubType[CriteriaSubType.WC]:
      subtype = 'Waist Circumference';
      break;
    case CriteriaSubType[CriteriaSubType.WEIGHT]:
      subtype = 'Weight';
      break;
  }
  return subtype;
}

export function ageTypeToText(ageType: AgeType) {
  switch (ageType) {
    case AgeType.AGE:
      return 'Current Age';
    case AgeType.AGEATCDR:
      return 'Age at CDR';
    case AgeType.AGEATCONSENT:
      return 'Age  at Consent';
  }
}

export function genderOrSexTypeToText(genderOrSexType: GenderOrSexType) {
  switch (genderOrSexType) {
    case GenderOrSexType.GENDER:
      return 'Gender Identity';
    case GenderOrSexType.SEXATBIRTH:
      return 'Sex at Birth';
  }
}

export function highlightMatches(
  terms: Array<string>,
  name: string,
  fullText?: boolean,
  id?: string
) {
  id = id || '';
  const _class = (id !== '' ? 'match' + id + ' ' : '') + 'search-keyword';
  name = stripHtml(name);
  terms.forEach(term => {
    if (fullText) {
      const searchTerms = term.trim().split(new RegExp(',| '));
      searchTerms
        .filter(text => text.length > 2)
        .forEach((searchTerm, s) => {
          let re;
          if (s === (searchTerms.length - 1)) {
            re = new RegExp(searchTerm, 'gi');
          } else {
            re = new RegExp('\\b' + searchTerm + '\\b', 'gi');
          }
          name = name.replace(re, '<span class="' + _class + '">$&</span>');
        });
    } else {
      const re = new RegExp(term.replace(/(?=[\[\]()+])/g, '\\'), 'gi');
      name = name.replace(re, '<span class="' + _class + '">$&</span>');
    }
  });
  return name;
}

export function stripHtml(string: string) {
  return string.replace(/<(.|\n)*?>/g, '');
}

export function getChartObj(chartObj: any) {
  if (chartObj && typeof ResizeObserver === 'function') {
    // Unbind window.onresize handler so we don't do double redraws
    if (chartObj.unbindReflow) {
      chartObj.unbindReflow();
    }
    const chartRef = chartObj.container.parentElement;
    // check that chartRef is of type Element
    if (chartRef && chartRef.tagName) {
      // create observer to redraw charts on div resize
      const ro = new ResizeObserver(() => {
        if (chartObj.hasRendered) {
          chartObj.reflow();
        }
      });
      ro.observe(chartRef);
    }
  }
}

export function generateId(prefix?: string): string {
  prefix = prefix || 'id';
  let newId = `${prefix}_${genSuffix()}`;
  const ids = idsInUse.getValue();
  while (ids.has(newId)) {
    newId = `${prefix}_${genSuffix()}`;
  }
  ids.add(newId);
  idsInUse.next(ids);
  return newId;
}

function genSuffix(): string {
  return Math.random().toString(36).substr(2, 9);
}

export function parseCohortDefinition(json: string) {
  const data = JSON.parse(json);
  const sr = {dataFilters: data.datafilters || []};
  for (const role of ['includes', 'excludes']) {
    sr[role] = data[role].map(grp => {
      grp.items = grp.items.map(item => {
        item.searchParameters = item.searchParameters.map(sp => {
          const {parameterId, name, domain, type, subtype, group, attributes, conceptId,
            ancestorData, standard} = sp;
          return {
            parameterId,
            name,
            domainId: domain,
            type,
            subtype,
            group,
            conceptId,
            code: '',
            attributes,
            hasAttributes: attributes && attributes.length > 0,
            hasAncestorData: ancestorData,
            isStandard: standard
          };
        });
        if (!grp.temporal) {
          item.temporalGroup = 0;
        }
        item.status = 'active';
        return item;
      });
      grp.mention = grp.mention ? grp.mention : TemporalMention.ANYMENTION;
      grp.time = grp.time ? grp.time : TemporalTime.DURINGSAMEENCOUNTERAS;
      grp.timeValue = grp.timeValue ? grp.timeValue : '';
      grp.timeFrame = grp.timeFrame ? grp.timeFrame : '';
      grp.status = 'active';
      return grp;
    });
  }
  return sr;
}

export function mapRequest(sr: any) {
  const grpFilter = (role: string) => sr[role].reduce((acc, grp) => {
    if (grp.status === 'active' && hasActiveItems(grp)) {
      acc.push(mapGroup(grp));
    }
    return acc;
  }, []);
  return <SearchRequest>{
    includes: grpFilter('includes'),
    excludes: grpFilter('excludes'),
    dataFilters: sr.dataFilters
  };
}

export function mapGroup(group: any) {
  const {id, temporal, mention, name, time, timeValue} = group;
  const items = group.items.reduce((acc, it) => {
    if (it.status === 'active') {
      acc.push(mapGroupItem(it, temporal));
    }
    return acc;
  }, []);
  let searchGroup = <SearchGroup>{id, items, temporal};
  if (name) {
    searchGroup.name = name;
  }
  if (temporal) {
    searchGroup = {...searchGroup, mention, time, timeValue: parseInt(timeValue, 10)};
  }
  return searchGroup;
}

export function mapGroupItem(item: any, temporal: boolean) {
  const {id, type, modifiers, name, temporalGroup} = item;
  const searchParameters = item.searchParameters.map(mapParameter);
  const searchGroupItem = <SearchGroupItem>{id, type, searchParameters, modifiers};
  if (name) {
    searchGroupItem.name = name;
  }
  if (temporal) {
    searchGroupItem.temporalGroup = temporalGroup;
  }
  return searchGroupItem;
}

export function mapParameter(sp: any) {
  const {parameterId, name, domainId, type, subtype, group, attributes, conceptId,
    hasAncestorData, isStandard} = sp;
  const param = <SearchParameter>{
    parameterId,
    name: stripHtml(name),
    domain: domainId,
    type,
    group,
    attributes,
    ancestorData: hasAncestorData,
    standard: isStandard
  };
  if (conceptId !== null && conceptId !== undefined) {
    param.conceptId = conceptId;
  }
  if (domainId === Domain.SURVEY) {
    param.subtype = subtype;
  }
  return param;
}

export function hasActiveItems(group: any) {
  return group.items.some(it => it.status === 'active');
}

export function getTypeAndStandard(searchParameters: Array<any>, type: Domain) {
  switch (type) {
    case Domain.PERSON:
      const _type = searchParameters[0].type === CriteriaType.DECEASED
        ? CriteriaType.AGE : searchParameters[0].type;
      return {type: _type, standard: false};
    case Domain.PHYSICALMEASUREMENT:
      return {type: searchParameters[0].type, standard: false};
    case Domain.SURVEY:
      return {type: searchParameters[0].type, standard: false};
    case Domain.VISIT:
      return {type: searchParameters[0].type, standard: true};
    default:
      return {type: null, standard: null};
  }
}

export function sanitizeNumericalInput(input: string) {
  return (input && input.length > 10) ? input.slice(0, 10) : input;
}
