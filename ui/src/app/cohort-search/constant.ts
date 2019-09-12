import {AttrName, CriteriaType, DomainType, Operator} from 'generated/fetch';

export const LIST_PROGRAM_TYPES = [
  {
    name: 'Surveys',
    domain: DomainType.SURVEY,
    type: CriteriaType.PPI,
    standard: false
  },
  {
    name: 'Physical Measurements',
    domain: DomainType.PHYSICALMEASUREMENT,
    type: CriteriaType.PPI,
    standard: false,
    fullTree: true
  },
];

export const LIST_DOMAIN_TYPES = [
  {
    name: 'Demographics', domain: DomainType.PERSON, children: [
      {name: 'Current Age/Deceased', domain: DomainType.PERSON, type: CriteriaType.AGE},
      {name: 'Gender', domain: DomainType.PERSON, type: CriteriaType.GENDER},
      {name: 'Race', domain: DomainType.PERSON, type: CriteriaType.RACE},
      {name: 'Ethnicity', domain: DomainType.PERSON, type: CriteriaType.ETHNICITY},
    ]
  },
  {name: 'Conditions', domain: DomainType.CONDITION},
  {name: 'Procedures', domain: DomainType.PROCEDURE},
  {name: 'Drugs', domain: DomainType.DRUG},
  {name: 'Measurements', domain: DomainType.MEASUREMENT},
  {
    name: 'Visits',
    domain: DomainType.VISIT,
    type: CriteriaType.VISIT,
    standard: true,
    fullTree: true
  }
];

export const PM_UNITS = {
  'HEIGHT': 'cm',
  'WEIGHT': 'kg' ,
  'BMI': '',
  'WC': 'cm',
  'HC': 'cm',
  'BP': '',
  'HR-DETAIL': 'beats/min'
};

/* Systolic (conceptId: 903118) should always be the first element in the attributes array */
export const PREDEFINED_ATTRIBUTES = {
  'Hypotensive': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['90'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['60'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Normal': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['120'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['80'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Pre-Hypertensive': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['121', '139'],
      operator: Operator.BETWEEN
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['81', '89'],
      operator: Operator.BETWEEN
    }
  ],
  'Hypertensive': [
    {
      conceptId: 903118,
      name: AttrName.NUM,
      operands: ['140'],
      operator: Operator.GREATERTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: AttrName.NUM,
      operands: ['90'],
      operator: Operator.GREATERTHANOREQUALTO
    }
  ],
  'BP_DETAIL': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: [],
      operator: AttrName.ANY,
      MIN: 0,
      MAX: 1000
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: [],
      operator: AttrName.ANY,
      MIN: 0,
      MAX: 1000
    }
  ],
};
