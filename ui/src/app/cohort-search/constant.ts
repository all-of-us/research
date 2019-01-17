import { Operator, TreeSubType, TreeType } from 'generated';

export const PROGRAM_TYPES = [
  {
    name: 'Surveys', type: TreeType.PPI,
    tooltip: 'Questions and associated response options for participant completed surveys.'
  },
  {
    name: 'Physical Measurements',    type: TreeType.PM, fullTree: true,
    tooltip: 'Measurements taken at the time of participant enrollment, including blood pressure,' +
      ' heart rate, height, weight, body mass index (BMI), waist and hip circumference, ' +
      'pregnancy status and wheelchair use. '
  },
];

export const DOMAIN_TYPES = [
  {name: 'Demographics', type: TreeType.DEMO,
    tooltip: 'Age, gender, race, ethnicity and deceased status.'},
  {
    name: 'Conditions', type: TreeType.CONDITION,
    tooltip: 'Conditions listed by ICD9 or ICD10 orSNOMED standard codes.',
    codes: [
      {name: 'ICD9 Codes', type: TreeType.ICD9, subtype: TreeSubType.CM},
      {name: 'ICD10 Codes', type: TreeType.ICD10, subtype: TreeSubType.CM}
    ]
  },
  {
    name: 'Procedures', type: TreeType.PROCEDURE,
    tooltip: 'Procedures are listed by ICD9, ICD10, or CPT or SNOMED standard codes. ',
    codes: [
      {name: 'ICD9 Codes', type: TreeType.ICD9, subtype: TreeSubType.PROC},
      {name: 'ICD10 Codes', type: TreeType.ICD10, subtype: TreeSubType.PCS},
      {name: 'CPT Codes', type: TreeType.CPT, subtype: null}
    ]
  },
  {
    name: 'Drugs', type: TreeType.DRUG,
    tooltip: 'Drugs or medications are listed by ingredient and organized by therapeutic uses.'
  },
  {
    name: 'Measurements', type: TreeType.MEAS,
    tooltip: 'Measurements refer to laboratory tests and vital signs and are organized ' +
      'in the LOINC (Logical Observation Identifiers Names and Codes) code hierarchy. '
  },
  {
    name: 'Visits', type: TreeType.VISIT, fullTree: true,
    tooltip: 'Visits refer to the type of facility at which the participant ' +
      'received medical care (e.g. emergency room, outpatient, inpatient). '
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
