import { Operator, TreeSubType, TreeType } from 'generated';

export const PROGRAM_TYPES = [
  { name: 'Surveys',    type: TreeType.PPI },
  { name: 'Physical Measurements',    type: TreeType.PM, fullTree: true },
];

export const DOMAIN_TYPES = [
  {name: 'Demographics', type: TreeType.DEMO},
  {
    name: 'Conditions', type: TreeType.CONDITION, codes: [
      {name: 'ICD9 Codes', type: TreeType.ICD9, subtype: TreeSubType.CM},
      {name: 'ICD10 Codes', type: TreeType.ICD10, subtype: TreeSubType.ICD10CM}
    ]
  },
  {
    name: 'Procedures', type: TreeType.PROCEDURE, codes: [
      {name: 'ICD9 Codes', type: TreeType.ICD9, subtype: TreeSubType.PROC},
      {name: 'ICD10 Codes', type: TreeType.ICD10, subtype: TreeSubType.ICD10PCS},
      {name: 'CPT Codes', type: TreeType.CPT, subtype: null}
    ]
  },
  {name: 'Drugs', type: TreeType.DRUG},
  {name: 'Measurements', type: TreeType.MEAS},
  {name: 'Visits', type: TreeType.VISIT, fullTree: true}
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

export const PREDEFINED_ATTRIBUTES = {
  'Hypotensive': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['90'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['60'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Normal': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['120'],
      operator: Operator.LESSTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['80'],
      operator: Operator.LESSTHANOREQUALTO
    }
  ],
  'Pre-Hypertensive': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['121', '139'],
      operator: Operator.BETWEEN
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['81', '89'],
      operator: Operator.BETWEEN
    }
  ],
  'Hypertensive': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: ['140'],
      operator: Operator.GREATERTHANOREQUALTO
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: ['90'],
      operator: Operator.GREATERTHANOREQUALTO
    }
  ],
  'BP_DETAIL': [
    {
      conceptId: 903118,
      name: 'Systolic',
      operands: [null],
      operator: null,
      MIN: 0,
      MAX: 1000
    },
    {
      conceptId: 903115,
      name: 'Diastolic',
      operands: [null],
      operator: null,
      MIN: 0,
      MAX: 1000
    }
  ],
};
